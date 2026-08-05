#!/usr/bin/env bash
set -euo pipefail

# ─────────────────────────────────────────────
# BUY-01 — Startup script
# Usage:
#   ./scripts/start.sh          → build + start all services
#   ./scripts/start.sh --fresh  → tear down volumes, rebuild, start
#   ./scripts/start.sh --stop   → stop all services
#   ./scripts/start.sh --logs   → follow logs after start
# ─────────────────────────────────────────────

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "$ROOT_DIR"

FRESH=false
STOP=false
FOLLOW_LOGS=false

for arg in "$@"; do
    case "$arg" in
        --fresh) FRESH=true ;;
        --stop) STOP=true ;;
        --logs) FOLLOW_LOGS=true ;;
    esac
done

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

info()    { echo -e "${CYAN}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

check_command() {
    command -v "$1" >/dev/null 2>&1 || error "$1 is not installed."
}

check_command docker
check_command curl

setup_docker() {
    if docker info >/dev/null 2>&1; then
        success "Docker is running"
        return
    fi

    if [[ "$(uname)" == "Darwin" ]] && command -v colima >/dev/null 2>&1; then
        warn "Starting Colima..."
        colima start
        export DOCKER_HOST="unix://${HOME}/.colima/default/docker.sock"
    fi

    docker info >/dev/null 2>&1 || error "Docker is not running."
}

check_keystore() {
    local keystore="Backend/gateway/src/main/resources/keystore.p12"

    if [[ ! -f "$keystore" ]]; then
        warn "Generating self-signed certificate..."
        bash scripts/create_Self-Signed-Certificate.sh
    fi

    success "keystore OK"
}

check_env() {

    [[ -f .env ]] || error ".env not found."

    local required=(
        DB_USERNAME
        DB_PASSWORD
        USERS_DB_NAME
        PRODUCTS_DB_NAME
        USER_DB_URI
        PRODUCT_DB_URI
        JWT_SECRET
    )

    for var in "${required[@]}"; do
        grep -q "^${var}=" .env || error "Missing ${var} in .env"
    done

    success ".env OK"
}

wait_for_http() {

    local name="$1"
    local url="$2"
    local expected="${3:-200}"

    info "Waiting for $name..."

    for ((i=1;i<=30;i++)); do

        code=$(curl -sk -o /dev/null -w "%{http_code}" "$url" || true)

        if [[ "$code" == "$expected" || "$code" == "401" || "$code" == "403" ]]; then
            success "$name is UP ($code)"
            return
        fi

        sleep 3
    done

    warn "$name not ready."
}

wait_for_container_health() {

    local container="$1"

    info "Waiting for $container..."

    for ((i=1;i<=30;i++)); do

        status=$(docker inspect \
            --format='{{.State.Health.Status}}' \
            "$container" 2>/dev/null || echo "unknown")

        if [[ "$status" == "healthy" ]]; then
            success "$container is healthy"
            return
        fi

        sleep 3
    done

    error "$container did not become healthy.
Run:
docker logs $container"
}

if $STOP; then
    info "Stopping services..."
    docker compose down
    success "Stopped."
    exit 0
fi

setup_docker
check_env
check_keystore

echo
info "Starting BUY-01..."
echo

if $FRESH; then
    docker compose down -v
else
    docker compose down --remove-orphans || true
fi

docker compose up --build -d

echo
wait_for_container_health users-db
wait_for_container_health products-db

echo
info "Waiting for Spring Boot..."
sleep 25


echo
wait_for_http "Discovery" "http://localhost:8761/eureka/apps" 200
wait_for_http "Gateway" "https://localhost:8443/api/product" 200
wait_for_http "User Service" "http://localhost:8081/api/auth/login" 401
wait_for_http "Product Service" "http://localhost:8083/actuator/health" 200
wait_for_http "Media Service" "http://localhost:8082/actuator/health" 200

echo
success "BUY-01 is running."

echo
echo "Eureka : http://localhost:8761"
echo "Gateway: https://localhost:8443"
echo "User   : http://localhost:8081"
echo "Media  : http://localhost:8082"
echo "Product: http://localhost:8083"
echo

if $FOLLOW_LOGS; then
    docker compose logs -f
fi