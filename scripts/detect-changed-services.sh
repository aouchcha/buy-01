# #!/usr/bin/env bash
# set -euo pipefail

# # ============================================================================
# # Figures out which microservices had code changes between two git commits,
# # so the Jenkins pipeline only builds, tests, and deploys what actually
# # changed.
# #
# # The list of valid service names is read directly from docker-compose.yml
# # (the file that contains ONLY real application services — Jenkins itself
# # lives in the separate docker-compose.jenkins.yml file), so this script
# # never needs to be edited by hand when a service is added or removed.
# # ============================================================================

# APPLICATION_COMPOSE_FILE="docker-compose.yml"
# INFRASTRUCTURE_COMPOSE_FILE="docker-compose.jenkins.yml"

# # The commit/branch to compare FROM. Defaults to the previous commit.
# COMPARE_FROM_REFERENCE="${1:-HEAD~1}"

# # The commit/branch to compare TO. Defaults to the current commit.
# COMPARE_TO_REFERENCE="${2:-HEAD}"

# # ---- Step 1: get the list of real application service names ----
# # "docker compose config --services" reads docker-compose.yml and prints the
# # service names defined in it (order-service, payment-service, frontend).
# # It never sees jenkins-controller, backend-agent, or frontend-agent,
# # because those live in a different file that isn't passed in here.
# ALL_SERVICE_NAMES=($(docker compose -f "$APPLICATION_COMPOSE_FILE" -f "$INFRASTRUCTURE_COMPOSE_FILE" config --services))
# INFRASTRUCTURE_SERVICE_NAMES=($(docker compose -f "$INFRASTRUCTURE_COMPOSE_FILE" config --services))

# echo "All service names:" >&2
# printf '%s\n' "${ALL_SERVICE_NAMES[@]}" >&2

# echo "Infrastructure service names:" >&2
# printf '%s\n' "${INFRASTRUCTURE_SERVICE_NAMES[@]}" >&2

# echo "FROM: $COMPARE_FROM_REFERENCE" >&2
# echo "TO:   $COMPARE_TO_REFERENCE" >&2


# APPLICATION_SERVICE_NAMES=()
# for SERVICE_NAME in "${ALL_SERVICE_NAMES[@]}"; do
#   IS_INFRASTRUCTURE=false
#   for INFRA_NAME in "${INFRASTRUCTURE_SERVICE_NAMES[@]}"; do
#     if [[ "$SERVICE_NAME" == "$INFRA_NAME" ]]; then
#       IS_INFRASTRUCTURE=true
#       break
#     fi
#   done
#   if [[ "$IS_INFRASTRUCTURE" == false ]]; then
#     APPLICATION_SERVICE_NAMES+=("$SERVICE_NAME")
#   fi
# done

# # ---- Step 2: get every file path that changed between the two commits ----
# if ! git rev-parse "$COMPARE_FROM_REFERENCE" >/dev/null 2>&1; then
#   # There is no earlier commit to compare against (first commit ever, or a
#   # shallow clone with no history). Safest choice: treat every service as
#   # changed so nothing is silently skipped.
#   echo "No previous commit found to compare against — treating all services as changed." >&2
#   printf '%s\n' "${APPLICATION_SERVICE_NAMES[@]}"
#   exit 0
# fi

# CHANGED_FILE_PATHS=$(git diff --name-only "$COMPARE_FROM_REFERENCE" "$COMPARE_TO_REFERENCE")

# # ---- Step 3: for each real service, check whether any changed file lives inside its folder ----
# echo "Detected services:" >&2
# printf '%s\n' "${APPLICATION_SERVICE_NAMES[@]}" >&2

# echo "" >&2
# echo "Changed files:" >&2
# echo "$CHANGED_FILE_PATHS" >&2

# echo "" >&2

# for SERVICE_NAME in "${APPLICATION_SERVICE_NAMES[@]}"; do
#     echo "Checking $SERVICE_NAME" >&2

#     if echo "$CHANGED_FILE_PATHS" | grep "${SERVICE_NAME}/"; then
#         echo "MATCH -> $SERVICE_NAME" >&2
#         echo "$SERVICE_NAME"
#     else
#         echo "NO MATCH" >&2
#     fi
# done


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