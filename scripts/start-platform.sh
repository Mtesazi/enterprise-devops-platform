#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="${LOG_DIR:-/tmp/enterprise-platform-logs}"
mkdir -p "$LOG_DIR"

wait_for_http() {
  local url="$1"
  local name="$2"
  local timeout_seconds="${3:-120}"
  local start_time
  start_time=$(date +%s)

  echo "==> Waiting for ${name} on ${url}..."
  until curl -fsS "$url" >/dev/null 2>&1; do
    local now
    now=$(date +%s)
    if (( now - start_time > timeout_seconds )); then
      echo "ERROR: ${name} did not become ready in ${timeout_seconds}s" >&2
      return 1
    fi
    sleep 2
  done
}

stop_by_pattern() {
  local pattern="$1"
  local pids
  pids=$(ps -eo pid,cmd --no-headers | awk -v pat="$pattern" '$0 ~ pat {print $1}')
  if [[ -n "$pids" ]]; then
    echo "$pids" | while read -r pid; do
      if [[ -n "$pid" ]]; then
        kill "$pid" || true
      fi
    done
  fi
}

echo "==> Stopping docker compose stack..."
docker compose -f "$ROOT_DIR/docker-compose.yml" down --remove-orphans >/dev/null 2>&1 || true

echo "==> Stopping stale Java processes..."
stop_by_pattern 'spring-boot|ConfigServerApplication|DiscoveryServerApplication|EmployeeServiceApplication|DepartmentServiceApplication|AuthServiceApplication|GatewayServiceApplication'
sleep 2

echo "==> Starting infrastructure..."
docker compose -f "$ROOT_DIR/docker-compose.yml" up -d postgres zookeeper kafka

start_service() {
  local name="$1"
  local module_path="$2"
  local url="$3"
  local log_file="$LOG_DIR/${name}.log"

  echo "==> Starting ${name}..."
  nohup mvn -f "$ROOT_DIR/$module_path" -q spring-boot:run >"$log_file" 2>&1 &
  wait_for_http "$url" "$name" 180
  echo "Started ${name}"
}

start_service "config-server" "services/config-server/pom.xml" "http://localhost:8888/actuator/health"
start_service "discovery-server" "services/discovery-server/pom.xml" "http://localhost:8761/actuator/health"
start_service "department-service" "services/department-service/pom.xml" "http://localhost:8082/actuator/health"
start_service "employee-service" "services/employee-service/pom.xml" "http://localhost:8081/actuator/health"
start_service "auth-service" "services/auth-service/pom.xml" "http://localhost:8083/actuator/health"
start_service "gateway-service" "services/gateway-service/pom.xml" "http://localhost:8080/actuator/health"

echo "==> Startup commands launched."
echo "Check logs in ${LOG_DIR}"
echo "Eureka: http://localhost:8761"
echo "Gateway: http://localhost:8080"
