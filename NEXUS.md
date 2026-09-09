# Nexus Repository Manager — Setup & Integration

Artifact management for buy-01: Nexus stores Maven artifacts (JARs) for the
7 Spring Boot services and Docker images for all 8 services (7 backend +
`marketplace-ui`), and acts as a caching proxy for Maven Central so builds
don't depend on the public internet at build time.

> **Status**: written and syntax-checked, **not verified end-to-end** — no
> Docker daemon was available in the environment this was built in, so
> nothing here has actually been run against a live Nexus instance. Follow
> the verification commands in each section before trusting this in CI.

## 1. Setup

Nexus runs as a service in `docker-compose.infra.yml`, alongside SonarQube
and Elasticsearch, under the `infra` profile.

```bash
docker compose --profile infra -f docker-compose.yml -f docker-compose.infra.yml \
  --env-file .env up -d nexus
```

- Image: `sonatype/nexus3` — runs as its built-in `nexus` user (uid/gid 200),
  pinned explicitly via `user: "200:200"` in the compose file. **Not root**,
  satisfying the project constraint.
- Data persists in the `nexus_data` named volume (`/nexus-data`).
- Ports: `8081` (UI + Maven repositories), `8082` (Docker hosted repository
  connector — separate port because Docker's registry protocol needs its own
  HTTP connector in Nexus).
- Give it 2-3 minutes on first boot — Nexus is a JVM app and is slow to start.

**Verify:**
```bash
curl -s http://localhost:8081/service/rest/v1/status
# → empty 200 response means Nexus is up
```

### Repositories

Nexus 3 ships 4 Maven repositories by default — nothing to create:

| Repository | Type | Purpose |
|---|---|---|
| `maven-central` | proxy | caches Maven Central so builds don't refetch from the internet every time |
| `maven-releases` | hosted | our published release JARs |
| `maven-snapshots` | hosted | our published snapshot JARs |
| `maven-public` | group | what builds actually point at — merges the three above |

`docker-hosted` does **not** exist by default and is created by the
provisioning script below, together with the Docker Bearer Token realm
(required for `docker login`/push/pull against Nexus) and RBAC.

### Provisioning script

`scripts/nexus-provision.sh` is idempotent (safe to re-run) and:
1. rotates the auto-generated initial admin password to one you choose,
2. creates the `docker-hosted` repository (port 8082, basic auth),
3. enables the `DockerToken` realm,
4. creates a `ci-publisher` role (write access to the hosted repos only) and
   a `developer` role (read-only), and a `ci` user for Jenkins.

```bash
NEXUS_ADMIN_PASSWORD='<choose-a-strong-password>' \
NEXUS_CI_PASSWORD='<choose-a-strong-password>' \
  ./scripts/nexus-provision.sh
```

**Verify:**
```bash
curl -s -u admin:<NEXUS_ADMIN_PASSWORD> http://localhost:8081/service/rest/v1/repositories | jq '.[].name'
# → should list maven-central, maven-releases, maven-snapshots, maven-public, docker-hosted
```

Then in Nexus's UI (`http://localhost:8081`), take the screenshots the
project brief asks for: repository list, the `docker-hosted` repository
config, and the `ci-publisher` / `developer` roles under
Security → Roles.

## 2. Maven integration

`settings.xml` at the repo root is committed (it holds no secrets, only
`${env.X}` placeholders) and is passed explicitly via `-s settings.xml`:

- `<mirrors>` forces **all** dependency resolution through
  `maven-public` — this is what satisfies "resolve dependencies exclusively
  through Nexus" (a `mirrorOf=*` mirror, not per-`pom.xml` repository
  blocks, which is the approach Sonatype itself recommends).
- `<servers>` supplies credentials for `mvn deploy`, read from the
  environment (`NEXUS_CI_USER` / `NEXUS_CI_PASSWORD`) — never hardcoded.

Every service's `pom.xml` (`Backend/*/pom.xml`) got a `<distributionManagement>`
block pointing `nexus-releases` / `nexus-snapshots` at
`${env.NEXUS_URL}/repository/maven-releases|snapshots/`, matching the
`<server>` ids in `settings.xml`.

**Local test (once you have a JDK 21 + Maven and Nexus is up):**
```bash
cd Backend/product
NEXUS_URL=http://localhost:8081 NEXUS_CI_USER=ci NEXUS_CI_PASSWORD='<...>' \
  mvn -s ../../settings.xml -DskipTests deploy
```

## 3. Versioning

Services were left at `0.0.1-SNAPSHOT` in `pom.xml` — that value is never
published as-is. In CI, `versions-maven-plugin` rewrites the version to the
7-character Git commit hash (`env.CURRENT_COMMIT_SHORT_HASH`, already
computed in the `Checkout Source Code` stage and already used to tag Docker
images) immediately before `mvn deploy`:

```bash
mvn -s ../../settings.xml org.codehaus.mojo:versions-maven-plugin:2.16.2:set \
  -DnewVersion=<commit-hash> -DgenerateBackupPoms=false
```

This means a Docker image tag and the Maven artifact version inside it are
always the same string — you can go from a running container back to the
exact JAR (and commit) it was built from, which is the traceability/rollback
requirement in the brief. To pull an older version:

```bash
curl -u ci:<password> -O \
  http://localhost:8081/repository/maven-releases/Product/Product/<commit-hash>/Product-<commit-hash>.jar
```

## 4. Docker integration

```bash
docker login localhost:8082 -u ci
docker tag product:<tag> localhost:8082/product:<tag>
docker push localhost:8082/product:<tag>
docker pull localhost:8082/product:<tag>   # from any machine that can reach Nexus
```

## 5. CI/CD pipeline (Jenkinsfile)

Two new stages, both gated behind the existing `Quality Gate` stage so
nothing broken or failing SonarQube's gate gets published:

- **`Publish Backend Artifacts to Nexus`** — for each changed backend
  service: bump the version to the commit hash, then `mvn deploy`.
- **`Build Docker Images`** (extended) — after `docker compose build`, tag
  and push the image to `nexus:8082` for every changed service (backend
  *and* `marketplace-ui`).

**One manual one-time step required** (can't be scripted from this repo):
create a Jenkins credential of type **"Username with password"** with ID
`nexus-ci-credentials` (username `ci`, password = whatever you set as
`NEXUS_CI_PASSWORD` when provisioning) under *Manage Jenkins → Credentials*.
This mirrors the existing `sonarqube-token` credential already used in the
`Static Code Analysis` stage.

## 6. Security & RBAC

- `admin` — full access, password rotated out of the auto-generated default
  by the provisioning script. Not used by CI or by developers day-to-day.
- `ci-publisher` role / `ci` user — used only by Jenkins. Can push to
  `maven-releases`, `maven-snapshots`, `docker-hosted`. Cannot delete
  repositories, manage users, or touch Nexus configuration.
- `developer` role — read/browse only, for anyone who needs to pull
  artifacts without publishing.

Anonymous access is left at Nexus's default (disabled for write, and the
Docker realm only permits authenticated pulls) — nothing was changed here
beyond enabling the Docker realm itself.

## What's not covered here

- Actually running any of this — verify against a live Nexus + Jenkins
  before relying on it (see the verification commands above).
- TLS for Nexus itself (it sits behind the internal `buy01-network` only,
  same as Elasticsearch/SonarQube in this repo).
- The Jenkins credential creation (manual, one-time, listed above).
