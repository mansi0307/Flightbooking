package com.example.flightbooking.repository;

import com.example.flightbooking.model.Booking;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryBookingRepository implements BookingRepository {
    private final ConcurrentMap<UUID, Booking> store = new ConcurrentHashMap<>();

    @Override
    public Booking save(Booking booking) {
        if (booking.getBookingId() == null) {
            throw new IllegalArgumentException("bookingId must not be null");
        }
        store.put(booking.getBookingId(), booking);
        return booking;
    }

    @Override
    public Optional<Booking> findById(UUID bookingId) {
        return Optional.ofNullable(store.get(bookingId));
    }

    @Override
    public List<Booking> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(UUID bookingId) {
        store.remove(bookingId);
    }
}

