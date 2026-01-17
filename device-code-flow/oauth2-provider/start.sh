#!/bin/bash
export KC_BOOTSTRAP_ADMIN_PASSWORD=admin


# Create certs directory
mkdir -p ./certs

# Generate self-signed certificate
openssl req -x509 -newkey rsa:2048 \
  -keyout ./certs/key.pem \
  -out ./certs/cert.pem \
  -days 90 -nodes \
  -subj "/C=GB/ST=England/L=Manchester/O=finos/OU=Technology/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,DNS:oauth2-playbook.dev,IP:127.0.0.1"

# Start Keycloak with Docker Compose
docker-compose up