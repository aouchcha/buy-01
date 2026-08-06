#!/usr/bin/env bash

set -euo pipefail

COMPOSE_FILES=(
  -f docker-compose.yml
  -f docker-compose.jenkins.yml
)

PROJECTS=(
  "buy-01"
  "mr-jenk"
)

services=$(docker compose --profile infra "${COMPOSE_FILES[@]}" config --services)

for service in $services; do
    echo "========== $service =========="

    for project in "${PROJECTS[@]}"; do
        container="${project}-${service}-1"

        if docker inspect "$container" >/dev/null 2>&1; then
            echo "[$container]"
            docker inspect "$container" \
                --format '{{range $name, $net := .NetworkSettings.Networks}}{{printf "%s (%s)\n" $name $net.NetworkID}}{{end}}'
        fi
    done

    echo
done