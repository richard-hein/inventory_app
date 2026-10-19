# inventory-ai-api

Spring Boot REST API for inventory management — built to get solid in security, Postgres, and advanced Redis first, then Spring AI (RAG) with Ollama + pgvector.

## Stack

- **Boot 4.1.1**, Java 21, Maven
- **Postgres 16** (pgvector), **Redis 7**, **Ollama** — all via `inventory-ai-api/compose.yaml`
- Spring Security (JWT, OAuth2 resource server), Data JPA, Flyway, Validation, Cache, Actuator
- Spring AI: Ollama chat + embeddings, pgvector vector store
- springdoc-openapi 3.1.1 (Swagger UI), Lombok + MapStruct, Testcontainers for tests

## Prerequisites

- Docker + Docker Compose
- JDK 21

## Getting started

```powershell
cd inventory-ai-api
docker compose up -d
docker exec (docker ps -qf "ancestor=ollama/ollama") ollama pull llama3.1:8b
docker exec (docker ps -qf "ancestor=ollama/ollama") ollama pull nomic-embed-text
.\mvnw.cmd spring-boot:run
```

- Health: `GET http://localhost:8080/actuator/health` → `{"status":"UP"}`
- Swagger: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Run tests: `.\mvnw.cmd test`

## Configuration

Key values in `inventory-ai-api/src/main/resources/application.yml`:

| Property | Default | Notes |
|---|---|---|
| `app.jwt.secret` | dev fallback | Set `JWT_SECRET` env var in real runs — ≥32 bytes, never commit it |
| `app.jwt.access-ttl-minutes` | `15` | Access token TTL |
| `app.jwt.refresh-ttl-days` | `7` | Refresh token TTL |
| `spring.datasource.url` | `localhost:5533/inventory` | Keep in sync with `compose.yaml` |
| `ai.ollama.chat.model` | `llama3.1:8b` | Requires `ollama pull` |
| `ai.ollama.embedding.model` | `nomic-embed-text` | 768 dims — matches `vector(768)` columns |

Ports: API `8080`, Postgres `5533`, Redis `6379`, Ollama `11434`.

## API

### Auth — `/api/auth` (public)

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` | Create user (`ROLE_USER`), returns 201 / 409 on duplicate |
| POST | `/api/auth/login` | Returns access + refresh token |
| POST | `/api/auth/refresh` | Rotates refresh token, returns new pair |
| POST | `/api/auth/logout` | Revokes the refresh token, 204 |

### Products — `/products` (JWT required; create is admin-only)

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/products` | Authenticated | Paged list (default 20/page) |
| GET | `/products/{id}` | Authenticated | Single product |
| POST | `/products` | `ROLE_ADMIN` | Create product, returns 201 |

Seeded admin: `admin@local` / `Admin123!`

```powershell
# get a token
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"email":"admin@local","password":"Admin123!"}'
```

## Auth flow

1. Login issues a signed **HS256 access token** (15 min, claims: `sub`=email, `uid`, `roles`, `jti`) and a **random refresh token** (7 days).
2. Only the SHA-256 hash of the refresh token is stored in `refresh_tokens`.
3. `/refresh` rotates: old token is revoked, new pair issued. Reusing a revoked token revokes **all** the user's refresh tokens (reuse detection).
4. Stateless sessions, CSRF disabled, BCrypt passwords, method security via `@PreAuthorize`.

## Database

Flyway migrations in `inventory-ai-api/src/main/resources/db/migration/`:

- **V1** — `roles`, `users`, `user_roles`, `products` (with `vector(768)` embedding column), `inventory` (optimistic `version`), `orders` + `order_items` (idempotency key), `audit_logs`; seeds `ROLE_USER`/`ROLE_ADMIN`
- **V2** — `refresh_tokens`; seeds the admin user

Never edit an applied migration — add `V{n}__*.sql` instead. `ddl-auto: validate` checks JPA against the schema.

## Project layout

```
inventory-ai-api/
├── compose.yaml                  # pgvector + redis + ollama
├── flow.md                       # learning path / build order
├── M2-TASKS.md                   # JWT auth checklist (done)
└── src/main/java/com/example/inventory/
    ├── config/                   # SecurityConfig, filters
    ├── security/                 # JwtService
    ├── common/                   # GlobalExceptionHandler, ApiError
    ├── auth/                     # users, roles, refresh tokens, endpoints
    └── product/                  # entity, repo, service, controller, DTOs
```

Rule: one domain = one folder, `controller → service → repository`, controllers return DTOs (never entities), validate at the boundary (`@Valid`).

## Roadmap

| Milestone | Feature | Status |
|---|---|---|
| M1 | Products + Flyway baseline | Done |
| M2 | JWT auth: register/login/refresh/logout, rotation, admin routes | Done |
| M3 | Inventory + orders, transactional stock decrement, optimistic locking | Planned |
| M4 | Redis caching for product reads, per-type TTLs, write eviction | Planned |
| M5 | Redisson checkout lock, login rate limit, `Idempotency-Key` | Planned |
| M6 | Audit logs, integration tests (Postgres + Redis) | Planned |
| M7 | Spring AI RAG: product ingestion → pgvector similarity → `/ai/search`, `/ai/ask` | Planned |

See `inventory-ai-api/flow.md` for the full learning path and done criteria.

> This repo also contains `inventory_ai_frontend/` (Next.js) — not covered here yet.
