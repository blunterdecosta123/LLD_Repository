import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class ParkingLotMainThreadSafe {
    enum Status {
        AVIALABLE,
        OCCUPIED,
    }

    enum VehicleType {
        BIKE,
        CAR,
        TRUCK,
    }

    static abstract class Vehicle {
        private VehicleType type;
        private final int id;

        public Vehicle(VehicleType type, int id) {
            this.type = type;
            this.id = id;
        }

        public VehicleType getType() {
            return type;
        }

        public int getId() {
            return id;
        }
    }

    static class BikeVehicle extends Vehicle {
        public BikeVehicle(int id) {
            super(VehicleType.BIKE, id);
        }
    }

    static class CarVehicle extends Vehicle {
        public CarVehicle(int id) {
            super(VehicleType.CAR, id);
        }
    }

    static class TruckVehicle extends Vehicle {
        public TruckVehicle(int id) {
            super(VehicleType.TRUCK, id);
        }
    }

    static abstract class ParkingSpot {
        private final int id;
        private Status status;
        private final VehicleType spotType;
        private final ReentrantLock lock;

        public ParkingSpot(int id, Status status, VehicleType spotType) {
            this.id = id;
            this.status = status;
            this.spotType = spotType;
            this.lock = new ReentrantLock();
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public VehicleType getSpotType() {
            return spotType;
        }

        public int getId() {
            return id;
        }

        public ReentrantLock getLock() {
            return lock;
        }
    }

    static class CarParkingSpot extends ParkingSpot {
        public CarParkingSpot(int id) {
            super(id, Status.AVIALABLE, VehicleType.CAR);
        }
    }

    static class BikeParkingSpot extends ParkingSpot {
        public BikeParkingSpot(int id) {
            super(id, Status.AVIALABLE, VehicleType.BIKE);
        }
    }

    static class TruckParkingSpot extends ParkingSpot {
        public TruckParkingSpot(int id) {
            super(id, Status.AVIALABLE, VehicleType.TRUCK);
        }
    }

    static class ParkingFloor {
        private final int id;
        private final List<ParkingSpot> parkingSpots;

        public ParkingFloor(int id, List<ParkingSpot> parkingSpots) {
            this.id = id;
            this.parkingSpots = parkingSpots;
        }

        public int getId() {
            return id;
        }

        public List<ParkingSpot> getParkingSpots() {
            return parkingSpots;
        }

        public void addParkingSpot(ParkingSpot parkingSpot) {
            parkingSpots.add(parkingSpot);
        }

        public void removeParkingSpot(ParkingSpot parkingSpot) {
            parkingSpots.remove(parkingSpot);
        }
    }

    static class ParkingLot {
        private final int id;
        private final List<ParkingFloor> parkingFloors;

        public ParkingLot(int id, List<ParkingFloor> parkingFloors) {
            this.id = id;
            this.parkingFloors = parkingFloors;
        }

        public int getId() {
            return id;
        }

        public List<ParkingFloor> getParkingLots() {
            return parkingFloors;
        }

        public void addParkingLot(ParkingFloor parkingLot) {
            parkingFloors.add(parkingLot);
        }

        public void removeParkingLot(ParkingFloor parkingLot) {
            parkingFloors.remove(parkingLot);
        }

        void printStatus() {
            System.out.println("Parking Floor " + id);
            for (ParkingFloor parkingLot : parkingFloors) {
                System.out.println("Parking Lot " + parkingLot.getId());
                for (ParkingSpot parkingSpot : parkingLot.getParkingSpots()) {
                    System.out.println(
                            parkingSpot.getSpotType() + " " + parkingSpot.getId() + " " + parkingSpot.getStatus());
                }
            }

        }
    }

    static class Booking {
        private final String id;
        private final Vehicle vehicle;
        private final ParkingSpot parkingSpot;
        private LocalDateTime entryTime;
        private LocalDateTime exitTime;
        private double feeAmount;
        public Booking(Vehicle vehicle, ParkingSpot parkingSpot) {
            this.id = UUID.randomUUID().toString();
            this.vehicle = vehicle;
            this.parkingSpot = parkingSpot;
            this.entryTime = LocalDateTime.now();
        }

        public String getId() {
            return id;
        }

        public Vehicle getVehicle() {
            return vehicle;
        }

        public ParkingSpot getParkingSpot() {
            return parkingSpot;
        }

        public int getDurationinHours() {
            return (int) Math.ceil(Duration.between(entryTime, exitTime).toMinutes() / 60.0);
        }

        public void setExitTime(LocalDateTime exitTime) {
            this.exitTime = exitTime;
        }

        public LocalDateTime getExitTime() {
            return exitTime;
        }

        public void setEntryTime(LocalDateTime entryTime) {
            this.entryTime = entryTime;
        }

        public LocalDateTime getEntryTime() {
            return entryTime;
        }
        public double getFeeAmount() {
            return feeAmount;
        }
        public void setFeeAmount(double feeAmount) {
            this.feeAmount = feeAmount;
        }
    }

    interface ParkingFeeStrategy {
        double calculateFee(Booking booking);
    }

    static class FixedFeeParkingFeeStrategy implements ParkingFeeStrategy {
        @Override
        public double calculateFee(Booking booking) {
            Vehicle vehicle = booking.getVehicle();
            double base = 10.0;
            switch (vehicle.getType()) {
                case BIKE:
                    base = 5.0;
                    break;
                case CAR:
                    base = 10.0;
                    break;
                case TRUCK:
                    base = 15.0;
                    break;
            }
            return base * booking.getDurationinHours();
        }
    }

    static class PremiumParkingFeeStrategy implements ParkingFeeStrategy {
        @Override
        public double calculateFee(Booking booking) {
            Vehicle vehicle = booking.getVehicle();
            double base = 15.0;
            switch (vehicle.getType()) {
                case BIKE:
                    base = 15.0;
                    break;
                case CAR:
                    base = 25.0;
                    break;
                case TRUCK:
                    base = 35.0;
                    break;
            }
            return base * booking.getDurationinHours();
        }
    }

    static class BookingManager {
        private final ConcurrentHashMap<String, Booking> bookings;
        private final ConcurrentHashMap<Integer, Integer> spotToVehicle;
        private final ConcurrentHashMap<Integer, ParkingSpot> vehicleToSpot;
        private final List<Booking> bookingList;
        private final ParkingLot parkingLot;
        private final List<Vehicle> vehicles;
        private final ParkingFeeStrategy parkingFeeStrategy;

        public BookingManager(ParkingLot parkingLot, List<Vehicle> vehicles,
                ParkingFeeStrategy parkingFeeStrategy) {
            this.vehicles = vehicles;
            this.parkingLot = parkingLot;
            this.bookings = new ConcurrentHashMap<>();
            this.spotToVehicle = new ConcurrentHashMap<>();
            this.bookingList = new CopyOnWriteArrayList<>();
            this.vehicleToSpot = new ConcurrentHashMap<>();
            this.parkingFeeStrategy = parkingFeeStrategy;
        }

        public ConcurrentHashMap<String, Booking> getBookings() {
            return bookings;
        }

        public ConcurrentHashMap<Integer, Integer> getSpotToVehicle() {
            return spotToVehicle;
        }

        public List<Booking> getBookingList() {
            return bookingList;
        }

        public ParkingLot getParkingLot() {
            return parkingLot;
        }

        public List<Vehicle> getVehicles() {
            return vehicles;
        }

        public ParkingFeeStrategy getParkingFeeStrategy() {
            return parkingFeeStrategy;
        }

        public ConcurrentHashMap<Integer, ParkingSpot> getVehicleToSpot() {
            return vehicleToSpot;
        }

        List<ParkingSpot> getAvailableSpots(VehicleType vehicleType) {
            List<ParkingSpot> availableSpots = new ArrayList<>();
            for (ParkingFloor parkingFloor : parkingLot.getParkingLots()) {
                for (ParkingSpot parkingSpot : parkingFloor.getParkingSpots()) {
                    if (parkingSpot.getStatus() == Status.AVIALABLE && parkingSpot.getSpotType() == vehicleType) {
                        availableSpots.add(parkingSpot);
                    }
                }
            }
            Collections.sort(availableSpots, Comparator.comparingInt(ParkingSpot::getId));
            return availableSpots;
        }

        boolean ParkVehicle(Vehicle vehicle) {
            List<ParkingSpot> availableSpots = getAvailableSpots(vehicle.getType());
            if (availableSpots.isEmpty()) {
                return false;
            }
            for (ParkingSpot parkingSpot : availableSpots) {
                parkingSpot.getLock().lock();
                try {
                    if (parkingSpot.getStatus() == Status.AVIALABLE) {
                        parkingSpot.setStatus(Status.OCCUPIED);
                        spotToVehicle.putIfAbsent(parkingSpot.getId(), vehicle.getId());
                        vehicleToSpot.putIfAbsent(vehicle.getId(), parkingSpot);
                        bookingList.add(new Booking(vehicle, parkingSpot));
                        return true;
                    }
                } finally {
                    parkingSpot.getLock().unlock();
                }
            }
            return false;
        }

        boolean exitVehicle(Booking booking) {
            int vehicleId = booking.getVehicle().getId();
            if (vehicleToSpot.containsKey(vehicleId)) {
                ParkingSpot parkingSpot = vehicleToSpot.get(vehicleId);
                parkingSpot.getLock().lock();
                try {
                    if (parkingSpot.getStatus() == Status.OCCUPIED) {
                        parkingSpot.setStatus(Status.AVIALABLE);
                        vehicleToSpot.remove(vehicleId);
                        spotToVehicle.remove(parkingSpot.getId());
                        booking.setExitTime(LocalDateTime.now().plusMinutes(90));
                        System.out.println("Vehicle " + vehicleId + " exited. Fee: " + parkingFeeStrategy.calculateFee(booking));
                        booking.setFeeAmount(parkingFeeStrategy.calculateFee(booking));
                        bookingList.remove(booking);
                        return true;
                    }
                } finally {
                    parkingSpot.getLock().unlock();
                }
            }

            System.out.println("Vehicle " + vehicleId + " is not in the parking lot");
            return false;
        }

    }

    public static void main(String[] args) {
        List<ParkingSpot> parkingSpotsLot1 = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            parkingSpotsLot1.add(new CarParkingSpot(i));
        }
        ParkingFloor parkingLot1 = new ParkingFloor(1, parkingSpotsLot1);
        List<ParkingSpot> parkingSpotsLot2 = new ArrayList<>();
        for (int i = 10; i < 20; i++) {
            parkingSpotsLot2.add(new CarParkingSpot(i));
        }
        parkingSpotsLot2.add(new BikeParkingSpot(20));
        ParkingFloor parkingLot2 = new ParkingFloor(2, parkingSpotsLot2);
        List<ParkingFloor> parkingFloors = new ArrayList<>();
        parkingFloors.add(parkingLot1);
        parkingFloors.add(parkingLot2);
        ParkingLot parkingLot = new ParkingLot(1, parkingFloors);
        Vehicle car = new CarVehicle(1);
        Vehicle bike = new BikeVehicle(2);
        BookingManager bookingManager = new BookingManager(parkingLot, List.of(car, bike),
                new PremiumParkingFeeStrategy());
        bookingManager.ParkVehicle(car);
        bookingManager.ParkVehicle(bike);
        parkingLot.printStatus();
        System.out.println("Bookings: " + bookingManager.getBookingList().size());
        bookingManager.exitVehicle(bookingManager.getBookingList().get(0));
        bookingManager.exitVehicle(bookingManager.getBookingList().get(0));
    }
}
