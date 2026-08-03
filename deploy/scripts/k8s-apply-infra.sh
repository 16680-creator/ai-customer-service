#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${NAMESPACE:-ai-customer-service}"
INIT_DATABASE="${INIT_DATABASE:-false}"
INFRA_ROLLOUT_TIMEOUT="${INFRA_ROLLOUT_TIMEOUT:-900s}"

dump_mysql_debug() {
  echo "==== MySQL debug: pods ===="
  kubectl get pods -n "${NAMESPACE}" -l app=mysql -o wide || true

  echo "==== MySQL debug: statefulsets ===="
  kubectl get statefulset -n "${NAMESPACE}" mysql-master mysql-slave -o wide || true

  echo "==== MySQL debug: pvc ===="
  kubectl get pvc -n "${NAMESPACE}" || true

  echo "==== MySQL debug: storageclass ===="
  kubectl get storageclass || true

  echo "==== MySQL debug: recent events ===="
  kubectl get events -n "${NAMESPACE}" --sort-by=.lastTimestamp | tail -n 80 || true

  for pod in mysql-master-0 mysql-slave-0 mysql-slave-1; do
    echo "==== MySQL debug: describe ${pod} ===="
    kubectl describe pod -n "${NAMESPACE}" "${pod}" || true

    echo "==== MySQL debug: logs ${pod} ===="
    kubectl logs -n "${NAMESPACE}" "${pod}" --tail=120 || true

    echo "==== MySQL debug: previous logs ${pod} ===="
    kubectl logs -n "${NAMESPACE}" "${pod}" --previous --tail=120 || true
  done
}

delete_stale_mysql_pods() {
  local statefulset="$1"
  local selector="$2"
  local desired_image

  desired_image="$(kubectl get statefulset -n "${NAMESPACE}" "${statefulset}" -o jsonpath='{.spec.template.spec.containers[?(@.name=="mysql")].image}' 2>/dev/null || true)"
  if [[ -z "${desired_image}" ]]; then
    return
  fi

  echo "Checking ${statefulset} pods use image ${desired_image}..."
  while read -r pod current_image; do
    if [[ -n "${pod}" && "${current_image}" != "${desired_image}" ]]; then
      echo "Deleting stale pod ${pod}: ${current_image} -> ${desired_image}"
      kubectl delete pod -n "${NAMESPACE}" "${pod}" --ignore-not-found=true
    fi
  done < <(kubectl get pods -n "${NAMESPACE}" -l "${selector}" -o jsonpath='{range .items[*]}{.metadata.name}{" "}{.spec.containers[?(@.name=="mysql")].image}{"\n"}{end}' 2>/dev/null || true)
}

echo "Applying namespace..."
echo "Infrastructure rollout timeout: ${INFRA_ROLLOUT_TIMEOUT}"
kubectl apply -f deploy/k8s/namespace.yaml

if ! kubectl get storageclass local-path >/dev/null 2>&1; then
  echo "StorageClass local-path is missing." >&2
  echo "Install local-path-provisioner first:" >&2
  echo "kubectl apply -f https://raw.githubusercontent.com/rancher/local-path-provisioner/master/deploy/local-path-storage.yaml" >&2
  echo "If GitHub raw is slow, download the yaml with a mirror and then run: kubectl apply -f local-path-storage.yaml" >&2
  exit 1
fi

echo "Applying MySQL..."
kubectl apply -f deploy/k8s/mysql.yaml
delete_stale_mysql_pods "mysql-master" "app=mysql,role=master"
delete_stale_mysql_pods "mysql-slave" "app=mysql,role=slave"
if ! kubectl rollout status statefulset/mysql-master -n "${NAMESPACE}" --timeout="${INFRA_ROLLOUT_TIMEOUT}"; then
  dump_mysql_debug
  exit 1
fi
if ! kubectl rollout status statefulset/mysql-slave -n "${NAMESPACE}" --timeout="${INFRA_ROLLOUT_TIMEOUT}"; then
  dump_mysql_debug
  exit 1
fi

if [[ "${INIT_DATABASE}" == "true" ]]; then
  NAMESPACE="${NAMESPACE}" bash deploy/scripts/k8s-init-mysql.sh
fi

echo "Applying infrastructure components..."
kubectl apply -f deploy/k8s/nacos.yaml
kubectl apply -f deploy/k8s/redis.yaml
kubectl apply -f deploy/k8s/elasticsearch.yaml
kubectl apply -f deploy/k8s/rocketmq.yaml
kubectl apply -f deploy/k8s/minio.yaml

kubectl rollout status deployment/nacos -n "${NAMESPACE}" --timeout="${INFRA_ROLLOUT_TIMEOUT}"
kubectl rollout status statefulset/redis -n "${NAMESPACE}" --timeout="${INFRA_ROLLOUT_TIMEOUT}"
kubectl rollout status statefulset/elasticsearch -n "${NAMESPACE}" --timeout="${INFRA_ROLLOUT_TIMEOUT}"
kubectl rollout status deployment/rocketmq-namesrv -n "${NAMESPACE}" --timeout="${INFRA_ROLLOUT_TIMEOUT}"
kubectl rollout status deployment/rocketmq-broker -n "${NAMESPACE}" --timeout="${INFRA_ROLLOUT_TIMEOUT}"
kubectl rollout status statefulset/minio -n "${NAMESPACE}" --timeout="${INFRA_ROLLOUT_TIMEOUT}"

echo "Infrastructure deployment completed."
