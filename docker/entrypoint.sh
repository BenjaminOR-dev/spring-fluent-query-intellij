#!/bin/sh
set -e
# Ensure wrapper is executable when bind-mounted from Windows/WSL hosts
if [ -f ./gradlew ]; then
  chmod +x ./gradlew 2>/dev/null || true
fi
exec "$@"
