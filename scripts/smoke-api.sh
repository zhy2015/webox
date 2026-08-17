#!/usr/bin/env bash
set -euo pipefail

api_base="${WEBOX_API_BASE:-http://localhost:8080}"
tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/webox-smoke.XXXXXX")"
menu_restore_pending="false"
admin_cookie_jar=""
admin_csrf_token=""

cleanup() {
  if [[ "$menu_restore_pending" == "true" ]]; then
    curl -sS -b "$admin_cookie_jar" \
      -H 'Content-Type: application/json' \
      -H "X-XSRF-TOKEN: $admin_csrf_token" \
      -X PUT --data-binary "@$tmp_dir/menu-original-request.json" \
      "$api_base/api/v1/console/menus/$delivery_date" > /dev/null || true
  fi
  rm -rf "$tmp_dir"
}
trap cleanup EXIT

cookie_jar="$tmp_dir/cookies.txt"
email="acceptance+$(date +%s)@webox.local"
password="Verify123"
delivery_date="$(node -e '
  const parts = new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Shanghai", year: "numeric", month: "2-digit", day: "2-digit" })
    .formatToParts(new Date(Date.now() + 86400000))
    .reduce((values, part) => ({ ...values, [part.type]: part.value }), {});
  process.stdout.write(`${parts.year}-${parts.month}-${parts.day}`);
')"

curl -fsS -c "$cookie_jar" "$api_base/api/v1/auth/csrf" > "$tmp_dir/csrf.json"
csrf_token="$(node -e 'let value=""; process.stdin.on("data", chunk => value += chunk).on("end", () => process.stdout.write(JSON.parse(value).token));' < "$tmp_dir/csrf.json")"

curl -fsS -b "$cookie_jar" -c "$cookie_jar" \
  -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: $csrf_token" \
  -d "{\"email\":\"$email\",\"password\":\"$password\"}" \
  "$api_base/api/v1/auth/register" > "$tmp_dir/user.json"

duplicate_status="$(curl -sS -o "$tmp_dir/duplicate-email.json" -w '%{http_code}' \
  -b "$cookie_jar" -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $csrf_token" \
  -d "{\"email\":\"$email\",\"password\":\"$password\"}" \
  "$api_base/api/v1/auth/register")"
if [[ "$duplicate_status" != "409" ]]; then
  echo "Expected duplicate registration to return 409, received $duplicate_status" >&2
  exit 1
fi

weak_password_status="$(curl -sS -o "$tmp_dir/weak-password.json" -w '%{http_code}' \
  -b "$cookie_jar" -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $csrf_token" \
  -d "{\"email\":\"weak-$email\",\"password\":\"weak\"}" \
  "$api_base/api/v1/auth/register")"
if [[ "$weak_password_status" != "400" ]]; then
  echo "Expected weak password registration to return 400, received $weak_password_status" >&2
  exit 1
fi

console_status="$(curl -sS -o "$tmp_dir/forbidden.json" -w '%{http_code}' \
  -b "$cookie_jar" "$api_base/api/v1/console/dishes")"
if [[ "$console_status" != "403" ]]; then
  echo "Expected employee Console access to return 403, received $console_status" >&2
  exit 1
fi

curl -fsS -b "$cookie_jar" \
  -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $csrf_token" \
  -X PUT -d '{"allergens":["Peanuts"],"cuisines":["Chinese"],"spiceLevel":"Medium","tasteIntensity":"Balanced","budgetMin":20,"budgetMax":50}' \
  "$api_base/api/v1/me/preferences" > "$tmp_dir/preferences.json"
curl -fsS -b "$cookie_jar" "$api_base/api/v1/me/preferences" > "$tmp_dir/preferences-read.json"
node -e '
  const fs = require("fs");
  const value = JSON.parse(fs.readFileSync(process.argv[1], "utf8"));
  if (!value.allergens.includes("Peanuts") || value.budgetMax !== 50) {
    throw new Error("Saved preferences were not returned correctly");
  }
' "$tmp_dir/preferences-read.json"
invalid_budget_status="$(curl -sS -o "$tmp_dir/invalid-budget.json" -w '%{http_code}' \
  -b "$cookie_jar" -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $csrf_token" \
  -X PUT -d '{"allergens":[],"cuisines":[],"spiceLevel":"","tasteIntensity":"","budgetMin":50,"budgetMax":20}' \
  "$api_base/api/v1/me/preferences")"
if [[ "$invalid_budget_status" != "400" ]]; then
  echo "Expected an invalid budget range to return 400, received $invalid_budget_status" >&2
  exit 1
fi

curl -fsS -b "$cookie_jar" "$api_base/api/v1/menus/$delivery_date" > "$tmp_dir/menu.json"

admin_cookie_jar="$tmp_dir/admin-cookies.txt"
curl -fsS -c "$admin_cookie_jar" "$api_base/api/v1/auth/csrf" > "$tmp_dir/admin-csrf.json"
admin_csrf_token="$(node -e 'let value=""; process.stdin.on("data", chunk => value += chunk).on("end", () => process.stdout.write(JSON.parse(value).token));' < "$tmp_dir/admin-csrf.json")"
curl -fsS -b "$admin_cookie_jar" -c "$admin_cookie_jar" \
  -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: $admin_csrf_token" \
  -d '{"email":"admin@webox.local","password":"Admin123"}' \
  "$api_base/api/v1/auth/login" > "$tmp_dir/admin.json"
curl -fsS -b "$admin_cookie_jar" \
  "$api_base/api/v1/console/menus/$delivery_date" > "$tmp_dir/admin-menu.json"

node -e '
  const fs = require("fs");
  const menu = JSON.parse(fs.readFileSync(process.argv[1], "utf8"));
  const target = menu.find(item => item.remainingStock > 0 && item.initialStock > 0);
  if (!target) throw new Error("No menu item can be used for the Console propagation check");
  const original = menu.map(item => ({ dishId: item.dishId, stock: item.initialStock }));
  const modified = menu.map(item => ({
    dishId: item.dishId,
    stock: item.dishId === target.dishId ? item.initialStock - 1 : item.initialStock
  }));
  fs.writeFileSync(process.argv[2], JSON.stringify(original));
  fs.writeFileSync(process.argv[3], JSON.stringify(modified));
  fs.writeFileSync(process.argv[4], JSON.stringify({ dishId: target.dishId, remainingStock: target.remainingStock - 1 }));
' "$tmp_dir/admin-menu.json" "$tmp_dir/menu-original-request.json" "$tmp_dir/menu-modified-request.json" "$tmp_dir/menu-expected.json"

menu_restore_pending="true"
curl -fsS -b "$admin_cookie_jar" \
  -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: $admin_csrf_token" \
  -X PUT --data-binary "@$tmp_dir/menu-modified-request.json" \
  "$api_base/api/v1/console/menus/$delivery_date" > "$tmp_dir/menu-updated.json"
curl -fsS -b "$cookie_jar" "$api_base/api/v1/menus/$delivery_date" > "$tmp_dir/employee-menu-updated.json"
node -e '
  const fs = require("fs");
  const expected = JSON.parse(fs.readFileSync(process.argv[1], "utf8"));
  const menu = JSON.parse(fs.readFileSync(process.argv[2], "utf8"));
  const actual = menu.find(item => item.id === expected.dishId);
  if (!actual || actual.remainingStock !== expected.remainingStock) {
    throw new Error(`Console stock change was not visible to the employee menu; expected ${expected.remainingStock}`);
  }
' "$tmp_dir/menu-expected.json" "$tmp_dir/employee-menu-updated.json"
curl -fsS -b "$admin_cookie_jar" \
  -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: $admin_csrf_token" \
  -X PUT --data-binary "@$tmp_dir/menu-original-request.json" \
  "$api_base/api/v1/console/menus/$delivery_date" > "$tmp_dir/menu-restored.json"
menu_restore_pending="false"

node -e '
  const fs = require("fs");
  const menu = JSON.parse(fs.readFileSync(process.argv[1], "utf8"));
  const configurable = menu.find(item => item.remainingStock >= 2 &&
    (item.optionGroups || []).some(group => group.required && !group.multiple && group.options.length >= 2));
  const dish = configurable || menu.find(item => item.remainingStock > 0);
  if (!dish) throw new Error("No in-stock dish is available for the smoke order");
  const requiredGroups = (dish.optionGroups || [])
    .filter(group => group.required)
  const selectedOptionIds = requiredGroups.map(group => group.options[0]?.id).filter(Boolean);
  const alternateGroup = requiredGroups.find(group => !group.multiple && group.options.length >= 2);
  const items = alternateGroup ? [
    { dishId: dish.id, quantity: 1, selectedOptionIds },
    { dishId: dish.id, quantity: 1, selectedOptionIds: requiredGroups.map(group =>
      group.id === alternateGroup.id ? group.options[1].id : group.options[0]?.id).filter(Boolean) }
  ] : [{ dishId: dish.id, quantity: 1, selectedOptionIds }];
  const payload = {
    deliveryDate: process.argv[3],
    mealPeriod: "Lunch",
    deliveryAddress: "Acceptance Desk, Building A",
    items
  };
  fs.writeFileSync(process.argv[2], JSON.stringify(payload));
' "$tmp_dir/menu.json" "$tmp_dir/order-request.json" "$delivery_date"

idempotency_key="acceptance-$(date +%s)-$RANDOM"
curl -fsS -b "$cookie_jar" \
  -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: $csrf_token" \
  -H "Idempotency-Key: $idempotency_key" \
  --data-binary "@$tmp_dir/order-request.json" \
  "$api_base/api/v1/orders" > "$tmp_dir/order.json"

curl -fsS -b "$cookie_jar" \
  -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: $csrf_token" \
  -H "Idempotency-Key: $idempotency_key" \
  --data-binary "@$tmp_dir/order-request.json" \
  "$api_base/api/v1/orders" > "$tmp_dir/order-replay.json"

read -r order_id order_number replay_number < <(node -e '
  const fs = require("fs");
  const order = JSON.parse(fs.readFileSync(process.argv[1], "utf8"));
  const replay = JSON.parse(fs.readFileSync(process.argv[2], "utf8"));
  process.stdout.write(`${order.id} ${order.orderNumber} ${replay.orderNumber}\n`);
' "$tmp_dir/order.json" "$tmp_dir/order-replay.json")

if [[ "$order_number" != "$replay_number" ]]; then
  echo "Idempotent replay returned a different order" >&2
  exit 1
fi

curl -fsS -b "$cookie_jar" \
  -H "X-XSRF-TOKEN: $csrf_token" \
  -X POST "$api_base/api/v1/orders/$order_id/cancel" > "$tmp_dir/cancelled.json"

cancelled_status="$(node -e 'let value=""; process.stdin.on("data", chunk => value += chunk).on("end", () => process.stdout.write(JSON.parse(value).status));' < "$tmp_dir/cancelled.json")"
if [[ "$cancelled_status" != "Cancelled" ]]; then
  echo "Expected the smoke order to be cancelled, received $cancelled_status" >&2
  exit 1
fi

echo "API smoke passed: $order_number was created once and cancelled for $email"
