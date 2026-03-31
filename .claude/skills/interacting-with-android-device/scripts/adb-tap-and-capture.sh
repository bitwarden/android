#!/bin/bash
# Tap at coordinates and capture screenshot
# Usage: ./adb-tap-and-capture.sh <x> <y> [wait_seconds]

if [ -z "$1" ] || [ -z "$2" ]; then
    echo "Usage: $0 <x> <y> [wait_seconds]"
    exit 1
fi

X=$1
Y=$2
WAIT=${3:-2}

# Validate numeric inputs
if ! [[ "$X" =~ ^[0-9]+$ ]] || ! [[ "$Y" =~ ^[0-9]+$ ]]; then
    echo "Error: x and y must be positive integers"
    exit 1
fi
if ! [[ "$WAIT" =~ ^[0-9]+\.?[0-9]*$ ]]; then
    echo "Error: wait_seconds must be a number"
    exit 1
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

# Tap and capture
$ADB shell input tap $X $Y && sleep $WAIT && $ADB shell screencap -p /sdcard/screen.png && $ADB pull /sdcard/screen.png .
echo "Screenshot saved to: $(pwd)/screen.png"
