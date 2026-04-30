# buy-01
# 🛒 Buy01 — E-Commerce Microservices Platform

> A full-stack marketplace built with **Spring Boot microservices** on the backend and **Angular** on the frontend. Sellers manage their product catalog and media; clients browse and discover products.

---

## 📑 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Microservices & API Reference](#microservices--api-reference)
  - [API Gateway](#api-gateway)
  - [User Service](#user-service)
  - [Product Service](#product-service)
  - [Media Service](#media-service)
- [Frontend — Angular SPA](#frontend--angular-spa)
- [Authentication & Authorization](#authentication--authorization)
- [Database Design](#database-design)
- [Security Measures](#security-measures)
- [Error Handling](#error-handling)
- [Kafka Events (Optional)](#kafka-events-optional)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Running with Docker Compose](#running-with-docker-compose)
  - [Running Services Individually](#running-services-individually)
- [Environment Variables](#environment-variables)
- [Health Checks](#health-checks)
- [Folder Structure](#folder-structure)

---

## Overview

**Buy01** is an end-to-end e-commerce platform composed of independently deployable services. Users register as either **Clients** (browse products) or **Sellers** (manage their catalog and media). The platform enforces strict ownership, role-based access control, and secure file handling throughout.

### User Roles

| Role | Capabilities |
|------|-------------|
| `CLIENT` | Browse and view products |
| `SELLER` | Create, update, delete own products; upload/manage images; update profile/avatar |
| `ADMIN` *(optional)* | Moderation and platform-level oversight |

---

## Architecture

```
                        ┌─────────────────────────────────┐
                        │         Angular Frontend         │
                        │  (SPA — Auth, Dashboard, Store)  │
                        └────────────────┬────────────────┘
                                         │ HTTPS
                        ┌────────────────▼────────────────┐
                        │           API Gateway            │
                        │  (CORS, Auth Propagation,        │
                        │   Rate Limiting, Routing)        │
                        └──┬──────────┬──────────┬────────┘
                           │          │           │
              ┌────────────▼──┐  ┌────▼──────┐  ┌▼────────────┐
              │  User Service  │  │ Product   │  │   Media     │
              │  (Auth/Profile)│  │ Service   │  │  Service    │
              └───────┬────────┘  └────┬──────┘  └──────┬──────┘
                      │               │                  │
              ┌───────▼───────────────▼──────────────────▼──────┐
              │                   MongoDB                         │
              │       (users / products / media metadata)        │
              └──────────────────────────────────────────────────┘
                      │               │                  │
              ┌───────▼───────────────▼──────────────────▼──────┐
              │               Kafka (optional)                    │
              │    PRODUCT_CREATED | IMAGE_UPLOADED events        │
              └──────────────────────────────────────────────────┘
```

**Service Discovery** (Eureka or similar) registers all services; the Gateway resolves routes dynamically.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.x, Spring Security, Spring Cloud Gateway |
| Auth | JWT (or OAuth2) |
| Database | MongoDB |
| Messaging | Apache Kafka *(optional)* |
| Service Discovery | Eureka (Spring Cloud Netflix) |
| Frontend | Angular 17+, Angular Material / Bootstrap |
| File Storage | Object storage (e.g., MinIO, AWS S3, or local disk) |
| Containerization | Docker, Docker Compose |
| HTTPS | Let's Encrypt / self-signed for local dev |

---

## Project Structure

```
buy01/
├── api-gateway/              # Spring Cloud Gateway
├── discovery-service/        # Eureka Service Registry
├── user-service/             # Auth, profiles, roles
├── product-service/          # Product CRUD
├── media-service/            # Image upload/download
├── frontend/                 # Angular SPA
├── docker-compose.yml
└── README.md
```

---

## Microservices & API Reference

### API Gateway

**Base URL:** `https://<host>/`

The Gateway is the single entry point for all external traffic. It handles:

- **Routing** requests to the appropriate downstream service
- **JWT validation** and propagation of auth headers
- **CORS** policy enforcement
- **Rate limiting** *(optional)* on auth and media endpoints

| Route Prefix | Forwarded To |
|--------------|-------------|
| `/auth/**` | User Service |
| `/api/users/**` | User Service |
| `/api/products/**` | Product Service |
| `/api/media/**` | Media Service |
| `/actuator/**` | Each respective service |

---

### User Service

**Base path:** `/` (routed via gateway to user-service)

Handles registration, login, JWT issuance, and user profile management.

#### Auth Endpoints

| Method | Endpoint | Auth Required | Role | Description |
|--------|----------|:---:|------|-------------|
| `POST` | `/auth/register` | ❌ | — | Register as CLIENT or SELLER |
| `POST` | `/auth/login` | ❌ | — | Login, returns JWT token |

**`POST /auth/register` — Request Body**
```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "role": "CLIENT | SELLER"
}
```

**`POST /auth/login` — Request Body**
```json
{
  "email": "string",
  "password": "string"
}
```

**`POST /auth/login` — Response**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "role": "SELLER"
}
```

#### Profile Endpoints

| Method | Endpoint | Auth Required | Role | Description |
|--------|----------|:---:|------|-------------|
| `GET` | `/api/users/me` | ✅ | CLIENT / SELLER | Get current user profile |
| `PUT` | `/api/users/me` | ✅ | CLIENT / SELLER | Update profile info |
| `PUT` | `/api/users/me/avatar` | ✅ | SELLER | Upload/update avatar (delegates to Media Service) |

**`GET /api/users/me` — Response**
```json
{
  "id": "string",
  "username": "string",
  "email": "string",
  "role": "SELLER",
  "avatarUrl": "string | null",
  "createdAt": "ISO8601"
}
```

**`PUT /api/users/me` — Request Body**
```json
{
  "username": "string",
  "email": "string"
}
```

---

### Product Service

**Base path:** `/api/products`

Manages the product catalog. Public read access; write operations restricted to authenticated SELLERs who own the resource.

#### Public Endpoints

| Method | Endpoint | Auth Required | Role | Description |
|--------|----------|:---:|------|-------------|
| `GET` | `/api/products` | ❌ | — | List all products (paginated) |
| `GET` | `/api/products/{id}` | ❌ | — | Get a single product by ID |

**`GET /api/products` — Query Params**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `page` | int | 0 | Page number (0-indexed) |
| `size` | int | 20 | Items per page |

**`GET /api/products` — Response**
```json
{
  "content": [
    {
      "id": "string",
      "name": "string",
      "description": "string",
      "price": 49.99,
      "sellerId": "string",
      "imageUrls": ["string"],
      "createdAt": "ISO8601"
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "page": 0,
  "size": 20
}
```

#### Seller-Only Endpoints

| Method | Endpoint | Auth Required | Role | Description |
|--------|----------|:---:|------|-------------|
| `POST` | `/api/products` | ✅ | SELLER | Create a new product |
| `PUT` | `/api/products/{id}` | ✅ | SELLER (owner) | Update own product |
| `DELETE` | `/api/products/{id}` | ✅ | SELLER (owner) | Delete own product |
| `POST` | `/api/products/{id}/images` | ✅ | SELLER (owner) | Link an image URL to a product |
| `DELETE` | `/api/products/{id}/images/{imageId}` | ✅ | SELLER (owner) | Remove an image reference from product |

**`POST /api/products` — Request Body**
```json
{
  "name": "string",
  "description": "string",
  "price": 49.99,
  "imageUrls": ["string"]
}
```

**`POST /api/products` — Response** `201 Created`
```json
{
  "id": "string",
  "name": "string",
  "description": "string",
  "price": 49.99,
  "sellerId": "string",
  "imageUrls": [],
  "createdAt": "ISO8601"
}
```

> **Ownership enforcement:** On `PUT` and `DELETE`, the service verifies `product.sellerId == JWT subject`. Returns `403 Forbidden` if the authenticated seller does not own the product.

---

### Media Service

**Base path:** `/api/media`

Handles image uploads, storage, and retrieval. All uploads are restricted to SELLERs and undergo strict validation.

#### Endpoints

| Method | Endpoint | Auth Required | Role | Description |
|--------|----------|:---:|------|-------------|
| `POST` | `/api/media/images` | ✅ | SELLER | Upload a new image |
| `GET` | `/api/media/images/{id}` | ❌ | — | Retrieve/serve an image |
| `DELETE` | `/api/media/images/{id}` | ✅ | SELLER (owner) | Delete an owned image |

**`POST /api/media/images` — Request**

- Content-Type: `multipart/form-data`
- Field: `file` (binary image)
- Validations enforced:
  - MIME type must be `image/*` (verified by content sniffing, not just extension)
  - File size must be **≤ 2 MB**

**`POST /api/media/images` — Response** `201 Created`
```json
{
  "id": "string",
  "url": "/api/media/images/{id}",
  "filename": "string",
  "mimeType": "image/jpeg",
  "sizeBytes": 102400,
  "uploadedBy": "sellerId",
  "uploadedAt": "ISO8601"
}
```

**`GET /api/media/images/{id}` — Response**

Returns the raw image binary with appropriate headers:
```
Content-Type: image/jpeg
Cache-Control: public, max-age=86400
ETag: "abc123"
```

**`DELETE /api/media/images/{id}` — Response** `204 No Content`

> Returns `403 Forbidden` if the requesting seller does not own the image.

#### Upload Validation Rules

| Rule | Detail |
|------|--------|
| MIME type | Must be `image/*`; validated by sniffing file bytes, not file extension |
| File size | Maximum **2 MB** (2,097,152 bytes) |
| Filename | Sanitized to prevent path traversal |
| Content | Reject files that claim to be images but are not (e.g., renamed `.exe`) |

---

## Frontend — Angular SPA

### Pages & Routes

| Route | Component | Guard | Description |
|-------|-----------|-------|-------------|
| `/` | `ProductListComponent` | — | Public product grid |
| `/products/:id` | `ProductDetailComponent` | — | Single product detail view |
| `/auth/login` | `LoginComponent` | — | Sign-in form |
| `/auth/register` | `RegisterComponent` | — | Sign-up form with role selection |
| `/dashboard` | `DashboardComponent` | `AuthGuard`, `RoleGuard(SELLER)` | Seller overview |
| `/dashboard/products` | `ProductManageComponent` | `AuthGuard`, `RoleGuard(SELLER)` | Create/edit/delete products |
| `/dashboard/media` | `MediaManageComponent` | `AuthGuard`, `RoleGuard(SELLER)` | Upload/manage images |
| `/profile` | `ProfileComponent` | `AuthGuard` | View/edit profile; seller avatar upload |

### Key Angular Features

#### Route Guards

```typescript
// AuthGuard — blocks unauthenticated access
canActivate(): boolean {
  return this.authService.isLoggedIn() || this.router.navigate(['/auth/login']);
}

// RoleGuard — restricts by role
canActivate(route: ActivatedRouteSnapshot): boolean {
  const required = route.data['role'];
  return this.authService.hasRole(required) || this.router.navigate(['/']);
}
```

#### HTTP Interceptors

**Token Interceptor** — attaches JWT to every outgoing request:
```typescript
intercept(req: HttpRequest<any>, next: HttpHandler) {
  const token = this.authService.getToken();
  const cloned = token
    ? req.clone({ headers: req.headers.set('Authorization', `Bearer ${token}`) })
    : req;
  return next.handle(cloned);
}
```

**Error Interceptor** — handles `401` (redirect to login) and `403` (show forbidden message):
```typescript
catchError((error: HttpErrorResponse) => {
  if (error.status === 401) this.router.navigate(['/auth/login']);
  if (error.status === 403) this.notificationService.error('Access denied');
  return throwError(() => error);
});
```

#### Forms

All forms use **Angular Reactive Forms** with inline validation:

- `LoginForm` — email, password; show field errors on blur
- `RegisterForm` — username, email, password, role (CLIENT/SELLER); password strength hint
- `ProductForm` — name, description, price (must be > 0); image attachment section
- `MediaUploadForm` — file picker with client-side MIME and size validation before API call

#### File Upload (Client-Side Validation)

```typescript
onFileSelected(file: File): void {
  if (!file.type.startsWith('image/')) {
    this.error = 'Only image files are allowed.';
    return;
  }
  if (file.size > 2 * 1024 * 1024) {
    this.error = 'File must be 2 MB or smaller.';
    return;
  }
  this.uploadImage(file);
}
```

#### Notifications

Use Angular Material **Snackbar** or Bootstrap **Toast** for:
- Upload success / failure
- File too large or wrong type
- Forbidden actions (`403`)
- Session expired (`401`)

---

## Authentication & Authorization

```
Client                    Gateway                  User Service
  │                          │                          │
  ├── POST /auth/login ──────►│                          │
  │                          ├── forward ───────────────►│
  │                          │                          ├── verify credentials
  │                          │                          ├── issue JWT
  │                          │◄── JWT ──────────────────┤
  │◄── JWT ──────────────────┤                          │
  │                          │                          │
  ├── GET /api/products ─────►│                          │
  │   Authorization: Bearer  ├── validate JWT           │
  │                          ├── propagate X-User-Id    │
  │                          ├── forward to Product Svc │
  │◄── 200 OK ───────────────┤                          │
```

- **JWT** is issued on login, signed with a secret key, and includes `sub` (userId), `role`, and `exp`.
- The **Gateway** validates the JWT signature and forwards the decoded identity (`X-User-Id`, `X-User-Role`) as headers to downstream services.
- Downstream services **trust** these headers (they are internal-only; external headers are stripped by the Gateway).
- **Passwords** are hashed with **BCrypt** (never stored in plain text, never returned in responses).

---

## Database Design

### MongoDB Collections

**`users`**
```json
{
  "_id": "ObjectId",
  "username": "string",
  "email": "string",
  "passwordHash": "string",
  "role": "CLIENT | SELLER",
  "avatarMediaId": "string | null",
  "createdAt": "Date",
  "updatedAt": "Date"
}
```

**`products`**
```json
{
  "_id": "ObjectId",
  "name": "string",
  "description": "string",
  "price": "Decimal128",
  "sellerId": "ObjectId (ref: users)",
  "imageIds": ["ObjectId (ref: media)"],
  "createdAt": "Date",
  "updatedAt": "Date"
}
```

**`media`**
```json
{
  "_id": "ObjectId",
  "filename": "string",
  "mimeType": "string",
  "sizeBytes": "Long",
  "storagePath": "string",
  "uploadedBy": "ObjectId (ref: users)",
  "uploadedAt": "Date"
}
```

> **Note:** Images are stored on object storage (MinIO / S3 / local filesystem). Only the metadata and storage path live in MongoDB — never the binary data itself.

---

## Security Measures

| Concern | Implementation |
|---------|---------------|
| Transport | HTTPS end-to-end (Let's Encrypt for production, self-signed for dev) |
| Passwords | BCrypt hash + salt in User Service; never exposed in API responses |
| Auth tokens | Short-lived JWT; `Authorization: Bearer` header only |
| CORS | Configured at Gateway; restricts `Origin`, `Methods`, `Headers` |
| File validation | MIME sniffing (magic bytes), extension check, size limit (2 MB) |
| Ownership | `sellerId == JWT subject` checked in Product and Media services |
| Input validation | Bean Validation (`@Valid`) on all request bodies; reject malformed input with `400` |
| Header stripping | Gateway strips `X-User-Id` / `X-User-Role` from incoming external requests |
| Rate limiting | *(Optional)* Gateway rate limit on `/auth/login` and `POST /api/media/images` |
| XSS / Injection | Angular's built-in sanitization; parameterized MongoDB queries |

---

## Error Handling

### Backend — HTTP Status Codes

| Status | When |
|--------|------|
| `200 OK` | Successful GET / PUT |
| `201 Created` | Successful POST (resource created) |
| `204 No Content` | Successful DELETE |
| `400 Bad Request` | Validation failure, invalid MIME type, file too large |
| `401 Unauthorized` | Missing or invalid JWT |
| `403 Forbidden` | Valid JWT but insufficient role or not resource owner |
| `404 Not Found` | Resource does not exist or is not owned by caller |
| `409 Conflict` | Duplicate registration (email already exists) |
| `500 Internal Server Error` | Caught by global exception handler; returns structured error body |

### Error Response Body (all services)

```json
{
  "timestamp": "ISO8601",
  "status": 400,
  "error": "Bad Request",
  "message": "File size exceeds the 2 MB limit",
  "path": "/api/media/images"
}
```

All services use a `@ControllerAdvice` global exception handler to avoid unhandled `5xx` responses.

### Frontend — Angular Error UX

- Inline form errors displayed on field blur (required, minlength, pattern, price > 0)
- Snackbar / toast notifications for API errors (upload failures, forbidden, session expired)
- Redirect to `/auth/login` on `401`; show error page on `403`

---

## Kafka Events (Optional)

If Kafka is enabled, the following events are published:

| Event | Published By | Payload | Consumers |
|-------|-------------|---------|-----------|
| `PRODUCT_CREATED` | Product Service | `{ productId, sellerId, name }` | Audit log, cache invalidation |
| `PRODUCT_UPDATED` | Product Service | `{ productId, sellerId }` | Cache invalidation |
| `PRODUCT_DELETED` | Product Service | `{ productId, sellerId }` | Media cleanup, cache |
| `IMAGE_UPLOADED` | Media Service | `{ mediaId, uploadedBy, mimeType }` | Thumbnail generation, audit |

These events are useful for decoupled audit logging, cache invalidation, and future features like thumbnail generation or recommendation engines.

---

## Getting Started

### Prerequisites

- **Docker** and **Docker Compose** installed
- **Java 17+** (if running services locally without Docker)
- **Node.js 18+** and **Angular CLI** (if running frontend locally)

### Running with Docker Compose

```bash
# Clone the repository
git clone https://github.com/your-org/buy01.git
cd buy01

# Copy environment config
cp .env.example .env
# Edit .env with your secrets (JWT_SECRET, MongoDB URI, etc.)

# Build and start all services
docker-compose up --build

# Services will be available at:
# API Gateway:        http://localhost:8080
# Angular Frontend:   http://localhost:4200
# Eureka Dashboard:   http://localhost:8761
# MongoDB:            localhost:27017
# Kafka:              localhost:9092 (if enabled)
```

### Running Services Individually

```bash
# 1. Start infrastructure
docker-compose up mongodb kafka zookeeper eureka -d

# 2. Start User Service
cd user-service
./mvnw spring-boot:run

# 3. Start Product Service
cd product-service
./mvnw spring-boot:run

# 4. Start Media Service
cd media-service
./mvnw spring-boot:run

# 5. Start API Gateway
cd api-gateway
./mvnw spring-boot:run

# 6. Start Angular frontend
cd frontend
npm install
ng serve
```

---

## Environment Variables

| Variable | Service | Description |
|----------|---------|-------------|
| `JWT_SECRET` | User Service, Gateway | Secret key for signing/verifying JWT tokens |
| `JWT_EXPIRATION_MS` | User Service | Token TTL in milliseconds (default: `3600000`) |
| `MONGODB_URI` | All services | MongoDB connection string |
| `EUREKA_SERVER_URL` | All services | Eureka registry URL |
| `MEDIA_STORAGE_PATH` | Media Service | Local path or S3 bucket for image storage |
| `MEDIA_MAX_SIZE_BYTES` | Media Service | Upload size limit (default: `2097152` = 2 MB) |
| `KAFKA_BOOTSTRAP_SERVERS` | Product Service, Media Service | Kafka broker address |
| `CORS_ALLOWED_ORIGINS` | API Gateway | Comma-separated list of allowed frontend origins |
| `ANGULAR_API_BASE_URL` | Frontend | Base URL of the API Gateway |

---

## Health Checks

All microservices expose Spring Boot Actuator health endpoints:

| Service | Endpoint |
|---------|----------|
| API Gateway | `GET /actuator/health` |
| User Service | `GET /actuator/health` |
| Product Service | `GET /actuator/health` |
| Media Service | `GET /actuator/health` |

Docker Compose uses these for container health checks before marking a service as ready.

---

## Folder Structure

```
buy01/
├── api-gateway/
│   └── src/main/java/.../gateway/
│       ├── config/           # Security, CORS, rate limiting config
│       └── filters/          # Auth propagation filter
│
├── discovery-service/
│   └── src/main/java/.../discovery/
│
├── user-service/
│   └── src/main/java/.../user/
│       ├── controller/       # AuthController, UserController
│       ├── service/          # AuthService, UserService
│       ├── repository/       # UserRepository (MongoDB)
│       ├── model/            # User, Role
│       ├── dto/              # RegisterRequest, LoginResponse, UserDto
│       └── security/         # JwtUtil, SecurityConfig
│
├── product-service/
│   └── src/main/java/.../product/
│       ├── controller/       # ProductController
│       ├── service/          # ProductService
│       ├── repository/       # ProductRepository (MongoDB)
│       ├── model/            # Product
│       └── dto/              # ProductRequest, ProductResponse
│
├── media-service/
│   └── src/main/java/.../media/
│       ├── controller/       # MediaController
│       ├── service/          # MediaService, StorageService
│       ├── repository/       # MediaRepository (MongoDB)
│       ├── model/            # Media
│       ├── dto/              # MediaResponse
│       └── validation/       # FileValidator (MIME sniffing)
│
├── frontend/
│   └── src/app/
│       ├── auth/             # Login, Register components + AuthService
│       ├── products/         # ProductList, ProductDetail components
│       ├── dashboard/        # Seller dashboard, ProductManage, MediaManage
│       ├── profile/          # Profile component
│       ├── shared/           # Guards, Interceptors, Notification service
│       └── core/             # HTTP client wrappers, models
│
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## Resources

- [Spring Boot Microservices Guide](https://spring.io/guides/tutorials/rest/)
- [Spring Security — JWT/OAuth2](https://spring.io/projects/spring-security)
- [MongoDB Documentation](https://www.mongodb.com/docs/)
- [Angular Documentation](https://angular.io/docs)
- [Let's Encrypt — HTTPS](https://letsencrypt.org/getting-started/)
- [Apache Kafka Quickstart](https://kafka.apache.org/quickstart)
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [Netflix Eureka](https://spring.io/projects/spring-cloud-netflix)