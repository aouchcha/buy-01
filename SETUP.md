# Setup & Run Guide — buy-01

Two independent stacks:
- **The application** (marketplace: gateway, discovery, user/product/media services, Angular UI, MongoDB) — `docker-compose.yml`.
- **CI/CD infra** (Jenkins + SonarQube) — `docker-compose.jenkins.yml`, started with `--profile infra`.

You can run either one alone, or both.

## 1. Prerequisites

- Docker Desktop (or Colima on macOS) running — check with `docker info`.
- `curl` installed locally.

## 2. First-time setup

```bash
cp .env.example .env
```

Fill in every `changeme` value in `.env`. Leave `BACKEND_SLAVE_SECRET` and
`FRONTEND_SLAVE_SECRET` empty — `scripts/run.sh` fills those in automatically
(see step 4).

## 3. Run the application

```bash
./scripts/start.sh
```

This script checks Docker and `.env`, generates the self-signed SSL keystore
if missing, then builds and starts every service, waiting for each to become
healthy.

| Service        | URL                                  |
|----------------|---------------------------------------|
| Marketplace UI | http://localhost:4200 (→ https://localhost:4443) |
| Gateway        | https://localhost:8443               |
| Eureka         | http://localhost:8761                |
| User service   | http://localhost:8081                |
| Media service  | http://localhost:8082                |
| Product service| http://localhost:8083                |

Other modes: `./scripts/start.sh --fresh` (wipe volumes and rebuild),
`./scripts/start.sh --stop`, `./scripts/start.sh --logs`.

## 4. Run the CI/CD infra (Jenkins + SonarQube)

```bash
export USER_ID=$(id -u)   # or: source scripts/get_id.sh
./scripts/run.sh
```

`scripts/run.sh` starts Jenkins + SonarQube (+ their databases and the two
build agents), waits for Jenkins to be reachable, then reads the real
per-node JNLP secrets straight from the running Jenkins controller and syncs
them into `.env` as `BACKEND_SLAVE_SECRET`/`FRONTEND_SLAVE_SECRET` — these
can't be known ahead of time, Jenkins generates them at boot. It only
restarts the stack if a secret actually changed.

| Service   | URL                        | Login                                              |
|-----------|----------------------------|-----------------------------------------------------|
| Jenkins   | `${JENKINS_URL}` (default http://localhost:8080) | `JENKINS_ADMIN_USERNAME` / `JENKINS_ADMIN_PASSWORD` from `.env` |
| SonarQube | http://localhost:9000      | `admin` / `admin` on first boot (forces a password change) |

## 5. First-time SonarQube bootstrap (one-off, manual)

SonarQube can't be fully bootstrapped from code — two tokens have to be
generated from its UI the first time:

1. Log into http://localhost:9000, change the default admin password.
2. **Admin token** (Administration → Security → Users → Tokens, user needs
   "Administer System"): paste it into `.env` as `SONARQUBE_ADMIN_TOKEN`.
   Used only by `scripts/provision-sonar-webhook.sh`, which then runs
   automatically on every `docker compose up` to register the Jenkins
   webhook — no manual webhook setup needed after this.
3. **Analysis token** (same Tokens page, can be a more restricted user):
   paste it into `.env` as `SONARQUBE_TOKEN`. Used by the Jenkinsfile to run
   the actual scans. This token's user needs "Execute Analysis" rights (and
   "Create Projects" the first time a project like `buy01-frontend` is
   scanned) — if a build fails with `You're not authorized to run analysis`,
   this is the token/permission to check first.
4. Re-run `./scripts/run.sh` (or restart just the provisioner:
   `docker compose --profile infra -f docker-compose.yml -f docker-compose.jenkins.yml up -d sonarqube-webhook-provisioner`).

## 6. Stop everything

```bash
./scripts/start.sh --stop
docker compose --profile infra -f docker-compose.yml -f docker-compose.jenkins.yml down
```

`scripts/clear.sh` also exists but wipes **all** Docker state on the machine
(every container/image/volume/network, not just this project) — last resort
only.

## Troubleshooting

- **Jenkins boot-loops with `UnknownConfiguratorException: ... root element: nodes`**
  — plugin versions drifted (`plugins.txt` used to pin everything to
  `:latest`) or the `jenkins_home` volume has stale state. Plugins are now
  pinned to fixed versions and `jenkins.yaml` is bind-mounted (not baked into
  the image), so a plain `docker compose up` always applies the committed
  config.
- **Frontend `ng test` fails with `SchemaValidationException`** — the new
  Vitest-based test builder (`@angular/build:unit-test`) doesn't accept the
  old Karma flag `--watch=false`; use `--no-watch`.
- **SonarQube: `No LCOV files were found using coverage/lcov.info`** — the
  builder writes coverage to `coverage/marketplace-ui/lcov.info` (nested
  under the project name), not `coverage/lcov.info`.
- **SonarQube TypeScript sensor: `Argument for '--module' option must be...`**
  — `tsconfig.json` uses `"module": "preserve"` (TS 5.4+), which SonarQube's
  bundled TypeScript analyzer doesn't recognize yet. `marketplace-ui/tsconfig.sonar.json`
  overrides it to `es2022` for analysis only; the real Angular build is
  untouched.
- **Backend: `No report imported` for JaCoCo** — `jacoco:report` must run on
  its default `verify` phase (not `test`, which races with Surefire); the
  Jenkinsfile runs `mvn clean verify`, not `mvn clean package`, for that reason.
