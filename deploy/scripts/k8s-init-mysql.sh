#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${NAMESPACE:-ai-customer-service}"
MYSQL_POD="${MYSQL_POD:-mysql-master-0}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root}"
INIT_SQL="${INIT_SQL:-deploy/mysql/init.sql}"

if [[ ! -f "${INIT_SQL}" ]]; then
  echo "Init SQL not found: ${INIT_SQL}" >&2
  exit 1
fi

echo "Waiting for MySQL pod ${MYSQL_POD}..."
kubectl wait --for=condition=Ready "pod/${MYSQL_POD}" -n "${NAMESPACE}" --timeout=300s

echo "Initializing MySQL databases from ${INIT_SQL}"
kubectl exec -i "${MYSQL_POD}" -n "${NAMESPACE}" -- \
  mysql -uroot "-p${MYSQL_ROOT_PASSWORD}" < "${INIT_SQL}"

echo "MySQL initialization completed."
