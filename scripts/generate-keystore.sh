#!/usr/bin/env bash
# Generates a personal release keystore for signing the WeedWRT Downloader APK.
#
# Usage: ./scripts/generate-keystore.sh [output-dir] [alias]
#
# After generating:
#   1. The keystore is written to <output-dir>/weedwrt-release.jks (default: android-app/keystore/)
#   2. The matching android-app/keystore.properties is (re)written
#   3. For CI signing, add these GitHub secrets:
#        KEYSTORE_BASE64    -> base64 -w0 weedwrt-release.jks
#        KEYSTORE_PASSWORD  -> the generated store password
#        KEY_ALIAS          -> the alias
#        KEY_PASSWORD       -> the key password
set -euo pipefail

OUT_DIR="${1:-android-app/keystore}"
ALIAS="${2:-weedwrt}"
STOREPASS="$(openssl rand -hex 16)"

mkdir -p "$OUT_DIR"

keytool -genkeypair -v \
    -keystore "$OUT_DIR/weedwrt-release.jks" \
    -alias "$ALIAS" \
    -keyalg RSA -keysize 2048 -validity 10950 \
    -storepass "$STOREPASS" -keypass "$STOREPASS" \
    -dname "CN=WeedWRT Downloader, OU=WeedWRT, O=WeedWRT, C=SA"

PROPS_DIR="$(dirname "$OUT_DIR")"
cat > "$PROPS_DIR/keystore.properties" <<EOF
storePassword=$STOREPASS
keyAlias=$ALIAS
keyPassword=$STOREPASS
EOF

echo ""
echo "Keystore : $OUT_DIR/weedwrt-release.jks"
echo "Props    : $PROPS_DIR/keystore.properties"
echo "Password : $STOREPASS"
echo ""
echo "IMPORTANT: keep these files private. Do not commit your personal keystore."
