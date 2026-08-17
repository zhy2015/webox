# Backend

This directory contains the Java 17 and Spring Boot application.

The backend owns authentication and authorization, menu and dish data, preferences, order validation, idempotency, price calculation, inventory transactions, cancellation, administrator operations, and durable persistence. Client input is never trusted for price, role, order state, or stock.

The intended code organization is a modular monolith grouped by business capability: `auth`, `catalog`, `menu`, `preference`, `order`, `inventory`, `admin`, `recommendation`, and `dashboard`. Each capability keeps its API, application, domain, and infrastructure concerns close together without allowing direct cross-module repository access.

Backend tests belong beside the application under `src/test`. Database schema changes are versioned with Flyway under `src/main/resources/db/migration`.

Run with JDK 17:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw test
./mvnw spring-boot:run
```
