#!/usr/bin/env bash
set -euo pipefail

REGISTRY="${REGISTRY:-192.168.56.12:5000}"
CONTAINERD_CONFIG="/etc/containerd/config.toml"
CERTS_DIR="/etc/containerd/certs.d/${REGISTRY}"

if [[ "$(id -u)" -ne 0 ]]; then
  echo "Please run this script with sudo." >&2
  exit 1
fi

if [[ ! -f "${CONTAINERD_CONFIG}" ]]; then
  mkdir -p /etc/containerd
  containerd config default > "${CONTAINERD_CONFIG}"
fi

mkdir -p "${CERTS_DIR}"
cat > "${CERTS_DIR}/hosts.toml" <<EOF
server = "http://${REGISTRY}"

[host."http://${REGISTRY}"]
  capabilities = ["pull", "resolve"]
  skip_verify = true
EOF

if ! grep -q 'config_path = "/etc/containerd/certs.d"' "${CONTAINERD_CONFIG}"; then
  sed -i 's#config_path = ""#config_path = "/etc/containerd/certs.d"#' "${CONTAINERD_CONFIG}"
fi

if grep -q 'SystemdCgroup = false' "${CONTAINERD_CONFIG}"; then
  sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' "${CONTAINERD_CONFIG}"
fi

systemctl restart containerd
systemctl status containerd --no-pager

echo "containerd has been configured for insecure registry: ${REGISTRY}"
