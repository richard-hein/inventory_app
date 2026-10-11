# M2 — JWT Auth: task checklist

Status: ☐ not done · ☑ done · ⏳ in progress
Stack decisions: Spring **resource server** validates JWT (no hand-rolled filter) · jjwt deps get **removed** · admin seeded in V2.
Login: `admin@local` / `Admin123!`

## 1. Schema ☑ (done today)
- [x] `V2__refresh_tokens_and_admin.sql` — refresh_tokens table + admin seed
- [ ] Boot the app once: Flyway logs `Migrating schema ... to version "2"` and `ddl-auto: validate` passes
  ```powershell
  .\mvnw.cmd spring-boot:run   # check logs, then Ctrl+C
  ```

## 2. Entities + repos ☐
- [ ] `auth/User.java` — table `users`, `password_hash` column, `enabled`, `createdAt` only (no `updatedAt` — V1 has none), `@ManyToMany` via `@JoinTable(name="user_roles")`
- [ ] `auth/Role.java` — table `roles`, `id` + `name`
- [ ] `auth/UserRepository.java` — `Optional<User> findByEmail(String email)`
- [ ] `auth/RoleRepository.java` — `Optional<Role> findByName(String name)`
- [ ] `.\mvnw.cmd compile` → green

## 3. JwtService + security beans ☐
- [ ] `security/JwtService.java`
  - issue access token: claims `sub`=email, `uid`, `roles` list, `jti`; TTL from `app.jwt.access-ttl-minutes`
  - mint refresh: 256-bit random → store **SHA-256 hex** (never raw)
  - verify refresh by hash
- [ ] `config/SecurityConfig.java` additions
  - `JwtDecoder` bean — HS256 from `app.jwt.secret` (must be ≥32 bytes — it is)
  - `JwtEncoder` bean — `NimbusJwtEncoder` + `ImmutableSecret` (same key)
  - `BCryptPasswordEncoder` bean
  - `AuthenticationManager` bean (via `AuthenticationConfiguration`)
  - `JwtAuthenticationConverter`: `roles` claim → `ROLE_*` authorities
  - `@EnableMethodSecurity`

## 4. Auth endpoints ☐
- [ ] `auth/UserDetailsServiceImpl.java` — `findByEmail` → spring-security `User` with role authorities
- [ ] DTO records: `RegisterRequest`, `LoginRequest`, `AuthResponse(accessToken, refreshToken, expiresInSeconds)`
- [ ] `auth/AuthService.java`
  - register: BCrypt-encode, default `ROLE_USER`, dup email → 409 (existing `DataIntegrityViolationException` handler covers it)
  - login: `AuthenticationManager.authenticate` → issue both tokens
  - refresh: look up by hash → revoked? **revoke all user's tokens** (reuse detection) → expired? 401 → else rotate (revoke old, insert new)
- [ ] `auth/AuthController.java` — `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh` (all public)

## 5. Lock down SecurityConfig ☐
- [ ] permitAll: `/auth/**`, actuator, swagger, **`GET`** `/products/**` only
- [ ] everything else `.authenticated()`
- [ ] `@PreAuthorize("hasRole('ADMIN')")` on `ProductController.create` (replaces the `/products/**` open placeholder)

## 6. Cleanup ☐
- [ ] `pom.xml` — delete all 3 `jjwt` dependency blocks
- [ ] `.\mvnw.cmd compile` → green

## 7. Verify (no tests yet — curl checklist) ☐
```powershell
.\mvnw.cmd spring-boot:run
```
| # | Call | Expected |
|---|------|----------|
| 1 | `POST /auth/register` new user | 201 |
| 2 | same register again | 409 |
| 3 | `POST /auth/login` admin@local | 200, access+refresh |
| 4 | `GET /products` no token | 200 (public) |
| 5 | `POST /products` no token | 401 |
| 6 | `POST /products` admin token | 201 |
| 7 | register user → login → `POST /products` | 403 |
| 8 | tampered/expired token | 401 |
| 9 | `POST /auth/refresh` with token | 200, rotated |
| 10 | reuse the **old** refresh token | 401 + all that user's refresh tokens revoked |

## Deferred (on purpose)
- Test file (you skipped it — flow.md golden rule says come back to it)
- `POST /auth/logout` (explicit refresh revoke)
- Login rate limit → **M5**
- After M2: M3 = Inventory + Orders + optimistic locking
