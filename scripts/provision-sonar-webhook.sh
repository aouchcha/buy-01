#!/bin/sh
set -eu

# ============================================================================
# Idempotently reconciles the SonarQube -> Jenkins webhook via the SonarQube
# REST API, so `waitForQualityGate` in the Jenkinsfile gets a real-time
# callback instead of relying on polling. Runs as a one-shot container
# every `docker compose up` (see docker-compose.jenkins.yml).
#
# "Reconciles", not "creates once": an existing webhook keeps whatever URL and
# secret it was created with. If that secret drifts from the one Jenkins holds
# in the `sonarqube-webhook-secret` credential, SonarQube still delivers the
# payload but Jenkins rejects its HMAC signature and drops it — the build then
# sits in waitForQualityGate until the timeout fires, with no error anywhere.
# So we overwrite URL and secret on every run instead of skipping.
# ============================================================================

: "${SONAR_HOST_URL:?SONAR_HOST_URL is required}"
: "${SONARQUBE_ADMIN_TOKEN:?SONARQUBE_ADMIN_TOKEN is required}"
: "${JENKINS_URL:?JENKINS_URL is required}"
: "${SONARQUBE_WEBHOOK_SECRET:?SONARQUBE_WEBHOOK_SECRET is required}"

WEBHOOK_NAME="jenkins-buy01"
WEBHOOK_URL="${JENKINS_URL%/}/sonarqube-webhook/"

# Basic auth with the token as the username — deliberately not a Bearer header,
# which the 9.9 LTS server answers with 401.
sonar_api() {
    _method=$1
    _endpoint=$2
    shift 2

    _body=$(mktemp)
    _code=$(
        curl -s -o "${_body}" -w '%{http_code}' \
            -u "${SONARQUBE_ADMIN_TOKEN}:" \
            -X "${_method}" "${SONAR_HOST_URL}${_endpoint}" "$@"
    )

    if [ "${_code}" -ge 300 ]; then
        echo "${_method} ${_endpoint} failed with HTTP ${_code}:" >&2
        cat "${_body}" >&2
        echo >&2
        if [ "${_code}" = "403" ]; then
            echo "Hint: the webhook endpoints need a token with the global" >&2
            echo "Administer permission. A project analysis token (sqp_...) or" >&2
            echo "a global analysis token (sqa_...) is not enough — use an" >&2
            echo "admin user token (squ_...) for SONARQUBE_ADMIN_TOKEN." >&2
        fi
        rm -f "${_body}"
        return 1
    fi

    cat "${_body}"
    rm -f "${_body}"
}

echo "Waiting for SonarQube web API at ${SONAR_HOST_URL} ..."
until curl -sf "${SONAR_HOST_URL}/api/system/status" | grep -q '"status":"UP"'; do
    sleep 5
done

EXISTING_WEBHOOKS=$(sonar_api GET /api/webhooks/list)

# One JSON object per line, so the name we match and the key we extract are
# guaranteed to come from the same webhook regardless of field order.
WEBHOOK_KEY=$(
    printf '%s' "${EXISTING_WEBHOOKS}" \
        | tr '{' '\n' \
        | grep "\"name\":\"${WEBHOOK_NAME}\"" \
        | sed -n 's/.*"key":"\([^"]*\)".*/\1/p' \
        | head -n 1
)

if [ -n "${WEBHOOK_KEY}" ]; then
    echo "Webhook '${WEBHOOK_NAME}' exists — reconciling URL and secret"
    sonar_api POST /api/webhooks/update \
        --data-urlencode "webhook=${WEBHOOK_KEY}" \
        --data-urlencode "name=${WEBHOOK_NAME}" \
        --data-urlencode "url=${WEBHOOK_URL}" \
        --data-urlencode "secret=${SONARQUBE_WEBHOOK_SECRET}" >/dev/null
    echo "Webhook updated -> ${WEBHOOK_URL}"
else
    echo "Creating webhook '${WEBHOOK_NAME}' -> ${WEBHOOK_URL}"
    sonar_api POST /api/webhooks/create \
        --data-urlencode "name=${WEBHOOK_NAME}" \
        --data-urlencode "url=${WEBHOOK_URL}" \
        --data-urlencode "secret=${SONARQUBE_WEBHOOK_SECRET}" >/dev/null
    echo "Webhook created."
fi
