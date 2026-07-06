#!/usr/bin/env bash
set -euo pipefail

REGISTRY="${REGISTRY:-192.168.56.12:5000}"
VERSION="${VERSION:-dev}"
SERVICES="${SERVICES:-ai-cs-gateway ai-cs-user ai-cs-knowledge ai-cs-chat ai-cs-search ai-cs-message ai-cs-notify}"

image_name() {
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

echo "Registry: ${REGISTRY}"
echo "Version: ${VERSION}"
echo "Services: ${SERVICES}"

for service in ${SERVICES}; do
  image="$(image_name "${service}")"
  full_image="${REGISTRY}/aics/${image}:${VERSION}"
  latest_image="${REGISTRY}/aics/${image}:latest"

  echo "Building ${service} -> ${full_image}"
  docker build \
    -f "${service}/Dockerfile" \
    -t "${full_image}" \
    -t "${latest_image}" \
    .

  echo "Pushing ${full_image}"
  docker push "${full_image}"
  docker push "${latest_image}"
done

echo "Image build and push completed."
