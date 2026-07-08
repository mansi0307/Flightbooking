package com.example.flightbooking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public class CreateFlightRequest {
    @NotBlank(message = "flightNumber must not be blank")
    private String flightNumber;

    @NotBlank(message = "origin must not be blank")
    private String origin;

    @NotBlank(message = "destination must not be blank")
    private String destination;

    @NotNull(message = "departureTime must not be null")
    private Instant departureTime;

    @Positive(message = "totalSeats must be greater than 0")
    private int totalSeats;

    public CreateFlightRequest() {
    }

    public CreateFlightRequest(String flightNumber, String origin, String destination, Instant departureTime, int totalSeats) {
        this.flightNumber = flightNumber;
        this.origin = origin;
        this.destination = destination;
        this.departureTime = departureTime;
        this.totalSeats = totalSeats;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Instant getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(Instant departureTime) {
        this.departureTime = departureTime;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }
}

