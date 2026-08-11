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

## 5. First-time SonarQube bootstrap (one-off)

SonarQube can't be fully bootstrapped from code: its admin password is set
interactively on first login, and the two tokens the pipeline needs can only be
minted once that password exists.

1. Log into http://localhost:9000 and change the default admin password. Put it
   in `.env` as `SONARQUBE_ADMIN_PASSWORD`.
2. Run `./scripts/provision-sonar-tokens.sh`. It mints both tokens with the
   correct **Type** and writes them into `.env`, keeping the previous values in
   `.env.bak`. If `SONARQUBE_ADMIN_PASSWORD` is blank it prompts instead.
3. Re-run `./scripts/run.sh` (or restart just the provisioner:
   `docker compose --profile infra -f docker-compose.yml -f docker-compose.jenkins.yml up -d sonarqube-webhook-provisioner`).

### Why the token Type matters

The Type of each token matters more than its permissions, and getting it wrong
is the most common way to break this pipeline:

| `.env` key | Type | Why that Type |
|---|---|---|
| `SONARQUBE_TOKEN` | **Global Analysis Token** | The pipeline analyses one project per service (`buy01-discovery`, `buy01-gateway`, `buy01-media`, `buy01-product`, `buy01-user`, `buy01-frontend`) and auto-provisions them on first scan. A *Project* Analysis Token is bound to a single key, so every other key fails with `You're not authorized to run analysis`. |
| `SONARQUBE_ADMIN_TOKEN` | **User Token** on an admin | `scripts/provision-sonar-webhook.sh` calls `/api/webhooks/*`, which rejects *any* analysis token with `403 Insufficient privileges`. |

> Do **not** use the token from the project dashboard's "Provide a token /
> Run analysis" wizard for `SONARQUBE_TOKEN`. That wizard issues a *Project*
> Analysis Token scoped to the one project you opened it from.

To mint them by hand instead: Administration → Security → Users → (admin) →
Tokens, and pick the Type from the dropdown.

Two more things this stack needs, both easy to lose:

- **`.env` must be mode `0644`**, not `0600`. Docker runs rootless here, so the
  host uid owning `.env` maps to uid 0 inside the agents while the agent process
  is uid 1000 — it matches only the "other" permission bits. At `0600` the build
  and deploy stages die with `open /home/jenkins/.env: permission denied`.
  Confidentiality still comes from `/home/<user>` being `0700`.
- **Exactly one SonarQube webhook.** `provision-sonar-webhook.sh` owns the one
  named `jenkins-buy01` and keeps its secret in step with
  `SONARQUBE_WEBHOOK_SECRET`. A second webhook hitting the same Jenkins URL with
  a different secret aborts the build outright — the Sonar plugin treats a failed
  HMAC check as fatal, not as something to skip: `Pipeline aborted due to failed
  webhook verification`. Delete any webhook you added by hand.

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
