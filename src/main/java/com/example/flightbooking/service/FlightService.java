package com.example.flightbooking.service;

import com.example.flightbooking.model.Flight;
import com.example.flightbooking.repository.FlightRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FlightService {
    private static final Logger logger = LoggerFactory.getLogger(FlightService.class);
    private final FlightRepository repository;

    public FlightService(FlightRepository repository) {
        this.repository = repository;
    }

    public Flight createFlight(String flightNumber, String origin, String destination, java.time.Instant departureTime, int totalSeats) {
        logger.info("Creating flight: {} from {} to {}, {} total seats", flightNumber, origin, destination, totalSeats);
        
        // Validate inputs
        if (flightNumber == null || flightNumber.trim().isEmpty()) {
            logger.warn("Flight creation failed: flightNumber is null or empty");
            throw new IllegalArgumentException("flightNumber must not be null or empty");
        }
        if (origin == null || origin.trim().isEmpty()) {
            logger.warn("Flight creation failed: origin is null or empty");
            throw new IllegalArgumentException("origin must not be null or empty");
        }
        if (destination == null || destination.trim().isEmpty()) {
            logger.warn("Flight creation failed: destination is null or empty");
            throw new IllegalArgumentException("destination must not be null or empty");
        }
        if (departureTime == null) {
            logger.warn("Flight creation failed: departureTime is null");
            throw new IllegalArgumentException("departureTime must not be null");
        }
        if (totalSeats <= 0) {
            logger.warn("Flight creation failed: totalSeats must be > 0, got {}", totalSeats);
            throw new IllegalArgumentException("totalSeats must be greater than 0");
        }

        // Check if flight already exists
        if (repository.findById(flightNumber).isPresent()) {
            logger.warn("Flight creation failed: flight {} already exists", flightNumber);
            throw new IllegalStateException("Flight with flightNumber " + flightNumber + " already exists");
        }

        Flight flight = new Flight(flightNumber, origin, destination, departureTime, totalSeats, totalSeats);
        Flight savedFlight = repository.save(flight);
        logger.info("Flight created successfully: {}", flightNumber);
        logger.debug("Flight details - Origin: {}, Destination: {}, Departure: {}, Total Seats: {}", 
            origin, destination, departureTime, totalSeats);
        return savedFlight;
    }

    public Optional<Flight> getFlightByNumber(String flightNumber) {
        logger.debug("Retrieving flight: {}", flightNumber);
        Optional<Flight> flight = repository.findById(flightNumber);
        if (flight.isPresent()) {
            logger.debug("Flight found: {}, available seats: {}", flightNumber, flight.get().getAvailableSeats());
        } else {
            logger.debug("Flight not found: {}", flightNumber);
        }
        return flight;
    }

    public List<Flight> listAll() {
        logger.debug("Retrieving all flights");
        List<Flight> flights = repository.findAll();
        logger.debug("Found {} flights", flights.size());
        return flights;
    }

    public void delete(String flightNumber) {
        logger.info("Deleting flight: {}", flightNumber);
        repository.deleteById(flightNumber);
        logger.info("Flight deleted: {}", flightNumber);
    }
}

