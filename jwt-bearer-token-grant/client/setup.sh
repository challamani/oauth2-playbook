#!/bin/bash

# 1. Generate a Private Key
openssl genrsa -out private_key.pem 2048

# 2. Extract the Public Key in PEM format
openssl rsa -in private_key.pem -pubout -out public_key.pem

# 3. (Optional) Convert to a Certificate if using x5t/thumbprints
openssl req -new -x509 -key private_key.pem -out cert.pem -days 365