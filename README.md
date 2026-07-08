# Flightbooking (Spring Boot 3) - scaffold

This is a minimal Spring Boot 3 (Java 17, Maven) scaffold for a flight booking service.

Features created:
- Standard package layout: model, repository (in-memory), service, controller
- In-memory repository implementation (no database)
- Actuator health endpoint enabled at GET /actuator/health

How to build and run:

1. Build:

```bash
mvn -DskipTests package
```

2. Run:

```bash
mvn spring-boot:run
```

Then open: http://localhost:8080/actuator/health

Notes:
- No API endpoints are implemented yet; controller package is scaffolded.
- No authentication. In-memory storage only.

