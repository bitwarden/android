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

# Extract coordinates from XML using grep + awk
# Search by text attribute first
MATCH=$(grep -o "text=\"[^\"]*${SEARCH_TEXT}[^\"]*\"[^>]*bounds=\"\[[0-9,]*\]\[[0-9,]*\]\"" view.xml | head -1)

if [ -z "$MATCH" ]; then
    MATCH=$(grep -o "bounds=\"\[[0-9,]*\]\[[0-9,]*\]\"[^>]*text=\"[^\"]*${SEARCH_TEXT}[^\"]*\"" view.xml | head -1)
fi

# Fallback: search by content-desc attribute (common in Compose UIs)
if [ -z "$MATCH" ]; then
    MATCH=$(grep -o "content-desc=\"[^\"]*${SEARCH_TEXT}[^\"]*\"[^>]*bounds=\"\[[0-9,]*\]\[[0-9,]*\]\"" view.xml | head -1)
fi

if [ -z "$MATCH" ]; then
    MATCH=$(grep -o "bounds=\"\[[0-9,]*\]\[[0-9,]*\]\"[^>]*content-desc=\"[^\"]*${SEARCH_TEXT}[^\"]*\"" view.xml | head -1)
fi

if [ -z "$MATCH" ]; then
    echo "ERROR: Element with text '$SEARCH_TEXT' not found" >&2
    exit 1
fi

echo "$MATCH" | grep -o 'bounds="\[[0-9,]*\]\[[0-9,]*\]"' | sed 's/bounds="//;s/"//' | awk -F'[][,]' '{
    left=$2; top=$3; right=$5; bottom=$6
    printf "%d %d\n", (left+right)/2, (top+bottom)/2
}'
