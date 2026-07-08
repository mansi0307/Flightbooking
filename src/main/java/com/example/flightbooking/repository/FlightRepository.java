package com.example.flightbooking.repository;

import com.example.flightbooking.model.Flight;

import java.util.List;
import java.util.Optional;

public interface FlightRepository {
    Flight save(Flight flight);

    Optional<Flight> findById(String flightNumber);

    List<Flight> findAll();

    void deleteById(String flightNumber);
}

