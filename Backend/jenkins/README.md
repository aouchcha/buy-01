# Jenkins CI/CD Guide — E-commerce Microservices (Spring Boot + Angular)

Full implementation: controller, backend/frontend agents, JCasC, plugins, webhook wiring,
service-detection script, and the Jenkinsfile — every variable named for what it holds, no
abbreviations, with comments explaining what each part does.

Repo layout:

```
your-ecommerce-repo/
├── order-service/                 (Spring Boot, own Dockerfile)
├── payment-service/               (Spring Boot, own Dockerfile)
├── frontend/                      (Angular, own Dockerfile)
├── docker-compose.yml             (APPLICATION services only)
├── docker-compose.jenkins.yml     (Jenkins controller + agents — kept separate on purpose)
├── Jenkinsfile
├── scripts/
│   └── detect-changed-services.sh
└── jenkins/
    ├── controller/
    │   ├── Dockerfile
    │   ├── plugins.txt
    │   └── jenkins.yaml            (JCasC)
    ├── agent-backend/
    │   └── Dockerfile
    └── agent-frontend/
        └── Dockerfile
```

Why two compose files: `docker-compose.yml` is the file Docker Compose uses automatically when
you don't specify `-f`. Keeping only real application services in it means any command run
without extra flags (including the change-detection script) naturally only ever "sees"
`order-service`, `payment-service`, `frontend` — never `jenkins-controller` or the agents. You
start everything together with:

```bash
docker compose -f docker-compose.yml -f docker-compose.jenkins.yml up -d --build
```

---

## PHASE 2 — Controller Image

### `jenkins/controller/Dockerfile`

```dockerfile
FROM jenkins/jenkins:lts-jdk17

# Skip the interactive setup wizard — JCasC (below) configures everything it would have asked for
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"
ENV CASC_JENKINS_CONFIG="/var/jenkins_home/casc_configs/jenkins.yaml"

USER root

# Install the Docker CLI so pipeline stages running here can run docker/docker compose commands
RUN apt-get update && apt-get install -y \
    ca-certificates curl gnupg lsb-release && \
    install -m 0755 -d /etc/apt/keyrings && \
    curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian bookworm stable" \
    > /etc/apt/sources.list.d/docker.list && \
    apt-get update && apt-get install -y docker-ce-cli docker-compose-plugin && \
    rm -rf /var/lib/apt/lists/*

USER jenkins

# Install every plugin listed in plugins.txt at image-build time — no Plugin Manager UI needed
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# Bake the JCasC config into the image as well
COPY jenkins.yaml /var/jenkins_home/casc_configs/jenkins.yaml
```

The controller does not get Maven or Node installed — it only orchestrates; the two agents do
the actual building.

### `jenkins/controller/plugins.txt`

```
git:latest
github:latest
github-branch-source:latest
workflow-aggregator:latest
pipeline-stage-view:latest
credentials-binding:latest
configuration-as-code:latest
junit:latest
mailer:latest
matrix-auth:latest
job-dsl:latest
```

- `git`, `github`, `github-branch-source` → clone the repo, discover branches, receive webhooks.
- `workflow-aggregator` → Pipeline + Multibranch Pipeline support.
- `credentials-binding` → lets secrets be injected from environment variables instead of typed
  into the UI.
- `configuration-as-code` → the plugin that actually reads `jenkins.yaml` on startup.
- `junit` → turns JUnit XML output into a readable pass/fail report in Jenkins.
- `mailer` → lets Jenkins send email notifications over SMTP (used by the `mail` step in the
  Jenkinsfile, and reads its server settings from the JCasC config below).
- `matrix-auth` → lets JCasC define user permissions.
- `job-dsl` → lets JCasC create the Multibranch Pipeline job itself (see below), instead of you
  clicking "New Item."

Swap `latest` for pinned version numbers once things work, so a rebuild months from now
installs the exact same versions.

### `jenkins/controller/jenkins.yaml` (JCasC)

```yaml
jenkins:
  systemMessage: "Jenkins controller — configured entirely as code"
  numExecutors: 0        # the controller does no building itself; all work goes to agents
  securityRealm:
    local:
      allowsSignup: false
      users:
        - id: "${JENKINS_ADMIN_USERNAME}"
          password: "${JENKINS_ADMIN_PASSWORD}"
  authorizationStrategy:
    globalMatrix:
      permissions:
        - "Overall/Administer:${JENKINS_ADMIN_USERNAME}"

  nodes:
    - permanent:
        name: "backend-agent"
        labelString: "backend"
        numExecutors: 2
        remoteFS: "/home/jenkins/agent"
        launcher:
          inbound:
            workDirSettings:
              disabled: false
    - permanent:
        name: "frontend-agent"
        labelString: "frontend"
        numExecutors: 2
        remoteFS: "/home/jenkins/agent"
        launcher:
          inbound:
            workDirSettings:
              disabled: false

credentials:
  system:
    domainCredentials:
      - credentials:
          - usernamePassword:
              scope: GLOBAL
              id: "github-access-token"
              username: "${GITHUB_USERNAME}"
              password: "${GITHUB_TOKEN}"
              description: "GitHub token used to check out code and report build status"
          - usernamePassword:
              scope: GLOBAL
              id: "smtp-credentials"
              username: "${SMTP_USERNAME}"
              password: "${SMTP_PASSWORD}"
              description: "SMTP login used by Jenkins to send build notification emails"

unclassified:
  mailer:
    smtpHost: "${SMTP_HOST}"
    smtpPort: "${SMTP_PORT}"
    useSsl: false
    charset: "UTF-8"
    authentication:
      username: "${SMTP_USERNAME}"
      password: "${SMTP_PASSWORD}"

  location:
    url: "${JENKINS_URL}"
    adminAddress: "${SMTP_USERNAME}"

jobs:
  - script: >
      multibranchPipelineJob('ecommerce-platform') {
        branchSources {
          github {
            id('ecommerce-platform-source')
            repoOwner('your-github-username')
            repository('your-ecommerce-repo')
            credentialsId('github-access-token')
          }
        }
        orphanedItemStrategy {
          discardOldItems {
            numToKeep(20)
          }
        }
        triggers {
          periodicFolderTrigger {
            interval('1440')   // once-a-day safety-net poll; the webhook handles real-time triggers
          }
        }
      }
```

What each part does: creates the admin user from environment variables (no wizard), declares
both agents by name (Jenkins auto-generates a connection secret for each), stores your GitHub
token and SMTP login as named credentials sourced from environment variables, configures the
global SMTP server settings so any `mail` step in any pipeline can send through it, and —
through the `job-dsl` plugin — creates the Multibranch Pipeline job itself, pointed at your
repo, so "New Item" is never clicked manually.

A note on the `mailer` block above: field names for plugin-specific JCasC sections can shift
slightly between plugin versions. If Jenkins refuses to boot with a schema error on this block,
open **Manage Jenkins → Configuration as Code → View Configuration** (or its "Schema" link) once
you're in — it shows the exact current field names for whatever plugin version you have
installed, so you can correct any mismatch quickly.

If you're using Gmail as your SMTP provider: `SMTP_HOST` is `smtp.gmail.com`, `SMTP_PORT` is
`587`, and `SMTP_PASSWORD` must be a Google **App Password** (Google Account → Security → App
Passwords), not your normal login password — Gmail rejects plain-password SMTP logins once
2-Step Verification is on, which it is by default on most accounts.

---

## PHASE 3 — Agent Images

### `jenkins/agent-backend/Dockerfile`

```dockerfile
FROM jenkins/inbound-agent:latest-jdk17

USER root
RUN apt-get update && apt-get install -y \
    maven ca-certificates curl gnupg lsb-release && \
    install -m 0755 -d /etc/apt/keyrings && \
    curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian bookworm stable" \
    > /etc/apt/sources.list.d/docker.list && \
    apt-get update && apt-get install -y docker-ce-cli docker-compose-plugin && \
    rm -rf /var/lib/apt/lists/*
USER jenkins
```

### `jenkins/agent-frontend/Dockerfile`

```dockerfile
FROM jenkins/inbound-agent:latest-jdk17

USER root
RUN apt-get update && apt-get install -y curl ca-certificates gnupg && \
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y nodejs && \
    npm install -g @angular/cli && \
    install -m 0755 -d /etc/apt/keyrings && \
    curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian bookworm stable" \
    > /etc/apt/sources.list.d/docker.list && \
    apt-get update && apt-get install -y docker-ce-cli docker-compose-plugin && \
    rm -rf /var/lib/apt/lists/*
USER jenkins
```





Both connect **outbound** to the controller — no inbound port, no exposure needed for either
agent.

### Getting the agent connection secrets (one-time, after first boot)

Jenkins generates a secret per agent the first time it starts. Run this once after your first
`docker compose up`, then save the two values into `.env` — they stay valid on any machine from
then on:

```bash
docker compose -f docker-compose.jenkins.yml exec jenkins-controller \
  java -jar jenkins-cli.jar -s http://localhost:8080/ groovy = <<< \
  "println(jenkins.model.Jenkins.get().getComputer('backend-agent').getJnlpMac())"
```

Repeat with `'frontend-agent'` for the second value.

---

## PHASE 5 — Service Change Detection Script

### `scripts/detect-changed-services.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

# ============================================================================
# Figures out which microservices had code changes between two git commits,
# so the Jenkins pipeline only builds, tests, and deploys what actually
# changed.
#
# The list of valid service names is read directly from docker-compose.yml
# (the file that contains ONLY real application services — Jenkins itself
# lives in the separate docker-compose.jenkins.yml file), so this script
# never needs to be edited by hand when a service is added or removed.
# ============================================================================

APPLICATION_COMPOSE_FILE="docker-compose.yml"

# The commit/branch to compare FROM. Defaults to the previous commit.
COMPARE_FROM_REFERENCE="${1:-HEAD~1}"

# The commit/branch to compare TO. Defaults to the current commit.
COMPARE_TO_REFERENCE="${2:-HEAD}"

# ---- Step 1: get the list of real application service names ----
# "docker compose config --services" reads docker-compose.yml and prints the
# service names defined in it (order-service, payment-service, frontend).
# It never sees jenkins-controller, backend-agent, or frontend-agent,
# because those live in a different file that isn't passed in here.
APPLICATION_SERVICE_NAMES=($(docker compose -f "$APPLICATION_COMPOSE_FILE" config --services))

# ---- Step 2: get every file path that changed between the two commits ----
if ! git rev-parse "$COMPARE_FROM_REFERENCE" >/dev/null 2>&1; then
  # There is no earlier commit to compare against (first commit ever, or a
  # shallow clone with no history). Safest choice: treat every service as
  # changed so nothing is silently skipped.
  echo "No previous commit found to compare against — treating all services as changed."
  printf '%s\n' "${APPLICATION_SERVICE_NAMES[@]}"
  exit 0
fi

CHANGED_FILE_PATHS=$(git diff --name-only "$COMPARE_FROM_REFERENCE" "$COMPARE_TO_REFERENCE")

# ---- Step 3: for each real service, check whether any changed file lives inside its folder ----
for SERVICE_NAME in "${APPLICATION_SERVICE_NAMES[@]}"; do
  if echo "$CHANGED_FILE_PATHS" | grep -q "^${SERVICE_NAME}/"; then
    echo "$SERVICE_NAME"
  fi
done
```

Every variable name says what it holds. Nothing here needs to be edited when you add a fourth
microservice — just add it to `docker-compose.yml`, and step 1 of this script picks it up
automatically on the next run.

---

## PHASE 6–7 — The Jenkinsfile

```groovy
pipeline {
    agent none

    environment {
        // Short git commit hash — used to tag Docker images so every build
        // produces a uniquely identifiable image, e.g. order-service:a1b2c3d
        CURRENT_COMMIT_SHORT_HASH = "${env.GIT_COMMIT.take(7)}"

        // Who receives build status emails. Edit this directly — it's the
        // one line to change if the team's contact address changes.
        NOTIFICATION_EMAIL_RECIPIENT = "your-team@example.com"
    }

    stages {

        stage('Checkout Source Code') {
            agent { label 'backend' }
            steps {
                checkout scm
                // Save the checked-out code so later stages running on a
                // DIFFERENT agent (frontend-agent) can reuse it without
                // cloning the repository a second time.
                stash name: 'source-code', includes: '**'
            }
        }

        stage('Detect Which Services Changed') {
            agent { label 'backend' }
            steps {
                unstash 'source-code'
                script {
                    // For a pull request, compare against its target branch.
                    // Otherwise compare against the previous commit on this branch.
                    def commitToCompareAgainst = env.CHANGE_TARGET ? "origin/${env.CHANGE_TARGET}" : "HEAD~1"

                    def detectionScriptOutput = sh(
                        script: "chmod +x scripts/detect-changed-services.sh && ./scripts/detect-changed-services.sh ${commitToCompareAgainst} HEAD",
                        returnStdout: true
                    ).trim()

                    // Store the result as a comma-separated string so later
                    // stages — which may run on a different agent — can read
                    // it back from the environment.
                    env.CHANGED_SERVICE_NAMES = detectionScriptOutput.replaceAll("\n", ",")
                    echo "Services changed in this commit: ${env.CHANGED_SERVICE_NAMES}"
                }
            }
        }

        stage('Build And Test') {
            parallel {

                stage('Backend Services') {
                    agent { label 'backend' }
                    when {
                        expression {
                            env.CHANGED_SERVICE_NAMES.contains('order-service') ||
                            env.CHANGED_SERVICE_NAMES.contains('payment-service')
                        }
                    }
                    steps {
                        unstash 'source-code'
                        script {
                            def allChangedServiceNames = env.CHANGED_SERVICE_NAMES.split(',')
                            def changedBackendServiceNames = allChangedServiceNames.findAll {
                                it == 'order-service' || it == 'payment-service'
                            }

                            changedBackendServiceNames.each { serviceName ->
                                dir(serviceName) {
                                    sh 'mvn clean package'
                                    junit 'target/surefire-reports/*.xml'
                                }
                            }
                        }
                    }
                }

                stage('Frontend Application') {
                    agent { label 'frontend' }
                    when {
                        expression { env.CHANGED_SERVICE_NAMES.contains('frontend') }
                    }
                    steps {
                        unstash 'source-code'
                        dir('frontend') {
                            sh 'npm ci'
                            sh 'ng test --watch=false --browsers=ChromeHeadless'
                            sh 'ng build --configuration production'
                        }
                    }
                }
            }
        }

        stage('Build Docker Images') {
            agent { label 'backend' }
            when { expression { env.CHANGED_SERVICE_NAMES?.trim() } }
            steps {
                unstash 'source-code'
                script {
                    def allChangedServiceNames = env.CHANGED_SERVICE_NAMES.split(',')

                    allChangedServiceNames.each { serviceName ->
                        sh "docker build -t ${serviceName}:${CURRENT_COMMIT_SHORT_HASH} ./${serviceName}"
                    }
                }
            }
        }

        stage('Deploy To Main Environment') {
            agent { label 'backend' }
            when { branch 'main' }
            steps {
                script {
                    def allChangedServiceNames = env.CHANGED_SERVICE_NAMES.split(',')

                    allChangedServiceNames.each { serviceName ->
                        // No -f flag needed: docker-compose.yml is the default
                        // file, and it only contains application services.
                        sh "IMAGE_TAG=${CURRENT_COMMIT_SHORT_HASH} docker compose up -d ${serviceName}"
                    }
                }
            }
        }

        stage('Check Deployed Services Are Healthy') {
            agent { label 'backend' }
            when { branch 'main' }
            steps {
                script {
                    def allChangedServiceNames = env.CHANGED_SERVICE_NAMES.split(',')

                    allChangedServiceNames.each { serviceName ->
                        def healthCheckExitCode = sh(
                            script: "curl -sf http://${serviceName}:8080/actuator/health",
                            returnStatus: true
                        )
                        if (healthCheckExitCode != 0) {
                            error("Health check failed for service: ${serviceName}")
                        }

                        // Only after confirming the new version is genuinely
                        // healthy do we mark this image as "known good" — the
                        // version we fall back to if a future deploy fails.
                        sh "docker tag ${serviceName}:${CURRENT_COMMIT_SHORT_HASH} ${serviceName}:previous-good"
                    }
                }
            }
        }
    }

    post {
        success {
            mail(
                to: "${NOTIFICATION_EMAIL_RECIPIENT}",
                subject: "SUCCESS: ${env.JOB_NAME} build #${env.BUILD_NUMBER} on branch ${env.BRANCH_NAME}",
                body: "Services affected: ${env.CHANGED_SERVICE_NAMES ?: 'none'}\n\nFull build log: ${env.BUILD_URL}"
            )
        }
        failure {
            mail(
                to: "${NOTIFICATION_EMAIL_RECIPIENT}",
                subject: "FAILED: ${env.JOB_NAME} build #${env.BUILD_NUMBER} on branch ${env.BRANCH_NAME}",
                body: "Check the console output for details: ${env.BUILD_URL}console"
            )
            script {
                def deploymentAlreadyHappenedOnThisRun = (env.BRANCH_NAME == 'main' && env.CHANGED_SERVICE_NAMES?.trim())
                if (deploymentAlreadyHappenedOnThisRun) {
                    echo "Rolling back to the last known-good image for each affected service..."
                    def allChangedServiceNames = env.CHANGED_SERVICE_NAMES.split(',')

                    allChangedServiceNames.each { serviceName ->
                        sh "IMAGE_TAG=previous-good docker compose up -d ${serviceName} || true"
                    }
                }
            }
        }
    }
}
```

Compared to naming things `svc` or `arr`, every variable here tells you what's inside it just
by reading its name — `allChangedServiceNames`, `changedBackendServiceNames`,
`commitToCompareAgainst`, `healthCheckExitCode`. If you rename a stage or add a service later,
nothing here needs decoding first.

One fix baked in versus a naive version: the `previous-good` tag is only applied **after** the
health check passes, not right after the build. That way, if a deploy fails, rollback always
points at a version that was actually confirmed healthy — not just "the last thing that got
built."

---

## docker-compose.yml (application services only)

```yaml
services:
  order-service:
    build: ./order-service
    image: order-service:${IMAGE_TAG:-latest}
    ports:
      - "8081:8080"

  payment-service:
    build: ./payment-service
    image: payment-service:${IMAGE_TAG:-latest}
    ports:
      - "8082:8080"

  frontend:
    build: ./frontend
    image: frontend:${IMAGE_TAG:-latest}
    ports:
      - "4200:80"
```

The `image: name:${IMAGE_TAG:-latest}` line on each service is what lets the Jenkinsfile pick
which tag gets deployed just by setting `IMAGE_TAG` before calling `docker compose up -d`,
without ever editing this file.

## docker-compose.jenkins.yml (Jenkins infrastructure only)

```yaml
services:
  jenkins-controller:
    build: ./jenkins/controller
    ports:
      - "8080:8080"
    volumes:
      - jenkins_home:/var/jenkins_home
      - /var/run/docker.sock:/var/run/docker.sock
    env_file: .env

  backend-agent:
    build: ./jenkins/agent-backend
    environment:
      JENKINS_URL: http://jenkins-controller:8080
      JENKINS_AGENT_NAME: backend-agent
      JENKINS_SECRET: ${BACKEND_AGENT_SECRET}
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    depends_on:
      - jenkins-controller

  frontend-agent:
    build: ./jenkins/agent-frontend
    environment:
      JENKINS_URL: http://jenkins-controller:8080
      JENKINS_AGENT_NAME: frontend-agent
      JENKINS_SECRET: ${FRONTEND_AGENT_SECRET}
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    depends_on:
      - jenkins-controller

volumes:
  jenkins_home:
```

---

## `.env` (kept out of git)

```bash
JENKINS_ADMIN_USERNAME=admin
JENKINS_ADMIN_PASSWORD=changeme
GITHUB_USERNAME=your-username
GITHUB_TOKEN=ghp_xxx
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-address@gmail.com
SMTP_PASSWORD=your-16-character-app-password
JENKINS_URL=http://localhost:8080
BACKEND_AGENT_SECRET=fill-in-after-first-boot
FRONTEND_AGENT_SECRET=fill-in-after-first-boot
```

---

## PHASE 4 — Webhook Wiring

```bash
# start everything
docker compose -f docker-compose.yml -f docker-compose.jenkins.yml up -d --build

# expose the controller
ngrok http 8080
```

Copy the `https://...ngrok-free.app` URL, then in your GitHub repo:
**Settings → Webhooks → Add webhook**
- Payload URL: `https://<your-ngrok-id>.ngrok-free.app/github-webhook/`
- Content type: `application/json`
- Events: just the push event (add pull request events too if you want PR-triggered builds)

Free ngrok URLs change on every restart — update the webhook URL each time you resume work, or
look into a paid/reserved domain or Cloudflare Tunnel later to make it permanent.

---

## PHASE 8 — Branch Protection

GitHub repo → **Settings → Branches → Add rule** for `main`:
- Require status checks to pass before merging → select the Jenkins check (it only appears in
  the list after it has reported at least once).
- Require branches to be up to date before merging (optional, stricter).

This is what physically disables the merge button on GitHub when the pipeline is red — no
custom automation needed for that part.