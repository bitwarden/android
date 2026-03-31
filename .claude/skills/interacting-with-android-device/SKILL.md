---
name: interacting-with-android-device
description: Instructions for capturing UI state, comparing with mocks, and interacting with an Android device using universal ADB commands.
allowed-tools:
  - Bash(adb *)

  - Bash(./.claude/skills/interacting-with-android-device/scripts/adb-*)
  - Bash(sleep *)
  - Bash(./gradlew install*)
  - Read
  - Glob
---

# Interacting with Android Device

## Quick Start: Using Helper Scripts

Helper scripts in the `.claude/skills/interacting-with-android-device/scripts/` directory automate repetitive UI testing tasks and reduce token overhead.

**Available scripts:**
- `adb-capture.sh [--xml] [--screenshot] [--all]` - Capture current device state. Default (no flags): both screenshot and XML hierarchy.
- `adb-find-element.sh <text>` - Find element by text, return center coordinates (`X Y`). Dumps UI hierarchy, parses XML, calculates center from bounds.
- `adb-tap-and-capture.sh <x> <y> [wait_seconds=2]` - Tap at coordinates, wait, capture and pull screenshot.
- `adb-tap-element.sh <text> [wait_seconds=2]` - Find, tap, and capture in one command (recommended). Combines `adb-find-element.sh` + `adb-tap-and-capture.sh`.
- `adb-navigate.sh <home|back|app-drawer> [wait_seconds=1]` - Navigation actions via keyevent or swipe, then capture screenshot.

**Use these scripts instead of inlining commands** to save tokens and reduce mechanical steps.

To use `adb-find-element.sh` for manual coordinate extraction:
```bash
COORDS=$(./.claude/skills/interacting-with-android-device/scripts/adb-find-element.sh "Check for update")
X=$(echo $COORDS | awk '{print $1}')
Y=$(echo $COORDS | awk '{print $2}')
adb shell input tap $X $Y
```

## 1. Capturing Current State
To understand what is currently on the device:
```bash
# Capture both screenshot and UI hierarchy XML
./.claude/skills/interacting-with-android-device/scripts/adb-capture.sh

# Or capture only one
./.claude/skills/interacting-with-android-device/scripts/adb-capture.sh --xml        # UI hierarchy only
./.claude/skills/interacting-with-android-device/scripts/adb-capture.sh --screenshot  # Screenshot only
```
*   Read `view.xml` to find coordinates (`bounds`) and properties (like `text` or `resource-id`) of UI elements.
*   Use `screen.png` for visual verification against design mocks.

## 2. Interacting with the Device

### Using Scripts (Recommended)
Use helper scripts to reduce token overhead and automate mechanical steps:

*   **Find and tap an element by text**:
    ```bash
    ./.claude/skills/interacting-with-android-device/scripts/adb-tap-element.sh "System"
    ```
    This finds the element, taps it, captures screenshot—all in one command.

*   **Tap at specific coordinates**:
    ```bash
    ./.claude/skills/interacting-with-android-device/scripts/adb-tap-and-capture.sh 332 1367 2
    ```
    Parameters: `<x> <y> [wait_seconds]`

*   **Navigate (home, back, app-drawer)**:
    ```bash
    ./.claude/skills/interacting-with-android-device/scripts/adb-navigate.sh home
    ./.claude/skills/interacting-with-android-device/scripts/adb-navigate.sh back
    ./.claude/skills/interacting-with-android-device/scripts/adb-navigate.sh app-drawer
    ```

### Raw Commands (When Scripts Aren't Sufficient)

*   **Finding Coordinates**: From the dumped XML, find the `bounds` attribute of the element you want to interact with. The bounds are in `[left, top][right, bottom]` format. Use the center point for a tap: `x = (left + right) / 2`, `y = (top + bottom) / 2`.
*   **Inputting Text**: First tap the text field, then `adb shell input text "<your_text>"` (Note: handle spaces and special characters with quotes).
*   **Key Events** (if not using navigate script):
    *   Back: `adb shell input keyevent 4`
    *   Home: `adb shell input keyevent 3`
    *   Enter: `adb shell input keyevent 66`
*   **Scrolling/Swiping**: Use `adb shell input swipe <x1> <y1> <x2> <y2> <duration_ms>` where:
    *   `(x1, y1)` = starting point
    *   `(x2, y2)` = ending point
    *   `duration_ms` = duration in milliseconds (1000ms is typical; adjust for speed/distance)
    *   **Note**: For expanding containers/drawers, use large distances (e.g., 2400→300 for a 2992px tall screen)

## 3. Verification Workflow
Follow these steps for a complete UI test:
1.  **Build and Install**: Ensure the latest version of the app is running: `./gradlew installDebug`.
2.  **Inspect**: Run `adb-capture.sh` to dump the UI hierarchy and take a screenshot.
3.  **Compare**: Check the current UI against any mock image files in the project.
4.  **Interact**: Perform an action (like a button click) using the calculated coordinates and `adb shell input tap`.
5.  **Wait**: Sleep for a second (`sleep 1`) to allow for animations or network transitions.
6.  **Verify**: Dump the UI hierarchy again to confirm the UI has updated as expected (e.g., a new screen is shown, or a success message appeared in the XML).

## 4. Examples

### Example: Navigate to Settings and Check for Updates
**Using scripts:**
```bash
# Go to home screen
./.claude/skills/interacting-with-android-device/scripts/adb-navigate.sh home

# Open app drawer
./.claude/skills/interacting-with-android-device/scripts/adb-navigate.sh app-drawer

# Find and tap "Settings" app
./.claude/skills/interacting-with-android-device/scripts/adb-tap-element.sh "Settings" 2

# Find and tap "System" option
./.claude/skills/interacting-with-android-device/scripts/adb-tap-element.sh "System" 2

# Find and tap "Software updates"
./.claude/skills/interacting-with-android-device/scripts/adb-tap-element.sh "Software updates" 2

# Find and tap "Check for update" button
./.claude/skills/interacting-with-android-device/scripts/adb-tap-element.sh "Check for update" 5
```

### Example: Swiping
For swipe gestures not covered by the navigation script:
```bash
adb shell input swipe 672 2800 672 500 1000 && sleep 1 && adb shell screencap -p /sdcard/screen.png && adb pull /sdcard/screen.png .
```

## 5. Best Practices

### Coordinate Calculation
*   Always calculate coordinates from the `bounds` attribute in the XML dump, as layouts can vary across different screen sizes.
*   Parse bounds format `[left,top][right,bottom]` and compute center: `x = (left + right) / 2`, `y = (top + bottom) / 2`
*   Use shell tools to programmatically extract coordinates rather than estimating from screenshots
*   When multiple instances of an element exist (e.g., in prediction row and full list), verify you're using the correct one by checking the context

### Command Chaining and Efficiency
*   For custom operations not covered by scripts, combine tap + wait + capture:
    ```bash
    adb shell input tap X Y && sleep N && adb shell screencap -p /sdcard/screen.png && adb pull /sdcard/screen.png .
    ```
*   Always include a sleep duration (typically 1-5 seconds) between tap and capture to allow animations and transitions to complete
*   Pull the screenshot immediately after capture to avoid losing transient UI states

### Navigation and State Evaluation
*   **Dump XML before interaction**: Always extract the UI hierarchy before tapping to find precise element locations
*   **Verify after each interaction**: Don't assume an action succeeded—capture a screenshot after every tap to confirm the correct element was activated and the UI changed as expected
*   **Check both visual and structural state**: Use screenshot for visual verification, XML dump for structural confirmation (element presence, text content, state changes)
*   **Identify navigation failures early**: If a tap opened the wrong screen, use back button (`adb shell input keyevent 4`) to recover immediately rather than continuing with an incorrect state

### Interaction Patterns
*   **Scrolling before interaction**: When looking for an element, check if it's visible on screen first. If not, scroll using swipe gestures to reveal it
*   **Use consistent scroll direction**: For vertical scrolling in lists/settings, use downward swipes (higher Y → lower Y) to scroll down
*   **Handle app crashes gracefully**: Some apps may fail to launch. Don't retry the same action—use back button and try an alternative approach
*   **Sanitize Input**: When using `adb shell input text`, be mindful of special characters that might need escaping in a terminal shell
*   **Check Accessibility**: Use the `content-desc` and `text` properties in the XML hierarchy to ensure the UI is accessible for screen readers

## 6. Troubleshooting

### Device Not Connected
If `adb devices` returns an empty list:
*   Check USB connection or emulator status
*   Enable USB debugging on the device (Settings > Developer Options > USB Debugging)
*   Accept the RSA key prompt on the device if asked
*   Restart the device or disconnect/reconnect the USB cable
