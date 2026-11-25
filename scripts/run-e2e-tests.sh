#!/usr/bin/env bash
#
# Runs the end-to-end UI tests on a local device or emulator against a test server. CI doesn't
# use this: it runs them through Gradle and the test orchestrator (see test-device.yml).
# Gradle only installs the APKs here; the tests run with `am instrument`, because Gradle keeps
# instrumentation arguments, including the Seeder token, in its execution history.
#
# Usage:
#
#   $ ./scripts/run-e2e-tests.sh [--serial <device>] [--test <Class[#method]>]
#
# Where:
#   - --serial runs on that device or emulator instead of the only one connected.
#   - --test runs one class or method, e.g. VaultLoginTest#test_22270_logIn. The package
#     com.x8bit.bitwarden.e2e is added when the name has none. Without it, every e2e test runs.
#
# The tests need these variables exported in the environment; app/src/androidTest/README.md
# explains each one:
#   BW_TEST_SERVER_URL, BW_SEEDER_URL, BW_SEEDER_TOKEN
#
# Each test runs in its own `am instrument` call after `pm clear`, so every test starts from a clean
# app. That erases com.x8bit.bitwarden.dev's data: don't run this on a device whose debug install
# you want to keep.
#
# Examples:
#   $ ./scripts/run-e2e-tests.sh
#   $ ./scripts/run-e2e-tests.sh --test VaultLoginTest#test_22279_readUsernameAndPassword

set -euo pipefail

APP_ID="com.x8bit.bitwarden.dev"
TEST_APP_ID="$APP_ID.test"
RUNNER="$TEST_APP_ID/androidx.test.runner.AndroidJUnitRunner"
E2E_PACKAGE="com.x8bit.bitwarden.e2e"
CONFIGURATION_VARIABLES=(
  BW_TEST_SERVER_URL
  BW_SEEDER_URL
  BW_SEEDER_TOKEN
)

serial=""
test_filter=""

usage() {
  sed -n '3,28p' "$0" | sed 's/^# \{0,1\}//'
}

while [ $# -gt 0 ]; do
  case "$1" in
    --serial)
      serial="${2:?Missing value for $1}"
      shift 2
      ;;
    --test)
      test_filter="${2:?Missing value for $1}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo >&2 "❌ Unknown argument: $1"
      usage >&2
      exit 1
      ;;
  esac
done

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
cd "$(dirname "$script_dir")"

for name in "${CONFIGURATION_VARIABLES[@]}"; do
  if [ -z "${!name:-}" ]; then
    echo >&2 "❌ $name isn't set. Export it first (see app/src/androidTest/README.md)."
    exit 1
  fi
done

if [ -n "$serial" ]; then
  export ANDROID_SERIAL="$serial"
else
  device_count=$(adb devices | grep -c $'\tdevice$' || true)
  if [ "$device_count" -ne 1 ]; then
    echo >&2 "❌ Expected one connected device, found $device_count. Pass --serial to pick one."
    exit 1
  fi
fi

# `adb shell` joins its arguments into one command line that the device's shell parses again, so
# every value is single-quoted for that shell.
remote_quote() {
  local quote="'"
  printf "'%s'" "${1//$quote/$quote\\$quote$quote}"
}

echo "📦 Installing the app and the tests"
# Uninstalling first makes sure a stale install (e.g. from an emulator snapshot) can't pass for a
# clean start. The test APK goes with it, so both are reinstalled.
adb uninstall "$APP_ID" > /dev/null 2>&1 || true
adb uninstall "$TEST_APP_ID" > /dev/null 2>&1 || true
./gradlew --quiet :app:installStandardDebug :app:installStandardDebugAndroidTest

run_instrument() {
  adb shell am instrument -w "$@" \
    -e BW_TEST_SERVER_URL "$(remote_quote "$BW_TEST_SERVER_URL")" \
    -e BW_SEEDER_URL "$(remote_quote "$BW_SEEDER_URL")" \
    -e BW_SEEDER_TOKEN "$(remote_quote "$BW_SEEDER_TOKEN")" \
    "$RUNNER"
}

if [ -n "$test_filter" ]; then
  case "$test_filter" in
    *.*) selector="$test_filter" ;;
    *) selector="$E2E_PACKAGE.$test_filter" ;;
  esac
  selector_args=(-e class "$(remote_quote "$selector")")
else
  selector_args=(-e package "$E2E_PACKAGE")
fi

# A dry run (`-e log true`) lists the selected tests without running them.
listing=$(adb shell am instrument -w -r -e log true "${selector_args[@]}" "$RUNNER" | tr -d '\r')
if grep -q '^INSTRUMENTATION_FAILED\|^INSTRUMENTATION_RESULT: shortMsg=' <<< "$listing"; then
  echo >&2 "❌ Listing the tests failed:"
  echo >&2 "$listing"
  exit 1
fi

# JUnit reports an unmatched --test filter as a synthetic "initializationError" test.
tests=()
while IFS= read -r test_name; do
  tests+=("$test_name")
done < <(awk '
  /^INSTRUMENTATION_STATUS: class=/ { class = substr($0, index($0, "=") + 1) }
  /^INSTRUMENTATION_STATUS: test=/ { test = substr($0, index($0, "=") + 1) }
  /^INSTRUMENTATION_STATUS_CODE: 1$/ && test != "initializationError" { print class "#" test }
' <<< "$listing")

if [ ${#tests[@]} -eq 0 ]; then
  echo >&2 "❌ No tests matched ${test_filter:-$E2E_PACKAGE}."
  exit 1
fi

test_output=$(mktemp)
trap 'rm -f "$test_output"' EXIT

failed=()
for test_name in "${tests[@]}"; do
  echo "🧪 $test_name"
  adb shell pm clear "$APP_ID" > /dev/null
  # `am instrument -w` has no timeout, so output is streamed rather than shown at the end.
  run_instrument -e class "$(remote_quote "$test_name")" | tr -d '\r' | tee "$test_output" || true
  if ! grep -q '^OK (1 test)' "$test_output"; then
    failed+=("$test_name")
  fi
done

echo
echo "Ran ${#tests[@]} test(s), ${#failed[@]} failed."
if [ ${#failed[@]} -gt 0 ]; then
  printf '❌ %s\n' "${failed[@]}"
  exit 1
fi
echo "✅ All passed."
