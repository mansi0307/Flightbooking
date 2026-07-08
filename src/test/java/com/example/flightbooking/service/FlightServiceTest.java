package com.example.flightbooking.service;

import com.example.flightbooking.model.Flight;
import com.example.flightbooking.repository.FlightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FlightService Tests")
class FlightServiceTest {
    @Mock
    private FlightRepository flightRepository;

    private FlightService flightService;

    @BeforeEach
    void setUp() {
        flightService = new FlightService(flightRepository);
    }

    @Test
    @DisplayName("Should create flight successfully with valid inputs")
    void testCreateFlightSuccess() {
        // Arrange
        String flightNumber = "FL001";
        String origin = "New York";
        String destination = "Los Angeles";
        Instant departureTime = Instant.parse("2026-07-15T10:00:00Z");
        int totalSeats = 150;

        when(flightRepository.findById(flightNumber)).thenReturn(Optional.empty());
        when(flightRepository.save(any(Flight.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Flight flight = flightService.createFlight(flightNumber, origin, destination, departureTime, totalSeats);

        // Assert
        assertNotNull(flight);
        assertEquals(flightNumber, flight.getFlightNumber());
        assertEquals(origin, flight.getOrigin());
        assertEquals(destination, flight.getDestination());
        assertEquals(departureTime, flight.getDepartureTime());
        assertEquals(totalSeats, flight.getTotalSeats());
        assertEquals(totalSeats, flight.getAvailableSeats());
        verify(flightRepository).save(any(Flight.class));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when flight already exists")
    void testCreateFlightDuplicate() {
        // Arrange
        String flightNumber = "FL001";
        Flight existingFlight = new Flight(flightNumber, "NYC", "LAX", Instant.now(), 150, 150);
        when(flightRepository.findById(flightNumber)).thenReturn(Optional.of(existingFlight));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                flightService.createFlight(flightNumber, "NYC", "LAX", Instant.now(), 150)
        );
        assertTrue(exception.getMessage().contains("already exists"));
        verify(flightRepository, never()).save(any(Flight.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for blank flight number")
    void testCreateFlightBlankFlightNumber() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightService.createFlight("", "NYC", "LAX", Instant.now(), 150)
        );
        assertEquals("flightNumber must not be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for blank origin")
    void testCreateFlightBlankOrigin() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightService.createFlight("FL001", "", "LAX", Instant.now(), 150)
        );
        assertEquals("origin must not be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for blank destination")
    void testCreateFlightBlankDestination() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightService.createFlight("FL001", "NYC", "", Instant.now(), 150)
        );
        assertEquals("destination must not be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for null departure time")
    void testCreateFlightNullDepartureTime() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightService.createFlight("FL001", "NYC", "LAX", null, 150)
        );
        assertEquals("departureTime must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid total seats")
    void testCreateFlightInvalidTotalSeats() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                flightService.createFlight("FL001", "NYC", "LAX", Instant.now(), 0)
        );
        assertEquals("totalSeats must be greater than 0", exception.getMessage());
    }

    @Test
    @DisplayName("Should retrieve flight by number")
    void testGetFlightByNumber() {
        // Arrange
        String flightNumber = "FL001";
        Flight flight = new Flight(flightNumber, "NYC", "LAX", Instant.now(), 150, 150);
        when(flightRepository.findById(flightNumber)).thenReturn(Optional.of(flight));

        // Act
        Optional<Flight> result = flightService.getFlightByNumber(flightNumber);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(flight, result.get());
        verify(flightRepository).findById(flightNumber);
    }

    @Test
    @DisplayName("Should return empty when flight doesn't exist")
    void testGetFlightByNumberNotFound() {
        // Arrange
        String flightNumber = "NONEXISTENT";
        when(flightRepository.findById(flightNumber)).thenReturn(Optional.empty());

        // Act
        Optional<Flight> result = flightService.getFlightByNumber(flightNumber);

        // Assert
        assertTrue(result.isEmpty());
    }
}

