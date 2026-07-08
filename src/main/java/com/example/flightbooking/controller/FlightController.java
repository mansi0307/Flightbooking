package com.example.flightbooking.controller;

import com.example.flightbooking.dto.CreateFlightRequest;
import com.example.flightbooking.model.Flight;
import com.example.flightbooking.service.FlightService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/flights")
public class FlightController {
    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @PostMapping
    public ResponseEntity<?> createFlight(@RequestBody CreateFlightRequest request) {
        try {
            Flight flight = flightService.createFlight(
                    request.getFlightNumber(),
                    request.getOrigin(),
                    request.getDestination(),
                    request.getDepartureTime(),
                    request.getTotalSeats()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(flight);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid request: " + e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Conflict: " + e.getMessage());
        }
    }

    @GetMapping("/{flightNumber}")
    public ResponseEntity<?> getFlight(@PathVariable String flightNumber) {
        return flightService.getFlightByNumber(flightNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Flight with flightNumber " + flightNumber + " not found"));
    }
}


