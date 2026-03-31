#!/bin/bash
# Find element by text, tap it, and capture screenshot
# Usage: ./adb-tap-element.sh "Element Text" [wait_seconds]

if [ -z "$1" ]; then
    echo "Usage: $0 \"Element Text\" [wait_seconds]"
    exit 1
fi

ELEMENT_TEXT="$1"
WAIT=${2:-2}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Find element coordinates
COORDS=$($SCRIPT_DIR/adb-find-element.sh "$ELEMENT_TEXT")

if [ $? -ne 0 ]; then
    echo "Error: Could not find element with text: $ELEMENT_TEXT"
    exit 1
fi

X=$(echo $COORDS | awk '{print $1}')
Y=$(echo $COORDS | awk '{print $2}')

echo "Found element at coordinates: ($X, $Y)"
echo "Tapping..."

# Tap and capture
$SCRIPT_DIR/adb-tap-and-capture.sh $X $Y $WAIT
