#!/usr/bin/env bash
# Generate a self-signed TLS certificate for the marketplace-ui nginx server (local/dev use only).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CN="${CN:-localhost}"
DAYS="${DAYS:-825}"
OUT_DIR="${OUT_DIR:-$SCRIPT_DIR}"
FORCE=0

usage() {
    echo "Usage: $0 [-f|--force] [-c|--cn CN] [-d|--days DAYS] [-o|--out DIR]"
    echo "  -f, --force   overwrite existing server.crt/server.key"
    echo "  -c, --cn      certificate common name / DNS SAN (default: localhost)"
    echo "  -d, --days    validity in days (default: 825)"
    echo "  -o, --out     output directory (default: $SCRIPT_DIR)"
    exit 1
}

while [ $# -gt 0 ]; do
    case "$1" in
        -f|--force) FORCE=1; shift ;;
        -c|--cn) CN="$2"; shift 2 ;;
        -d|--days) DAYS="$2"; shift 2 ;;
        -o|--out) OUT_DIR="$2"; shift 2 ;;
        -h|--help) usage ;;
        *) echo "Unknown option: $1"; usage ;;
    esac
done

CRT="$OUT_DIR/server.crt"
KEY="$OUT_DIR/server.key"

if [ -f "$CRT" ] || [ -f "$KEY" ]; then
    if [ "$FORCE" -ne 1 ]; then
        echo "server.crt/server.key already exist in $OUT_DIR. Use --force to overwrite." >&2
        exit 1
    fi
fi

mkdir -p "$OUT_DIR"

openssl req -x509 -nodes -newkey rsa:2048 \
    -keyout "$KEY" \
    -out "$CRT" \
    -days "$DAYS" \
    -subj "/CN=${CN}" \
    -addext "subjectAltName=DNS:${CN},DNS:localhost,IP:127.0.0.1"

chmod 600 "$KEY"

echo "Generated self-signed certificate (CN=${CN}, valid ${DAYS} days):"
echo "  $CRT"
echo "  $KEY"
openssl x509 -in "$CRT" -noout -subject -dates -ext subjectAltName
