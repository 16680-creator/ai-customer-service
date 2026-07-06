#!/usr/bin/env bash
set -euo pipefail

REGISTRY="${REGISTRY:-192.168.56.12:5000}"
DAEMON_JSON="/etc/docker/daemon.json"

if [[ "$(id -u)" -ne 0 ]]; then
  echo "Please run this script with sudo." >&2
  exit 1
fi

mkdir -p /etc/docker

if [[ -f "${DAEMON_JSON}" ]]; then
  cp "${DAEMON_JSON}" "${DAEMON_JSON}.bak.$(date +%Y%m%d%H%M%S)"
fi

cat > "${DAEMON_JSON}" <<EOF
{
  "insecure-registries": ["${REGISTRY}"]
}
EOF

systemctl restart docker
systemctl status docker --no-pager

echo "Docker has been configured for insecure registry: ${REGISTRY}"
