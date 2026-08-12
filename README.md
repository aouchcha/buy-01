# buy-01 — E-Commerce Microservices Platform

An end-to-end e-commerce marketplace built with **Spring Boot microservices** on the backend and **Angular** on the frontend. Clients browse products; sellers manage their own catalog and product images. The platform is fully containerized and ships with a **Jenkins CI/CD pipeline** and **SonarQube** static analysis integration.

---

## Table of Contents

- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Environment Variables](#environment-variables)
- [Running Locally](#running-locally)
- [API Overview](#api-overview)
- [Security](#security)
- [Testing](#testing)
- [CI/CD Pipeline (Jenkins)](#cicd-pipeline-jenkins)
- [Static Code Analysis (SonarQube)](#static-code-analysis-sonarqube)
- [Deployment & Rollback](#deployment--rollback)
- [Notifications](#notifications)
- [Scripts](#scripts)

---

## Architecture

The system is composed of independently deployable Spring Boot services fronted by a gateway, plus an Angular single-page application:

```
                        ┌───────────────────┐
                        │   marketplace-ui   │  (Angular SPA, Nginx, HTTPS)
                        └─────────┬──────────┘
                                  │
                        ┌─────────▼──────────┐
                        │      gateway        │  (Spring Cloud Gateway, JWT, CORS)
                        └─────────┬──────────┘
                                  │
              ┌───────────────────┼───────────────────┐
              │                   │                   │
        ┌─────▼─────┐      ┌──────▼──────┐      ┌─────▼─────┐
        │   user    │      │   product    │      │   media    │
        │  service  │      │   service    │      │  service   │
        └─────┬─────┘      └──────┬──────┘      └─────┬─────┘
              │                   │                   │
        ┌─────▼─────┐      ┌──────▼──────┐      ┌─────▼─────┐
        │users-mongo│      │products-mongo│      │media-mongo │
        └───────────┘      └─────────────┘      └───────────┘

                   ┌───────────────────────┐
                   │      discovery         │  (Eureka service registry)
                   └───────────────────────┘

                   ┌───────────────────────┐
                   │  Kafka + Zookeeper     │  (async events between services)
                   └───────────────────────┘
```

- **discovery** — Eureka server for service registration and discovery.
- **gateway** — Single entry point; routes external traffic, applies CORS, JWT propagation, and cross-cutting filters.
- **user** — Authentication (register/login), profiles, roles (`CLIENT`, `SELLER`).
- **product** — Product CRUD, ownership enforcement, consumes media events to attach `imageUrls`.
- **media** — Image upload/download via Cloudflare R2 object storage, MIME/size validation (≤ 2 MB).
- **marketplace-ui** — Angular SPA with route guards, HTTP interceptors, and reactive forms.
- **Kafka** — Backbone for asynchronous events (e.g. product/media/user lifecycle events) so services stay decoupled.
- Each service maintains its **own MongoDB database** (database-per-service), and exposes `/actuator/health` for observability.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java, Spring Boot, Spring Cloud Gateway, Spring Security, Spring Data MongoDB |
| Service Discovery | Netflix Eureka |
| Messaging | Apache Kafka + Zookeeper |
| Database | MongoDB (one instance per service) |
| Object Storage | Cloudflare R2 (S3-compatible) for media files |
| Frontend | Angular (standalone components, signals, Reactive Forms) |
| Auth | JWT, propagated from Gateway to downstream services |
| Containerization | Docker, Docker Compose |
| CI/CD | Jenkins (declarative pipeline, distributed backend/frontend agents) |
| Code Quality | SonarQube |
| Web Server (frontend) | Nginx, self-signed TLS for local HTTPS |

---

## Project Structure

```
buy-01
├── Backend
│   ├── discovery/        # Eureka service registry
│   ├── gateway/           # API Gateway, JWT filter, security config
│   ├── user/               # Auth, profiles, roles
│   ├── product/           # Product CRUD, ownership checks
│   ├── media/               # Image upload/validation, R2 storage
│   └── jenkins/            # Jenkins master + backend/frontend agent images
├── marketplace-ui/         # Angular SPA
├── scripts/                 # Helper shell scripts (see Scripts section)
├── docker-compose.yml        # Application services
├── docker-compose.jenkins.yml # Infra: Mongo, Kafka, Jenkins, SonarQube
└── Jenkinsfile              # CI/CD pipeline definition
```

---

## Prerequisites

- Docker & Docker Compose
- Java 21+ and Maven (for local backend development outside containers)
- Node.js + npm (for local Angular development)
- A `.env` file at the project root (see below)

---

## Environment Variables

Create a `.env` file at the project root (see `.env.example` for the full list). Key variables include:

```env
# MongoDB
DB_USERNAME=
DB_PASSWORD=
USERS_DB_NAME=
PRODUCTS_DB_NAME=
MEDIA_DB_NAME=
USER_DB_URI=
PRODUCT_DB_URI=
MEDIA_DB_URI=

# JWT
JWT_SECRET=
JWT_EXPIRATION=

# CORS
CORS_ALLOWED_ORIGIN=http://localhost:4200

# Cloudflare R2 (media storage)
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
R2_BUCKET=
R2_ENDPOINT=
R2_PUBLIC_URL=

# Jenkins / SonarQube (CI infra)
BACKEND_SLAVE_SECRET=
FRONTEND_SLAVE_SECRET=
SONARQUBE_DB_USERNAME=
SONARQUBE_DB_PASSWORD=
SONARQUBE_DB_NAME=
SONARQUBE_DB_URI=
USER_ID=
```

> Never commit `.env` — it is git-ignored. Use `.env.example` as the template.

---

## Running Locally

### 1. Generate local TLS certificates (frontend HTTPS)

```bash
chmod +x scripts/create_Self-Signed-Certificate.sh
./scripts/create_Self-Signed-Certificate.sh
```

### 2. Start infrastructure (MongoDB instances, Kafka/Zookeeper)

```bash
docker compose --profile infra -f docker-compose.jenkins.yml  up -d 
```



### 4. Access the app

| Service | URL |
|---|---|
| Frontend (Angular) | https://localhost:4443 |
| Gateway | https://localhost:8443 |
| Eureka Dashboard | http://localhost:8761 |

### Stopping / cleaning up

```bash
./scripts/clear.sh
```

---

## API Overview

All external traffic goes through the **gateway** (`https://localhost:8443`), which routes to the appropriate service and enforces JWT auth.

**User Service**
- `POST /auth/register` — register as `CLIENT` or `SELLER`
- `POST /auth/login` — returns JWT
- `GET /me` / `PUT /me` — profile management (sellers can update avatar via Media Service)

**Product Service**
- `GET /products`, `GET /products/{id}` — public
- `POST /products`, `PUT /products/{id}`, `DELETE /products/{id}` — seller-only, ownership enforced

**Media Service**
- `POST /media/images` — seller-only, validates `image/*` MIME type and 2 MB limit
- `GET /media/images/{id}` — serves image with caching headers
- `DELETE /media/images/{id}` — seller must own the media

All services expose **`/actuator/health`** for liveness/readiness checks.

---

## Security

- **JWT** issued by the User Service, validated and propagated at the Gateway (`JwtAuthFilter`, `JwtFilter`, `SecurityConfig`).
- **BCrypt** password hashing — passwords are never exposed in responses.
- **Ownership enforcement** — sellers can only modify/delete their own products and media (`sellerId == auth.subject`).
- **File validation** — Media Service validates MIME type via content sniffing (Apache Tika) and enforces the 2 MB size limit, rejecting non-image payloads.
- **CORS** — enforced at the Gateway via `CORS_ALLOWED_ORIGIN`.
- **HTTPS** — Gateway and frontend both terminate TLS locally via a self-signed certificate/keystore (`keystore.p12`, `ssl/`); use Let's Encrypt in production.
- **Global exception handling** — each service has a `GlobalExceptionHandler` mapping errors to proper status codes (400/401/403/404) instead of leaking unhandled 5xx errors.

---

## Testing

- **Backend** — JUnit + Mockito per service (`ProductServiceTest`, `MediaServiceTest`, `UsersServiceTest`, controller tests with `@WebMvcTest`, etc.), run via `mvn clean package`.
- **Frontend** — Jasmine/Karma specs alongside each component/service (`*.spec.ts`), run via `npm test -- --watch=false --no-progress`.
- The Jenkins pipeline **fails the build** if any test fails, and publishes JUnit results via `junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'`.

---

## CI/CD Pipeline (Jenkins)

The `Jenkinsfile` implements a declarative pipeline with **distributed agents** (`backend` and `frontend` labels) and **change-based execution** — only services actually touched by a commit are built, tested, analyzed, and redeployed.

### Pipeline stages

1. **Checkout Source Code** — clones the repo on a `backend` agent, stashes the workspace so the `frontend` agent can reuse it without re-cloning.
2. **Detect Which Services Changed** — runs `scripts/detect-changed-services.sh` against the diff (PR target branch or `HEAD~1`) to compute `CHANGED_SERVICE_NAMES`.
3. **Build And Test** *(parallel)*:
   - **Backend Services** — for each changed backend service: `mvn clean package` + JUnit report publishing.
   - **Frontend Application** — if `marketplace-ui` changed: `npm ci`, `npm test`, `npm run build -- --configuration production`.
4. **Static Code Analysis** *(parallel)* — SonarQube scan per changed backend service (`mvn sonar:sonar -Dsonar.projectKey=buy01-<service>`) and `sonar-scanner` for the frontend, both wrapped in `withSonarQubeEnv('sonarqube-server')`.
5. **Quality Gate** — blocks the pipeline (5-minute timeout, `abortPipeline: true`) until SonarQube reports pass/fail.
6. **Build Docker Images** — builds a Docker image per changed service, tagged with the short commit SHA (`IMAGE_TAG=<7-char-sha>`).
7. **Deploy To Main Environment** — on the `main` branch only: pulls the CI `.env`, then `docker compose ... up -d --no-deps` for the application services.

### Triggers

- Build triggers are configured in the Jenkins job (e.g. GitHub webhook / poll SCM) to start automatically on new commits.
- Parameterized/matrix-style builds are achieved through the per-service `each { serviceName -> ... }` loops driven by change detection.

### Notifications

- The pipeline's `post { success / failure }` blocks send **email notifications** to the configured recipients, including the affected services and a link to the full build log/console output.

---

## Static Code Analysis (SonarQube)

SonarQube runs via Docker Compose (`docker-compose.jenkins.yml`, `sonarqube` + `sonarqube-db` services):

```bash
docker compose --profile infra -f docker-compose.jenkins.yml --env-file .env up -d sonarqube-db sonarqube
```

- Dashboard: `http://localhost:9001` (mapped from container port 9000).
- Each backend service is registered as its **own SonarQube project** (`buy01-discovery`, `buy01-gateway`, `buy01-user`, `buy01-product`, `buy01-media`), and the frontend as `marketplace-ui`, so quality metrics are tracked independently per service.
- Authentication to SonarQube from Jenkins uses a stored credential (`sonarqube-token`).
- The **Quality Gate** stage in the Jenkinsfile aborts the pipeline if a service fails its gate (major vulnerabilities, code smells, coverage/duplication thresholds), preventing low-quality or insecure code from reaching the deploy stage.
- Recommended process: configure a GitHub webhook (or GitHub Actions) so PRs/branches trigger analysis automatically, and require the quality gate + a code review approval before merging.

---

## Deployment & Rollback

- Deployment is driven by the **Deploy To Main Environment** stage, restricted to the `main` branch, and only redeploys the services affected by the change (`docker compose up -d --no-deps <service>`).
- Each image is tagged by **commit SHA** (`IMAGE_TAG`), so a rollback is a matter of re-running deployment with a previous known-good `IMAGE_TAG` (or reverting the commit and letting the pipeline redeploy), since older tagged images remain available in the image registry/build cache.
- Health checks (`/actuator/health` on each service, Mongo `healthcheck` in Compose) gate service startup ordering (`depends_on: condition: service_healthy`), reducing the chance of promoting a broken deployment.

---

## Notifications

Build and deployment results are emailed automatically:

- ✅ **Success** — recipients get the list of affected services and a link to the build log.
- ❌ **Failure** — recipients get a link directly to the console output for debugging.

Recipients are configured via the `NOTIFICATION_EMAIL_RECIPIENT` environment variable in the `Jenkinsfile`.

---

## Scripts

| Script | Purpose |
|---|---|
| `scripts/start.sh` | Bring up the full stack |
| `scripts/clear.sh` | Tear down containers/volumes |
| `scripts/check.sh` | Health/status checks across services |
| `scripts/create_Self-Signed-Certificate.sh` | Generates local TLS cert/key for HTTPS |
| `scripts/detect-changed-services.sh` | Diffs commits to determine which services changed (used by Jenkins) |
| `scripts/get_id.sh` | Helper to fetch container/user IDs (e.g. for Docker socket permissions) |

---

## Evaluation Checklist

- ⚙️ **Functionality** — role-based flows (CLIENT/SELLER), product & media CRUD, browsing
- 🔐 **Security** — JWT, BCrypt, ownership checks, CORS, TLS
- 🧩 **Architecture** — clean service boundaries, Eureka discovery, Kafka events, Gateway
- 🚫 **Reliability** — global exception handling, health checks, no unhandled 5xx
- 🎨 **UX** — responsive Angular UI, guards/interceptors, inline validation
- 🧪 **CI/CD** — automated build → test → analyze → deploy, quality gates, rollback via tagged images, email notifications