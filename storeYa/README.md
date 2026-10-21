# storeYa — Backend (Spring Boot REST API)

REST API for the storeYa e-commerce app: Spring Boot 3.5 on Java 17, PostgreSQL through
Spring Data JPA, JWTs issued by Keycloak, Redis-backed rate limiting, VNPay sandbox
payments, Cloudinary for product images and Gmail for OTP mail.

Listens on **http://localhost:8081**. The Angular client lives in
[`../storeYa_frontend`](../storeYa_frontend) and calls it from `http://localhost:4200`.

## Requirements

- JDK 17 (`<java.version>` in `pom.xml`)
- Maven — not needed, use the wrapper: `./mvnw` (`mvnw.cmd` on Windows)
- Running services: PostgreSQL (database `training`), Redis, Keycloak
- Optional, only for the feature they gate: Gmail app password (OTP), Cloudinary keys
  (image delete), VNPay sandbox merchant credentials (payment)

There is no `docker-compose` file in this repo — the containers have to be started by hand.

## Quick start

```bash
cp .env.example .env      # then fill in the secrets
./mvnw spring-boot:run    # http://localhost:8081
./mvnw clean test
```

`.env` is loaded by `spring.config.import=optional:file:.env[.properties]`, which resolves
against the **working directory** — the file must sit in `storeYa/`, not in the repo root.
When launching from IntelliJ, set the run configuration's working directory to `storeYa/`;
otherwise placeholders like `DB_PASSWORD` stay unresolved and startup fails.

## Configuration

Every value below is `${ENV_VAR:default}` in `src/main/resources/application.properties`.

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `training` | PostgreSQL coordinates |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` / *required* | DB credentials |
| `JPA_DDL_AUTO` | `update` | Hibernate schema strategy |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Rate-limit and OTP counters |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8080/realms/training-realm` | JWT issuer; must be reachable at startup (OIDC metadata) |
| `KEYCLOAK_AUTH_SERVER_URL` / `KEYCLOAK_REALM` / `KEYCLOAK_CLIENT_ID` / `KEYCLOAK_CLIENT_SECRET` | `http://localhost:8080` / `training-realm` / `training` / *required* | Keycloak client used by login |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | `smtp.gmail.com` / `587` / *required* / *required* | OTP mail over STARTTLS |
| `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` | `dz0dqy62d` / *required* / *required* | Product image storage |
| `VNPAY_TMN_CODE` / `VNPAY_HASH_SECRET` | *required* / *required* | VNPay merchant credentials |
| `VNPAY_PAY_URL` / `VNPAY_API_URL` / `VNPAY_VERSION` | sandbox URLs / `2.1.0` | VNPay endpoints |
| `VNPAY_RETURN_URL` | `http://localhost:4200/payment-result` | Browser landing route in the Angular app |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Comma-separated exact origins, no wildcards |
| `TRUST_PROXY` | `false` | When `true`, `X-Forwarded-For` may be used as the rate-limit key |
| `RATE_LIMIT_ENABLED` | `true` | Master switch for `RateLimitFilter` |
| `RATE_LIMIT_AUTH_PER_MINUTE` / `RATE_LIMIT_IPN_PER_MINUTE` | `10` / `120` | Per-bucket window size |
| `RATE_LIMIT_OTP_SEND` / `RATE_LIMIT_OTP_ATTEMPTS` | `3` / `5` | OTP send per 15 minutes / verify attempts |

Secrets never appear in this file's defaults, and `VnPayConfig` excludes `vnp_HashSecret`
from `toString()` so it cannot ride into a log line.

## Response envelope

Controllers answer through `ApiResponse` (`success`, `status`, `message`, `data`; null
fields omitted) and `PageResponse` (`content`, `pageNumber`, `pageSize`, `totalPages`,
`last`). `GlobalExceptionHandler` maps `SecurityException` to 403,
`ResponseStatusException` to its own status, `MethodArgumentNotValidException` to 400 with
the field list, `IllegalArgumentException` to 400, and any other `RuntimeException` to a
generic 500 (cause logged, not returned).

## API

### `/api/auth` — public

| Method | Path |
| --- | --- |
| POST | `/api/auth/login` |
| POST | `/api/auth/register` |
| POST | `/api/auth/forgot-password` |
| POST | `/api/auth/reset-password` |
| POST | `/api/auth/logout` |
| POST | `/api/auth/refresh` |

### `/api/products`

| Method | Path | Access |
| --- | --- | --- |
| GET | `/api/products/public/all` | public |
| GET | `/api/products/public/{id}` | public |
| GET | `/api/products?page=&size=` | authenticated |
| POST | `/api/products` | admin |
| PUT | `/api/products/{id}` | admin |
| DELETE | `/api/products/{id}` | admin |
| POST | `/api/products/cloudinary/delete` | admin |

### `/api/categories`

| Method | Path | Access |
| --- | --- | --- |
| GET | `/api/categories/public/all` | public |
| POST | `/api/categories/get` | authenticated |
| POST | `/api/categories` | admin |
| PUT | `/api/categories/{id}` | admin |
| DELETE | `/api/categories/{id}` | admin |

### `/api/carts` — authenticated

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/api/carts` | |
| POST | `/api/carts/items?productId=&quantity=` | |
| PUT | `/api/carts/items/update/{cartItemId}?quantity=` | id is the **CartItem** id |
| DELETE | `/api/carts/items/{cartItemId}` | |
| DELETE | `/api/carts` | clears the cart |

All five answer with `CartDTO` — never the entity, which would drag the whole
`CartItem -> Product -> Category` graph into the payload.

### `/api/payments`

| Method | Path | Access |
| --- | --- | --- |
| POST | `/api/payments` | authenticated |
| GET | `/api/payments` | authenticated |
| POST | `/api/payments/{paymentId}/confirm` | admin |
| POST | `/api/payments/retry` | authenticated |
| POST | `/api/payments/cancel` | authenticated (cancels by `setCode`) |
| GET | `/api/payments/vnpay-ipn` | public, guarded by HMAC signature |

### `/api/users`

| Method | Path | Access |
| --- | --- | --- |
| GET | `/api/users/me` | authenticated |
| PUT | `/api/users/me` | authenticated |
| GET | `/api/users` | admin |
| GET | `/api/users/{id}` | admin |
| POST | `/api/users` | admin |
| PUT | `/api/users/{id}` | admin |
| DELETE | `/api/users/{id}` | admin |

## Security model

`security/SecurityConfig.java` builds the chain from three pattern lists
(`PUBLIC_ENDPOINTS`, `SELF_ENDPOINTS`, `ADMIN_ONLY_ENDPOINTS`) applied in declaration
order, then `anyRequest().authenticated()`. CSRF is disabled, every `OPTIONS` request is
allowed, and CORS accepts only the exact origins from `CORS_ALLOWED_ORIGINS` with
`allowCredentials(false)`.

Authorities come from the JWT claim `realm_access.roles`, copied **verbatim** by
`CustomAuthoritiesConverter` — there is no `ROLE_` prefix, which is why guards use
`hasAuthority('admin')` and never `hasRole('admin')`.

`UrlPatternContractTest` is the tripwire for the bug class that once left `/api/users`
unguarded: a chain pattern of `/api/user/**` does not match the real path `/api/users`, so
the request silently fell through to `anyRequest().authenticated()` and everything looked
fine.

## Rate limiting

`RateLimitFilter` runs at `@Order(-200)`, before the security filter chain, over two
buckets: `auth` (`/api/auth/**`) and `ipn` (`/api/payments/vnpay-ipn`). Each is a fixed
one-minute window keyed `RL:<bucket>:<ip>`; over the limit returns 429 with
`Retry-After: 60` in the standard envelope. Counting is one atomic Redis `INCR` +
`PEXPIRE` round trip (`RedisWindowCounter`), and it **fails open** — if Redis is down the
request is allowed through rather than locking customers out of sign-in or checkout.
`X-Forwarded-For` is only trusted when `TRUST_PROXY=true`, so a client cannot spoof past
its own bucket.

## Project layout

```
src/main/java/com/storeya/
├── StoreYaApplication.java
└── shop/
    ├── config/        CloudinaryConfig, ConfigMapper (ModelMapper bean), VnPayConfig
    ├── controller/    6 REST controllers
    │   └── advice/    GlobalExceptionHandler
    ├── dto/           CartDTO, ProductDTO, ... + request/ + response/ (ApiResponse, PageResponse, TokenResponse)
    ├── entity/        JPA entities
    ├── enums/         PaymentMethod, PaymentStatus (stored as STRING)
    ├── mapper/        entity -> DTO
    ├── repository/    6 Spring Data JPA repositories
    ├── security/      SecurityConfig, CustomAuthoritiesConverter, JwtAuthenticationEntryPoint,
    │                  RateLimitFilter, RedisWindowCounter
    ├── service/       auth, cart, category, cloudinary, pay, product, user, vnpay
    │                  (each an I*Service interface plus its implementation)
    └── utils/         VNPayUtil (HMAC-SHA512), StringRandom
```

## Data model

`User`, `Category 1—* Product 1—* ProductImage`, `Cart 1—* CartItem *—1 Product`,
`Payment *—1 User` and `Payment 1—* OrderItem *—1 Product`. There is no `Order` entity —
`Payment` is the order aggregate, and its `code` (`HD` + 12 characters from
`A-Z0-9`) doubles as the retry/cancel token a caller supplies, so it is generated as a
security-sensitive value, not a sequential id. `Cart.userId` is a plain column, not a
relation.

## Testing

```bash
./mvnw clean test    # 58 tests run, 6 skipped by design
```

Always pass `clean`: the VS Code Java builder writes its own class files into
`target/test-classes`, and Maven's incremental check can then skip a source file that no
longer matches the stale class on disk.

| Test | Guards |
| --- | --- |
| `StoreYaApplicationTests` | Spring context boots against real Postgres/Keycloak |
| `UrlPatternContractTest` | every controller path maps to the intended access decision |
| `SecurityAuthorizationMatrixTest` | live filter-chain enforcement — see below |
| `CartControllerShapeTest` | all five cart endpoints answer with the same `CartDTO` shape |
| `RateLimitFilterTest` | only auth/IPN counted, 429 shape, untrusted `X-Forwarded-For` ignored |
| `RedisWindowCounterTest` | one round trip per increment, fail-open on Redis outage |
| `OtpServiceTest` | OTP issue/verify rules and attempt ceiling |
| `PaymentCodeStrengthTest` | order code length, prefix and alphabet coverage |
| `PayServiceRetryOwnershipTest` | retry/cancel only touches the caller's own order |
| `UserServiceGuardTest` | self-service vs admin-only user access |
| `VnPayIpnGuardTest`, `VnPaySignatureCompareTest`, `VnPayLogRedactionTest` | IPN signature checks and secret-safe logging |

`SecurityAuthorizationMatrixTest` is annotated
`@EnabledIfEnvironmentVariable(named = "STOREYA_IT", matches = "true")` and stays skipped
unless Postgres, Keycloak and Redis are all reachable:

```bash
STOREYA_IT=true ./mvnw clean test -Dtest=SecurityAuthorizationMatrixTest
```

## Notes

- `spring.jpa.hibernate.ddl-auto` defaults to `update`, so the schema is mutated in place —
  point it at a scratch database.
- `spring.jpa.open-in-view` is not set and therefore still `true`.
- `keycloak-admin-client` is declared in `pom.xml` but unused; `AuthService` talks to
  Keycloak with `RestTemplate`.
