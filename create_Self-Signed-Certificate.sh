#!/bin/bash

set -e

KEYSTORE_PATH="Backend/gateway/src/main/resources/keystore.p12"
ALIAS="gateway"
PASSWORD="${SSL_KEYSTORE_PASSWORD:-buy01pass}"
VALIDITY=3650
DNAME="CN=localhost, OU=buy01, O=buy01, L=Unknown, ST=Unknown, C=MA"

echo "Generating Self-Signed Certificate (keystore.p12)..."

DOCKER_HOST=unix:///Users/yahya/.colima/default/docker.sock \
docker run --rm \
  -v "$(pwd)/Backend/gateway/src/main/resources:/out" \
  eclipse-temurin:21-jdk-alpine sh -c "
    keytool -genkeypair \
      -alias ${ALIAS} \
      -keyalg RSA \
      -keysize 2048 \
      -storetype PKCS12 \
      -keystore /out/keystore.p12 \
      -validity ${VALIDITY} \
      -storepass '${PASSWORD}' \
      -dname '${DNAME}' \
      -noprompt && echo 'keystore.p12 generated OK'
  "

echo "Certificate saved to: ${KEYSTORE_PATH}"
echo "Alias    : ${ALIAS}"
echo "Validity : ${VALIDITY} days"
echo "Password : set via SSL_KEYSTORE_PASSWORD env var (default: buy01pass)"
