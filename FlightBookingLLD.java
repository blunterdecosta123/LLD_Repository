import java.time.LocalDate;
import java.util.Arrays;
import java.util.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class FlightBookingLLD {
    enum Status {
        AVAILABLE,
        OCCUPIED
    }
    enum SeatType{
        WINDOW,
        AISLE,
        MIDDLE
    }
    static class Seat {
        private final int id;
        private Status status;
        private final SeatType seatType;
        private final ReentrantLock lock;

        public Seat(int id, Status status, SeatType seatType) {
            this.id = id;
            this.status = status;
            this.seatType = seatType;
            this.lock = new ReentrantLock();
        }

        public int getId() {
            return id;
        }

        public Status getStatus() {
            return status;
        }

        public SeatType getSeatType() {
            return seatType;
        }

        public void setStatus(Status status) {
            this.status = status;
        }
        public ReentrantLock getLock() {
            return lock;
        }
    }
    static class Flight {
        private final int id;
        private final String destination;
        private final String origin;
        private final LocalDate departureDate;
        private final List<Seat> seats;

        public Flight(int id, String origin, String destination, LocalDate departureDate, List<Seat> seats) {
            this.id = id;
            this.origin = origin;
            this.destination = destination;
            this.departureDate = departureDate;
            this.seats = seats;
        }
        public int getId() {
            return id;
        }

        public String getDestination() {
            return destination;
        }

        public String getOrigin() {
            return origin;
        }

        public LocalDate getDepartureDate() {
            return departureDate;
        }

        public List<Seat> getSeats() {
            return seats;
        }
        public void printStatus(){
            System.out.println("Flight id: "+id);
            System.out.println("Destination: "+destination);
            System.out.println("Origin: "+origin);
            System.out.println("Departure date: "+departureDate);
            System.out.println("Seats:");
            for(Seat seat: seats){
                System.out.println(seat.getId()+" "+seat.getStatus()+" "+seat.getSeatType());
            }
            System.out.println();
        }
    }
    static class Passenger {
        private final int id;
        private final String name;
        private final int age;

        public Passenger(int id, String name, int age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }
    static class Booking {
        private final String id;
        private final Passenger passenger;
        private final Flight flight;
        private final List<Seat> seats;
        private double paymentFee;
        public Booking(Passenger passenger, Flight flight, LocalDate bookingDate, List<Seat> seats) {
            this.id = UUID.randomUUID().toString();
            this.passenger = passenger;
            this.flight = flight;
            this.seats = seats;
        }

        public String getId() {
            return id;
        }

        public Passenger getPassenger() {
            return passenger;
        }
        public Flight getFlight() {
            return flight;
        }

        public List<Seat> getSeats() {
            return seats;
        }
        public double getPaymentFee() {
            return paymentFee;
        }
        public void setPaymentFee(double paymentFee) {
            this.paymentFee = paymentFee;
        }
    }
    static interface PaymentFeeStrategy {
        double calculatePaymentFee(Booking booking);
    }
    static class EconomyClassPaymentFeeStrategy implements PaymentFeeStrategy {
        @Override
        public double calculatePaymentFee(Booking booking) {
            return booking.getSeats().size() * 100;
        }
    }
    static class BusinessClassPaymentFeeStrategy implements PaymentFeeStrategy {
        @Override
        public double calculatePaymentFee(Booking booking) {
            return booking.getSeats().size() * 200;
        }
    }
    static class FlightBookingManager {
        ConcurrentHashMap<Integer, Integer> flightToPassenger;
        ConcurrentHashMap<Integer, List<Integer>> passengerToSeat;
        ConcurrentHashMap<String, Booking> bookings;
        private final List<Flight> flights;
        private final List<Passenger> passengers;
        private final List<Booking> bookingList;
        private final PaymentFeeStrategy paymentFeeStrategy;
        public FlightBookingManager(List<Flight> flights, List<Passenger> passengers, PaymentFeeStrategy paymentFeeStrategy) {
            flightToPassenger = new ConcurrentHashMap<>();
            passengerToSeat = new ConcurrentHashMap<>();
            bookings = new ConcurrentHashMap<>();
            bookingList = new CopyOnWriteArrayList<>();
            this.flights = flights;
            this.passengers = passengers;
            this.paymentFeeStrategy = paymentFeeStrategy;
        }
        public boolean findAvailableSeats(int flightId){
            Flight flight = null;
            for(Flight f: flights){
                if(f.getId() == flightId){
                    flight = f;
                    break;
                }
            }
            if(flight == null){
                System.out.println("No flight available");
                return false;
            }
            List<Seat> availableSeats = flight.getSeats().stream().filter(seat -> seat.getStatus() == Status.AVAILABLE).toList();
            if(availableSeats.isEmpty()){
                System.out.println("No seat available");
                return false;
            }
            System.out.println("Available seats:");
            for(Seat seat: availableSeats){
                System.out.println(seat.getId()+" "+seat.getSeatType()+" "+seat.getStatus());
            }
            return true;
        }
        public boolean bookSeats(int passengerId, List<Integer> seatIds,String source, String destination, LocalDate departureDate){ 
            List<Flight> availableFlights = flights.stream().filter(flight -> flight.getOrigin().equals(source) && flight.getDestination().equals(destination) && flight.getDepartureDate().equals(departureDate)).toList();
            if(availableFlights.isEmpty()){
                System.out.println("No flight available");
                return false;
            }
            Flight flight = availableFlights.get(0);
            Passenger passenger = null;
            for(Passenger p: passengers){
                if(p.getId() == passengerId){
                    passenger = p;
                    break;
                }
            }
            if (passenger == null) {
                System.out.println("No passenger available");
                return false;
            }
            List<Seat> seatsToBook = flight.getSeats().stream().filter(s -> seatIds.contains(s.getId())).sorted(Comparator.comparing(Seat::getId)).toList();
            for (Seat seat : seatsToBook) {
                seat.getLock().lock();
                try {
                    if (seat.getStatus() == Status.OCCUPIED) {
                        System.out.println("Seat already occupied");
                        return false;
                    }
                } finally {
                    seat.getLock().unlock();
                }
            }
            Booking booking = new Booking(passenger, flight, LocalDate.now(), seatsToBook);
            for (Seat seat : seatsToBook) {
                seat.getLock().lock();
                try {
                    seat.setStatus(Status.OCCUPIED);
                    
                    
                    
                } finally {
                    seat.getLock().unlock();
                }
            }
            bookings.putIfAbsent(booking.getId(), booking);
            bookingList.add(booking);
            flightToPassenger.putIfAbsent(flight.getId(), passenger.getId());
            passengerToSeat.putIfAbsent(passenger.getId(), seatIds);
            System.out.println("Booking successful");
            return true;
        }
        public boolean cancelBooking(String bookingId){
            Booking booking = bookings.get(bookingId);
            if(booking == null){
                System.out.println("No booking found");
                return false;
            }
            for(Seat seat: booking.getSeats()){
                seat.getLock().lock();
                try{
                    if(seat.getStatus() == Status.AVAILABLE){
                        System.out.println("Seat already available");
                        return false;
                    }
                }
                finally{
                    seat.getLock().unlock();
                }
            }
            List<Seat> seats = booking.getSeats();
            for(Seat seat: seats){
                seat.getLock().lock();
                try{
                    if(seat.getStatus() == Status.OCCUPIED){
                        seat.setStatus(Status.AVAILABLE);
                    }
                }finally{
                    seat.getLock().unlock();
                }
            }
            flightToPassenger.remove(booking.getFlight().getId());
            passengerToSeat.remove(booking.getPassenger().getId());
            bookings.remove(bookingId);
            bookingList.remove(booking);
            System.out.println("Booking cancelled");
            return true;
        }
        public double calculatePaymentFee(String bookingId){
            Booking booking = bookings.get(bookingId);
            if(booking == null){
                System.out.println("No booking found");
                return -1;
            }
            double paymentFee = paymentFeeStrategy.calculatePaymentFee(booking);
            booking.setPaymentFee(paymentFee);
            return paymentFee;
        }
        public void printAllBookings(){
            for(Booking booking: bookingList){
                System.out.println("Booking id: "+booking.getId());
                System.out.println("Passenger id: "+booking.getPassenger().getId());
                System.out.println("Flight id: "+booking.getFlight().getId());
                System.out.println("Seats:");
                for(Seat seat: booking.getSeats()){
                    System.out.println(seat.getId()+" "+seat.getSeatType() +" "+seat.getStatus());
                }
                System.out.println("Payment fee: "+booking.getPaymentFee());
                System.out.println();
            }
        }
    }
    public static void main(String[] args) {
        List<Flight> flights = new CopyOnWriteArrayList<>();
        flights.add(new Flight(1, "New York", "London", LocalDate.of(2022, 1, 1), Arrays.asList(new Seat(1, Status.AVAILABLE, SeatType.WINDOW), new Seat(2, Status.AVAILABLE, SeatType.MIDDLE), new Seat(3, Status.AVAILABLE, SeatType.AISLE))));
        flights.add(new Flight(2, "London", "New York", LocalDate.of(2022, 1, 2), Arrays.asList(new Seat(4, Status.AVAILABLE, SeatType.WINDOW), new Seat(5, Status.AVAILABLE, SeatType.MIDDLE), new Seat(6, Status.AVAILABLE, SeatType.AISLE))));
        flights.add(new Flight(3, "New York", "London", LocalDate.of(2022, 1, 3), Arrays.asList(new Seat(7, Status.AVAILABLE, SeatType.WINDOW), new Seat(8, Status.AVAILABLE, SeatType.MIDDLE), new Seat(9, Status.AVAILABLE, SeatType.AISLE))));
        flights.add(new Flight(4, "London", "New York", LocalDate.of(2022, 1, 4), Arrays.asList(new Seat(10, Status.AVAILABLE, SeatType.WINDOW), new Seat(11, Status.AVAILABLE, SeatType.MIDDLE), new Seat(12, Status.AVAILABLE, SeatType.AISLE))));
        List<Passenger> passengers = new CopyOnWriteArrayList<>();
        passengers.add(new Passenger(1, "Pranjay", 25));
        passengers.add(new Passenger(2, "Rahul", 30));
        FlightBookingManager flightBookingManager = new FlightBookingManager(flights, passengers, new EconomyClassPaymentFeeStrategy());
        System.out.println("Finding available seats");
        flightBookingManager.findAvailableSeats(1);
        System.out.println("Booking seats");
        flightBookingManager.bookSeats(1, Arrays.asList(1, 2), "New York", "London", LocalDate.of(2022, 1, 1));
        System.out.println("Calculating payment fee");
        double paymentFee = flightBookingManager.calculatePaymentFee(flightBookingManager.bookingList.get(0).getId());
        System.out.println("Payment fee: "+paymentFee);
        String bookingId = flightBookingManager.bookingList.get(0).getId();
        flightBookingManager.printAllBookings();
        System.out.println("Cancelling booking");
        flightBookingManager.cancelBooking(bookingId);
        flightBookingManager.printAllBookings();
    }
}
