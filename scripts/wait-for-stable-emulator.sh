#!/usr/bin/env bash
#
# Waits until a freshly booted emulator's system services stay up, for CI.
#
# sys.boot_completed can be set before system_server settles: on CI the package service has
# disappeared between boot and the APK install. This waits until system_server keeps the same pid
# and the package manager keeps answering for STABLE_SECONDS in a row, printing every restart it
# sees, then prints the crash log buffer for diagnosis. It runs before the app is installed, so the
# crash buffer can't hold anything from the app.
#
# Usage:
#   $ ./scripts/wait-for-stable-emulator.sh

set -euo pipefail

STABLE_SECONDS=60
TIMEOUT_SECONDS=600
POLL_SECONDS=5

started=$(date +%s)
stable_since=""
last_pid=""

while true; do
  now=$(date +%s)
  elapsed=$((now - started))

  pid=$(adb shell pidof system_server 2> /dev/null | tr -d '\r' || true)
  if adb shell pm path android > /dev/null 2>&1; then
    package_ok=true
  else
    package_ok=false
  fi

  if [ "$pid" != "$last_pid" ]; then
    echo "[${elapsed}s] system_server pid: ${last_pid:-none} -> ${pid:-none}"
    last_pid="$pid"
    stable_since=""
  fi

  if [ -n "$pid" ] && [ "$package_ok" = true ]; then
    stable_since="${stable_since:-$now}"
    if [ $((now - stable_since)) -ge "$STABLE_SECONDS" ]; then
      echo "[${elapsed}s] system_server stable for ${STABLE_SECONDS}s (pid $pid)."
      break
    fi
  else
    if [ -n "$stable_since" ]; then
      echo "[${elapsed}s] package manager stopped answering (pid ${pid:-none})."
    fi
    stable_since=""
  fi

  if [ "$elapsed" -ge "$TIMEOUT_SECONDS" ]; then
    echo "❌ system_server didn't stay up for ${STABLE_SECONDS}s within ${TIMEOUT_SECONDS}s."
    echo "--- crash buffer ---"
    adb logcat -b crash -d | tail -n 200 || true
    exit 1
  fi

  sleep "$POLL_SECONDS"
done

echo "--- crash buffer ---"
adb logcat -b crash -d | tail -n 200 || true
