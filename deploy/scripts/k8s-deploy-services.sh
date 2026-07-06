#!/usr/bin/env bash
set -euo pipefail

REGISTRY="${REGISTRY:-192.168.56.12:5000}"
VERSION="${VERSION:-dev}"
NAMESPACE="${NAMESPACE:-ai-customer-service}"
SERVICES="${SERVICES:-ai-cs-gateway ai-cs-user ai-cs-knowledge ai-cs-chat ai-cs-search ai-cs-message ai-cs-notify}"

deployment_name() {
  case "$1" in
    ai-cs-gateway) echo "api-gateway" ;;
    ai-cs-user) echo "user-service" ;;
    ai-cs-knowledge) echo "knowledge-service" ;;
    ai-cs-chat) echo "ai-chat-service" ;;
    ai-cs-search) echo "search-service" ;;
    ai-cs-message) echo "message-service" ;;
    ai-cs-notify) echo "notify-service" ;;
    *) echo "Unknown service module: $1" >&2; exit 1 ;;
  esac
}

container_name() {
  deployment_name "$1"
}

image_name() {
  deployment_name "$1"
}

kubectl apply -f deploy/k8s/namespace.yaml
kubectl apply -f deploy/k8s/services/

for service in ${SERVICES}; do
  deployment="$(deployment_name "${service}")"
  container="$(container_name "${service}")"
  image="$(image_name "${service}")"
  full_image="${REGISTRY}/aics/${image}:${VERSION}"

  echo "Deploying ${deployment} with image ${full_image}"
  kubectl set image "deployment/${deployment}" "${container}=${full_image}" -n "${NAMESPACE}"
  kubectl rollout status "deployment/${deployment}" -n "${NAMESPACE}" --timeout=300s
done

echo "Business service deployment completed."
