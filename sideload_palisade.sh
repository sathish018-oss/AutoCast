#!/usr/bin/env bash
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ADB_BIN="$SCRIPT_DIR/platform-tools/adb"

echo "=========================================================="
echo " AutoCast Pro - Sideload Helper for 2023 Hyundai Palisade"
echo "=========================================================="
echo ""

if [ ! -f "$ADB_BIN" ]; then
    ADB_BIN="adb"
fi

echo "Checking connected Android phones..."
$ADB_BIN devices

echo ""
echo "Installing AutoCast Pro with Play Store installer identity (com.android.vending)..."
$ADB_BIN install -i com.android.vending -r "$SCRIPT_DIR/AutoCast-Debug-APK/app-debug.apk"

if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================================="
    echo " SUCCESS! AutoCast Pro installed with Play Store identity."
    echo " Now connect your phone to your 2023 Hyundai Palisade!"
    echo "=========================================================="
else
    echo ""
    echo "Notice: Make sure your phone is plugged in via USB and USB Debugging is ON."
fi
