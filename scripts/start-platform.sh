#!/usr/bin/env bash
set -e

cd /home/bam/Projects/enterprise-devops-platform

# Stop Docker services from this project
printf '\n==> Stopping docker compose stack...\n'
docker compose down >/dev/null 2>&1 || true

# Stop stale Java processes from previous IDE runs
printf '\n==> Stopping stale Java processes...\n'
ps -eo pid,cmd --no-headers | awk '$2 ~ /java/ {print $1}' | xargs -r kill >/dev/null 2>&1 || true

# Start infrastructure
printf '\n==> Starting infrastructure...\n'
docker compose up -d postgres zookeeper kafka config-server discovery-server

# Wait for config-server
printf '\n==> Waiting for config-server on 8888...\n'
for i in {1..60}; do
  if curl -fsS http://localhost:8888/actuator/health >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

# Wait for discovery-server
printf '\n==> Waiting for discovery-server on 8761...\n'
for i in {1..60}; do
  if curl -fsS http://localhost:8761/actuator/health >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

# Start services in dependency order
printf '\n==> Starting application services...\n'
mkdir -p /tmp/enterprise-platform-logs

start_service() {
  local dir=$1
  local name=$2
  (cd "$dir" && mvn spring-boot:run > "/tmp/enterprise-platform-logs/${name}.log" 2>&1) &
  echo "Started ${name}"
}

start_service services/department-service department-service
sleep 6
start_service services/employee-service employee-service
sleep 6
start_service services/auth-service auth-service
sleep 6
start_service services/gateway-service gateway-service

printf '\n==> Startup commands launched.\n'
printf 'Check logs in /tmp/enterprise-platform-logs\n'
printf 'Eureka: http://localhost:8761\n'
printf 'Gateway: http://localhost:8080\n'
