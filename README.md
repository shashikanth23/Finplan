# FinPlan: salary and financial planning platform

A user records income, expenses, loans and savings goals. The platform works out take-home pay, disposable income,
the EMI they can afford, the gross salary they need, and a 0-100 financial health score, with what-if scenarios.

Stack: Java 21, Spring Boot 3.3, PostgreSQL, Redis, Kafka, Docker, Kubernetes, React + TypeScript.

## Architecture

```
React (nginx)  ->  Gateway :8080  (JWT check, Redis rate limit, CORS)
                      |-- /api/auth/**      -> auth-service :8081      -> auth-db (Postgres)
                      |-- /api/planner/**   -> planner-service :8083   (stateless calculators)
                      '-- everything else   -> finance-service :8082   -> finance-db (Postgres)
                                                  |  Redis: dashboard cache
                                                  |  circuit breaker -> planner-service (falls back to in-process engine)
                                                  '  outbox table -> Kafka "finance-events" -> notification-service
engine (plain Java library): tax, salary, EMI, affordability, health score, scenarios. Used by planner + finance.
```

| Module | Responsibility |
|---|---|
| `engine` | All financial maths. No Spring, no I/O, fully unit-tested. |
| `auth-service` | Register, login, JWT (BCrypt passwords, 15-minute access token). |
| `planner-service` | HTTP facade over the engine: tax, required salary, EMI + schedule, affordable loan, scenarios, expense categoriser. |
| `finance-service` | Profile, income, expenses, loans, goals, dashboard. Redis cache, transactional outbox, Kafka producer. |
| `notification-service` | Kafka consumer: idempotent, retry then dead-letter topic. Delivery is a log line (plug in email/SMS). |
| `gateway` | Edge JWT validation, per-IP rate limiting (Redis), routing, CORS. |
| `frontend` | Dashboard, income, expenses, EMI manager, goals, calculators, printable report. |

## Run it

Needs JDK 21, Maven 3.9+, Node 20, Docker.

```bash
# 1. Everything in containers (UI at http://localhost:3000)
docker compose --profile apps up --build

# 2. Or: infrastructure in Docker, services from your IDE/terminal
docker compose up -d
mvn -pl auth-service spring-boot:run
mvn -pl planner-service spring-boot:run
mvn -pl finance-service spring-boot:run
mvn -pl notification-service spring-boot:run
mvn -pl gateway spring-boot:run
cd frontend && npm install && npm run dev      # http://localhost:5173
```

Tests: `mvn verify` (the repository test needs Docker and skips itself without it). Frontend: `cd frontend && npm run build`.

Kubernetes (kind/minikube): build the six images, load them into the cluster, then
`kubectl apply -f k8s/00-config.yaml -f k8s/10-infra.yaml -f k8s/20-apps.yaml -f k8s/30-ingress.yaml`.
Replace the placeholder secret first.

## What was verified, and what was not

Run for real while building:
- 36 targeted tests pass across the engine, shared JWT security, auth, notification and gateway modules.
- The frontend passes `tsc --noEmit` and `vite build`.

NOT verified here:
- This environment has Java 27, while the project targets Java 21; the finance-service's existing Lombok processor
  is incompatible with Java 27, so its tests and a full `mvn verify` must be run with Java 21 (as configured in CI).
- Testcontainers against PostgreSQL, Kafka/Redis wiring, and Kubernetes manifests have not been exercised against
  live infrastructure.

## API (via gateway, all except register/login need `Authorization: Bearer <token>`)

| Method and path | Purpose |
|---|---|
| `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` | Accounts and tokens |
| `GET/PUT /api/profile` | Tax regime and deductions |
| `GET/POST /api/incomes`, `PUT/DELETE /api/incomes/{id}` | Income (same shape for `/expenses`, `/debts`, `/goals`) |
| `GET /api/dashboard` | Full summary: salary, tax, EMI burden, disposable income, required salary, health score |
| `POST /api/planner/salary/required` | "What salary do I need?" |
| `POST /api/planner/affordability`, `/emi/affordable-loan` | "What EMI / loan can I afford?" |
| `POST /api/planner/scenario` | "What if my salary changes?" |
| `POST /api/planner/emi` | EMI, total interest, schedule |
| `POST /api/planner/tax`, `/salary/net`, `/summary`, `/categorize` | Building blocks |

## Design decisions (and how to explain them)

- **Worked example.** Expenses 35k + EMIs 20k + savings 15k = 70k take-home needed. With 10k of PF/deductions the
  engine returns a 80k gross: 9.6 lakh a year is inside the new-regime rebate, so tax is zero.
- **Reverse tax calculation.** Take-home is not monotonic in gross pay: just above the 12 lakh rebate limit marginal
  relief taxes each extra rupee at 100% plus 4% cess, and the old regime has a rebate cliff. A plain binary search
  would return wrong answers there, so `requiredGross` scans a grid and refines rupee by rupee. This is tested
  against brute force.
- **Money.** `BigDecimal` everywhere, `NUMERIC` columns with CHECK constraints, one rounding helper.
- **Engine as a library.** Pure functions are trivially testable and shared. planner-service exposes them over HTTP so
  they can scale and ship rule changes (tax slabs) independently.
- **Resilience.** finance-service calls planner-service with 1s/2s timeouts and a circuit breaker; on failure it
  computes the same result in-process and marks `source: local-fallback`. Degraded answers are not cached.
- **Redis cache-aside.** Dashboard cached 5 minutes, evicted after the writing transaction commits (not before, so a
  rollback never clears a valid cache). Redis errors degrade to a cache miss.
- **Transactional outbox.** The event row is written in the same DB transaction as the change, then relayed to Kafka;
  this avoids the dual-write problem. Delivery is at-least-once; the consumer dedupes by `eventId`.
- **Ordering.** Events are keyed by user id, so one user's events stay ordered within a partition.
- **Security.** BCrypt, stateless JWT, owner-scoped queries (another user's id returns 404, same as missing),
  identical login error for unknown email and wrong password. Local development uses HS256; production profile
  requires RS256, with the private signing key in auth-service and only the public verification key in other services.
- **Rate limiting.** The gateway trusts `X-Forwarded-For` only when the immediate network peer matches
  `TRUSTED_PROXY_CIDRS`; configure this with the ingress controller's actual pod CIDR. With no trusted proxy CIDR,
  the gateway safely rate-limits by its direct peer instead of trusting caller-supplied headers.
- **Database per service**, Flyway owns the schema, Hibernate only validates.

## Known limitations

- Tax: FY 2026-27 slabs for residents under 60. No surcharge, HRA/LTA, senior-citizen slabs, or capital gains.
  Slabs are plain arrays in `TaxCalculator` for easy updates. Verify against the current Finance Act before relying on it.
- Auth: no refresh tokens (users sign in again after 15 minutes), token kept in localStorage. Production RS256 keys
  are base64-encoded DER: PKCS#8 private key as `JWT_PRIVATE_KEY` in auth-service and X.509 public key as
  `JWT_PUBLIC_KEY` in gateway, planner-service and finance-service. The `prod` Spring profile rejects HS256.
- Outbox publisher claims rows with `FOR UPDATE SKIP LOCKED` and preserves per-user order across finance replicas.
- Notification event IDs are stored in the notification service's own Postgres database, so deduplication survives
  restarts and multiple replicas. When replacing the current log delivery with an external provider, pass the event ID
  as the provider's idempotency key to cover the narrow case where delivery succeeds but the DB commit fails.
- Bank/transaction integration is not built. It needs a regulated provider (in India, the Account Aggregator
  framework) and consent flows. The keyword categoriser (`/api/planner/categorize`) is the part that exists.
- No refresh of loan balances over time; remaining balance is entered by the user.
- Observability stops at Prometheus metrics endpoints; no tracing or dashboards yet.
