#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${NAMESPACE:-ai-customer-service}"
INIT_DATABASE="${INIT_DATABASE:-false}"

echo "Applying namespace..."
kubectl apply -f deploy/k8s/namespace.yaml

echo "Applying MySQL..."
kubectl apply -f deploy/k8s/mysql.yaml
kubectl rollout status statefulset/mysql-master -n "${NAMESPACE}" --timeout=300s

if [[ "${INIT_DATABASE}" == "true" ]]; then
  NAMESPACE="${NAMESPACE}" bash deploy/scripts/k8s-init-mysql.sh
fi

echo "Applying infrastructure components..."
kubectl apply -f deploy/k8s/nacos.yaml
kubectl apply -f deploy/k8s/redis.yaml
kubectl apply -f deploy/k8s/elasticsearch.yaml
kubectl apply -f deploy/k8s/rocketmq.yaml
kubectl apply -f deploy/k8s/minio.yaml

kubectl rollout status deployment/nacos -n "${NAMESPACE}" --timeout=300s || true
kubectl rollout status statefulset/redis -n "${NAMESPACE}" --timeout=300s || true
kubectl rollout status statefulset/elasticsearch -n "${NAMESPACE}" --timeout=300s || true
kubectl rollout status deployment/rocketmq-namesrv -n "${NAMESPACE}" --timeout=300s || true
kubectl rollout status deployment/rocketmq-broker -n "${NAMESPACE}" --timeout=300s || true
kubectl rollout status statefulset/minio -n "${NAMESPACE}" --timeout=300s || true

echo "Infrastructure deployment completed."
