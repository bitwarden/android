#!/bin/bash
# Common navigation actions
# Usage: ./adb-navigate.sh <action> [wait_seconds]
# Actions: home, back, app-drawer

if [ -z "$1" ]; then
    echo "Usage: $0 <action> [wait_seconds]"
    echo "Actions:"
    echo "  home       - Go to home screen"
    echo "  back       - Press back button"
    echo "  app-drawer - Open app drawer"
    exit 1
fi

ACTION=$1
WAIT=${2:-1}

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

case $ACTION in
    home)
        echo "Going to home screen..."
        $ADB shell input keyevent 3
        ;;
    back)
        echo "Pressing back button..."
        $ADB shell input keyevent 4
        ;;
    app-drawer)
        echo "Opening app drawer..."
        SCREEN_SIZE=$($ADB shell wm size | grep -o '[0-9]*x[0-9]*' | tail -1)
        SW=$(echo "$SCREEN_SIZE" | cut -dx -f1)
        SH=$(echo "$SCREEN_SIZE" | cut -dx -f2)
        CX=$((SW / 2))
        FROM_Y=$((SH * 93 / 100))
        TO_Y=$((SH * 17 / 100))
        $ADB shell input swipe $CX $FROM_Y $CX $TO_Y 1000
        ;;
    *)
        echo "Unknown action: $ACTION"
        exit 1
        ;;
esac

sleep $WAIT
$ADB shell screencap -p /sdcard/screen.png && $ADB pull /sdcard/screen.png .
echo "Screenshot saved to: $(pwd)/screen.png"
