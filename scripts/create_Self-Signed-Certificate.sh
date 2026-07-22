#!/bin/bash
set -e

KEYSTORE_DIR="Backend/gateway/src/main/resources"
KEYSTORE_PATH="${KEYSTORE_DIR}/keystore.p12"

ALIAS="gateway"
PASSWORD="${SSL_KEYSTORE_PASSWORD:-buy01pass}"
VALIDITY=3650
DNAME="CN=localhost, OU=buy01, O=buy01, L=Unknown, ST=Unknown, C=MA"

mkdir -p "$KEYSTORE_DIR"

echo "Generating Self-Signed Certificate..."

docker run --rm \
  -v "$(pwd)/${KEYSTORE_DIR}:/out" \
  eclipse-temurin:21-jdk-alpine \
  sh -c "
    keytool \
      -genkeypair \
      -alias '${ALIAS}' \
      -keyalg RSA \
      -keysize 2048 \
      -storetype PKCS12 \
      -keystore /out/keystore.p12 \
      -validity ${VALIDITY} \
      -storepass '${PASSWORD}' \
      -dname '${DNAME}' \
      -noprompt
  "

echo
echo "Certificate generated:"
echo "  ${KEYSTORE_PATH}"