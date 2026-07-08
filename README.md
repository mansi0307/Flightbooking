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

Get a flight:
```bash
curl http://localhost:8080/flights/FL001
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

Cancel a booking (replace UUID with actual bookingId):
```bash
curl -X DELETE http://localhost:8080/bookings/550e8400-e29b-41d4-a716-446655440000
```

Check health:
```bash
curl http://localhost:8080/actuator/health
```

## Implementation Notes

- **Validation**: Fields are validated on service layer:
  - `flightNumber`, `origin`, `destination` must not be null or empty
  - `departureTime` must not be null
  - `totalSeats` must be > 0
  - `flightNumber` must be unique (duplicates return 400)
  - `passengerName`, `passengerEmail` must not be null or empty (for bookings)
  - `seatCount` must be > 0 (for bookings)
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

- Add booking endpoints (POST, GET, PATCH, DELETE)
- Add filtering/search for flights
- Add database persistence
- Add authentication & authorization
- Add input validation with Bean Validation
- Add unit & integration tests

