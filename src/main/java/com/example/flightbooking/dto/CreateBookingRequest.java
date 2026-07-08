package com.example.flightbooking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class CreateBookingRequest {
    @NotBlank(message = "passengerName must not be blank")
    private String passengerName;

    @NotBlank(message = "passengerEmail must not be blank")
    @Email(message = "passengerEmail must be a valid email address")
    private String passengerEmail;

    @Positive(message = "seatCount must be greater than 0")
    private int seatCount;

    public CreateBookingRequest() {
    }

    public CreateBookingRequest(String passengerName, String passengerEmail, int seatCount) {
        this.passengerName = passengerName;
        this.passengerEmail = passengerEmail;
        this.seatCount = seatCount;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public String getPassengerEmail() {
        return passengerEmail;
    }

    public void setPassengerEmail(String passengerEmail) {
        this.passengerEmail = passengerEmail;
    }

    public int getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(int seatCount) {
        this.seatCount = seatCount;
    }
}

