#!/bin/sh
set -eu

# ============================================================================
# Idempotently creates the SonarQube -> Jenkins webhook via the SonarQube
# REST API, so `waitForQualityGate` in the Jenkinsfile gets a real-time
# callback instead of relying on polling. Runs as a one-shot container
# every `docker compose up` (see docker-compose.jenkins.yml); safe to run
# repeatedly since it checks for the webhook before creating it.
# ============================================================================

: "${SONAR_HOST_URL:?SONAR_HOST_URL is required}"
: "${SONARQUBE_ADMIN_TOKEN:?SONARQUBE_ADMIN_TOKEN is required}"
: "${JENKINS_URL:?JENKINS_URL is required}"
: "${SONARQUBE_WEBHOOK_SECRET:?SONARQUBE_WEBHOOK_SECRET is required}"

WEBHOOK_NAME="jenkins-buy01"
WEBHOOK_URL="${JENKINS_URL%/}/sonarqube-webhook/"

echo "Waiting for SonarQube web API at ${SONAR_HOST_URL} ..."
until curl -sf -u "${SONARQUBE_ADMIN_TOKEN}:" "${SONAR_HOST_URL}/api/system/status" | grep -q '"status":"UP"'; do
    sleep 5
done

EXISTING_WEBHOOKS=$(curl -sf -u "${SONARQUBE_ADMIN_TOKEN}:" "${SONAR_HOST_URL}/api/webhooks/list")

if echo "${EXISTING_WEBHOOKS}" | grep -q "\"name\":\"${WEBHOOK_NAME}\""; then
    echo "Webhook '${WEBHOOK_NAME}' already exists — nothing to do."
else
    echo "Creating webhook '${WEBHOOK_NAME}' -> ${WEBHOOK_URL}"
    curl -sf -u "${SONARQUBE_ADMIN_TOKEN}:" \
        -X POST "${SONAR_HOST_URL}/api/webhooks/create" \
        --data-urlencode "name=${WEBHOOK_NAME}" \
        --data-urlencode "url=${WEBHOOK_URL}" \
        --data-urlencode "secret=${SONARQUBE_WEBHOOK_SECRET}"
    echo "Webhook created."
fi
