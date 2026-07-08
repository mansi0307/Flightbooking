# Flightbooking (Spring Boot 3)

A Spring Boot 3 (Java 17, Maven) REST API for flight booking service with in-memory storage.

## Architecture

- **Model**: `Flight`, `Booking`
- **Repository**: Thread-safe in-memory (`ConcurrentHashMap`) implementations
- **Service**: Business logic layer with validation
- **Controller**: REST endpoints
- **DTO**: Request/response objects

## API Endpoints

### Health Check
- `GET /actuator/health` — Returns application health status (200 OK)

### Flights

#### Create Flight
- **Endpoint**: `POST /flights`
- **Request Body**:
  ```json
  {
    "flightNumber": "FL001",
    "origin": "New York",
    "destination": "Los Angeles",
    "departureTime": "2026-07-15T10:00:00Z",
    "totalSeats": 150
  }
  ```
- **Response**:
  - **201 Created**: Flight created successfully. Response includes the created flight with `availableSeats` initialized to `totalSeats`.
  - **400 Bad Request**: Invalid input (e.g., empty flightNumber, invalid totalSeats, null fields) or duplicate flightNumber already exists.

#### Get Flight
- **Endpoint**: `GET /flights/{flightNumber}`
- **Response**:
  - **200 OK**: Returns the flight object.
  - **404 Not Found**: Flight does not exist.

### Bookings

#### Create Booking
- **Endpoint**: `POST /flights/{flightNumber}/bookings`
- **Request Body**:
  ```json
  {
    "passengerName": "John Doe",
    "passengerEmail": "john@example.com",
    "seatCount": 2
  }
  ```
- **Response**:
  - **201 Created**: Booking created successfully. Returns booking object with:
    - `bookingId` (UUID)
    - `flightNumber`
    - `passengerName`, `passengerEmail`
    - `seatCount`
    - `status` = "CONFIRMED"
    - `createdAt` (Instant)
    - **Flight's `availableSeats` is atomically decremented by `seatCount`**
  - **400 Bad Request**: Invalid input:
    - `passengerName` or `passengerEmail` is null/empty
    - `seatCount` <= 0
  - **404 Not Found**: Flight with `flightNumber` does not exist.
  - **409 Conflict**: Not enough available seats (`availableSeats < seatCount`). No overbooking allowed.

#### Cancel Booking
- **Endpoint**: `DELETE /bookings/{bookingId}`
- **Response**:
  - **204 No Content**: Booking cancelled successfully. Booking status changed to "CANCELLED", flight's `availableSeats` atomically restored by `seatCount`.
  - **404 Not Found**: Booking with `bookingId` does not exist.
  - **409 Conflict**: Booking is already cancelled.

## Build & Run

### Prerequisites
- Java 17+
- Maven (or install via `brew install maven` on macOS)

### Build
```bash
cd /Users/mansi/IdeaProjects/Flightbooking
mvn -DskipTests package
```

### Run
```bash
mvn spring-boot:run
```
or
```bash
java -jar target/flightbooking-0.0.1-SNAPSHOT.jar
```

The application starts on `http://localhost:8080`

### Run Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=BookingServiceTest

# Run with coverage (requires maven-surefire plugin)
mvn test jacoco:report
```

### Test Endpoints (using curl)

Create a flight:
```bash
curl -X POST http://localhost:8080/flights \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "FL001",
    "origin": "New York",
    "destination": "Los Angeles",
    "departureTime": "2026-07-15T10:00:00Z",
    "totalSeats": 150
  }'
```

Test invalid flight (validation error returns 400):
```bash
curl -X POST http://localhost:8080/flights \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "",
    "origin": "New York",
    "destination": "Los Angeles",
    "departureTime": "2026-07-15T10:00:00Z",
    "totalSeats": 150
  }'
```

Get a flight:
```bash
curl http://localhost:8080/flights/FL001
```

Test flight not found (returns 404):
```bash
curl http://localhost:8080/flights/NONEXISTENT
```

Create a booking:
```bash
curl -X POST http://localhost:8080/flights/FL001/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "passengerName": "John Doe",
    "passengerEmail": "john@example.com",
    "seatCount": 2
  }'
```

Test invalid booking (validation error returns 400):
```bash
curl -X POST http://localhost:8080/flights/FL001/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "passengerName": "",
    "passengerEmail": "invalid-email",
    "seatCount": 0
  }'
```

Cancel a booking (replace UUID with actual bookingId):
```bash
curl -X DELETE http://localhost:8080/bookings/550e8400-e29b-41d4-a716-446655440000
```

Check health:
```bash
curl http://localhost:8080/actuator/health
```

## Unit & Integration Tests

The project includes comprehensive tests covering:

### Test Classes

**BookingServiceTest** (Unit Tests with Mocks)
- Successful booking creation with valid inputs
- Booking on unknown flight (404 NoSuchElementException)
- Booking more seats than available (409 Conflict)
- Invalid passenger name/email validation
- Invalid seat count validation
- Booking cancellation success and seat restoration
- Cancellation of already-cancelled booking
- Cancellation of non-existent booking
- Concurrent booking requests (10 threads for 10 seats) - no overbooking
- Concurrent booking requests (10 threads for 5 seats) - excess requests rejected

**FlightServiceTest** (Unit Tests with Mocks)
- Successful flight creation
- Duplicate flight detection (409)
- Blank/null validation for all flight fields
- Flight retrieval by number
- Flight not found handling

**BookingIntegrationTest** (Integration Tests with Real Repositories)
- End-to-end flight creation and booking
- Concurrent bookings with real repositories - 20 threads for 10 seats
- Concurrent create and cancel operations - consistency check
- Seat restoration atomicity on cancellation
- Booking on non-existent flight
- Cancellation of non-existent booking
- Booking more seats than available

### Test Coverage Summary

| Scenario | Test Status | Thread Safety |
|----------|-------------|----------------|
| Successful booking | ✅ COVERED | N/A |
| Booking unknown flight (404) | ✅ COVERED | N/A |
| Overbooking attempt (409) | ✅ COVERED | ✅ ATOMIC |
| Concurrent bookings (10 threads, 10 seats) | ✅ COVERED | ✅ SERIALIZED |
| Concurrent bookings (20 threads, 10 seats) | ✅ COVERED | ✅ SERIALIZED |
| Concurrent create + cancel | ✅ COVERED | ✅ CONSISTENT |
| Seat restoration on cancel | ✅ COVERED | ✅ ATOMIC |
| Validation errors | ✅ COVERED | N/A |

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=BookingServiceTest
mvn test -Dtest=FlightServiceTest
mvn test -Dtest=BookingIntegrationTest

# Run with verbose output
mvn test -X

# Run specific test method
mvn test -Dtest=BookingServiceTest#testConcurrentBookingNoProblem
```

### Key Test Insights

1. **Atomicity Guarantee**: Concurrent booking requests for the same flight are serialized via synchronized blocks on the flight object, ensuring:
   - No two bookings can be processed simultaneously for the same flight
   - Seat availability is checked and decremented atomically
   - No race conditions between check and decrement

2. **Thread Safety Verification**: Integration tests use `ExecutorService` with `CountDownLatch` to:
   - Fire truly concurrent requests
   - Verify exact count of successful/failed bookings
   - Validate final seat count matches expected value
   - Confirm no overbooking occurred

3. **Error Handling**: Tests verify all exception cases:
   - Validation errors via Bean Validation
   - Not-found errors (404)
   - Conflict errors (409)
   - Service layer validation

## Implementation Notes

- **Error Handling**: 
  - Global exception handler (@RestControllerAdvice) converts all exceptions to consistent JSON error responses
  - Error response format: `timestamp`, `status`, `error`, `message`, `path`
  - Handles: validation errors, not-found errors, conflict errors, bad request errors
  - Example error response:
    ```json
    {
      "timestamp": "2026-07-08T15:30:45Z",
      "status": 400,
      "error": "Validation Failed",
      "message": "flightNumber: flightNumber must not be blank, origin: origin must not be blank",
      "path": "/flights"
    }
    ```

- **Validation**: 
  - Bean Validation annotations on request DTOs:
    - `CreateFlightRequest`: @NotBlank on flightNumber/origin/destination, @NotNull on departureTime, @Positive on totalSeats
    - `CreateBookingRequest`: @NotBlank on passengerName/passengerEmail, @Email on passengerEmail, @Positive on seatCount
  - Fields are validated at controller level with @Valid
  - Service layer also validates for additional safety
  - `flightNumber` must be unique (duplicates return 409)
  - `passengerName`, `passengerEmail` must not be null or empty (for bookings)
  - `seatCount` must be > 0 (for bookings)
  - `totalSeats` must be > 0 (for flights)
  
- **Thread Safety**: 
  - In-memory repositories use `ConcurrentHashMap` for thread-safe storage
  - **Booking creation uses synchronization on the flight object** to ensure atomicity:
    - Read available seats
    - Check if overbooking would occur
    - Atomically decrement available seats
    - Save booking
  - **Booking cancellation uses synchronization on the flight object** to ensure atomicity:
    - Check booking status
    - Restore available seats
    - Atomically update flight and booking
  - Two concurrent booking operations (create/cancel) for the same flight are serialized, preventing overbooking/inconsistency
  
- **Time Format**: `departureTime` and `createdAt` use `java.time.Instant` (ISO 8601 format in JSON)
- **Storage**: All data is stored in-memory and lost on application restart
- **No Authentication**: No authentication/authorization layer implemented

## Future Enhancements

- Add GET /bookings/{bookingId} endpoint to retrieve a booking
- Add GET /bookings endpoint to list all bookings
- Add filtering/search for flights (by origin, destination, date range)
- Add database persistence (JPA/Hibernate)
- Add authentication & authorization
- Add controller/integration tests
- Add OpenAPI/Swagger documentation

