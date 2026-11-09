# Saga Orchestration with Spring Boot Microservices

An e-commerce checkout built with the **Saga orchestration pattern**. Four independent Spring Boot
services each own their own database. A central orchestrator inside the Order Service tells each
participant what to do and, when a step fails, undoes the steps that already succeeded by calling
their compensating actions in reverse order.

| Service           | Port | Role                       | Command                            | Compensation                                         |
|-------------------|------|----------------------------|------------------------------------|------------------------------------------------------|
| Order Service     | 8080 | Orchestrator + order state | `POST /api/orders` starts the saga | –                                                    |
| Inventory Service | 8081 | Participant                | `POST /api/inventory/reservations` | `POST /api/inventory/reservations/{orderId}/release` |
| Payment Service   | 8082 | Participant                | `POST /api/payments`               | `POST /api/payments/{orderId}/refund`                |
| Shipping Service  | 8083 | Participant                | `POST /api/shipments`              | `POST /api/shipments/{orderId}/cancel`               |

Stack: Spring Boot 4.1, Java 17+, Spring Data JPA, H2 (one in-memory database per service), `RestClient`.

## How the saga works

```
 Client
   │  POST /api/orders
   ▼
┌─────────── ORDER SERVICE :8080 ───────────┐
│ 1. Save the order as PENDING              │
│ 2. OrderSagaOrchestrator runs the steps:  │
│      RESERVE_INVENTORY ───────────────────┼──►  Inventory Service :8081
│      PROCESS_PAYMENT ─────────────────────┼──►  Payment Service   :8082
│      CREATE_SHIPMENT ─────────────────────┼──►  Shipping Service  :8083
│ 3. All steps succeeded → COMPLETED        │
│    A step failed → compensate the         │
│    completed steps in REVERSE → CANCELLED │
└───────────────────────────────────────────┘
```

For example, when Shipping rejects the address after stock and payment have succeeded:

```
RESERVE_INVENTORY  EXECUTE     SUCCESS
PROCESS_PAYMENT    EXECUTE     SUCCESS
CREATE_SHIPMENT    EXECUTE     FAILED    Shipping is not available to 'Atlantis'
PROCESS_PAYMENT    COMPENSATE  SUCCESS   (payment refunded)
RESERVE_INVENTORY  COMPENSATE  SUCCESS   (stock released)
=> order CANCELLED
```

### Design points

- **One component owns the workflow.** The step order lives in `OrderSagaOrchestrator`. Participants
  only expose a command and a compensation and never call each other (that would be choreography).
- **Each step is a local transaction.** Every participant commits in its own database.
  `OrderService.placeOrder` is deliberately not `@Transactional`: no distributed transaction and no
  lock held across services.
- **`SagaStep` = command + compensation.** Adding a step (say, loyalty points) means writing one class
  and adding it to the orchestrator's list.
- **Compensations run in reverse (LIFO), are retried with backoff**, and if one still fails the order
  is marked `COMPENSATION_FAILED` for follow-up instead of silently ending up inconsistent.
- **Failures are classified** (`RemoteCall`). A 4xx or "connection refused" means the participant
  definitely did not apply the change. A timeout or 5xx means the outcome is unknown, so that step is
  compensated as well.
- **Participants are idempotent.** Commands and compensations are keyed by `orderId` (a unique column),
  so retries and duplicate compensations are harmless.
- **Saga progress is persisted** in `saga_step_log` and returned by `GET /api/orders/{id}`.
- **Data flows through the orchestrator.** Inventory returns the unit price, the orchestrator
  computes the total, and Payment charges that total.

## Run it

Requirements: Java 17+ and Maven 3.9+.

```bash
mvn clean package     # builds the four services and runs the orchestrator unit tests
```

Start each service in its own terminal:

```bash
java -jar inventory-service/target/inventory-service-1.0.0.jar
java -jar payment-service/target/payment-service-1.0.0.jar
java -jar shipping-service/target/shipping-service-1.0.0.jar
java -jar order-service/target/order-service-1.0.0.jar
```

(`mvn -pl inventory-service spring-boot:run` and so on works too.)

Seed data:

| Product                   | Stock | Unit price |
|---------------------------|-------|------------|
| P-100 Wireless Mouse      | 10    | 25.00      |
| P-200 Mechanical Keyboard | 5     | 80.00      |
| P-300 4K Monitor          | 0     | 300.00     |

| Customer wallet | Balance |
|-----------------|---------|
| C-1             | 1000.00 |
| C-2             | 50.00   |

The Shipping Service refuses deliveries to **Atlantis** (`shipping.unserviceable-cities`).

## Try every path

A small shell helper:

```bash
order() {  # usage: order <customerId> <productId> <quantity> <city>
  curl -s -X POST localhost:8080/api/orders -H 'Content-Type: application/json' \
    -d "{\"customerId\":\"$1\",\"productId\":\"$2\",\"quantity\":$3,\"shippingAddress\":{\"street\":\"1 Demo Street\",\"city\":\"$4\",\"zipCode\":\"12345\"}}"
  echo
}
```

Run these in order:

| # | Command                                                      | What happens                                              | Final status |
|---|--------------------------------------------------------------|-----------------------------------------------------------|--------------|
| 1 | `order C-1 P-100 2 Springfield`                              | All three steps succeed                                   | `COMPLETED`  |
| 2 | `order C-1 P-300 1 Springfield`                              | Out of stock at step 1, nothing to undo                   | `CANCELLED`  |
| 3 | `order C-2 P-200 1 Springfield`                              | Payment declined (50.00 < 80.00), stock released          | `CANCELLED`  |
| 4 | `order C-1 P-200 1 Atlantis`                                 | Shipping rejected, payment refunded, stock released       | `CANCELLED`  |
| 5 | Stop the Shipping Service, then `order C-1 P-100 1 Springfield` | "Shipping Service is unavailable", refund + release    | `CANCELLED`  |

Confirm the compensations really happened in the participants:

```bash
curl -s localhost:8081/api/inventory               # P-100 = 8, P-200 back to 5
curl -s localhost:8082/api/payments/accounts/C-1   # 950.00: only order 1 stayed charged
curl -s localhost:8082/api/payments/4              # status REFUNDED
curl -s localhost:8080/api/orders/4                # the full saga log
```

Response for scenario 4 (timestamps omitted):

```json
{
  "orderId": 4,
  "customerId": "C-1",
  "productId": "P-200",
  "quantity": 1,
  "totalAmount": 80.00,
  "status": "CANCELLED",
  "failureReason": "Shipping Service rejected the request: Shipping is not available to 'Atlantis'",
  "paymentId": 2,
  "sagaLog": [
    { "step": "RESERVE_INVENTORY", "action": "EXECUTE",    "outcome": "SUCCESS" },
    { "step": "PROCESS_PAYMENT",   "action": "EXECUTE",    "outcome": "SUCCESS" },
    { "step": "CREATE_SHIPMENT",   "action": "EXECUTE",    "outcome": "FAILED",
      "detail": "Shipping Service rejected the request: Shipping is not available to 'Atlantis'" },
    { "step": "PROCESS_PAYMENT",   "action": "COMPENSATE", "outcome": "SUCCESS" },
    { "step": "RESERVE_INVENTORY", "action": "COMPENSATE", "outcome": "SUCCESS" }
  ]
}
```

The orchestration logic is also covered by unit tests with the participants mocked
(`mvn -pl order-service test`), including the timeout and failed-compensation cases.

## Project layout

```
saga-orchestration-demo/
├── pom.xml                                parent POM (Spring Boot 4.1.1, Java 17)
├── order-service/                         :8080  orchestrator
│   └── src/main/java/com/example/order/
│       ├── saga/
│       │   ├── OrderSagaOrchestrator.java  runs the steps, compensates in reverse
│       │   ├── SagaStep.java               command + compensation contract
│       │   ├── StepFailedException.java
│       │   └── steps/                      ReserveInventoryStep, ProcessPaymentStep, CreateShipmentStep
│       ├── client/                         InventoryClient, PaymentClient, ShippingClient, RemoteCall
│       ├── config/                         ParticipantClientsConfig (RestClient + timeouts)
│       ├── model/                          Order, OrderStatus, SagaStepLog
│       └── controller/ service/ repository/ dto/
├── inventory-service/                     :8081  reserve / release stock
├── payment-service/                       :8082  charge / refund
└── shipping-service/                      :8083  create / cancel shipment
```

## Using Spring Boot 3.5 instead

The Java code also compiles against Spring Boot 3.5. Set the parent version to 3.5.x, replace
`spring-boot-starter-webmvc` with `spring-boot-starter-web` in every module, and remove the
`spring-boot-starter-restclient` dependency from `order-service/pom.xml`.

## Taking it to production

- **Recover in-flight sagas.** If the Order Service crashes mid-saga, the order stays `PENDING`. Add a
  scheduled job that finds stale `PENDING` orders and resumes or compensates them from `saga_step_log`.
- **Go asynchronous.** Replace the HTTP calls with command/reply messages (Kafka, RabbitMQ) and use the
  transactional outbox pattern so that a state change and its message are published atomically.
- **Handle late commands.** After a timeout, a compensation can reach a participant before the original
  command does. Record a "cancelled" marker per `orderId` so the late command gets rejected.
- **Consider a workflow engine** such as Temporal, Camunda or Eventuate Tram Sagas for durable state,
  timers and retries out of the box.
- **Trace it.** Propagate a correlation id through all services (Micrometer Tracing) so a single saga
  can be followed end to end.
