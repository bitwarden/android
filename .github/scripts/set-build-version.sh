#!/usr/bin/env bash
set -euo pipefail

# Runs fastlane setBuildVersionInfo and appends Version Name/Number to GITHUB_STEP_SUMMARY.
# Usage: set-build-version.sh <version_code> [version_name] [toml_path]

VERSION_CODE="${1:?Usage: $0 <version_code> [version_name] [toml_path]}"
VERSION_NAME="${2:-}"
TOML_FILE="${3:-gradle/libs.versions.toml}"

bundle exec fastlane setBuildVersionInfo \
  versionCode:"$VERSION_CODE" \
  versionName:"$VERSION_NAME"

if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
    VERSION_NAME=""
    regex='appVersionName = "([^"]+)"'
    if [[ "$(cat "$TOML_FILE")" =~ $regex ]]; then
        VERSION_NAME="${BASH_REMATCH[1]}"
    fi
    echo "Version Name: ${VERSION_NAME}" >> "$GITHUB_STEP_SUMMARY"
    echo "Version Number: $VERSION_CODE" >> "$GITHUB_STEP_SUMMARY"
fi
