package com.example.flightbooking.model;

import java.time.LocalDateTime;

public class Booking {
    private String id;
    private String flightId;
    private String passengerName;
    private LocalDateTime bookingTime;

    public Booking() {
    }

    public Booking(String id, String flightId, String passengerName, LocalDateTime bookingTime) {
        this.id = id;
        this.flightId = flightId;
        this.passengerName = passengerName;
        this.bookingTime = bookingTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFlightId() {
        return flightId;
    }

    public void setFlightId(String flightId) {
        this.flightId = flightId;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public LocalDateTime getBookingTime() {
        return bookingTime;
    }

    public void setBookingTime(LocalDateTime bookingTime) {
        this.bookingTime = bookingTime;
    }
}

