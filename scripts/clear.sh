# Destructive, unscoped cleanup of this user's docker storage.
docker rm --all --force 2>/dev/null || true
docker system prune -a --volumes -f