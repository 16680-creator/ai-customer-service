#!/usr/bin/env bash
set -euo pipefail

REGISTRY="${REGISTRY:-192.168.56.12:5000}"
VERSION="${VERSION:-dev}"
SERVICES="${SERVICES:-ai-cs-gateway ai-cs-user ai-cs-knowledge ai-cs-chat ai-cs-search ai-cs-message ai-cs-notify}"
MAVEN_IMAGE="${MAVEN_IMAGE:-docker.m.daocloud.io/library/maven:3.9-eclipse-temurin-17}"
BUILD_JAR_DIR="${BUILD_JAR_DIR:-deploy/build-jars}"

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
echo "Maven image: ${MAVEN_IMAGE}"

MAVEN_PROJECTS="$(echo "${SERVICES}" | tr ' ' ',')"

echo "Building Maven modules once: ${MAVEN_PROJECTS}"
mkdir -p "${HOME}/.m2"
docker run --rm \
  -u "$(id -u):$(id -g)" \
  -v "${PWD}:/workspace" \
  -v "${HOME}/.m2:/maven/.m2" \
  -w /workspace \
  "${MAVEN_IMAGE}" \
  mvn -Dmaven.repo.local=/maven/.m2/repository -pl "${MAVEN_PROJECTS}" -am package -DskipTests -B

rm -rf "${BUILD_JAR_DIR}"
mkdir -p "${BUILD_JAR_DIR}"

for service in ${SERVICES}; do
  image="$(image_name "${service}")"
  full_image="${REGISTRY}/aics/${image}:${VERSION}"
  latest_image="${REGISTRY}/aics/${image}:latest"
  jar_file="$(find "${service}/target" -maxdepth 1 -type f -name "*.jar" ! -name "*.original" | head -n 1)"

  if [[ -z "${jar_file}" ]]; then
    echo "No runnable jar found for ${service} under ${service}/target." >&2
    exit 1
  fi

  if command -v unzip >/dev/null 2>&1; then
    if ! unzip -p "${jar_file}" META-INF/MANIFEST.MF | grep -q "org.springframework.boot.loader"; then
      echo "Jar is not a Spring Boot executable jar: ${jar_file}" >&2
      echo "Check spring-boot-maven-plugin repackage configuration." >&2
      exit 1
    fi
  else
    echo "unzip is not available; skipping executable jar manifest check."
  fi

  cp "${jar_file}" "${BUILD_JAR_DIR}/${service}.jar"

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
