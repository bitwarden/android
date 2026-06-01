---
name: interacting-with-android-device
description: Instructions for capturing UI state, comparing with mocks, and interacting with an Android device using MCP tools backed by ADB.
allowed-tools: mcp__android-device__capture, mcp__android-device__find_element, mcp__android-device__tap_at, mcp__android-device__tap_element, mcp__android-device__navigate, mcp__android-device__input_text, Bash(adb:*), Bash(sleep:*), Bash(./gradlew install*:*), Read, Glob
---

# Interacting with Android Device

## Quick Start: MCP Tools

The `android-device` MCP server provides 6 tools for device interaction. These replace the previous shell scripts with proper XML parsing, structured dumpsys parsing, and native obstruction detection.

**Available tools:**
- `capture` — Capture UI hierarchy XML and/or screenshot. Params: `{ xml?: boolean, screenshot?: boolean }`. Default: both.
- `find_element` — Find element by `text` or `content-desc`, return coordinates with **obstruction detection**. Params: `{ text: string }`. Returns JSON with coordinates, bounds, and obstruction status.
- `tap_at` — Tap at specific coordinates, wait, capture screenshot. Params: `{ x, y, waitSeconds? }`.
- `tap_element` — Find, tap, and capture in one call (recommended). Params: `{ text, waitSeconds? }`. Auto-adjusts coordinates when obstructed.
- `navigate` — Navigation actions: home, back, app-drawer. Params: `{ action, waitSeconds? }`. Captures screenshot after action.
- `input_text` — Type text into the focused field. Params: `{ text, clear? }`. Set `clear: true` to erase existing content first.

**Use these MCP tools instead of raw ADB commands** to save tokens, get structured results, and benefit from automatic obstruction detection.

## 1. Capturing Current State
To understand what is currently on the device, use the `capture` tool:
*   It saves `view.xml` (UI hierarchy) and `screen.png` (screenshot) to the working directory
*   Read `view.xml` to find coordinates (`bounds`) and properties (like `text` or `resource-id`) of UI elements
*   Use `screen.png` for visual verification against design mocks

## 2. Interacting with the Device

### Using MCP Tools (Recommended)

*   **Find and tap an element by text** — use `tap_element`:
    This finds the element, detects obstructions, taps (with adjusted coordinates if needed), and captures a screenshot — all in one call.

*   **Tap at specific coordinates** — use `tap_at`:
    When you already have coordinates from `find_element` or manual inspection.

*   **Navigate (home, back, app-drawer)** — use `navigate`:
    Performs the action and captures a screenshot.

*   **Find element without tapping** — use `find_element`:
    Returns coordinates and full element info. Useful when you need to inspect before acting.

*   **Type text into a field** — use `input_text`:
    Types text into the currently focused field. Set `clear: true` to erase existing content first. Tap the field before calling this if it isn't already focused.

### Raw ADB Commands (When MCP Tools Aren't Sufficient)
*   **Key Events**:
    *   Back: `adb shell input keyevent 4`
    *   Home: `adb shell input keyevent 3`
    *   Enter: `adb shell input keyevent 66`
*   **Scrolling/Swiping**: Use `adb shell input swipe <x1> <y1> <x2> <y2> <duration_ms>` where:
    *   `(x1, y1)` = starting point
    *   `(x2, y2)` = ending point
    *   `duration_ms` = duration in milliseconds (1000ms is typical; adjust for speed/distance)
    *   **Note**: For expanding containers/drawers, use large distances (e.g., 2400->300 for a 2992px tall screen)

## 3. Obstruction Detection

The `find_element` and `tap_element` tools automatically detect when another element would intercept the tap. This catches:
*   **System overlays** (Layer 1): TalkBack floating menu, PiP windows, accessibility services — detected via `dumpsys window windows` touchable regions
*   **In-app elements** (Layer 2): FABs, dialogs, bottom sheets, snackbars — detected by finding the topmost clickable element at the tap point in the UI hierarchy

When obstruction is detected:
*   Coordinates are **auto-adjusted** to the center of the largest unobstructed strip (top/bottom/left/right of the obstructor)
*   The response includes the obstructor identity, bounds, and visible region info
*   If fully obscured (no visible region), the original center is returned as best-effort
*   **Compose parent wrapper** pattern (identical bounds) is recognized as non-obstruction

## 4. Verification Workflow
Follow these steps for a complete UI test:
1.  **Build and Install**: Ensure the latest version of the app is running: `./gradlew installDebug`.
2.  **Inspect**: Use `capture` to dump the UI hierarchy and take a screenshot.
3.  **Compare**: Check the current UI against any mock image files in the project.
4.  **Interact**: Use `tap_element` to tap a UI element by text. The tool handles coordinate calculation and obstruction detection automatically.
5.  **Verify**: Use `capture` again to confirm the UI has updated as expected (e.g., a new screen is shown, or a success message appeared).

## 5. Examples

### Example: Navigate to Settings and Check for Updates
```
# Go to home screen
navigate({ action: "home" })

# Open app drawer
navigate({ action: "app-drawer" })

# Find and tap through settings
tap_element({ text: "Settings", waitSeconds: 2 })
tap_element({ text: "System", waitSeconds: 2 })
tap_element({ text: "Software updates", waitSeconds: 2 })
tap_element({ text: "Check for update", waitSeconds: 5 })
```

### Example: Swiping
For swipe gestures not covered by the navigate tool, use raw ADB:
```bash
adb shell input swipe 672 2800 672 500 1000 && sleep 1 && adb shell screencap -p /sdcard/screen.png && adb pull /sdcard/screen.png .
```

## 6. Best Practices

### Coordinate Calculation
*   Prefer `find_element` or `tap_element` over manual coordinate calculation — they handle bounds parsing, center computation, and obstruction detection automatically
*   When multiple instances of an element exist (e.g., in prediction row and full list), check the `find_element` response to verify you're targeting the correct one

### Navigation and State Evaluation
*   **Verify after each interaction**: Don't assume an action succeeded — use `capture` after interactions to confirm the UI changed as expected
*   **Check both visual and structural state**: Use screenshot for visual verification, XML dump for structural confirmation (element presence, text content, state changes)
*   **Identify navigation failures early**: If a tap opened the wrong screen, use `navigate({ action: "back" })` to recover immediately

### Interaction Patterns
*   **Scrolling before interaction**: When looking for an element, check if it's visible on screen first. If not, scroll using swipe gestures to reveal it
*   **Use consistent scroll direction**: For vertical scrolling in lists/settings, use downward swipes (higher Y -> lower Y) to scroll down
*   **Handle app crashes gracefully**: Don't retry the same action — use back button and try an alternative approach
*   **Check Accessibility**: Use the `content-desc` and `text` properties in the UI hierarchy to ensure the UI is accessible for screen readers

## 7. Troubleshooting

### Device Not Connected
If tools report ADB errors:
*   Check USB connection or emulator status
*   Enable USB debugging on the device (Settings > Developer Options > USB Debugging)
*   Accept the RSA key prompt on the device if asked
*   Restart the device or disconnect/reconnect the USB cable
*   Run `adb devices` to verify the device is visible

### MCP Server Not Available
If tools are not listed in `/mcp`:
*   Ensure Node.js 18+ is installed
*   The server auto-builds on first use via `.mcp.json` at the project root
*   Check `.claude/mcp/android-device-server/` exists with `package.json`
*   Try manual build: `cd .claude/mcp/android-device-server && npm install && npm run build`
