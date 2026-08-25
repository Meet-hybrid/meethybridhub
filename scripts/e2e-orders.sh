#!/usr/bin/env bash
set -euo pipefail

# End-to-end order/payment/installment smoke test.
# Prerequisites:
#   - the application is running
#   - curl, jq, and openssl are installed
#   - OWNER_TOKEN belongs to a verified store owner
#   - CUSTOMER_TOKEN belongs to a verified customer
#
# Example:
#   OWNER_TOKEN=... CUSTOMER_TOKEN=... ./scripts/e2e-orders.sh

BASE_URL="${BASE_URL:-http://localhost:8080}"
WEBHOOK_SECRET="${PAYMENT_WEBHOOK_SECRET:-dev-webhook-secret}"
OWNER_TOKEN="${OWNER_TOKEN:?Set OWNER_TOKEN to a verified store-owner access token}"
CUSTOMER_TOKEN="${CUSTOMER_TOKEN:?Set CUSTOMER_TOKEN to a verified customer access token}"

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

json_field() {
  local body="$1"
  local field="$2"
  jq -er "$field" <<<"$body"
}

request() {
  local method="$1"
  local url="$2"
  local token="$3"
  local body="${4:-}"
  local extra_header="${5:-}"
  local response_file
  response_file=$(mktemp)
  local status
  local -a curl_args=(-sS -o "$response_file" -w '%{http_code}' -X "$method" "$BASE_URL$url")
  local header
  if [[ -n "$token" ]]; then
    curl_args+=(-H "Authorization: Bearer $token")
  fi
  if [[ -n "$extra_header" ]]; then
    IFS='|' read -ra extra_headers <<< "$extra_header"
    for header in "${extra_headers[@]}"; do
      curl_args+=(-H "$header")
    done
  fi

  if [[ -n "$body" ]]; then
    curl_args+=(-H 'Content-Type: application/json' -d "$body")
  else
    :
  fi
  status=$(curl "${curl_args[@]}")

  RESPONSE_BODY=$(<"$response_file")
  rm -f "$response_file"
  RESPONSE_STATUS="$status"
}

expect_status() {
  local expected="$1"
  [[ "$RESPONSE_STATUS" == "$expected" ]] || fail "expected HTTP $expected, got $RESPONSE_STATUS: $RESPONSE_BODY"
}

echo "1/8 Creating a test store"
request POST /api/v1/stores "$OWNER_TOKEN" \
  '{"name":"E2E Test Store","description":"Automated order flow test"}'
expect_status 201
STORE_ID=$(json_field "$RESPONSE_BODY" '.id')

echo "2/8 Creating catalog, variant, and inventory"
request POST /api/v1/categories "$OWNER_TOKEN" \
  '{"name":"E2E Test Category","description":"Automated catalog test"}' \
  "X-Store-Id: $STORE_ID"
expect_status 201
CATEGORY_ID=$(json_field "$RESPONSE_BODY" '.id')

request POST /api/v1/products "$OWNER_TOKEN" \
  "$(jq -cn --argjson categoryId "$CATEGORY_ID" '{name:"E2E Test Product",description:"Automated product test",price:5000,categoryId:$categoryId}')" \
  "X-Store-Id: $STORE_ID"
expect_status 201
PRODUCT_ID=$(json_field "$RESPONSE_BODY" '.id')

request POST "/api/v1/products/$PRODUCT_ID/variants" "$OWNER_TOKEN" \
  '{"sku":"E2E-TEST-SKU","size":"42","color":"Black","quantity":10}' \
  "X-Store-Id: $STORE_ID"
expect_status 201
VARIANT_ID=$(json_field "$RESPONSE_BODY" '.id')

echo "3/8 Placing an authenticated customer order"
request POST /api/v1/orders "$CUSTOMER_TOKEN" \
  "$(jq -cn --argjson variantId "$VARIANT_ID" '{customerEmail:"e2e-customer@example.com",shippingAddress:"1 Test Street, Lagos",items:[{variantId:$variantId,quantity:2}]}')" \
  "X-Store-Id: $STORE_ID"
expect_status 201
ORDER_ID=$(json_field "$RESPONSE_BODY" '.id')
ORDER_TOTAL=$(json_field "$RESPONSE_BODY" '.totalAmount')

echo "4/8 Initializing an idempotent payment"
IDEMPOTENCY_KEY="e2e-payment-$ORDER_ID"
request POST "/api/v1/orders/$ORDER_ID/payments" "$CUSTOMER_TOKEN" \
  '{"paymentMethod":"CARD"}' \
  "X-Store-Id: $STORE_ID|Idempotency-Key: $IDEMPOTENCY_KEY"
expect_status 200
PAYMENT_ID=$(json_field "$RESPONSE_BODY" '.id')
TRANSACTION_ID=$(json_field "$RESPONSE_BODY" '.transactionId')

request POST "/api/v1/orders/$ORDER_ID/payments" "$CUSTOMER_TOKEN" \
  '{"paymentMethod":"CARD"}' \
  "X-Store-Id: $STORE_ID|Idempotency-Key: $IDEMPOTENCY_KEY"
expect_status 200
[[ "$(json_field "$RESPONSE_BODY" '.id')" == "$PAYMENT_ID" ]] || fail "idempotency created a duplicate payment"

echo "5/8 Sending a signed successful payment webhook"
WEBHOOK_BODY=$(jq -cn --arg transactionId "$TRANSACTION_ID" --argjson amount "$ORDER_TOTAL" --argjson storeId "$STORE_ID" \
  '{data:{transaction_id:$transactionId,status:"success",amount:$amount,metadata:{store_id:$storeId}}}')
SIGNATURE=$(printf '%s' "$WEBHOOK_BODY" | openssl dgst -sha256 -hmac "$WEBHOOK_SECRET" -hex | awk '{print $2}')
request POST /api/v1/payments/webhook "" "$WEBHOOK_BODY" "X-Store-Id: $STORE_ID|X-Korapay-Signature: $SIGNATURE"
expect_status 200

request GET "/api/v1/orders/$ORDER_ID" "$CUSTOMER_TOKEN" '' "X-Store-Id: $STORE_ID"
expect_status 200
[[ "$(json_field "$RESPONSE_BODY" '.status')" == "PROCESSING" ]] || fail "paid order did not become PROCESSING"

echo "6/8 Creating and reading an installment plan"
request POST "/api/v1/orders/$ORDER_ID/installments" "$CUSTOMER_TOKEN" \
  '{"installmentCount":4}' "X-Store-Id: $STORE_ID"
expect_status 201
PLAN_ID=$(json_field "$RESPONSE_BODY" '.id')
[[ "$(json_field "$RESPONSE_BODY" '.payments | length')" == "4" ]] || fail "installment schedule has wrong length"

request GET "/api/v1/installments/$PLAN_ID" "$CUSTOMER_TOKEN" '' "X-Store-Id: $STORE_ID"
expect_status 200

echo "7/8 Checking expected validation failures"
request POST /api/v1/orders "$CUSTOMER_TOKEN" \
  "$(jq -cn --argjson variantId "$VARIANT_ID" '{customerEmail:"e2e-customer@example.com",shippingAddress:"1 Test Street, Lagos",items:[{variantId:$variantId,quantity:999}]}')" \
  "X-Store-Id: $STORE_ID"
expect_status 400

echo "8/8 Checking webhook signature rejection"
request POST /api/v1/payments/webhook "" "$WEBHOOK_BODY" "X-Store-Id: $STORE_ID|X-Korapay-Signature: invalid"
expect_status 401

echo "PASS: order, payment, webhook, installment, idempotency, tenant, and validation flows completed"
