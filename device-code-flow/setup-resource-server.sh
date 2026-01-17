#!/bin/bash

cd ./device-code-flow/resource-server

openssl req -x509 -newkey rsa:2048 \
  -keyout ./src/main/resources/key.pem \
  -out ./src/main/resources/cert.pem \
  -days 90 -nodes \
  -subj "/C=GB/ST=England/L=Manchester/O=example/OU=Technology/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,DNS:oauth2-playbook.dev,IP:127.0.0.1"

./mvnw clean package quarkus:dev