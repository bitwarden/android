#!/bin/bash
# Find element by text and return center coordinates
# Usage: ./adb-find-element.sh "Search Text"

if [ -z "$1" ]; then
    echo "Usage: $0 \"Element Text\""
    exit 1
fi

SEARCH_TEXT="$1"

# Check if adb is in PATH
if ! command -v adb &> /dev/null; then
    # Try common default paths
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

# Dump UI hierarchy
$ADB shell uiautomator dump /sdcard/view.xml > /dev/null 2>&1 && $ADB pull /sdcard/view.xml . > /dev/null 2>&1
echo "UI hierarchy saved to: $(pwd)/view.xml" >&2

# Extract coordinates using Python
python3 << EOF
import xml.etree.ElementTree as ET
import sys

try:
    root = ET.parse('view.xml').getroot()
    found = False

    for node in root.iter():
        text = node.get('text', '')
        if '$SEARCH_TEXT' in text:
            bounds = node.get('bounds')
            if bounds:
                bounds_str = bounds.strip('[]')
                parts = bounds_str.split('][')
                left_top = parts[0].strip('[]').split(',')
                right_bottom = parts[1].strip('[]').split(',')
                left, top = int(left_top[0]), int(left_top[1])
                right, bottom = int(right_bottom[0]), int(right_bottom[1])
                center_x = (left + right) // 2
                center_y = (top + bottom) // 2
                print(f"{center_x} {center_y}")
                found = True
                break

    if not found:
        print("ERROR: Element not found", file=sys.stderr)
        sys.exit(1)
except Exception as e:
    print(f"ERROR: {e}", file=sys.stderr)
    sys.exit(1)
EOF
