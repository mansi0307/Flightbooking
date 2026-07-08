package com.example.flightbooking.service;

import com.example.flightbooking.model.Booking;
import com.example.flightbooking.model.Flight;
import com.example.flightbooking.repository.InMemoryBookingRepository;
import com.example.flightbooking.repository.InMemoryFlightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Booking Integration Tests")
class BookingIntegrationTest {
    private InMemoryFlightRepository flightRepository;
    private InMemoryBookingRepository bookingRepository;
    private BookingService bookingService;
    private FlightService flightService;

    @BeforeEach
    void setUp() {
        flightRepository = new InMemoryFlightRepository();
        bookingRepository = new InMemoryBookingRepository();
        bookingService = new BookingService(flightRepository, bookingRepository);
        flightService = new FlightService(flightRepository);
    }

    @Test
    @DisplayName("End-to-end: Create flight and book successfully")
    void testEndToEndFlightAndBooking() {
        // Arrange & Act - Create flight
        Flight flight = flightService.createFlight(
                "FL001",
                "New York",
                "Los Angeles",
                Instant.parse("2026-07-15T10:00:00Z"),
                100
        );

        // Assert flight created
        assertNotNull(flight);
        assertEquals(100, flight.getAvailableSeats());

        // Act - Create booking
        Booking booking = bookingService.createBooking(
                "FL001",
                "John Doe",
                "john@example.com",
                5
        );

        // Assert booking created and seats reduced
        assertNotNull(booking);
        assertEquals(Booking.Status.CONFIRMED, booking.getStatus());
        assertEquals(95, flight.getAvailableSeats());

        // Verify booking is in repository
        assertTrue(bookingRepository.findById(booking.getBookingId()).isPresent());
    }

    @Test
    @DisplayName("Concurrent bookings don't cause overbooking - real repositories")
    void testConcurrentBookingNoOverbooking() throws InterruptedException {
        // Arrange
        Flight flight = flightService.createFlight(
                "FL002",
                "New York",
                "Los Angeles",
                Instant.parse("2026-07-15T10:00:00Z"),
                10
        );

        int numberOfThreads = 20;
        int seatsPerBooking = 1;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Act - Fire concurrent booking requests
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            executorService.submit(() -> {
                try {
                    Booking booking = bookingService.createBooking(
                            "FL002",
                            "Passenger " + threadIndex,
                            "passenger" + threadIndex + "@example.com",
                            seatsPerBooking
                    );
                    assertNotNull(booking);
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    // Expected when seats run out
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Assert
        assertTrue(completed, "Concurrent bookings did not complete in time");
        assertEquals(10, successCount.get(), "Exactly 10 bookings should succeed");
        assertEquals(10, failureCount.get(), "Exactly 10 bookings should fail");
        assertEquals(0, flight.getAvailableSeats(), "No seats should be available");
        assertEquals(10, bookingRepository.findAll().size(), "Repository should have 10 bookings");
    }

    @Test
    @DisplayName("Concurrent create and cancel operations maintain consistency")
    void testConcurrentCreateAndCancelConsistency() throws InterruptedException {
        // Arrange - Create flight with 20 seats
        Flight flight = flightService.createFlight(
                "FL003",
                "New York",
                "Los Angeles",
                Instant.parse("2026-07-15T10:00:00Z"),
                20
        );

        int numberOfCreators = 10;
        int numberOfCancellers = 5;
        int totalThreads = numberOfCreators + numberOfCancellers;
        AtomicInteger successfulCreations = new AtomicInteger(0);
        AtomicInteger successfulCancellations = new AtomicInteger(0);

        // First, create some confirmed bookings
        Booking[] bookingsToCancel = new Booking[numberOfCancellers];
        for (int i = 0; i < numberOfCancellers; i++) {
            bookingsToCancel[i] = bookingService.createBooking(
                    "FL003",
                    "Passenger " + i,
                    "passenger" + i + "@example.com",
                    1
            );
        }
        int expectedAvailableBeforeConcurrency = 20 - numberOfCancellers; // 15 seats left

        // Act - Concurrent creates and cancels
        ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch latch = new CountDownLatch(totalThreads);

        // Creators
        for (int i = 0; i < numberOfCreators; i++) {
            final int creatorIndex = i;
            executorService.submit(() -> {
                try {
                    Booking booking = bookingService.createBooking(
                            "FL003",
                            "NewPassenger " + creatorIndex,
                            "newpassenger" + creatorIndex + "@example.com",
                            1
                    );
                    assertNotNull(booking);
                    successfulCreations.incrementAndGet();
                } catch (IllegalStateException e) {
                    // May fail if no seats available
                } finally {
                    latch.countDown();
                }
            });
        }

        // Cancellers - Cancel the pre-created bookings
        for (int i = 0; i < numberOfCancellers; i++) {
            final int cancellerIndex = i;
            executorService.submit(() -> {
                try {
                    bookingService.cancelBooking(bookingsToCancel[cancellerIndex].getBookingId());
                    successfulCancellations.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all operations to complete
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Assert
        assertTrue(completed, "Concurrent operations did not complete in time");
        assertEquals(numberOfCancellers, successfulCancellations.get(), "All cancellations should succeed");

        // Verify consistency
        int totalBookings = bookingRepository.findAll().size();
        long confirmedCount = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == Booking.Status.CONFIRMED)
                .count();
        long cancelledCount = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == Booking.Status.CANCELLED)
                .count();

        assertEquals(numberOfCancellers, cancelledCount, "Cancelled bookings count mismatch");
        assertEquals(numberOfCancellers + successfulCreations.get(), confirmedCount, "Confirmed bookings count mismatch");

        // Verify seat count is correct
        int expectedSeats = 20 - (int) confirmedCount;
        assertEquals(expectedSeats, flight.getAvailableSeats(), "Available seats calculation error");
    }

    @Test
    @DisplayName("Booking cancellation restores seats atomically")
    void testCancellationRestoresSeatAtomically() {
        // Arrange
        Flight flight = flightService.createFlight(
                "FL004",
                "New York",
                "Los Angeles",
                Instant.parse("2026-07-15T10:00:00Z"),
                50
        );

        Booking booking = bookingService.createBooking(
                "FL004",
                "John Doe",
                "john@example.com",
                10
        );

        assertEquals(40, flight.getAvailableSeats());

        // Act
        bookingService.cancelBooking(booking.getBookingId());

        // Assert
        assertEquals(50, flight.getAvailableSeats());
        assertEquals(Booking.Status.CANCELLED, booking.getStatus());
    }

    @Test
    @DisplayName("Cannot book on flight that doesn't exist")
    void testBookingOnNonExistentFlight() {
        // Act & Assert
        assertThrows(java.util.NoSuchElementException.class, () ->
                bookingService.createBooking(
                        "NONEXISTENT",
                        "John Doe",
                        "john@example.com",
                        1
                )
        );
    }

    @Test
    @DisplayName("Cannot cancel booking that doesn't exist")
    void testCancelNonExistentBooking() {
        // Act & Assert
        assertThrows(java.util.NoSuchElementException.class, () ->
                bookingService.cancelBooking(java.util.UUID.randomUUID())
        );
    }

    @Test
    @DisplayName("Cannot book more seats than available on flight")
    void testBookingMoreSeatsThanAvailable() {
        // Arrange
        Flight flight = flightService.createFlight(
                "FL005",
                "New York",
                "Los Angeles",
                Instant.parse("2026-07-15T10:00:00Z"),
                10
        );

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                bookingService.createBooking(
                        "FL005",
                        "John Doe",
                        "john@example.com",
                        20
                )
        );
    }
}

