package com.example.flightbooking.repository;

import com.example.flightbooking.model.Flight;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryFlightRepository implements FlightRepository {
    private final ConcurrentMap<String, Flight> store = new ConcurrentHashMap<>();

    @Override
    public Flight save(Flight flight) {
        store.put(flight.getId(), flight);
        return flight;
    }

    @Override
    public Optional<Flight> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Flight> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }
}

