# inventory-ai-api — Project Flow (you write the code, this is the map)

Goal: get solid in **Security + Postgres + advanced Redis**, then **Spring AI (RAG)**.
Method: thin vertical slices. One small end-to-end feature per step, tested, then next.

## 0. Stack (already wired in `pom.xml`)

- Boot **4.1.1**, Java **21**, Maven. AI BOM **2.0.1** (supports Boot 4.1.x).
- WebMVC, Validation, Data JPA, Security + OAuth2 resource server, Data Redis, Cache,
  Flyway, Postgres driver, Actuator.
- Spring AI: `spring-ai-starter-model-ollama`, `spring-ai-starter-vector-store-pgvector`,
  `spring-ai-vector-store-advisor`.
- Docs: springdoc-openapi **3.1.1** (Boot 4 line). JWT: `jjwt` 0.12.6.
  Locks/rate-limit: `redisson-spring-boot-starter` 4.7.0 (bundles `-41`, correct for Boot 4.1).
- Tests: `spring-boot-testcontainers`, `testcontainers-{postgresql,ollama}`, redis via generic container.
- Infra: `compose.yaml` (pgvector:pg16 + redis:7 + ollama), `application.yml`,
  `db/migration/V1__baseline.sql` (tables + `vector` extension + seed roles).

## 1. First run (do this today)

```powershell
cd C:\Users\DELL\Desktop\inventory-ai-api
docker compose up -d
docker exec (docker ps -qf "ancestor=ollama/ollama") ollama pull llama3.1:8b
docker exec (docker ps -qf "ancestor=ollama/ollama") ollama pull nomic-embed-text
.\mvnw.cmd spring-boot:run
```

Check: `GET http://localhost:8080/actuator/health` → `{"status":"UP"}`.
Swagger: `http://localhost:8080/swagger-ui.html`. OpenAPI JSON: `/v3/api-docs`.

Best practice: never edit `compose.yaml` credentials without updating `application.yml`
(and vice versa). Keep `JWT_SECRET` in env, never in git.

## 2. Where YOUR code goes (package `com.example.inventory`)

```
config/     SecurityConfig, Redis/Cache config, OpenAPI config
security/   JwtService, filters, current-user helper
common/     GlobalExceptionHandler, ApiError, pagination helpers
product/    entity, repository, service, controller, DTOs (one folder per domain)
inventory/  ...
order/      ...
audit/      ...
ai/         ingestion job, retriever, /ai endpoints (much later)
```

Rule: one domain = one folder, `controller → service → repository`, controllers return
DTO records (never entities). Validate at the boundary (`@Valid`).

## 3. Learning path (build in this order, don't skip)

| Step | You build | Skill it teaches |
|------|-----------|------------------|
| M1 | `Product` entity + repo + `GET/POST /products` + Flyway `V2` if you change schema | JPA, constraints, validation |
| M2 | Register/login, JWT access+refresh, `SecurityConfig` (stateless), `@PreAuthorize` on admin routes | Security core |
| M3 | `Inventory` + `Orders` with `@Transactional`, stock decrement that can't oversell (optimistic `version` first, pessimistic lock when you prove the race with a test) | Postgres consistency |
| M4 | `@Cacheable` product reads, manual evict on write, per-type TTLs | Redis caching |
| M5 | Redisson lock around checkout, sliding-window rate limit on `/auth/*`, `Idempotency-Key` on `POST /orders` | Advanced Redis |
| M6 | `audit_logs` writes, `GlobalExceptionHandler`, Testcontainers integration test (Postgres+Redis) | Polish + proof |
| M7+ | Ollama chat call → product ingestion → pgvector similarity → `GET /ai/search` → `POST /ai/ask` (read-only, role-checked, rate-limited) | Spring AI RAG |

Golden rule: each step must run green (`.\mvnw.cmd test`) before starting the next.

## 4. Best practices (the short list that matters)

1. **DB is the truth.** Constraints (`UNIQUE`, `CHECK`, FK) in migrations, not just JPA
   annotations. Every schema change = new `V{n}__*.sql`, never edit applied migrations.
2. **Security at the boundary.** Stateless sessions, CSRF off only for pure JWT APIs,
   CORS allow-listed, passwords with BCrypt, refresh-token rotation + reuse detection,
   brute-force throttle on login (Redis counter), audit who/what/when/IP.
3. **Cache is a copy.** Name keys `domain:id:version`, short TTL for stock, long for catalog,
   always evict on write, design for stampede (single-flight) on hot keys.
4. **AI is read-only first.** RAG retrieves only rows the caller may see (role/tenant filter
   before similarity), tools never mutate, rate-limit `/ai/*`, log model + retrieved doc ids.
5. **Prove it.** One integration test per risky path (oversell race, expired token,
   stale cache) with Testcontainers. If it's not tested, it's not done.

## 5. Done criteria

- `docker compose up -d` + `.\mvnw.cmd test` green on a fresh clone.
- Oversell impossible under concurrent checkout test. Stale stock impossible after update.
- `/ai/ask` answers only from retrieved rows, refuses out-of-scope, rate-limited + audited.
