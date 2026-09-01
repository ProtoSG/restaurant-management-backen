# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Start DB (dev)
docker compose -f docker-compose.dev.yml up -d

# Run app (dev profile, port 8080)
./mvnw spring-boot:run

# Build (skip tests)
./mvnw clean package -DskipTests

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=OrderEntityTest

# API docs (when running)
http://localhost:8080/api/docs
```

Dev DB: `localhost:5433`, user/pass `postgres/postgres`, db `ddbb_restaurant_management_dev`.

All endpoints are prefixed `/api` (`server.servlet.context-path` in `application.yml`).

Required env vars (no defaults — app fails to start if missing): `JWT_SECRET_KEY` (base64 secret), `ADMIN_USERNAME` and `ADMIN_PASSWORD` (seed admin user), `ANTHROPIC_API_KEY` (voice-order extraction), `OPENAI_API_KEY` (voice-order transcription — Whisper/gpt-4o-mini-transcribe).

Optional env vars (empty default — the app must always start without them): `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET` (product photo storage, see `docker-compose.dev.yml`'s `minio-dev` service for local dev — ports 9010/9001, not MinIO's usual 9000/9001, since another project's MinIO already runs on this dev machine).

## Architecture

Vertical slice layout — each domain is a self-contained package under `com.restaurant_management.restaurant_management_backend`:

| Package | Responsibility |
|---------|---------------|
| `auth` | JWT auth, roles, user management |
| `orders` | Order lifecycle, items, payments |
| `tables` | Table state (FREE/OCCUPIED) |
| `menu/categories` | Menu categories |
| `menu/products` | Menu products |
| `transactions` | Payment records per order |
| `analytics` | Reporting/metrics queries |
| `websocket` | STOMP events on `/topic/orders` |
| `shared` | Security, exceptions, enums, `SystemConfig` |
| `voiceorder` | Voice-order extraction, validation, and confirm — ADMIN/CASHIER/WAITER |

Each slice follows: `Controller → Service (interface + impl) → Repository → entity + DTOs + mapper`.

## Key domain rules

**Order status flow:** `CREATED → IN_PROGRESS → READY → PAID` or `→ PARTIALLY_PAID → PAID`. `CANCELLED` from any non-paid state. (`PENDING` status was removed — not a valid state.)

- Order auto-transitions `CREATED → IN_PROGRESS` on first item added.
- Only `CREATED`, `READY`, `PARTIALLY_PAID` can receive payments.
- `DINE_IN` orders require `tableId`; `TAKEAWAY` orders don't use tables.
- Paying an order with `PARTIALLY_PAID` status via `/pay/{method}` throws — must use `/pay-partial`.

**Takeaway surcharge:** Applied per `OrderItem` when `isTakeaway=true`. Drinks category (`"bebidas"`) is exempt. Surcharge amount is read from `system_config` table (`takeaway_surcharge` key), defaulting to 1 if missing.

**Transactions:** Each payment call creates a `Transaction` record linked to the order. `Order.getPaidAmount()` sums only `COMPLETED` transactions. `Order.getRemainingAmount()` = `total - paidAmount`.

**Table state:** `Table.free()` / `Table.occupy()` manage `TableStatus`. Table is freed automatically on order payment or cancellation.

**WebSocket:** Every order mutation publishes a `OrderEvent` (type + orderId) to `/topic/orders` via STOMP.

**SystemConfig:** Key-value table for runtime config. Currently used for `takeaway_surcharge`.

**Product images:** `Product.imageUrl` (nullable) stores a public URL. ADMIN sets/replaces it via `POST /products/{id}/image` (multipart, JPG/PNG/WEBP, 5MB max) and clears it via `DELETE /products/{id}/image` — see `ProductImageStorageService`. Storage is a self-hosted MinIO bucket, entirely optional (see env vars above); when unconfigured, upload rejects with a clear 400 instead of the app failing to start. A product's image is never required — `imageUrl` being `null` is the normal, expected state until someone uploads one.

**DataInitializer:** Seeds roles and a full menu on first startup if tables are empty.

## Security

All requests pass through `JwtAuthFilter`. `SecurityConfig` enforces role-based access:
- Public: `/auth/login`, `/auth/refresh`, `/auth/pin-login`, `/auth/pin-login-users`, `/v3/api-docs/**`, `/docs/**`
- ADMIN only: `/auth/register`, `/config/**`, `/users/**` (incl. `PATCH/DELETE /users/{id}/pin`), menu write ops (`POST/PUT/DELETE /menu/**`)
- ADMIN or CASHIER: payment endpoints, `/analytics/**`
- ADMIN, CASHIER or WAITER: `POST /orders/*/ready`, `/voice-order-test/**` (CHEF has no app access — kitchen ticket printer is the only kitchen-facing signal)
- All other endpoints: any authenticated user

`@EnableMethodSecurity` is active — use `@PreAuthorize` for finer-grained checks.

JWT stored as `HttpOnly` cookies. Cookie config (secure, sameSite, expiry) is centralized in `auth.CookieService`. Refresh token path is scoped to `/api/auth/refresh`.

Roles: `ADMIN`, `CASHIER`, `WAITER`, `CHEF`.

## Profiles

| Profile | DB port | `ddl-auto` | Notes |
|---------|---------|-----------|-------|
| `dev` (default) | 5433 | `validate` | SQL logging on, Scalar docs enabled |
| `prod` | via env vars | `validate` | Cookie `secure=true` |

Switch via `SPRING_PROFILES_ACTIVE` env var or `--spring.profiles.active=prod`.

## Timezone

App and Jackson both use `America/Lima`. Analytics date boundaries are computed in this timezone.
