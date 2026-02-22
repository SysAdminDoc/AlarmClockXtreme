#!/bin/bash
# AlarmClockXtreme v0.5.0 - Reproducible Build Verification
# Verifies that the build is reproducible for F-Droid submission.
# Usage: ./scripts/verify-reproducible-build.sh

set -euo pipefail

echo "=== AlarmClockXtreme Reproducible Build Verification ==="
echo ""

# Check Java version
JAVA_VER=$(java -version 2>&1 | head -1)
echo "Java: $JAVA_VER"

# Build twice
echo ""
echo ">>> Build 1..."
./gradlew clean assembleFdroidRelease --no-daemon 2>/dev/null
APK1="app/build/outputs/apk/fdroid/release/app-fdroid-release-unsigned.apk"
if [ ! -f "$APK1" ]; then
    echo "ERROR: Build 1 failed - APK not found"
    exit 1
fi
HASH1=$(sha256sum "$APK1" | cut -d' ' -f1)
cp "$APK1" /tmp/build1.apk
echo "Build 1 SHA-256: $HASH1"

echo ""
echo ">>> Build 2..."
./gradlew clean assembleFdroidRelease --no-daemon 2>/dev/null
if [ ! -f "$APK1" ]; then
    echo "ERROR: Build 2 failed - APK not found"
    exit 1
fi
HASH2=$(sha256sum "$APK1" | cut -d' ' -f1)
echo "Build 2 SHA-256: $HASH2"

echo ""
if [ "$HASH1" = "$HASH2" ]; then
    echo "PASS: Builds are reproducible!"
    echo "SHA-256: $HASH1"
else
    echo "FAIL: Builds differ!"
    echo "Build 1: $HASH1"
    echo "Build 2: $HASH2"
    echo ""
    echo "Comparing APK contents..."
    mkdir -p /tmp/apk1 /tmp/apk2
    unzip -q /tmp/build1.apk -d /tmp/apk1
    unzip -q "$APK1" -d /tmp/apk2
    diff -rq /tmp/apk1 /tmp/apk2 || true
    rm -rf /tmp/apk1 /tmp/apk2
    exit 1
fi

rm -f /tmp/build1.apk
echo ""
echo "=== Verification Complete ==="
