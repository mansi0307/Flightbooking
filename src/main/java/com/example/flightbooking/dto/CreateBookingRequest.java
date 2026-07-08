package com.example.flightbooking.dto;

public class CreateBookingRequest {
    private String passengerName;
    private String passengerEmail;
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

