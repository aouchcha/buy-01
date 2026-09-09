#!/bin/bash
set -euo pipefail

# Idempotent provisioning for the Nexus service defined in docker-compose.infra.yml:
# - rotates the auto-generated admin password
# - creates the docker-hosted repository (Maven repos ship by default in Nexus 3:
#   maven-central proxy, maven-releases/maven-snapshots hosted, maven-public group)
# - enables the Docker Bearer Token realm (required for `docker login`/push/pull)
# - creates a least-privilege "ci-publisher" role/user for Jenkins and a
#   read-only "developer" role
#
# Requires: docker compose (to read the auto-generated admin password from the
# running container), curl, jq.
#
# Usage:
#   NEXUS_ADMIN_PASSWORD=<new-admin-password> NEXUS_CI_PASSWORD=<ci-password> \
#     ./scripts/nexus-provision.sh

NEXUS_URL="${NEXUS_URL:-http://localhost:8081}"
NEXUS_ADMIN_PASSWORD="${NEXUS_ADMIN_PASSWORD:?Set NEXUS_ADMIN_PASSWORD to the new admin password}"
NEXUS_CI_PASSWORD="${NEXUS_CI_PASSWORD:?Set NEXUS_CI_PASSWORD to the password for the Jenkins service account}"
COMPOSE="docker compose --profile infra -f docker-compose.yml -f docker-compose.infra.yml"

echo "Waiting for Nexus to be reachable at ${NEXUS_URL}..."
for _ in $(seq 1 60); do
  if curl -sf -o /dev/null "${NEXUS_URL}/service/rest/v1/status"; then
    break
  fi
  sleep 5
done
if ! curl -sf -o /dev/null "${NEXUS_URL}/service/rest/v1/status"; then
  echo "Nexus did not become ready in time." >&2
  exit 1
fi

echo "Reading the auto-generated initial admin password from the container..."
INITIAL_PASSWORD="$(${COMPOSE} exec -T nexus cat /nexus-data/admin.password 2>/dev/null || true)"

if [ -n "${INITIAL_PASSWORD}" ]; then
  echo "First run detected: rotating the admin password."
  CURRENT_ADMIN_AUTH=(-u "admin:${INITIAL_PASSWORD}")
  curl -sf -X PUT "${CURRENT_ADMIN_AUTH[@]}" \
    -H 'Content-Type: text/plain' \
    "${NEXUS_URL}/service/rest/v1/security/users/admin/change-password" \
    -d "${NEXUS_ADMIN_PASSWORD}"
else
  echo "No initial admin password file found: assuming the admin password was already rotated."
fi

ADMIN_AUTH=(-u "admin:${NEXUS_ADMIN_PASSWORD}")

nexus_get() {
  curl -sf "${ADMIN_AUTH[@]}" -H 'accept: application/json' "${NEXUS_URL}$1"
}

nexus_post() {
  curl -sf -X POST "${ADMIN_AUTH[@]}" \
    -H 'accept: application/json' -H 'Content-Type: application/json' \
    "${NEXUS_URL}$1" -d "$2"
}

repo_exists() {
  nexus_get "/service/rest/v1/repositories" | jq -e --arg name "$1" 'any(.[]; .name == $name)' >/dev/null
}

echo "Ensuring the docker-hosted repository exists..."
if repo_exists "docker-hosted"; then
  echo "  docker-hosted already exists, skipping."
else
  nexus_post "/service/rest/v1/repositories/docker/hosted" '{
    "name": "docker-hosted",
    "online": true,
    "storage": {
      "blobStoreName": "default",
      "strictContentTypeValidation": true,
      "writePolicy": "ALLOW"
    },
    "docker": {
      "v1Enabled": false,
      "forceBasicAuth": true,
      "httpPort": 8082
    }
  }'
  echo "  docker-hosted created."
fi

echo "Enabling the Docker Bearer Token realm..."
ACTIVE_REALMS="$(nexus_get "/service/rest/v1/security/realms/active")"
if echo "${ACTIVE_REALMS}" | jq -e 'any(.[]; . == "DockerToken")' >/dev/null; then
  echo "  DockerToken realm already active, skipping."
else
  NEW_REALMS="$(echo "${ACTIVE_REALMS}" | jq -c '. + ["DockerToken"]')"
  curl -sf -X PUT "${ADMIN_AUTH[@]}" \
    -H 'Content-Type: application/json' \
    "${NEXUS_URL}/service/rest/v1/security/realms/active" \
    -d "${NEW_REALMS}"
  echo "  DockerToken realm enabled."
fi

role_exists() {
  nexus_get "/service/rest/v1/security/roles" | jq -e --arg id "$1" 'any(.[]; .id == $id)' >/dev/null
}

echo "Ensuring the ci-publisher role exists..."
if role_exists "ci-publisher"; then
  echo "  ci-publisher already exists, skipping."
else
  nexus_post "/service/rest/v1/security/roles" '{
    "id": "ci-publisher",
    "name": "ci-publisher",
    "description": "Jenkins service account: publish Maven artifacts and Docker images",
    "privileges": [
      "nx-repository-view-maven2-maven-releases-add",
      "nx-repository-view-maven2-maven-releases-edit",
      "nx-repository-view-maven2-maven-snapshots-add",
      "nx-repository-view-maven2-maven-snapshots-edit",
      "nx-repository-view-docker-docker-hosted-add",
      "nx-repository-view-docker-docker-hosted-edit",
      "nx-repository-view-docker-docker-hosted-read"
    ],
    "roles": []
  }'
  echo "  ci-publisher created."
fi

echo "Ensuring the developer (read-only) role exists..."
if role_exists "developer"; then
  echo "  developer already exists, skipping."
else
  nexus_post "/service/rest/v1/security/roles" '{
    "id": "developer",
    "name": "developer",
    "description": "Read-only access to browse and pull/download artifacts",
    "privileges": [
      "nx-repository-view-maven2-*-read",
      "nx-repository-view-maven2-*-browse",
      "nx-repository-view-docker-*-read",
      "nx-repository-view-docker-*-browse"
    ],
    "roles": []
  }'
  echo "  developer created."
fi

user_exists() {
  nexus_get "/service/rest/v1/security/users?userId=$1" | jq -e 'length > 0' >/dev/null
}

echo "Ensuring the ci service account exists..."
if user_exists "ci"; then
  echo "  ci user already exists, skipping."
else
  nexus_post "/service/rest/v1/security/users" "$(jq -n --arg pw "${NEXUS_CI_PASSWORD}" '{
    userId: "ci",
    firstName: "Jenkins",
    lastName: "CI",
    emailAddress: "ci@buy01.local",
    password: $pw,
    status: "active",
    roles: ["ci-publisher"]
  }')"
  echo "  ci user created."
fi

echo "Nexus provisioning complete."
echo "Maven group repository:  ${NEXUS_URL}/repository/maven-public/"
echo "Docker hosted repository: <nexus-host>:8082"
