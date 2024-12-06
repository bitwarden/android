#!/bin/sh
# CI Build Info Updater
#
# Updates the CIBuildInfo.kt file with additional info from the CI build.
#
# Prerequisites:
#   - Git command line tools installed
#   - Write access to CIBuildInfo.kt file

if [ $# -ne 4 ]; then
    echo "Usage: $0 <repository> <branch> <commit_hash> <ci_run_url>"
    echo "E.g: $0 bitwarden/mobile main abc123 https://github.com/bitwarden/android/actions/runs/123"
    exit 1
fi

set -euo pipefail

repository=$1
branch=$2
commit_hash=$3
ci_run_url=$4

ci_build_info_file="UPDATEME"
branch=$(git branch --show-current)
commit_hash=$(git rev-parse --verify HEAD)

echo "🧱 Updating app CI Build info..."
echo "🧱 CI run url: ${ci_run_url}"


cat << EOF > ${ci_build_info_file}
UPDATEME
        ":seedling:": "${repository}/${branch}@${commit_hash}",
        ":octocat:": "${ci_run_url}",
UPDATEME
EOF

echo "✅ CI Build info updated successfully."
