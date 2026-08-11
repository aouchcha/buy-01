#!/usr/bin/env bash
set -euo pipefail

# ============================================================================
# Mints the two SonarQube tokens the pipeline needs and writes them into .env.
#
# They are deliberately different kinds of token, because they are used by
# code with different privileges:
#
#   SONARQUBE_TOKEN        GLOBAL_ANALYSIS_TOKEN — used by withSonarQubeEnv()
#                          in the Jenkinsfile. Must be *global*: the pipeline
#                          analyses one project per service (buy01-discovery,
#                          buy01-gateway, buy01-media, buy01-product,
#                          buy01-user, buy01-frontend), and a project analysis
#                          token is bound to a single key — every other key
#                          fails with "You're not authorized to run analysis".
#                          A global token also auto-provisions those projects
#                          on their first analysis.
#
#   SONARQUBE_ADMIN_TOKEN  USER_TOKEN on an admin — used by
#                          provision-sonar-webhook.sh, which calls
#                          /api/webhooks/*. Those endpoints reject analysis
#                          tokens with 403, so it cannot be the token above.
#
# The admin password is read from the terminal, never from a flag or an
# environment variable, so it does not land in the shell history or in `ps`.
# Neither token is ever echoed — only its prefix and length, so you can sanity
# check what was written without exposing the value.
# ============================================================================

SONAR_URL="${SONAR_URL:-http://localhost:9000}"
ADMIN_LOGIN="${ADMIN_LOGIN:-admin}"
ANALYSIS_TOKEN_NAME="${ANALYSIS_TOKEN_NAME:-buy01-jenkins-analysis}"
ADMIN_TOKEN_NAME="${ADMIN_TOKEN_NAME:-buy01-jenkins-admin}"

ENV_FILE="$(cd "$(dirname "$0")/.." && pwd)/.env"

[ -f "${ENV_FILE}" ] || { echo "No .env at ${ENV_FILE}" >&2; exit 1; }

printf 'Reading SonarQube admin credentials for %s at %s\n' "${ADMIN_LOGIN}" "${SONAR_URL}" >&2

# Basic auth, not a Bearer header: the 9.9 LTS server rejects Bearer with 401.
#
# The temp file is removed on each exit path rather than from a `trap ... RETURN`:
# a RETURN trap is global, so it would also fire when the *calling* function
# returns, by which point `body` is out of scope and `set -u` aborts the script.
sonar_api() {
    local method=$1 endpoint=$2; shift 2
    local body code
    body=$(mktemp)

    code=$(
        curl -s -o "${body}" -w '%{http_code}' \
            -u "${ADMIN_LOGIN}:${ADMIN_PASSWORD}" \
            -X "${method}" "${SONAR_URL}${endpoint}" "$@"
    )

    if [ "${code}" -ge 300 ]; then
        echo "${method} ${endpoint} failed with HTTP ${code}:" >&2
        cat "${body}" >&2; echo >&2
        [ "${code}" = "401" ] && echo "Hint: wrong admin password." >&2
        [ "${code}" = "403" ] && echo "Hint: ${ADMIN_LOGIN} lacks global Administer." >&2
        rm -f "${body}"
        return 1
    fi

    cat "${body}"
    rm -f "${body}"
}

# /api/authentication/validate answers 200 {"valid":false} for a bad password
# rather than 401, so the status code alone cannot tell us whether the
# credentials were accepted — the body has to be inspected.
#
# Retried rather than fatal on the first miss: the prompt is silent, so a typo
# is invisible and exiting immediately means re-running the whole script.
# Prefer SONARQUBE_ADMIN_PASSWORD, from the environment or straight out of
# .env, so this can run unattended; the prompt is only the fallback.
ADMIN_PASSWORD="${SONARQUBE_ADMIN_PASSWORD:-}"
if [ -z "${ADMIN_PASSWORD}" ]; then
    ADMIN_PASSWORD=$(sed -n 's/^SONARQUBE_ADMIN_PASSWORD=//p' "${ENV_FILE}" | head -n1)
fi

authenticated=0

if [ -n "${ADMIN_PASSWORD}" ]; then
    if sonar_api GET /api/authentication/validate 2>/dev/null | grep -q '"valid":true'; then
        echo "Authenticated as ${ADMIN_LOGIN} using SONARQUBE_ADMIN_PASSWORD." >&2
        authenticated=1
    else
        echo "SONARQUBE_ADMIN_PASSWORD was rejected — falling back to the prompt." >&2
    fi
fi

# /api/authentication/validate answers 200 {"valid":false} for a bad password
# rather than 401, so the status code alone cannot tell us whether the
# credentials were accepted — the body has to be inspected.
#
# Retried rather than fatal on the first miss: the prompt is silent, so a typo
# is invisible and exiting immediately means re-running the whole script.
if [ "${authenticated}" -ne 1 ]; then
for attempt in 1 2 3; do
    printf 'SonarQube admin password for %s (attempt %d/3): ' "${ADMIN_LOGIN}" "${attempt}" >&2
    read -rs ADMIN_PASSWORD
    printf '\n' >&2

    if [ -z "${ADMIN_PASSWORD}" ]; then
        echo "  Empty input — nothing was typed." >&2
        continue
    fi

    # Silent input hides a stray space from a paste; flag it rather than let it
    # look like a wrong password.
    case "${ADMIN_PASSWORD}" in
        ' '*|*' ') echo "  Note: value starts or ends with a space — pasted by mistake?" >&2 ;;
    esac

    if sonar_api GET /api/authentication/validate 2>/dev/null | grep -q '"valid":true'; then
        authenticated=1
        break
    fi

    echo "  Rejected by SonarQube (200 {\"valid\":false} — wrong password)." >&2
done
fi

if [ "${authenticated}" -ne 1 ]; then
    cat >&2 <<EOF

Three rejected attempts, so the stored password is not what is being typed.
SonarQube forced a change away from admin/admin on first boot (SETUP.md:63).

If it is lost, reset it against the database — this instance is local and its
Postgres volume is yours, so this is the supported recovery path. The hash below
is bcrypt("admin"), so it puts the password back to admin/admin and SonarQube
then forces a new one at next login. Paste all four lines together: the quoted
heredoc is what keeps the \$ in the hash from being eaten by the shell.

  set -a; . ./.env; set +a
  docker exec -i buy-01-sonarqube-db-1 \\
      psql -U "\$SONARQUBE_DB_USERNAME" -d "\$SONARQUBE_DB_NAME" <<'SQL'
  UPDATE users SET
      crypted_password='\$2a\$12\$uCkkXmhW5ThVK8mpBvnXOOJRLd64LJeHTeCkSuB3lfaR2N0AYBaSi',
      salt=NULL, hash_method='BCRYPT', reset_password=true, user_local=true
  WHERE login='admin';
  SQL

Then restart SonarQube so it drops the cached session:

  docker compose --profile infra -f docker-compose.yml \\
      -f docker-compose.jenkins.yml --env-file .env restart sonarqube

Log in at ${SONAR_URL} as admin/admin, set a new password, put it in .env as
SONARQUBE_ADMIN_PASSWORD, and re-run this script.
EOF
    exit 1
fi

# A token name can only exist once per user, so revoking first makes reruns
# idempotent. Revoke is a no-op when the name is absent, hence the `|| true`.
mint() {
    local name=$1 type=$2 out

    sonar_api POST /api/user_tokens/revoke \
        --data-urlencode "login=${ADMIN_LOGIN}" \
        --data-urlencode "name=${name}" >/dev/null 2>&1 || true

    out=$(
        sonar_api POST /api/user_tokens/generate \
            --data-urlencode "login=${ADMIN_LOGIN}" \
            --data-urlencode "name=${name}" \
            --data-urlencode "type=${type}"
    )

    printf '%s' "${out}" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p'
}

set_env_var() {
    local key=$1 value=$2 tmp
    tmp=$(mktemp)
    # Rewrite the one line in place; awk keeps every other byte, including
    # comments and blank lines, exactly as it was.
    KEY="${key}" VALUE="${value}" awk '
        BEGIN { k = ENVIRON["KEY"]; v = ENVIRON["VALUE"]; done = 0 }
        index($0, k "=") == 1 { print k "=" v; done = 1; next }
        { print }
        END { if (!done) print k "=" v }
    ' "${ENV_FILE}" > "${tmp}"
    cat "${tmp}" > "${ENV_FILE}"   # preserve the original mode/owner
    rm -f "${tmp}"
}

report() { printf '  %-24s type=%-22s len=%d prefix=%s...\n' "$1" "$2" "${#3}" "${3:0:8}"; }

cp -p "${ENV_FILE}" "${ENV_FILE}.bak"

ANALYSIS_TOKEN=$(mint "${ANALYSIS_TOKEN_NAME}" GLOBAL_ANALYSIS_TOKEN)
ADMIN_TOKEN=$(mint "${ADMIN_TOKEN_NAME}" USER_TOKEN)

[ -n "${ANALYSIS_TOKEN}" ] && [ -n "${ADMIN_TOKEN}" ] \
    || { echo "SonarQube returned an empty token." >&2; exit 1; }

set_env_var SONARQUBE_TOKEN       "${ANALYSIS_TOKEN}"
set_env_var SONARQUBE_ADMIN_TOKEN "${ADMIN_TOKEN}"

echo "Wrote to ${ENV_FILE} (previous values kept in .env.bak):"
report SONARQUBE_TOKEN       GLOBAL_ANALYSIS_TOKEN "${ANALYSIS_TOKEN}"
report SONARQUBE_ADMIN_TOKEN USER_TOKEN            "${ADMIN_TOKEN}"

cat >&2 <<'EOF'

Next, so Jenkins picks up the new credentials and the webhook is recreated:

  docker compose --profile infra -f docker-compose.yml -f docker-compose.jenkins.yml \
    --env-file .env up -d --no-deps --force-recreate \
    jenkins-master backend-slave frontend-slave sonarqube-webhook-provisioner
EOF
