#!/usr/bin/env bash
set -euo pipefail

APPLICATION_COMPOSE_FILE="docker-compose.yml"
INFRASTRUCTURE_COMPOSE_FILE="docker-compose.jenkins.yml"

COMPARE_FROM_REFERENCE="${1:-HEAD~1}"
COMPARE_TO_REFERENCE="${2:-HEAD}"

# Get all compose services
ALL_SERVICE_NAMES=(
    $(docker compose \
        -f "$APPLICATION_COMPOSE_FILE" \
        -f "$INFRASTRUCTURE_COMPOSE_FILE" \
        config --services)
)

INFRASTRUCTURE_SERVICE_NAMES=(
    $(docker compose \
        -f "$INFRASTRUCTURE_COMPOSE_FILE" \
        config --services)
)


# Remove infrastructure services
APPLICATION_SERVICE_NAMES=()

for SERVICE_NAME in "${ALL_SERVICE_NAMES[@]}"; do
    IS_INFRASTRUCTURE=false

    for INFRA_NAME in "${INFRASTRUCTURE_SERVICE_NAMES[@]}"; do
        if [[ "$SERVICE_NAME" == "$INFRA_NAME" ]]; then
            IS_INFRASTRUCTURE=true
            break
        fi
    done

    if [[ "$IS_INFRASTRUCTURE" == false ]]; then
        APPLICATION_SERVICE_NAMES+=("$SERVICE_NAME")
    fi
done


echo "Services to check:" >&2
printf '%s\n' "${APPLICATION_SERVICE_NAMES[@]}" >&2


# First commit: rebuild everything
if ! git rev-parse "$COMPARE_FROM_REFERENCE" >/dev/null 2>&1; then
    echo "No previous commit. Building all services." >&2
    printf '%s\n' "${APPLICATION_SERVICE_NAMES[@]}"
    exit 0
fi


CHANGED_FILE_PATHS=$(git diff \
    --name-only \
    "$COMPARE_FROM_REFERENCE" \
    "$COMPARE_TO_REFERENCE")


echo "Changed files:" >&2
echo "$CHANGED_FILE_PATHS" >&2


# Detect changed services
for SERVICE_NAME in "${APPLICATION_SERVICE_NAMES[@]}"; do

    # Read build.context from docker compose
    SERVICE_CONTEXT=$(
        docker compose \
        -f "$APPLICATION_COMPOSE_FILE" \
        config | \
        awk "
        /^  ${SERVICE_NAME}:/ {found=1}
        found && /context:/ {
            print \$2
            exit
        }
        "
    )

    # Remove ./ prefix
    SERVICE_CONTEXT="${SERVICE_CONTEXT#./}"


    echo "Checking $SERVICE_NAME -> $SERVICE_CONTEXT" >&2


    if echo "$CHANGED_FILE_PATHS" | grep -q "^${SERVICE_CONTEXT}/"; then
        echo "MATCH -> $SERVICE_NAME" >&2
        echo "$SERVICE_NAME"
    else
        echo "NO MATCH -> $SERVICE_NAME" >&2
    fi

done