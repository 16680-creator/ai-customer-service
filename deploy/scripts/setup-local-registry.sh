#!/usr/bin/env bash
set -euo pipefail

REGISTRY_PORT="${REGISTRY_PORT:-5000}"
REGISTRY_DATA_DIR="${REGISTRY_DATA_DIR:-/opt/registry/data}"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required on this host. Install Docker before running this script." >&2
  exit 1
fi

sudo mkdir -p "${REGISTRY_DATA_DIR}"

if docker ps -a --format '{{.Names}}' | grep -qx 'local-registry'; then
  docker rm -f local-registry
fi

docker run -d \
  --name local-registry \
  --restart=always \
  -p "${REGISTRY_PORT}:5000" \
  -v "${REGISTRY_DATA_DIR}:/var/lib/registry" \
  registry:2

echo "Local registry is running on port ${REGISTRY_PORT}."
