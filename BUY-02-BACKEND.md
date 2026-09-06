# Buy-02 — Backend: what was added compared to `main`

This document compares the `buy-02` branch to `main` on the **backend only** (the Spring Boot microservices in `Backend/`), against the Buy-02 subject requirements.

## What was added

### New `orders` microservice (cart + orders)
`main` has no `orders` service. It was built from scratch in `buy-02` (`Backend/orders/`):

- **Cart (`CartController`, `/api/cart`)**
  - `GET /api/cart` — get the user's cart
  - `POST /api/cart/items` — add a product to the cart
  - `PATCH /api/cart/items` — update an item's quantity
  - `DELETE /api/cart/items/{productId}` — remove an item
  - `DELETE /api/cart` — clear the cart
  - User identified via the `X-User-Id` header (forwarded by the gateway)

- **Orders (`OrdersController`, `/api/orders`)**
  - `POST /api/orders` — create an order from the cart (pay on delivery)
  - `GET /api/orders/{id}` — order details
  - `GET /api/orders` — list the logged-in user's orders

- **MongoDB models**: `Cart`, `CartItems`, `Order`, `OrderStatus` (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED), `PaymentMethod`

- **Inter-service communication**: `ProductClient` (RestTemplate) to check the product/stock against the `product` service when adding to cart

- **Error handling**: `GlobalExceptionHandler` with dedicated exceptions (`ProductNotFoundException`, `InsufficientStockException`, `CartItemNotFoundException`, `EmptyCartException`, `OrderNotFoundException`) mapped to proper HTTP status codes (404, 409, 400)

- **Security**: `SecurityConfig` + `HeaderAuthFilter` (same pattern as the other services: the user is authenticated upstream by the gateway)

- Docker config (`dockerfile`), Maven wrapper, `sonar-project.properties`, a startup test (`OrdersApplicationTests` — Spring context load only)

### `product` service: added categories
- New `Category` enum (e.g. `LIVE_POULTRY`, `CHICKS`, `EXOTIC_BIRDS`, `CONSUMPTION_EGGS`, `HATCHING_EGGS`, `SPECIALTY_EGGS`, `FEED_AND_SUPPLIES`)
- `category` field added to the `Product` model, `ProductRequest` and `ProductResponse`
- `ProductService` updated to handle this field

### Other services (user, product, media, gateway, discovery)
- Per-module `pom.xml` and `sonar-project.properties` added (SonarQube integration), Jenkins pipeline configured for multi-module analysis
- Minor config tweaks (`application.properties`) and dead code removed from `Jwt.java` (gateway)

## What is still missing compared to the Buy-02 subject

These points are required by the subject but **are not implemented on the backend**:

- **Order management (beyond create/read)**
  - No endpoint to cancel, redo, or delete an order
  - No listing/search of orders for **sellers** (the service only knows `userId` — there is no concept of orders linked to a seller/their products)
  - No search by status/date on `GET /api/orders`

- **Profiles & analytics**
  - No endpoint for the buyer dashboard (most bought products, total amount spent)
  - No endpoint for the seller dashboard (best-selling products, total revenue)
  - This data doesn't exist anywhere on the backend (neither `user` nor `product` has anything of this kind)

- **Product search & filtering**
  - `ProductController` only exposes a plain `GET` (full list) + `GET /{id}` + `GET /myProducts`
  - No keyword search parameters, no filters (category, price), no server-side pagination/sorting

- **Standardized error format**
  - `GlobalExceptionHandler` returns a plain `String` with the correct HTTP status code, but not the `{ code, message, details }` schema required by the subject

- **Tests**
  - The `orders` service only has a startup test (`contextLoads`) — no unit or integration tests on `CartService` / `OrderService` / the controllers

## Summary

`buy-02` adds the **cart + simple order with pay-on-delivery** building block and the **category field** on products. However, advanced order management (cancel/redo, seller view), **analytics profiles** (buyer and seller), and **product search/filtering** are still missing on the backend.
