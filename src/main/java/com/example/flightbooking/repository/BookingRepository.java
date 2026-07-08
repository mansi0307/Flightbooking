package com.example.flightbooking.repository;

import com.example.flightbooking.model.Booking;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository {
    Booking save(Booking booking);

    Optional<Booking> findById(UUID bookingId);

    List<Booking> findAll();

    void deleteById(UUID bookingId);
}

