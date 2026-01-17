#!/bin/bash

CLIENT_ID="oauth2-playbook-device-flow"
SCOPE="users:admin"

DEVICE_AUTH_ENDPOINT="https://localhost:9443/realms/oauth2-playbook/protocol/openid-connect/auth/device"
# Initiate the device authorization request 
DEVICE_AUTH_RESPONSE=$(curl --insecure -X POST \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=${CLIENT_ID}" -d "scope=${SCOPE}" \
  ${DEVICE_AUTH_ENDPOINT})

# Extract values from the device auth response.
DEVICE_CODE=$(echo "${DEVICE_AUTH_RESPONSE}" | jq -r '.device_code')
USER_CODE=$(echo "${DEVICE_AUTH_RESPONSE}" | jq -r '.user_code')
VERIFICATION_URI=$(echo "${DEVICE_AUTH_RESPONSE}" | jq -r '.verification_uri')
VERIFICATION_URI_COMPLETE=$(echo "${DEVICE_AUTH_RESPONSE}" | jq -r '.verification_uri_complete')
EXPIRES_IN=$(echo "${DEVICE_AUTH_RESPONSE}" | jq -r '.expires_in')
INTERVAL=$(echo "${DEVICE_AUTH_RESPONSE}" | jq -r '.interval')

echo -e "\nOpen the link in a browser \033[3m[$VERIFICATION_URI]\033[0m, and authenticate with the Code:[$USER_CODE]"
echo -e "\n the associated device flow request will expires in $EXPIRES_IN seconds.\n"

# Poll the token endpoint
TOKEN_URL="https://localhost:9443/realms/oauth2-playbook/protocol/openid-connect/token"
ACCESS_TOKEN=""
POLL_INTERVAL=15 #Seconds

poll_token() {
  while true; do
    RESPONSE=$(curl --insecure -s -X POST "$TOKEN_URL" \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "client_id=${CLIENT_ID}" \
      -d "device_code=${DEVICE_CODE}" \
      -d "grant_type=urn:ietf:params:oauth:grant-type:device_code" \
      -d "scope=${SCOPE}")

    ERROR=$(echo "${RESPONSE}" | jq -r '.error')
    if [[ "${ERROR}" == "authorization_pending" ]]; then
      echo "Waiting for user authorization..."
      sleep $POLL_INTERVAL
    elif [[ "${ERROR}" == "expired_token" ]]; then
      echo "Device code expired. Restart the flow."
      exit 1
    elif [[ "${ERROR}" == "slow_down" ]]; then
      echo "Server requested slower polling. "
      POLL_INTERVAL=$((POLL_INTERVAL + 10))
      sleep $POLL_INTERVAL
    else
      ACCESS_TOKEN=$(echo "${RESPONSE}" | jq -r '.access_token')
      echo "Access Token: $ACCESS_TOKEN"
      break;
    fi
  done
}

#Start token polling
poll_token

echo -e "\nCreate a new user by pressing enter "
read

if [[ -n ${ACCESS_TOKEN} ]]; then
  CREATE_USER_RESPONSE=$(curl -X POST --insecure "https://localhost:8443/api/user" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -d '{  
            "userName": "DemoTester",
            "givenName": "Demo",
            "familyName": "Tester",
            "password": "Tester$000",
            "emails": [
                {
                    "value": "test@oauth2-playbook.dev",
                    "type": "home",
                    "primary": true
                }
            ]
        }')
  
  echo -e "\nCreate User Response: $CREATE_USER_RESPONSE"
  USER_ID=$(echo "${CREATE_USER_RESPONSE}" | jq -r '.id')

  #Get the created user details
  echo -e "\nGet the created user details by pressing enter"
  read
  FETCH_USER_RESPONSE=$(curl -X GET --insecure -H "Authorization: Bearer ${ACCESS_TOKEN}" "https://localhost:8443/api/user/$USER_ID")

  echo -e "\nGet - User Details: $FETCH_USER_RESPONSE\n"
fi