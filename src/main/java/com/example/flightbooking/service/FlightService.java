package com.example.flightbooking.service;

import com.example.flightbooking.model.Flight;
import com.example.flightbooking.repository.FlightRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FlightService {
    private final FlightRepository repository;

    public FlightService(FlightRepository repository) {
        this.repository = repository;
    }

    public Flight createFlight(String flightNumber, String origin, String destination, java.time.Instant departureTime, int totalSeats) {
        // Validate inputs
        if (flightNumber == null || flightNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("flightNumber must not be null or empty");
        }
        if (origin == null || origin.trim().isEmpty()) {
            throw new IllegalArgumentException("origin must not be null or empty");
        }
        if (destination == null || destination.trim().isEmpty()) {
            throw new IllegalArgumentException("destination must not be null or empty");
        }
        if (departureTime == null) {
            throw new IllegalArgumentException("departureTime must not be null");
        }
        if (totalSeats <= 0) {
            throw new IllegalArgumentException("totalSeats must be greater than 0");
        }

        // Check if flight already exists
        if (repository.findById(flightNumber).isPresent()) {
            throw new IllegalStateException("Flight with flightNumber " + flightNumber + " already exists");
        }

        Flight flight = new Flight(flightNumber, origin, destination, departureTime, totalSeats, totalSeats);
        return repository.save(flight);
    }

    public Optional<Flight> getFlightByNumber(String flightNumber) {
        return repository.findById(flightNumber);
    }

    public List<Flight> listAll() {
        return repository.findAll();
    }

    public void delete(String flightNumber) {
        repository.deleteById(flightNumber);
    }
}

