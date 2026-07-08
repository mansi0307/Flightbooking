package com.example.flightbooking.service;

import com.example.flightbooking.model.Booking;
import com.example.flightbooking.model.Flight;
import com.example.flightbooking.repository.BookingRepository;
import com.example.flightbooking.repository.FlightRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookingService {
    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);
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
        logger.info("Creating booking for flight {} - passenger: {}, seats: {}", flightNumber, passengerName, seatCount);
        
        // Validate passenger fields
        if (passengerName == null || passengerName.trim().isEmpty()) {
            logger.warn("Booking creation failed for {}: passengerName is null or empty", flightNumber);
            throw new IllegalArgumentException("passengerName must not be null or empty");
        }
        if (passengerEmail == null || passengerEmail.trim().isEmpty()) {
            logger.warn("Booking creation failed for {}: passengerEmail is null or empty", flightNumber);
            throw new IllegalArgumentException("passengerEmail must not be null or empty");
        }
        if (seatCount <= 0) {
            logger.warn("Booking creation failed for {}: invalid seatCount {}", flightNumber, seatCount);
            throw new IllegalArgumentException("seatCount must be greater than 0");
        }

        // Get flight (throws if not found)
        Flight flight = flightRepository.findById(flightNumber)
                .orElseThrow(() -> {
                    logger.warn("Booking creation failed: flight {} not found", flightNumber);
                    return new java.util.NoSuchElementException("Flight with flightNumber " + flightNumber + " not found");
                });

        // Synchronize on the flight object for atomic seat allocation
        synchronized (flight) {
            logger.debug("Checking availability for flight {} - available: {}, requested: {}", 
                flightNumber, flight.getAvailableSeats(), seatCount);
            
            // Check if enough seats available
            if (flight.getAvailableSeats() < seatCount) {
                logger.warn("Booking creation failed for flight {}: not enough seats. Available: {}, Requested: {}", 
                    flightNumber, flight.getAvailableSeats(), seatCount);
                throw new IllegalStateException("Not enough available seats. Available: " + flight.getAvailableSeats() + ", Requested: " + seatCount);
            }

            // Atomically decrement available seats
            flight.setAvailableSeats(flight.getAvailableSeats() - seatCount);
            logger.debug("Flight {} seats decremented by {}, remaining: {}", flightNumber, seatCount, flight.getAvailableSeats());

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

        Booking savedBooking = bookingRepository.save(booking);
        logger.info("Booking created successfully: {} for flight {} - passenger: {}, seats: {}", 
            bookingId, flightNumber, passengerName, seatCount);
        return savedBooking;
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
        logger.info("Cancelling booking: {}", bookingId);
        
        // Get booking (throws if not found)
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    logger.warn("Booking cancellation failed: booking {} not found", bookingId);
                    return new java.util.NoSuchElementException("Booking with bookingId " + bookingId + " not found");
                });

        // Check if already cancelled
        if (booking.getStatus() == Booking.Status.CANCELLED) {
            logger.warn("Booking cancellation failed: booking {} is already cancelled", bookingId);
            throw new IllegalStateException("Booking is already cancelled");
        }

        // Get flight (throws if not found)
        Flight flight = flightRepository.findById(booking.getFlightNumber())
                .orElseThrow(() -> {
                    logger.warn("Booking cancellation failed: flight {} not found", booking.getFlightNumber());
                    return new java.util.NoSuchElementException("Flight with flightNumber " + booking.getFlightNumber() + " not found");
                });

        // Synchronize on the flight object for atomic seat restoration
        synchronized (flight) {
            logger.debug("Restoring {} seats to flight {}", booking.getSeatCount(), booking.getFlightNumber());
            
            // Restore available seats
            flight.setAvailableSeats(flight.getAvailableSeats() + booking.getSeatCount());
            logger.debug("Flight {} seats restored, new available: {}", booking.getFlightNumber(), flight.getAvailableSeats());

            // Update flight in repository
            flightRepository.save(flight);
        }

        // Update booking status to CANCELLED
        booking.setStatus(Booking.Status.CANCELLED);
        bookingRepository.save(booking);
        logger.info("Booking cancelled successfully: {} - {} seats restored to flight {}", 
            bookingId, booking.getSeatCount(), booking.getFlightNumber());
    }

    public Optional<Booking> getBookingById(UUID bookingId) {
        logger.debug("Retrieving booking: {}", bookingId);
        return bookingRepository.findById(bookingId);
    }

    public java.util.List<Booking> listAll() {
        logger.debug("Retrieving all bookings");
        return bookingRepository.findAll();
    }
}

