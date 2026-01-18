#!/bin/bash

# Configuration
CLIENT_ID="oauth2-playbook-jwt-bearer-client"
REALM_URL="https://localhost:9443/realms/oauth2-playbook"

PRIVATE_KEY_FILE="private_key.pem"

# 1. Create JWT Header
header_json='{"alg":"RS256","typ":"JWT"}'
header_base64=$(echo -n "${header_json}" | openssl base64 -e -A | tr -d '=' | tr '/+' '_-')

# 2. Create JWT Payload
# Claims: iss (client_id), sub (client_id), aud (token endpoint), jti (unique), exp (+1 min)
now=$(date +%s)
exp=$((now + 60))
jti=$(cat /proc/sys/kernel/random/uuid) # Linux specific; use `uuidgen` on macOS

payload_json="{\"iss\":\"${CLIENT_ID}\",
    \"sub\":\"${CLIENT_ID}\",
    \"aud\":\"${REALM_URL}/protocol/openid-connect/token\",\"jti\":\"${jti}\",\"iat\":${now},\"exp\":${exp}}"

payload_base64=$(echo -n "${payload_json}" | openssl base64 -e -A | tr -d '=' | tr '/+' '_-')

# 3. Sign the JWT (Header + Payload)
signing_input="${header_base64}.${payload_base64}"
signature_bin=$(echo -n "${signing_input}" | openssl dgst -sha256 -sign "${PRIVATE_KEY_FILE}" -binary)
signature_base64=$(echo -n "${signature_bin}" | openssl base64 -e -A | tr -d '=' | tr '/+' '_-')

# 4. Assemble final JWT
CLIENT_ASSERTION="${header_base64}.${payload_base64}.${signature_base64}"

echo "Generated Client Assertion:"
echo "${CLIENT_ASSERTION}"
echo "---------------------------"

# 5. Call Keycloak using Curl
curl -X POST "${REALM_URL}/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer" \
  -d "assertion=${CLIENT_ASSERTION}" \
  -d "client_id=${CLIENT_ID}" \
  -d "scope=openid"