#!/usr/bin/env bash
echo "=========================================================="
echo " AutoCast Pro - Sideload Helper for 2023 Hyundai Palisade"
echo "=========================================================="
echo ""

if ! command -v adb &> /dev/null; then
    echo "ERROR: adb command not found. Make sure Android Platform Tools is installed."
    echo "Alternative: Install KingInstaller on your phone, select app-debug.apk, and tap Install as KingInstaller."
    exit 1
fi

echo "Installing AutoCast Pro with Play Store installer identity (com.android.vending)..."
adb install -i com.android.vending -r AutoCast-Debug-APK/app-debug.apk

if [ $? -eq 0 ]; then
    echo ""
    echo "SUCCESS! AutoCast Pro installed with Play Store package manager identity."
    echo "Now connect your phone to your 2023 Hyundai Palisade via USB/Wireless Android Auto!"
else
    echo ""
    echo "Installation failed. Make sure USB Debugging is enabled on your phone."
fi
