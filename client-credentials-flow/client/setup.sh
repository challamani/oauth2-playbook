#!/bin/bash

# Create credentials directory
mkdir -p ./credentials
mkdir -p ../oauth2-provider/certs
export KC_HTTPS_TRUST_STORE_PASSWORD="changeit"

# Generate self-signed certificate
openssl req -x509 -newkey rsa:2048 \
  -keyout ./credentials/key.pem \
  -out ./credentials/cert.pem \
  -days 90 -nodes \
  -subj "/C=GB/ST=England/L=Manchester/O=example/OU=Technology/CN=mtls-client-dev.local" \
  -addext "subjectAltName=DNS:localhost,DNS:oauth2-playbook.dev,IP:127.0.0.1"

echo "Client credentials setup completed. Certificates are stored in the 'credentials' directory."

echo "Import the client certificate into Keycloak as a trusted certificate for MTLS authentication."

# Import the client certificate into the truststore used by Keycloak
keytool -importcert \
  -alias mtls-client-local \
  -file ./credentials/cert.pem \
  -keystore ../oauth2-provider/certs/truststore.jks \
  -storepass ${KC_HTTPS_TRUST_STORE_PASSWORD} \
  -noprompt

echo "Client certificate imported into truststores."

echo "Truststore contents:"
keytool -list -v -keystore ../oauth2-provider/certs/truststore.jks -storepass ${KC_HTTPS_TRUST_STORE_PASSWORD}