#!/bin/bash

# Client credentials (MTLS) helper script
# Organized into small functions with user-friendly progress output.
# Behavior is unchanged from the original script.

CLIENT_ID="oauth2-playbook-client-credentials-mtls"
SCOPE="users:read"
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin

# Colors (use tput for portability; fall back to ANSI escapes)
if command -v tput >/dev/null 2>&1; then
  YELLOW=$(tput setaf 3)
  RESET=$(tput sgr0)
else
  YELLOW="\033[1;33m"
  RESET="\033[0m"
fi

printf "%b\n" "${YELLOW}This script assigns the 'oauth2-playbook-readonly' realm role to the\nservice-account user of the client '${CLIENT_ID}', then requests an\naccess token using client credentials (MTLS).${RESET}"

INFO() { echo "[INFO] $*"; }
ERROR() { echo "[ERROR] $*" >&2; }

get_admin_token() {
  INFO "Requesting admin token..."
  ADMIN_TOKEN=$(curl -s --insecure \
    -X POST "https://localhost:9443/realms/master/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password" \
    -d "client_id=admin-cli" \
    -d "username=${KEYCLOAK_ADMIN}" \
    -d "password=${KEYCLOAK_ADMIN_PASSWORD}" \
    | jq -r '.access_token')

  if [ -z "${ADMIN_TOKEN}" ] || [ "${ADMIN_TOKEN}" = "null" ]; then
    ERROR "Failed to obtain admin token."
    return 1
  fi

  INFO "Obtained admin token."
  echo "Admin Token: ${ADMIN_TOKEN}"
}

get_client_uuid() {
  INFO "Querying clients to find client id '${CLIENT_ID}'..."
  CLIENT_UUID=$(curl -s --insecure \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    https://localhost:9443/admin/realms/oauth2-playbook/clients \
    | jq -r ".[] | select(.clientId==\"${CLIENT_ID}\") | .id")

  if [ -z "${CLIENT_UUID}" ]; then
    ERROR "Client UUID not found for '${CLIENT_ID}'."
    return 1
  fi

  INFO "Found client UUID: ${CLIENT_UUID}"
}

get_service_account_id() {
  INFO "Retrieving service account ID for client ${CLIENT_UUID}..."
  SERVICE_ACCOUNT_ID=$(curl -s --insecure \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    https://localhost:9443/admin/realms/oauth2-playbook/clients/${CLIENT_UUID}/service-account-user \
    | jq -r '.id')

  if [ -z "${SERVICE_ACCOUNT_ID}" ]; then
    ERROR "Service account user not found for client ${CLIENT_UUID}."
    return 1
  fi

  INFO "Service Account ID: ${SERVICE_ACCOUNT_ID}"
}

get_role_object() {
  INFO "Retrieving role object for 'oauth2-playbook-readonly'..."
  ROLE_OBJECT=$(curl -s --insecure \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    https://localhost:9443/admin/realms/oauth2-playbook/roles/oauth2-playbook-readonly)

  if [ -z "${ROLE_OBJECT}" ] || [ "${ROLE_OBJECT}" = "null" ]; then
    ERROR "Failed to retrieve role object."
    return 1
  fi

  INFO "Role object retrieved."
}

assign_role() {
  echo
  INFO "About to assign realm role 'oauth2-playbook-readonly' to service account user '${SERVICE_ACCOUNT_ID}'."
  read -p "Press Enter to proceed with role assignment (or Ctrl-C to abort) ..." -r
  INFO "Assigning role now..."
  curl -X POST --insecure \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    https://localhost:9443/admin/realms/oauth2-playbook/users/${SERVICE_ACCOUNT_ID}/role-mappings/realm \
    -d "[${ROLE_OBJECT}]"
  INFO "Role assignment request sent."
}

request_access_token() {
  INFO "Requesting access token via client credentials (MTLS)..."
  TOKEN_ENDPOINT="https://localhost:9443/realms/oauth2-playbook/protocol/openid-connect/token"
  TOKEN_RESPONSE=$(curl -s --insecure -X POST "${TOKEN_ENDPOINT}" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    --cert ./credentials/cert.pem --key ./credentials/key.pem \
    -d "grant_type=client_credentials" \
    -d "client_id=${CLIENT_ID}" \
    -d "scope=${SCOPE}")

  ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token')
  if [ -z "${ACCESS_TOKEN}" ] || [ "${ACCESS_TOKEN}" = "null" ]; then
    ERROR "Failed to obtain access token."
    return 1
  fi

  INFO "Obtained access token via client credentials."
  echo "Access Token: ${ACCESS_TOKEN}"
}

invoke_protected_resource() {
    INFO "Invoking protected resource with access token..."
    RESOURCE_RESPONSE=$(curl -s --insecure -X GET "https://localhost:8443/api/users" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}")
    INFO "Protected resource response: ${RESOURCE_RESPONSE}"
    echo ${RESOURCE_RESPONSE} | jq .
}

main() {
  get_admin_token || exit 1
  get_client_uuid || exit 1
  get_service_account_id || exit 1
  get_role_object || exit 1
  assign_role
  request_access_token || exit 1
  invoke_protected_resource || exit 1
}

main "$@"