package com.example.flightbooking.service;

import com.example.flightbooking.model.Booking;
import com.example.flightbooking.model.Flight;
import com.example.flightbooking.repository.BookingRepository;
import com.example.flightbooking.repository.FlightRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookingService {
    private final FlightRepository flightRepository;
    private final BookingRepository bookingRepository;

    public BookingService(FlightRepository flightRepository, BookingRepository bookingRepository) {
        this.flightRepository = flightRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * Creates a booking for a flight with atomicity guarantee.
     * Synchronizes on the flight object to prevent concurrent overbooking.
     *
     * @param flightNumber the flight number
     * @param passengerName the passenger name
     * @param passengerEmail the passenger email
     * @param seatCount the number of seats to book
     * @return the created booking
     * @throws IllegalArgumentException if validation fails
     * @throws java.util.NoSuchElementException if flight not found
     * @throws IllegalStateException if not enough available seats
     */
    public Booking createBooking(String flightNumber, String passengerName, String passengerEmail, int seatCount) {
        // Validate passenger fields
        if (passengerName == null || passengerName.trim().isEmpty()) {
            throw new IllegalArgumentException("passengerName must not be null or empty");
        }
        if (passengerEmail == null || passengerEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("passengerEmail must not be null or empty");
        }
        if (seatCount <= 0) {
            throw new IllegalArgumentException("seatCount must be greater than 0");
        }

        // Get flight (throws if not found)
        Flight flight = flightRepository.findById(flightNumber)
                .orElseThrow(() -> new java.util.NoSuchElementException("Flight with flightNumber " + flightNumber + " not found"));

        // Synchronize on the flight object for atomic seat allocation
        synchronized (flight) {
            // Check if enough seats available
            if (flight.getAvailableSeats() < seatCount) {
                throw new IllegalStateException("Not enough available seats. Available: " + flight.getAvailableSeats() + ", Requested: " + seatCount);
            }

            // Atomically decrement available seats
            flight.setAvailableSeats(flight.getAvailableSeats() - seatCount);

            // Update flight in repository
            flightRepository.save(flight);
        }

        // Create booking with CONFIRMED status
        UUID bookingId = UUID.randomUUID();
        Booking booking = new Booking(
                bookingId,
                flightNumber,
                passengerName,
                passengerEmail,
                seatCount,
                Booking.Status.CONFIRMED,
                Instant.now()
        );

        return bookingRepository.save(booking);
    }

    /**
     * Cancels a booking and restores seats to the flight atomically.
     * Synchronizes on the flight object to ensure atomicity of seat restoration.
     *
     * @param bookingId the booking ID
     * @throws java.util.NoSuchElementException if booking not found
     * @throws IllegalStateException if booking is already cancelled
     */
    public void cancelBooking(UUID bookingId) {
        // Get booking (throws if not found)
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Booking with bookingId " + bookingId + " not found"));

        // Check if already cancelled
        if (booking.getStatus() == Booking.Status.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }

        // Get flight (throws if not found)
        Flight flight = flightRepository.findById(booking.getFlightNumber())
                .orElseThrow(() -> new java.util.NoSuchElementException("Flight with flightNumber " + booking.getFlightNumber() + " not found"));

        // Synchronize on the flight object for atomic seat restoration
        synchronized (flight) {
            // Restore available seats
            flight.setAvailableSeats(flight.getAvailableSeats() + booking.getSeatCount());

            // Update flight in repository
            flightRepository.save(flight);
        }

        // Update booking status to CANCELLED
        booking.setStatus(Booking.Status.CANCELLED);
        bookingRepository.save(booking);
    }

    public Optional<Booking> getBookingById(UUID bookingId) {
        return bookingRepository.findById(bookingId);
    }

    public java.util.List<Booking> listAll() {
        return bookingRepository.findAll();
    }
}

