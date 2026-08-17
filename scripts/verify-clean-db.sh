#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
container_name="webox-clean-$$-$RANDOM"
tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/webox-clean-db.XXXXXX")"
backend_pid=""
backend_port="$((20000 + RANDOM % 10000))"

if [[ "$(uname -s)" == "Darwin" ]] && [[ -x /usr/libexec/java_home ]]; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

cleanup() {
  if [[ -n "$backend_pid" ]]; then
    kill "$backend_pid" 2>/dev/null || true
    wait "$backend_pid" 2>/dev/null || true
  fi
  docker rm -f "$container_name" > /dev/null 2>&1 || true
  rm -rf "$tmp_dir"
}
trap cleanup EXIT

docker run -d --rm --name "$container_name" \
  -e MYSQL_DATABASE=webox \
  -e MYSQL_USER=webox \
  -e MYSQL_PASSWORD=webox_clean \
  -e MYSQL_ROOT_PASSWORD=root_clean \
  -e TZ=Asia/Shanghai \
  -p 127.0.0.1::3306 \
  --health-cmd='mysqladmin ping -h localhost -uroot -proot_clean' \
  --health-interval=2s --health-timeout=3s --health-retries=30 \
  mysql:8.4 --character-set-server=utf8mb4 --collation-server=utf8mb4_0900_ai_ci > /dev/null

for _ in {1..45}; do
  status="$(docker inspect -f '{{.State.Health.Status}}' "$container_name")"
  if [[ "$status" == "healthy" ]]; then
    break
  fi
  if [[ "$status" == "unhealthy" ]]; then
    docker logs "$container_name" >&2
    exit 1
  fi
  sleep 1
done

mysql_port="$(docker port "$container_name" 3306/tcp | sed 's/.*://')"
if [[ -z "$mysql_port" ]]; then
  echo "Could not resolve the temporary MySQL port" >&2
  exit 1
fi

(
  cd "$project_root/backend"
  DB_URL="jdbc:mysql://127.0.0.1:$mysql_port/webox?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai" \
  DB_USERNAME=webox DB_PASSWORD=webox_clean SERVER_PORT="$backend_port" \
    ./mvnw spring-boot:run -q
) > "$tmp_dir/backend.log" 2>&1 &
backend_pid="$!"

for _ in {1..45}; do
  if curl -fsS "http://127.0.0.1:$backend_port/actuator/health" > "$tmp_dir/health.json" 2>/dev/null; then
    break
  fi
  if ! kill -0 "$backend_pid" 2>/dev/null; then
    cat "$tmp_dir/backend.log" >&2
    exit 1
  fi
  sleep 1
done

if [[ ! -f "$tmp_dir/health.json" ]]; then
  cat "$tmp_dir/backend.log" >&2
  echo "Temporary backend health check timed out" >&2
  exit 1
fi
health="$(<"$tmp_dir/health.json")"
if [[ "$health" != *'"status":"UP"'* ]]; then
  cat "$tmp_dir/backend.log" >&2
  echo "Temporary backend did not become healthy" >&2
  exit 1
fi

read -r migration_count user_count dish_count menu_count < <(
  docker exec "$container_name" mysql -N -uroot -proot_clean webox -e \
    "SELECT (SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1), (SELECT COUNT(*) FROM users), (SELECT COUNT(*) FROM dishes), (SELECT COUNT(*) FROM daily_menu_items);"
)

if (( migration_count < 1 || user_count < 2 || dish_count < 9 || menu_count < 18 )); then
  echo "Unexpected clean seed counts: migrations=$migration_count users=$user_count dishes=$dish_count menuItems=$menu_count" >&2
  exit 1
fi

echo "Clean database verification passed: migrations=$migration_count users=$user_count dishes=$dish_count menuItems=$menu_count"
