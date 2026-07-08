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
- **Thread Safety**: In-memory repositories use `ConcurrentHashMap` for thread-safe operations
- **Time Format**: `departureTime` uses `java.time.Instant` (ISO 8601 format in JSON)
- **Storage**: All data is stored in-memory and lost on application restart
- **No Authentication**: No authentication/authorization layer implemented

## Future Enhancements

- Add booking endpoints (POST, GET, PATCH, DELETE)
- Add filtering/search for flights
- Add database persistence
- Add authentication & authorization
- Add input validation with Bean Validation
- Add unit & integration tests

