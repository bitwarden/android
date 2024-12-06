#!/bin/sh
#
# Updates the CIBuildInfo.swift file to add any additional info we need from the CI build.
#
# Usage:
#
#   $ ./update_app_ci_build_info.sh <ci_run_id> <ci_run_number> <ci_run_attempt> "<compiler_flags>"

set -euo pipefail

if [ $# -lt 3 ]; then
  echo >&2 "Called without necessary arguments."
  echo >&2 "For example: \`Scripts/update_app_ci_build_info.sh 123123 111 1 \"DEBUG_MENU FEATURE1\"\`"
  exit 1
fi

ci_run_id=$1
ci_run_number=$2
ci_run_attempt=$3
compiler_flags=${4:-''}

ci_build_info_file="UPDATEME"
repository=$(git config --get remote.origin.url)
branch=$(git branch --show-current)
commit_hash=$(git rev-parse --verify HEAD)

echo "🧱 Updating app CI Build info..."
echo "🧱 CI run ID: ${ci_run_id}"
echo "🧱 CI run number: ${ci_run_number}"
echo "🧱 CI run attempt: ${ci_run_attempt}"
echo "🧱 Compiler Flags: ${compiler_flags}"

cat << EOF > ${ci_build_info_file}
UPDATEME
        "Repository": "${repository}",
        "Branch": "${branch}",
        "Commit hash": "${commit_hash}",
        "CI Run ID": "${ci_run_id}",
        "CI Run Number": "${ci_run_number}",
        "CI Run Attempt": "${ci_run_attempt}",
        "Compiler Flags": "${compiler_flags}",
UPDATEME
EOF

echo "✅ CI Build info updated successfully."
