#!/usr/bin/env bash
set -euo pipefail

REGISTRY="${REGISTRY:-192.168.56.12:5000}"
VERSION="${VERSION:-dev}"
NAMESPACE="${NAMESPACE:-ai-customer-service}"
SERVICES="${SERVICES:-ai-cs-gateway ai-cs-user ai-cs-knowledge ai-cs-chat ai-cs-search ai-cs-message ai-cs-notify}"
SERVICE_ROLLOUT_TIMEOUT="${SERVICE_ROLLOUT_TIMEOUT:-600s}"

dump_deployment_debug() {
  local deployment="$1"

  echo "==== Deployment debug: ${deployment} ===="
  kubectl get deployment "${deployment}" -n "${NAMESPACE}" -o wide || true

  echo "==== ReplicaSets for ${deployment} ===="
  kubectl get rs -n "${NAMESPACE}" -l "app=${deployment}" -o wide || true

  echo "==== Pods for ${deployment} ===="
  kubectl get pods -n "${NAMESPACE}" -l "app=${deployment}" -o wide || true

  echo "==== Recent events for ${deployment} ===="
  kubectl get events -n "${NAMESPACE}" --sort-by=.lastTimestamp | grep -E "${deployment}|Failed|BackOff|Unhealthy|Pulling|Pulled|Created|Started" | tail -n 80 || true

  while read -r pod; do
    if [[ -z "${pod}" ]]; then
      continue
    fi

    echo "==== Describe pod ${pod} ===="
    kubectl describe pod -n "${NAMESPACE}" "${pod}" || true

    echo "==== Logs pod ${pod} ===="
    kubectl logs -n "${NAMESPACE}" "${pod}" --all-containers --tail=120 || true

    echo "==== Previous logs pod ${pod} ===="
    kubectl logs -n "${NAMESPACE}" "${pod}" --all-containers --previous --tail=120 || true
  done < <(kubectl get pods -n "${NAMESPACE}" -l "app=${deployment}" -o jsonpath='{range .items[*]}{.metadata.name}{"\n"}{end}' 2>/dev/null || true)
}

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
echo "Service rollout timeout: ${SERVICE_ROLLOUT_TIMEOUT}"

for service in ${SERVICES}; do
  deployment="$(deployment_name "${service}")"
  container="$(container_name "${service}")"
  image="$(image_name "${service}")"
  full_image="${REGISTRY}/aics/${image}:${VERSION}"

  echo "Deploying ${deployment} with image ${full_image}"
  kubectl set image "deployment/${deployment}" "${container}=${full_image}" -n "${NAMESPACE}"
  if ! kubectl rollout status "deployment/${deployment}" -n "${NAMESPACE}" --timeout="${SERVICE_ROLLOUT_TIMEOUT}"; then
    dump_deployment_debug "${deployment}"
    exit 1
  fi
done

echo "Business service deployment completed."
