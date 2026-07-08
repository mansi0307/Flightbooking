package com.example.flightbooking.controller;

import com.example.flightbooking.dto.CreateBookingRequest;
import com.example.flightbooking.model.Booking;
import com.example.flightbooking.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/flights/{flightNumber}/bookings")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<?> createBooking(
            @PathVariable String flightNumber,
            @RequestBody CreateBookingRequest request) {
        try {
            Booking booking = bookingService.createBooking(
                    flightNumber,
                    request.getPassengerName(),
                    request.getPassengerEmail(),
                    request.getSeatCount()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(booking);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid request: " + e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Conflict: " + e.getMessage());
        }
    }
}

