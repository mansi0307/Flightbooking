package com.example.flightbooking.controller;

import com.example.flightbooking.dto.CreateBookingRequest;
import com.example.flightbooking.model.Booking;
import com.example.flightbooking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
            @Valid @RequestBody CreateBookingRequest request) {
        Booking booking = bookingService.createBooking(
                flightNumber,
                request.getPassengerName(),
                request.getPassengerEmail(),
                request.getSeatCount()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }
}

@RestController
@RequestMapping("/bookings")
class BookingCancelController {
    private final BookingService bookingService;

    public BookingCancelController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @DeleteMapping("/{bookingId}")
    public ResponseEntity<?> cancelBooking(@PathVariable UUID bookingId) {
        bookingService.cancelBooking(bookingId);
        return ResponseEntity.noContent().build();
    }
}



