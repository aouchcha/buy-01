# Destructive, unscoped cleanup of this user's Podman storage.
podman rm --all --force 2>/dev/null || true
podman system prune -a --volumes -f