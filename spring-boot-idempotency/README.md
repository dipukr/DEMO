# Idempotent Order API – Spring Boot + MS SQL Server

An example of atomic, database-backed idempotency for `POST /api/orders`.

## Requirements
- Java 17+
- Maven 3.9+
- A running MS SQL Server instance (e.g. SQL Server Developer or Express edition)

## Start locally

Create the database once (SQL Server must have TCP/IP enabled on port 1433 and SQL authentication on):

```bash
sqlcmd -S localhost -U sa -P '<password>' -C -Q "CREATE DATABASE idempotency_demo"
mvn spring-boot:run
```

Override the connection with `DB_URL`, `DB_USER` and `DB_PASSWORD`.

Schema is initialized by Spring Boot from `src/main/resources/schema.sql`.
In a real environment, use a migration tool such as Flyway.

## Make requests

Create an order:

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000' \
  -d '{"customerId":"C123","productId":"IPHONE-17","quantity":2}'
```

Send the identical request again: same response body and status; `Idempotency-Replayed: true`.
Send a different quantity with the same key: `409 Conflict`.
Send the same body with a new UUID key: a new order is created.

Check a created order using the returned `orderId`:

```bash
curl -i http://localhost:8080/api/orders/<order-id>
```

## Run integration tests

```bash
mvn test
```

The tests run against the SQL Server configured in `application.properties` (or `DB_URL` / `DB_USER` / `DB_PASSWORD`),
so that database must be reachable. Use `mvn package -DskipTests` to build without it.

## How concurrent requests are resolved

The `idempotency_records` primary key is `(operation, idempotency_key)`.
A conditional `INSERT ... WHERE NOT EXISTS (... WITH (UPDLOCK, HOLDLOCK))` is executed inside the **same database transaction**
that creates the order and records the HTTP response. A second concurrent transaction trying
the same key waits on the first transaction's unique-key conflict. After the first transaction
commits, the second transaction sees the saved response (SQL Server READ COMMITTED).
If the first transaction rolls back, the second transaction can take ownership and create the order.

No committed PENDING record can be left behind by a crash in the normal path.

## Important production boundaries

1. Protect the API with authentication and authorization. Do not trust `customerId` supplied by the
   client; derive customer identity from the authenticated principal and scope the idempotency
   key to that identity and operation.
2. Store and replay business error responses only if you intentionally commit that policy.
   Transient failures in this example roll back the transaction and can be retried.
3. This approach atomically protects **database work in the same SQL Server transaction**.
   It cannot make external payment gateway calls, Kafka publishes, emails, or other side effects
   part of that transaction. Use the transactional outbox, a provider-supported idempotency key,
   and idempotent consumers when external effects are involved.
4. Idempotency records are intentionally not deleted in this example. If you add retention
   cleanup, retries after expiration may create another order. Publish a retention contract.
5. Transaction conflicts may wait for an in-flight request. Apply appropriate database
   statement/lock timeouts and map timeouts to a retriable error such as 409 or 503 with
   `Retry-After`, based on your API policy.
6. Canonicalize requests consistently across deployments. In this demo Jackson serialization
   of the typed request record is the fingerprint source; coordinate DTO/schema changes when
   old keys remain valid. Consider versioned fingerprints in a long-lived API.
7. The sample returns 201 for both the first request and its successful replay, preserving
   the stored status. `Idempotency-Replayed` is an intentionally varying diagnostic header.
8. A new idempotency key can create a new order even if the payload is identical. Additional
   business constraints (e.g. unique checkout attempt) are separate.
9. Do not store sensitive secrets in cached response bodies. Apply appropriate encryption,
   retention, monitoring, and access controls.
10. The baseline keeps logic within one database transaction; avoid slow remote calls during
    it so lock contention stays low.

## Project layout

```
src/main/java/com/demo/idempotency/
├── IdempotencyApplication.java
├── api/
│   ├── OrderRequest.java
│   ├── OrderResponse.java
│   └── OrderController.java
└── service/
    ├── OrderResult.java
    └── OrderService.java
```

This example uses Spring Boot 3.5.12, Java 17, and MS SQL Server.
