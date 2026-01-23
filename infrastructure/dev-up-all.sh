#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.dev.yml}"
COMPOSE=(docker-compose -f "${COMPOSE_FILE}")

WAIT_TIMEOUT_SECONDS="${WAIT_TIMEOUT_SECONDS:-180}"
WAIT_SLEEP_SECONDS="${WAIT_SLEEP_SECONDS:-2}"

wait_for_url() {
  local url="$1"
  local timeout="${2:-$WAIT_TIMEOUT_SECONDS}"
  local sleep_s="${3:-$WAIT_SLEEP_SECONDS}"

  local start
  start=$(date +%s)

  while true; do
    if curl -fsS --max-time 2 "$url" >/dev/null 2>&1; then
      return 0
    fi

    local now
    now=$(date +%s)
    if (( now - start >= timeout )); then
      echo "❌ Timed out waiting for URL: $url" >&2
      return 1
    fi

    sleep "$sleep_s"
  done
}

wait_for_service_healthy() {
  local service="$1"
  local timeout="${2:-$WAIT_TIMEOUT_SECONDS}"
  local sleep_s="${3:-$WAIT_SLEEP_SECONDS}"

  local start
  start=$(date +%s)

  local cid
  cid=$(${COMPOSE[@]} ps -q "$service" 2>/dev/null || true)
  if [[ -z "$cid" ]]; then
    echo "❌ Could not find container for service '$service'" >&2
    return 1
  fi

  while true; do
    local status
    status=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$cid" 2>/dev/null || true)

    case "$status" in
      healthy)
        return 0
        ;;
      running)
        # no healthcheck defined; treat as ready
        return 0
        ;;
      exited|dead)
        echo "❌ Service '$service' is '$status'" >&2
        return 1
        ;;
    esac

    local now
    now=$(date +%s)
    if (( now - start >= timeout )); then
      echo "❌ Timed out waiting for service '$service' to become healthy (last status: $status)" >&2
      return 1
    fi

    sleep "$sleep_s"
  done
}

wait_for_service_started() {
  local service="$1"
  local timeout="${2:-$WAIT_TIMEOUT_SECONDS}"
  local sleep_s="${3:-$WAIT_SLEEP_SECONDS}"

  local start
  start=$(date +%s)

  local cid
  cid=$(${COMPOSE[@]} ps -q "$service" 2>/dev/null || true)
  if [[ -z "$cid" ]]; then
    echo "❌ Could not find container for service '$service'" >&2
    return 1
  fi

  while true; do
    local status
    status=$(docker inspect -f '{{.State.Status}}' "$cid" 2>/dev/null || true)
    case "$status" in
      running)
        return 0
        ;;
      exited|dead)
        echo "❌ Service '$service' is '$status'" >&2
        return 1
        ;;
    esac

    local now
    now=$(date +%s)
    if (( now - start >= timeout )); then
      echo "❌ Timed out waiting for service '$service' to start (last status: $status)" >&2
      return 1
    fi

    sleep "$sleep_s"
  done
}

smoke_check_turnaround_session_creation() {
  local flight_id="DEVUP$((RANDOM%9000+1000))"
  local ts
  ts=$(date -u +%Y-%m-%dT%H:%M:%SZ)

  echo "🧪 Smoke check: creating turnaround session for flight ${flight_id}..."

  curl -fsS -X POST http://localhost:8094/cv-event-ingest \
    -H 'Content-Type: application/json' \
    -d "{\"flight_id\":\"${flight_id}\",\"event_type\":\"bridge_connect\",\"timestamp\":\"${ts}\",\"icao_code\":\"VIDP\",\"stand\":\"A3\"}" >/dev/null

  # Retry DB lookup briefly (async pipeline)
  local start
  start=$(date +%s)
  while true; do
    if ${COMPOSE[@]} exec -T timescaledb psql -U postgres -d utam -Atc \
      "select count(*) from turnaround_sessions where flight_id='${flight_id}';" 2>/dev/null | grep -q '^1$'; then
      echo "✅ Smoke check passed: session row created (${flight_id})."
      return 0
    fi

    local now
    now=$(date +%s)
    if (( now - start >= 60 )); then
      echo "❌ Smoke check failed: no session row created for ${flight_id} within 60s" >&2
      return 1
    fi

    sleep 2
  done
}

main() {
  echo "🚀 Starting TAM dev stack (${COMPOSE_FILE})..."
  ${COMPOSE[@]} up -d

  echo "⏳ Waiting for core services..."
  wait_for_service_healthy redpanda
  wait_for_service_healthy timescaledb
  wait_for_service_healthy backend

  # NiFi healthcheck can flap on cold start; ensure container is up, then rely on API readiness.
  wait_for_service_started nifi

  echo "⏳ Waiting for NiFi API..."
  wait_for_url "http://localhost:8091/nifi-api/flow/about" "${WAIT_TIMEOUT_SECONDS}"

  echo "🔧 Configuring NiFi flows..."
  bash infrastructure/nifi/setup-nifi.sh

  echo "🔎 Verifying NiFi → Kafka ingestion..."
  bash infrastructure/nifi/verify_nifi.sh

  smoke_check_turnaround_session_creation

  echo "🎉 dev-up completed: stack is up, NiFi configured, and smoke checks passed."
}

main "$@"
