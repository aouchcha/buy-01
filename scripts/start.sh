#!/usr/bin/env bash
set -euo pipefail

# ─────────────────────────────────────────────
# BUY-01 — Startup script
# Usage:
#   ./scripts/start.sh          → build + start all services
#   ./scripts/start.sh --fresh  → tear down volumes, rebuild, start
#   ./scripts/start.sh --stop   → stop all services
#   ./scripts/start.sh --logs   → follow logs after start
#
# Gateway : HTTPS on port 8443 (self-signed certificate)
# Run scripts/create_Self-Signed-Certificate.sh first if keystore.p12 is missing.
# ─────────────────────────────────────────────

# Always run from project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "$ROOT_DIR"

FRESH=false
STOP=false
FOLLOW_LOGS=false

for arg in "$@"; do
  case $arg in
    --fresh) FRESH=true ;;
    --stop)  STOP=true ;;
    --logs)  FOLLOW_LOGS=true ;;
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

# ─── Check prerequisites ───────────────────────────────────────────────────────

check_command() {
  command -v "$1" &>/dev/null || error "$1 is not installed. Please install it first."
}

check_command docker
check_command curl

# ─── Check keystore ───────────────────────────────────────────────────────────

check_keystore() {
  local keystore="Backend/gateway/src/main/resources/keystore.p12"
  if [[ ! -f "$keystore" ]]; then
    warn "keystore.p12 not found. Generating self-signed certificate..."
    bash "${SCRIPT_DIR}/create_Self-Signed-Certificate.sh"
  else
    success "keystore.p12 found"
  fi
}

# ─── Docker runtime (macOS Colima vs Docker Desktop vs Linux) ─────────────────

setup_docker() {
  if docker info &>/dev/null 2>&1; then
    success "Docker is running"
    return
  fi

  if [[ "$(uname)" == "Darwin" ]] && command -v colima &>/dev/null; then
    warn "Docker not responding. Starting Colima..."
    colima start
    export DOCKER_HOST="unix://${HOME}/.colima/default/docker.sock"
    if docker info &>/dev/null 2>&1; then
      success "Colima started — DOCKER_HOST=${DOCKER_HOST}"
      return
    fi
  fi

  if [[ -S "${HOME}/.colima/default/docker.sock" ]]; then
    export DOCKER_HOST="unix://${HOME}/.colima/default/docker.sock"
    if docker info &>/dev/null 2>&1; then
      success "Using Colima socket — DOCKER_HOST=${DOCKER_HOST}"
      return
    fi
  fi

  if [[ -S /var/run/docker.sock ]]; then
    export DOCKER_HOST="unix:///var/run/docker.sock"
    if docker info &>/dev/null 2>&1; then
      success "Docker is running (Linux socket)"
      return
    fi
  fi

  error "Docker is not running. Please start Docker Desktop or Colima first."
}

# ─── Check .env ───────────────────────────────────────────────────────────────

check_env() {
  if [[ ! -f .env ]]; then
    error ".env file not found. Copy .env.example to .env and fill in the values."
  fi

  local required_vars=(DB_USERNAME DB_PASSWORD DB_NAME JWT_SECRET USER_DB_URI MEDIA_DB_URI PRODUCT_DB_URI)
  for var in "${required_vars[@]}"; do
    if ! grep -q "^${var}=" .env; then
      error "Missing required variable '${var}' in .env"
    fi
  done
  success ".env OK"
}

# ─── Wait for HTTP/HTTPS endpoint ─────────────────────────────────────────────

wait_for_http() {
  local name="$1"
  local url="$2"
  local expected_code="${3:-200}"
  local max_attempts=30
  local attempt=0
  local code="000"

  info "Waiting for ${name} at ${url} ..."
  while [[ $attempt -lt $max_attempts ]]; do
    code=$(curl -sk -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")
    if [[ "$code" == "$expected_code" || "$code" == "401" || "$code" == "403" ]]; then
      success "${name} is UP (HTTP ${code})"
      return 0
    fi
    attempt=$((attempt + 1))
    sleep 3
  done
  warn "${name} did not respond after $((max_attempts * 3))s (last code: ${code})"
  return 1
}

# ─── Stop ─────────────────────────────────────────────────────────────────────

if $STOP; then
  info "Stopping all services..."
  docker compose down
  success "All services stopped."
  exit 0
fi

# ─── Main ─────────────────────────────────────────────────────────────────────

setup_docker
check_env
check_keystore

echo ""
info "────────────────────────────────────────────"
info "  BUY-01 — Starting services"
info "────────────────────────────────────────────"
echo ""

if $FRESH; then
  warn "--fresh: tearing down containers and volumes..."
  docker compose down -v
else
  docker compose down --remove-orphans 2>/dev/null || true
fi

info "Building and starting all services..."
docker compose up --build -d

echo ""
info "Waiting for MongoDB to be healthy..."
attempt=0
max=30
while [[ $attempt -lt $max ]]; do
  status=$(docker inspect --format='{{.State.Health.Status}}' buy01DB 2>/dev/null || echo "unknown")
  if [[ "$status" == "healthy" ]]; then
    success "MongoDB is healthy"
    break
  fi
  attempt=$((attempt + 1))
  if [[ $attempt -eq $max ]]; then
    error "MongoDB did not become healthy after $((max * 3))s. Run: docker compose logs mongodb"
  fi
  sleep 3
done

echo ""
info "Waiting for services to be ready (Spring Boot takes ~20s)..."
sleep 25

echo ""
info "────────────────────────────────────────────"
info "  Health checks"
info "────────────────────────────────────────────"

wait_for_http "discovery-service" "http://localhost:8761/eureka/apps"          "200"
wait_for_http "gateway"           "https://localhost:8443/api/product"         "200"
wait_for_http "user-service"      "http://localhost:8081/api/auth/login"       "401"
wait_for_http "product-service"   "http://localhost:8083/api/product/health"   "200"
wait_for_http "media-service"     "http://localhost:8082/actuator/health"      "200"

echo ""
info "────────────────────────────────────────────"
info "  Eureka registered instances"
info "────────────────────────────────────────────"
curl -s http://localhost:8761/eureka/apps \
  | grep -oE '<appName>[^<]+</appName>|<status>[^<]+</status>' \
  | paste - - \
  | sed 's/<[^>]*>//g; s/\t/ → /g' \
  | while read -r line; do
      if echo "$line" | grep -q "UP"; then
        echo -e "  ${GREEN}✓${NC} $line"
      else
        echo -e "  ${RED}✗${NC} $line"
      fi
    done

echo ""
success "BUY-01 is running!"
echo ""
echo "  Eureka dashboard : http://localhost:8761"
echo "  API Gateway      : https://localhost:8443   (HTTPS — self-signed)"
echo "  User service     : http://localhost:8081"
echo "  Media service    : http://localhost:8082"
echo "  Product service  : http://localhost:8083"
echo ""
warn "Browser will show SSL warning for self-signed certificate — accept it to continue."
echo ""

if $FOLLOW_LOGS; then
  info "Following logs (Ctrl+C to stop)..."
  docker compose logs -f
fi
