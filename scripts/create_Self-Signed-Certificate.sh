#!/bin/bash
set -euo pipefail

KEYSTORE_DIR="Backend/gateway/src/main/resources"
KEYSTORE_PATH="${KEYSTORE_DIR}/keystore.p12"

ALIAS="gateway"
PASSWORD="${SSL_KEYSTORE_PASSWORD:-buy01pass}"
VALIDITY=3650
DNAME="CN=localhost, OU=buy01, O=buy01, L=Unknown, ST=Unknown, C=MA"

mkdir -p "${KEYSTORE_DIR}"

echo "Generating Self-Signed Certificate (keystore.p12)..."

docker run --rm \
  -v "$(pwd)/${KEYSTORE_DIR}:/out" \
  eclipse-temurin:21-jdk-alpine \
  sh -c "
    rm -f /out/keystore.p12

    keytool \
      -genkeypair \
      -alias '${ALIAS}' \
      -keyalg RSA \
      -sigalg SHA256withRSA \
      -keysize 2048 \
      -storetype PKCS12 \
      -keystore /out/keystore.p12 \
      -storepass '${PASSWORD}' \
      -keypass '${PASSWORD}' \
      -validity ${VALIDITY} \
      -dname '${DNAME}' \
      -ext 'SAN=dns:localhost,dns:*.localhost,ip:127.0.0.1,ip:::1' \
      -noprompt

    keytool \
      -list \
      -v \
      -keystore /out/keystore.p12 \
      -storepass '${PASSWORD}'
  "

echo
echo "========================================"
echo "Certificate generated successfully!"
echo "Keystore: ${KEYSTORE_PATH}"
echo "Alias: ${ALIAS}"
echo "Validity: ${VALIDITY} days"
echo "========================================"