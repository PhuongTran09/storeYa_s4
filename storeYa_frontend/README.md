# storeYa — Frontend (Angular)

Customer storefront and admin console for the storeYa shop: Angular 17 with standalone
components, TypeScript in strict mode (including `strictTemplates`), SCSS plus Tailwind 3.4
for styling, and RxJS `BehaviorSubject` streams for state.

Runs on **http://localhost:4200** and talks to the Spring Boot API on
`http://localhost:8081` — see [`../storeYa`](../storeYa).

## Requirements

- Node 18.13+ or 20.9+ (what Angular 17 requires; the repo pins no `engines` field)
- npm, from the lockfile in this directory
- The backend running on `:8081`, with `CORS_ALLOWED_ORIGINS` covering
  `http://localhost:4200`

## Quick start

```bash
npm install
npm start           # ng serve -> http://localhost:4200
npm run build       # production build into dist/
npm test            # ng test (Karma + ChromeHeadless)
```

Other scripts: `ng` (the CLI itself) and `watch` (`ng build --watch --configuration development`).

There is **no dev-server proxy** — no `proxy.conf.json`, no `proxyConfig` in `angular.json`.
The browser calls `http://localhost:8081/api` directly, so the backend's CORS allow-list is
what makes or breaks a local run.

## API base URL

`angular.json` swaps the config file per configuration:

| File | Value | Used by |
| --- | --- | --- |
| `src/environment/api.config.ts` | `http://localhost:8081/api` | `ng serve`, development build |
| `src/environment/api.config.prod.ts` | `/api` (same origin) | production build |

Note the folder is `src/environment/`, singular, and it holds only these two files. For a
hosted deployment behind a reverse proxy, `/api` must be forwarded to the backend and the
public origin added to the backend's `CORS_ALLOWED_ORIGINS`.

## Routes

From `src/app/app.routes.ts`:

| Path | Component | Guard |
| --- | --- | --- |
| `''` | redirect to `home` | — |
| `home` | `HomeComponent` (lazy) | — |
| `product/:id` | `ProductDetailComponent` | — |
| `cart` | `CartListComponent` | `authGuard` |
| `profile` | `ProfileComponent` | `authGuard` |
| `payments` | `PaymentsComponent` | `authGuard` |
| `history-payment` | `UserListPayComponent` | `authGuard` |
| `payment-result` | `PaymentResultComponent` | `paymentResultGuard` |
| `admin/dashboard` | `DashboardComponent` (lazy) | `authGuard` |
| `404` | `NotFoundComponent` (lazy) | — |
| `**` | redirect to `404` | — |

`product-add`, `product-list`, `category-add` and `category-list` are **not** routes — they
are child components switched inside the dashboard.

## Auth flow

- `POST /api/auth/login` with `{ mail, password }` returns `{ accessToken, refreshToken }`;
  both are kept in `localStorage` as `access_token` and `refresh_token`.
- `core/interceptors/auth.interceptor.ts` adds `Authorization: Bearer <accessToken>` to
  every request **except** those carrying the sentinel header `noauth` (all six auth calls
  use it, so a stale token cannot break login or refresh).
- On a 401 it refreshes once through a shared `refreshing` observable (`shareReplay(1)`)
  and retries the original request; Keycloak rotates refresh tokens, so two concurrent
  refreshes would log the user out. A failed refresh logs out. A 30-second watcher does the
  same proactively when the access token has under a minute left.
- `core/guards/auth.guards.ts` (note the plural filename) decodes the token payload and
  reads `realm_access.roles`. Unauthenticated visits to `/admin*` or `/user*` go to `/404`;
  other protected routes open the **login modal** through `NavbarService.openLoginModal()` —
  there is no `/login` route.
- `paymentResultGuard` admits `/payment-result` only when `vnp_ResponseCode` is present in
  the query string, otherwise it redirects home.

## Services and endpoints

All under `src/app/core/services/`:

| Service | Calls |
| --- | --- |
| `auth.service.ts` | `POST /auth/login`, `/auth/logout`, `/auth/refresh`, `/auth/register`, `/auth/forgot-password`, `/auth/reset-password` |
| `product.service.ts` | `GET /products`, `/products/public/all`, `/products/public/{id}`; `POST /products`, `/products/cloudinary/delete`; `PUT /products/{id}`; `DELETE /products/{id}` |
| `category.service.ts` | `GET /categories/public/all`; `POST /categories`; `PUT /categories/{id}`; `DELETE /categories/{id}` |
| `cart.service.ts` | `GET /carts`; `POST /carts/items`; `PUT /carts/items/update/{cartItemId}`; `DELETE /carts/items/{id}`; `DELETE /carts` |
| `payment.service.ts` | `POST /payments`, `POST /payments/{id}/confirm`, `POST /payments/cancel`; `GET /payments` |
| `user.service.ts` | `GET /users/me`, `PUT /users/me` |
| `navbar.service.ts`, `toast.service.ts` | UI state only, no HTTP |
| `upload.service.ts` | posts straight to `api.cloudinary.com` — currently referenced nowhere, i.e. dead code |

## Project layout

```
src/app/
├── components/       auth/ (login, register, forgot-password), carts/, category/,
│                     payments/, product/, profile/
├── core/
│   ├── guards/       auth.guards.ts (authGuard, paymentResultGuard)
│   ├── interceptors/ auth.interceptor.ts
│   └── services/     the table above
├── page/             home, dashboard, not-found, payment-result
├── shared/           layout/navbar (selector app-header), admin-layout/ (navbar-admin,
│                     sidebar-admin), confirm-dialog, pagination, toast,
│                     pipes/ (TruncatePipe, VndPipe), response/page-response.ts
└── environment/      api.config.ts, api.config.prod.ts
```

## Conventions

- Components are standalone; injection style is mixed — newer services use `inject()`,
  older ones use constructor injection.
- State lives in `BehaviorSubject` streams, not signals: `CartService.cart$` with derived
  `total$` and `itemCount$`, `AuthService.isAuthenticated$`, `NavbarService.loginModalState$`,
  `ToastService.toast$`.
- Model interfaces (`Product`, `Category`, `CartItem`, `PaymentDTO`, `User`) sit next to the
  service that uses them; there is no `models/` folder. The one shared type is `PageResponse`
  in `shared/response/`.
- Tailwind classes are scanned from `./src/**/*.{html,ts}` per `tailwind.config.js`; global
  `@tailwind` directives live in `src/styles.scss`.
- User-facing strings are Vietnamese.

## Testing

`npm test` runs Karma/ChromeHeadless and is wired correctly, but coverage is effectively
nil: of the 24 `.spec.ts` files, 23 are untouched CLI `"should create"` stubs and
`src/app/app.component.spec.ts` is empty. Treat the suite as scaffolding, not a safety net.

There is **no end-to-end setup** — no Cypress/Playwright/Protractor dependency and no `e2e`
target in `angular.json`, so `ng e2e` will not work.

## Gotchas

- `CartService` is provided on public pages too, so it only loads a cart when a token
  exists.
- `updateQuantity()` takes the **CartItem** id, not the product id.
- Clearing the cart is `DELETE /carts`; the older `DELETE /carts/clear` does not exist
  server-side and used to 404 silently while the server cart stayed filled.
- Cancelling an order posts `setCode` in the body — `POST /payments/{id}/cancel` 404s.
- The dashboard sidebar offers `settings`, `exports` and `orders`, but those views have no
  component yet (`currentView` starts as `null`).
- Several declared dependencies are unused in `src/`: `jwt-decode` (tokens are decoded by
  hand with `atob`), `cloudinary`, `@cloudinary/angular-5.x`, `@popperjs/core`,
  `font-awesome` and `@fortawesome/fontawesome-free` — icons actually come from a Font
  Awesome 6.5.2 CDN `<link>` in `src/index.html`.
