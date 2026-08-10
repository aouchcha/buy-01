#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# ============================================================================
# Brings up the full infra stack (Jenkins, SonarQube, databases, agents) and
# keeps the two JNLP agent secrets in .env in sync.
#
# Jenkins generates BACKEND_SLAVE_SECRET/FRONTEND_SLAVE_SECRET itself (per
# permanent node, at controller boot) — they can't be hardcoded in advance.
# On a first boot (or after the jenkins_home volume is wiped) the agents
# start with a stale/placeholder secret and fail to connect; this script
# reads the real secret straight from the running controller via the
# scriptText API, and only recycles the stack if something actually changed.
# ============================================================================

COMPOSE=(docker compose --profile infra -f docker-compose.yml -f docker-compose.jenkins.yml)
ENV_FILE=".env"

echo "==> Starting the infra stack..."
"${COMPOSE[@]}" up -d

echo "==> Waiting for Jenkins to accept requests..."
until "${COMPOSE[@]}" exec -T jenkins-master curl -sf -o /dev/null http://localhost:8080/login; do
    sleep 5
done

fetch_agent_secret() {
    local node_name="$1"
    "${COMPOSE[@]}" exec -T jenkins-master sh -c "
        set -e
        curl -s -c /tmp/jenkins-cookies -u \"\$JENKINS_ADMIN_USERNAME:\$JENKINS_ADMIN_PASSWORD\" \
          'http://localhost:8080/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,\":\",//crumb)' > /tmp/crumb.txt
        CRUMB=\$(cat /tmp/crumb.txt)
        curl -s -b /tmp/jenkins-cookies -u \"\$JENKINS_ADMIN_USERNAME:\$JENKINS_ADMIN_PASSWORD\" -H \"\$CRUMB\" \
          http://localhost:8080/scriptText \
          --data-urlencode 'script=println(jenkins.model.Jenkins.get().getComputer(\"$node_name\").getJnlpMac())'
    " | tr -d '\r\n'
}

current_env_value() {
    local key="$1"
    grep -m1 "^${key}=" "$ENV_FILE" 2>/dev/null | cut -d= -f2- || true
}

set_env_value() {
    local key="$1" value="$2" tmp
    tmp=$(mktemp)
    if grep -q "^${key}=" "$ENV_FILE" 2>/dev/null; then
        awk -v k="$key" -v v="$value" 'BEGIN{FS=OFS="="} $1==k {$0=k"="v} {print}' "$ENV_FILE" > "$tmp"
    else
        cp "$ENV_FILE" "$tmp"
        printf '%s=%s\n' "$key" "$value" >> "$tmp"
    fi
    mv "$tmp" "$ENV_FILE"
}

needs_restart=false

for pair in "backend-slave:BACKEND_SLAVE_SECRET" "frontend-slave:FRONTEND_SLAVE_SECRET"; do
    node_name="${pair%%:*}"
    var_name="${pair##*:}"

    echo "==> Checking the JNLP secret for ${node_name}..."
    live_secret="$(fetch_agent_secret "$node_name")"

    if ! echo "$live_secret" | grep -qE '^[0-9a-f]{64}$'; then
        echo "!! Could not read a valid secret for '${node_name}' (is that node defined under jenkins.nodes in JCasC, and has Jenkins finished booting?) — skipping." >&2
        continue
    fi

    stored_secret="$(current_env_value "$var_name")"

    if [ "$live_secret" != "$stored_secret" ]; then
        echo "==> ${var_name} is out of date in .env — updating it (value not printed)."
        set_env_value "$var_name" "$live_secret"
        needs_restart=true
    else
        echo "==> ${var_name} already matches Jenkins — nothing to do."
    fi
done

if [ "$needs_restart" = true ]; then
    echo "==> Agent secrets changed — recycling the stack so they reconnect with the right ones..."
    "${COMPOSE[@]}" down
    "${COMPOSE[@]}" up -d
else
    echo "==> Agent secrets already in sync — no restart needed."
fi

echo "==> Done. Jenkins: ${JENKINS_URL:-http://localhost:8080}"
