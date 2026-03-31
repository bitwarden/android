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

# Extract target bounds and compute center
BOUNDS_RAW=$(echo "$MATCH" | grep -o 'bounds="\[[0-9,]*\]\[[0-9,]*\]"' | sed 's/bounds="//;s/"//')
COORDS=$(echo "$BOUNDS_RAW" | awk -F'[][,]' '{
    printf "%d %d %d %d %d %d\n", ($2+$5)/2, ($3+$6)/2, $2, $3, $5, $6
}')
X=$(echo "$COORDS" | awk '{print $1}')
Y=$(echo "$COORDS" | awk '{print $2}')
T_LEFT=$(echo "$COORDS" | awk '{print $3}')
T_TOP=$(echo "$COORDS" | awk '{print $4}')
T_RIGHT=$(echo "$COORDS" | awk '{print $5}')
T_BOTTOM=$(echo "$COORDS" | awk '{print $6}')

# --- Obstruction detection ---
# Find the topmost clickable element at tap point (X, Y).
# In UIAutomator XML, document order reflects draw order: the last clickable
# element whose bounds contain the point is the one that receives the tap.
TOPMOST=$(awk -v tx="$X" -v ty="$Y" '
BEGIN { RS="<node "; FS="\""; lt=""; ld=""; lr=""; lb=""; lp="" }
NR > 1 {
    bounds=""; clickable=""; text=""; desc=""; resid=""; pkg=""
    for (i = 1; i <= NF; i++) {
        if ($(i) ~ /clickable=$/) clickable = $(i+1)
        if ($(i) ~ /bounds=$/) bounds = $(i+1)
        if ($(i) ~ /text=$/) text = $(i+1)
        if ($(i) ~ /content-desc=$/) desc = $(i+1)
        if ($(i) ~ /resource-id=$/) resid = $(i+1)
        if ($(i) ~ /package=$/) pkg = $(i+1)
    }
    if (clickable == "true" && bounds != "") {
        n = split(bounds, b, /[][,]/)
        bl = b[2]+0; bt = b[3]+0; br = b[5]+0; bb = b[6]+0
        if (tx+0 >= bl && tx+0 <= br && ty+0 >= bt && ty+0 <= bb) {
            lt = text; ld = desc; lr = resid; lb = bounds; lp = pkg
        }
    }
}
END { printf "%s\t%s\t%s\t%s\t%s\n", lt, ld, lr, lb, lp }
' view.xml)

TOP_TEXT=$(echo "$TOPMOST" | cut -f1)
TOP_DESC=$(echo "$TOPMOST" | cut -f2)
TOP_RESID=$(echo "$TOPMOST" | cut -f3)
TOP_BOUNDS=$(echo "$TOPMOST" | cut -f4)
TOP_PKG=$(echo "$TOPMOST" | cut -f5)

# Determine if topmost element matches the target
OBSTRUCTED=false
if [ -n "$TOP_BOUNDS" ]; then
    # Match if: search text appears in topmost text or content-desc
    TEXT_MATCH=false
    if [ -n "$TOP_TEXT" ] && echo "$TOP_TEXT" | grep -qi "$SEARCH_TEXT" 2>/dev/null; then
        TEXT_MATCH=true
    fi
    if [ -n "$TOP_DESC" ] && echo "$TOP_DESC" | grep -qi "$SEARCH_TEXT" 2>/dev/null; then
        TEXT_MATCH=true
    fi

    # Match if: topmost bounds are identical to target bounds (Compose parent wrapper)
    BOUNDS_MATCH=false
    if [ "$TOP_BOUNDS" = "$BOUNDS_RAW" ]; then
        BOUNDS_MATCH=true
    fi

    if [ "$TEXT_MATCH" = false ] && [ "$BOUNDS_MATCH" = false ]; then
        OBSTRUCTED=true
        # Build a human-readable identifier for the obstructing element
        OBS_ID=""
        [ -n "$TOP_TEXT" ] && OBS_ID="text=\"$TOP_TEXT\""
        [ -z "$OBS_ID" ] && [ -n "$TOP_DESC" ] && OBS_ID="desc=\"$TOP_DESC\""
        [ -z "$OBS_ID" ] && [ -n "$TOP_RESID" ] && OBS_ID="id=\"$TOP_RESID\""
        [ -z "$OBS_ID" ] && OBS_ID="bounds=$TOP_BOUNDS"
        [ -n "$TOP_PKG" ] && OBS_ID="$OBS_ID pkg=$TOP_PKG"
        echo "OBSTRUCTED: Tap at ($X,$Y) would hit [$OBS_ID] instead of '$SEARCH_TEXT' [$BOUNDS_RAW]" >&2
    fi
fi

# Output coordinates (stdout) — backward compatible
printf "%d %d\n" "$X" "$Y"

# Exit 3 if obstructed (caller can check), 0 if clear
if [ "$OBSTRUCTED" = true ]; then
    exit 3
fi
