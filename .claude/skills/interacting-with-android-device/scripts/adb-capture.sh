#!/bin/bash
# Capture current device state (screenshot and/or UI hierarchy)
# Usage: ./adb-capture.sh [--xml] [--screenshot] [--all]
# Default (no flags): captures both screenshot and XML hierarchy

CAPTURE_XML=false
CAPTURE_SCREENSHOT=false

# Parse flags
if [ $# -eq 0 ]; then
    CAPTURE_XML=true
    CAPTURE_SCREENSHOT=true
else
    for arg in "$@"; do
        case $arg in
            --xml)
                CAPTURE_XML=true
                ;;
            --screenshot)
                CAPTURE_SCREENSHOT=true
                ;;
            --all)
                CAPTURE_XML=true
                CAPTURE_SCREENSHOT=true
                ;;
            *)
                echo "Usage: $0 [--xml] [--screenshot] [--all]"
                echo "Default (no flags): captures both screenshot and XML hierarchy"
                exit 1
                ;;
        esac
    done
fi

# Check if adb is in PATH
if ! command -v adb &> /dev/null; then
    if [ -x ~/Library/Android/sdk/platform-tools/adb ]; then
        ADB=~/Library/Android/sdk/platform-tools/adb
    elif [ -x /usr/local/bin/adb ]; then
        ADB=/usr/local/bin/adb
    else
        echo "Error: adb not found. Install Android SDK or add platform-tools to PATH."
        exit 1
    fi
else
    ADB=adb
fi

if [ "$CAPTURE_XML" = true ]; then
    echo "Dumping UI hierarchy..."
    $ADB shell uiautomator dump /sdcard/view.xml && $ADB pull /sdcard/view.xml .
    echo "UI hierarchy saved to: $(pwd)/view.xml"
fi

if [ "$CAPTURE_SCREENSHOT" = true ]; then
    echo "Capturing screenshot..."
    $ADB shell screencap -p /sdcard/screen.png && $ADB pull /sdcard/screen.png .
    echo "Screenshot saved to: $(pwd)/screen.png"
fi
