#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/webox-verify.XXXXXX")"
backend_pid=""

cleanup() {
  if [[ -n "$backend_pid" ]]; then
    kill "$backend_pid" 2>/dev/null || true
    wait "$backend_pid" 2>/dev/null || true
  fi
  rm -rf "$tmp_dir"
}
trap cleanup EXIT

if [[ "$(uname -s)" == "Darwin" ]] && [[ -x /usr/libexec/java_home ]]; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

echo "[1/7] Starting standalone MySQL"
docker compose -f "$project_root/infra/compose.yaml" up -d --wait

echo "[2/7] Running backend tests"
(cd "$project_root/backend" && ./mvnw -q test)

echo "[3/7] Running frontend checks and production build"
(cd "$project_root/frontend" && npm ci --silent && npm run typecheck && npm test && npm run build)

if ! curl -fsS http://localhost:8080/actuator/health > /dev/null 2>&1; then
  echo "[4/7] Starting backend for integration checks"
  (cd "$project_root/backend" && ./mvnw spring-boot:run -q) > "$tmp_dir/backend.log" 2>&1 &
  backend_pid="$!"
  for _ in {1..30}; do
    if curl -fsS http://localhost:8080/actuator/health > /dev/null 2>&1; then
      break
    fi
    if ! kill -0 "$backend_pid" 2>/dev/null; then
      cat "$tmp_dir/backend.log" >&2
      exit 1
    fi
    sleep 1
  done
fi

health="$(curl -fsS http://localhost:8080/actuator/health)"
if [[ "$health" != *'"status":"UP"'* ]]; then
  echo "Backend health check did not report UP: $health" >&2
  exit 1
fi
curl -fsS http://localhost:8080/v3/api-docs > /dev/null

echo "[5/7] Running real-MySQL API smoke"
"$project_root/scripts/smoke-api.sh"

echo "[6/7] Running concurrent inventory smoke"
"$project_root/scripts/smoke-concurrency.sh"

echo "[7/7] Rehearsing an isolated empty database"
"$project_root/scripts/verify-clean-db.sh"

echo "WeBox verification passed"
