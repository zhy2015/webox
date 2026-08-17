#!/usr/bin/env bash
set -euo pipefail

api_base="${WEBOX_API_BASE:-http://localhost:8080}"
tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/webox-concurrency.XXXXXX")"
delivery_date="$(node -e '
  const parts = new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Shanghai", year: "numeric", month: "2-digit", day: "2-digit" })
    .formatToParts(new Date(Date.now() + 86400000))
    .reduce((values, part) => ({ ...values, [part.type]: part.value }), {});
  process.stdout.write(`${parts.year}-${parts.month}-${parts.day}`);
')"
restore_pending="false"
successful_order_id=""
successful_cookie=""
successful_token=""

cleanup() {
  if [[ -n "$successful_order_id" ]]; then
    curl -sS -b "$successful_cookie" -H "X-XSRF-TOKEN: $successful_token" \
      -X POST "$api_base/api/v1/orders/$successful_order_id/cancel" > /dev/null || true
  fi
  if [[ "$restore_pending" == "true" ]]; then
    curl -sS -b "$tmp_dir/admin-cookies.txt" -H 'Content-Type: application/json' \
      -H "X-XSRF-TOKEN: $(<"$tmp_dir/admin-token.txt")" \
      -X PUT --data-binary "@$tmp_dir/menu-original.json" \
      "$api_base/api/v1/console/menus/$delivery_date" > /dev/null || true
  fi
  rm -rf "$tmp_dir"
}
trap cleanup EXIT

create_session() {
  local name="$1"
  local email="$2"
  local password="$3"
  curl -fsS -c "$tmp_dir/$name-cookies.txt" "$api_base/api/v1/auth/csrf" > "$tmp_dir/$name-csrf.json"
  node -e 'const fs=require("fs"); process.stdout.write(JSON.parse(fs.readFileSync(process.argv[1], "utf8")).token);' \
    "$tmp_dir/$name-csrf.json" > "$tmp_dir/$name-token.txt"
  curl -fsS -b "$tmp_dir/$name-cookies.txt" -c "$tmp_dir/$name-cookies.txt" \
    -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $(<"$tmp_dir/$name-token.txt")" \
    -d "{\"email\":\"$email\",\"password\":\"$password\"}" \
    "$api_base/api/v1/auth/register" > "$tmp_dir/$name-user.json"
}

curl -fsS -c "$tmp_dir/admin-cookies.txt" "$api_base/api/v1/auth/csrf" > "$tmp_dir/admin-csrf.json"
node -e 'const fs=require("fs"); process.stdout.write(JSON.parse(fs.readFileSync(process.argv[1], "utf8")).token);' \
  "$tmp_dir/admin-csrf.json" > "$tmp_dir/admin-token.txt"
curl -fsS -b "$tmp_dir/admin-cookies.txt" -c "$tmp_dir/admin-cookies.txt" \
  -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $(<"$tmp_dir/admin-token.txt")" \
  -d '{"email":"admin@webox.local","password":"Admin123"}' \
  "$api_base/api/v1/auth/login" > "$tmp_dir/admin-user.json"
curl -fsS -b "$tmp_dir/admin-cookies.txt" \
  "$api_base/api/v1/console/menus/$delivery_date" > "$tmp_dir/admin-menu.json"

node -e '
  const fs = require("fs");
  const menu = JSON.parse(fs.readFileSync(process.argv[1], "utf8"));
  const target = menu.find(item => item.remainingStock > 0);
  if (!target) throw new Error("No in-stock item is available for the concurrency check");
  const allocated = target.initialStock - target.remainingStock;
  fs.writeFileSync(process.argv[2], JSON.stringify(menu.map(item => ({ dishId: item.dishId, stock: item.initialStock }))));
  fs.writeFileSync(process.argv[3], JSON.stringify(menu.map(item => ({
    dishId: item.dishId,
    stock: item.dishId === target.dishId ? allocated + 1 : item.initialStock
  }))));
  fs.writeFileSync(process.argv[4], String(target.dishId));
' "$tmp_dir/admin-menu.json" "$tmp_dir/menu-original.json" "$tmp_dir/menu-constrained.json" "$tmp_dir/target-dish.txt"

restore_pending="true"
curl -fsS -b "$tmp_dir/admin-cookies.txt" -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: $(<"$tmp_dir/admin-token.txt")" \
  -X PUT --data-binary "@$tmp_dir/menu-constrained.json" \
  "$api_base/api/v1/console/menus/$delivery_date" > "$tmp_dir/menu-constrained-response.json"

run_id="$(date +%s)-$RANDOM"
create_session "employee-one" "race-one+$run_id@webox.local" "Verify123"
create_session "employee-two" "race-two+$run_id@webox.local" "Verify123"
curl -fsS -b "$tmp_dir/employee-one-cookies.txt" "$api_base/api/v1/dishes/$(<"$tmp_dir/target-dish.txt")" > "$tmp_dir/dish.json"
node -e '
  const fs = require("fs");
  const dish = JSON.parse(fs.readFileSync(process.argv[1], "utf8"));
  const selectedOptionIds = (dish.optionGroups || []).filter(group => group.required)
    .map(group => group.options[0]?.id).filter(Boolean);
  fs.writeFileSync(process.argv[2], JSON.stringify({
    deliveryDate: process.argv[3], mealPeriod: "Lunch", deliveryAddress: "Concurrency Desk",
    items: [{ dishId: dish.id, quantity: 1, selectedOptionIds }]
  }));
' "$tmp_dir/dish.json" "$tmp_dir/order-request.json" "$delivery_date"

curl -sS -o "$tmp_dir/order-one.json" -w '%{http_code}' -b "$tmp_dir/employee-one-cookies.txt" \
  -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $(<"$tmp_dir/employee-one-token.txt")" \
  -H "Idempotency-Key: race-one-$run_id" --data-binary "@$tmp_dir/order-request.json" \
  "$api_base/api/v1/orders" > "$tmp_dir/status-one.txt" &
pid_one="$!"
curl -sS -o "$tmp_dir/order-two.json" -w '%{http_code}' -b "$tmp_dir/employee-two-cookies.txt" \
  -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $(<"$tmp_dir/employee-two-token.txt")" \
  -H "Idempotency-Key: race-two-$run_id" --data-binary "@$tmp_dir/order-request.json" \
  "$api_base/api/v1/orders" > "$tmp_dir/status-two.txt" &
pid_two="$!"
wait "$pid_one"
wait "$pid_two"

status_one="$(<"$tmp_dir/status-one.txt")"
status_two="$(<"$tmp_dir/status-two.txt")"
if [[ "$status_one $status_two" != "200 409" ]] && [[ "$status_one $status_two" != "409 200" ]]; then
  echo "Expected one 200 and one 409 from the inventory race, received $status_one and $status_two" >&2
  exit 1
fi

if [[ "$status_one" == "200" ]]; then
  success_body="$tmp_dir/order-one.json"
  failure_body="$tmp_dir/order-two.json"
  successful_cookie="$tmp_dir/employee-one-cookies.txt"
  successful_token="$(<"$tmp_dir/employee-one-token.txt")"
else
  success_body="$tmp_dir/order-two.json"
  failure_body="$tmp_dir/order-one.json"
  successful_cookie="$tmp_dir/employee-two-cookies.txt"
  successful_token="$(<"$tmp_dir/employee-two-token.txt")"
fi

successful_order_id="$(node -e 'const fs=require("fs"); process.stdout.write(String(JSON.parse(fs.readFileSync(process.argv[1], "utf8")).id));' "$success_body")"
failure_code="$(node -e 'const fs=require("fs"); process.stdout.write(JSON.parse(fs.readFileSync(process.argv[1], "utf8")).code);' "$failure_body")"
if [[ "$failure_code" != "INSUFFICIENT_STOCK" ]]; then
  echo "Expected INSUFFICIENT_STOCK from the losing request, received $failure_code" >&2
  exit 1
fi

curl -fsS -b "$successful_cookie" -H "X-XSRF-TOKEN: $successful_token" \
  -X POST "$api_base/api/v1/orders/$successful_order_id/cancel" > "$tmp_dir/cancelled.json"
successful_order_id=""
curl -fsS -b "$tmp_dir/admin-cookies.txt" -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: $(<"$tmp_dir/admin-token.txt")" \
  -X PUT --data-binary "@$tmp_dir/menu-original.json" \
  "$api_base/api/v1/console/menus/$delivery_date" > "$tmp_dir/menu-restored.json"
restore_pending="false"

echo "Concurrency smoke passed: one order won the final item and one was rejected without overselling"
