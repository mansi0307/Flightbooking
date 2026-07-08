package com.example.flightbooking.service;

import com.example.flightbooking.model.Booking;
import com.example.flightbooking.model.Flight;
import com.example.flightbooking.repository.BookingRepository;
import com.example.flightbooking.repository.FlightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Tests")
class BookingServiceTest {
    @Mock
    private FlightRepository flightRepository;

    @Mock
    private BookingRepository bookingRepository;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(flightRepository, bookingRepository);
    }

    @Test
    @DisplayName("Should create booking successfully with valid inputs")
    void testCreateBookingSuccess() {
        // Arrange
        String flightNumber = "FL001";
        String passengerName = "John Doe";
        String passengerEmail = "john@example.com";
        int seatCount = 2;

        Flight flight = new Flight(flightNumber, "NYC", "LAX", Instant.now(), 100, 50);
        when(flightRepository.findById(flightNumber)).thenReturn(Optional.of(flight));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Booking booking = bookingService.createBooking(flightNumber, passengerName, passengerEmail, seatCount);

        // Assert
        assertNotNull(booking);
        assertEquals(flightNumber, booking.getFlightNumber());
        assertEquals(passengerName, booking.getPassengerName());
        assertEquals(passengerEmail, booking.getPassengerEmail());
        assertEquals(seatCount, booking.getSeatCount());
        assertEquals(Booking.Status.CONFIRMED, booking.getStatus());
        assertNotNull(booking.getCreatedAt());
        assertNotNull(booking.getBookingId());

        // Verify flight seats were decremented
        assertEquals(48, flight.getAvailableSeats());
        verify(flightRepository).save(flight);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when flight doesn't exist")
    void testCreateBookingUnknownFlight() {
        // Arrange
        String flightNumber = "NONEXISTENT";
        String passengerName = "John Doe";
        String passengerEmail = "john@example.com";
        int seatCount = 2;

        when(flightRepository.findById(flightNumber)).thenReturn(Optional.empty());

        // Act & Assert
        NoSuchElementException exception = assertThrows(NoSuchElementException.class, () ->
                bookingService.createBooking(flightNumber, passengerName, passengerEmail, seatCount)
        );
        assertEquals("Flight with flightNumber NONEXISTENT not found", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when not enough seats available")
    void testCreateBookingNotEnoughSeats() {
        // Arrange
        String flightNumber = "FL001";
        String passengerName = "John Doe";
        String passengerEmail = "john@example.com";
        int seatCount = 10;

        Flight flight = new Flight(flightNumber, "NYC", "LAX", Instant.now(), 100, 5);
        when(flightRepository.findById(flightNumber)).thenReturn(Optional.of(flight));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                bookingService.createBooking(flightNumber, passengerName, passengerEmail, seatCount)
        );
        assertTrue(exception.getMessage().contains("Not enough available seats"));
        assertEquals(5, flight.getAvailableSeats()); // Seats should not change
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for blank passenger name")
    void testCreateBookingBlankPassengerName() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                bookingService.createBooking("FL001", "", "john@example.com", 2)
        );
        assertEquals("passengerName must not be null or empty", exception.getMessage());
        verify(flightRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for blank passenger email")
    void testCreateBookingBlankPassengerEmail() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                bookingService.createBooking("FL001", "John Doe", "", 2)
        );
        assertEquals("passengerEmail must not be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid seat count")
    void testCreateBookingInvalidSeatCount() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                bookingService.createBooking("FL001", "John Doe", "john@example.com", 0)
        );
        assertEquals("seatCount must be greater than 0", exception.getMessage());
    }

    @Test
    @DisplayName("Should cancel booking successfully and restore seats atomically")
    void testCancelBookingSuccess() {
        // Arrange
        UUID bookingId = UUID.randomUUID();
        String flightNumber = "FL001";
        Booking booking = new Booking(bookingId, flightNumber, "John Doe", "john@example.com", 2, Booking.Status.CONFIRMED, Instant.now());
        Flight flight = new Flight(flightNumber, "NYC", "LAX", Instant.now(), 100, 48);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(flightRepository.findById(flightNumber)).thenReturn(Optional.of(flight));
        when(flightRepository.save(any(Flight.class))).thenReturn(flight);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        // Act
        bookingService.cancelBooking(bookingId);

        // Assert
        assertEquals(Booking.Status.CANCELLED, booking.getStatus());
        assertEquals(50, flight.getAvailableSeats()); // Seats restored
        verify(flightRepository).save(flight);
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("Should throw exception when cancelling already cancelled booking")
    void testCancelBookingAlreadyCancelled() {
        // Arrange
        UUID bookingId = UUID.randomUUID();
        Booking booking = new Booking(bookingId, "FL001", "John Doe", "john@example.com", 2, Booking.Status.CANCELLED, Instant.now());

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                bookingService.cancelBooking(bookingId)
        );
        assertEquals("Booking is already cancelled", exception.getMessage());
        verify(flightRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when cancelling non-existent booking")
    void testCancelBookingNotFound() {
        // Arrange
        UUID bookingId = UUID.randomUUID();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        // Act & Assert
        NoSuchElementException exception = assertThrows(NoSuchElementException.class, () ->
                bookingService.cancelBooking(bookingId)
        );
        assertTrue(exception.getMessage().contains("Booking with bookingId"));
    }

    @Test
    @DisplayName("Should prevent overbooking under concurrent requests")
    void testConcurrentBookingNoProblem() throws InterruptedException {
        // Arrange
        String flightNumber = "FL001";
        int totalSeats = 10;
        int numberOfThreads = 10;
        int seatsPerBooking = 1;

        // Use real Flight and real synchronization (not mocked)
        Flight flight = new Flight(flightNumber, "NYC", "LAX", Instant.now(), totalSeats, totalSeats);
        AtomicInteger successfulBookings = new AtomicInteger(0);
        AtomicInteger failedBookings = new AtomicInteger(0);

        when(flightRepository.findById(flightNumber)).thenReturn(Optional.of(flight));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            successfulBookings.incrementAndGet();
            return invocation.getArgument(0);
        });
        when(flightRepository.save(any(Flight.class))).thenReturn(flight);

        // Act - Fire concurrent booking requests
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            final int bookingIndex = i;
            executorService.submit(() -> {
                try {
                    Booking booking = bookingService.createBooking(
                            flightNumber,
                            "Passenger " + bookingIndex,
                            "passenger" + bookingIndex + "@example.com",
                            seatsPerBooking
                    );
                    assertNotNull(booking);
                } catch (IllegalStateException e) {
                    // Expected when seats run out
                    failedBookings.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        executorService.shutdown();

        // Assert
        assertTrue(completed, "Concurrent bookings did not complete in time");
        int totalAttempts = successfulBookings.get() + failedBookings.get();
        assertEquals(numberOfThreads, totalAttempts, "Not all booking attempts were processed");
        assertEquals(totalSeats, successfulBookings.get(), "Number of successful bookings should equal total seats");
        assertEquals(0, failedBookings.get(), "No bookings should fail with exact seat count");
        assertEquals(0, flight.getAvailableSeats(), "All seats should be booked");
    }

    @Test
    @DisplayName("Should prevent overbooking - second request blocked when seats exhausted")
    void testConcurrentBookingOverbookingPrevention() throws InterruptedException {
        // Arrange
        String flightNumber = "FL001";
        int totalSeats = 5;
        int numberOfThreads = 10;
        int seatsPerBooking = 1;

        Flight flight = new Flight(flightNumber, "NYC", "LAX", Instant.now(), totalSeats, totalSeats);
        AtomicInteger successfulBookings = new AtomicInteger(0);
        AtomicInteger failedBookings = new AtomicInteger(0);

        when(flightRepository.findById(flightNumber)).thenReturn(Optional.of(flight));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            successfulBookings.incrementAndGet();
            return invocation.getArgument(0);
        });
        when(flightRepository.save(any(Flight.class))).thenReturn(flight);

        // Act - Fire 10 concurrent requests for 5 available seats
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            final int bookingIndex = i;
            executorService.submit(() -> {
                try {
                    bookingService.createBooking(
                            flightNumber,
                            "Passenger " + bookingIndex,
                            "passenger" + bookingIndex + "@example.com",
                            seatsPerBooking
                    );
                } catch (IllegalStateException e) {
                    failedBookings.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        executorService.shutdown();

        // Assert - Exactly 5 should succeed, 5 should fail
        assertTrue(completed, "Concurrent bookings did not complete in time");
        assertEquals(totalSeats, successfulBookings.get(), "Exactly 5 bookings should succeed");
        assertEquals(numberOfThreads - totalSeats, failedBookings.get(), "Exactly 5 bookings should fail");
        assertEquals(0, flight.getAvailableSeats(), "All available seats should be booked");
    }
}

