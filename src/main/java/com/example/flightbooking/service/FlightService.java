package com.example.flightbooking.service;

import com.example.flightbooking.model.Flight;
import com.example.flightbooking.repository.FlightRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlightService {
    private final FlightRepository repository;

    public FlightService(FlightRepository repository) {
        this.repository = repository;
    }

    public Flight createOrUpdate(Flight flight) {
        return repository.save(flight);
    }

    public List<Flight> listAll() {
        return repository.findAll();
    }

    public void delete(String id) {
        repository.deleteById(id);
    }
}

