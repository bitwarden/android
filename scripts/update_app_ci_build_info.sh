#!/bin/sh
# CI Build Info Updater
#
# Updates the CIBuildInfo.kt file with additional info from the CI build.
#
# Prerequisites:
#   - Git command line tools installed
#   - Write access to CIBuildInfo.kt file

if [ $# -ne 5 ]; then
    echo "Usage: $0 <repository> <branch> <commit_hash> <ci_run_number> <ci_run_attempt>"
    echo "E.g: $0 bitwarden/android main abc123 123 1"
    exit 1
fi

set -euo pipefail

repository=$1
branch=$2
commit_hash=$3
ci_run_number=$4
ci_run_attempt=$5

ci_build_info_file="../app/src/main/java/com/x8bit/bitwarden/ui/platform/feature/settings/about/utils/CIBuildInfo.kt"
git_source="${repository}/${branch}@${commit_hash}"
ci_run_source="${repository}/actions/runs/${ci_run_number}/attempts/${ci_run_attempt}"

echo "🧱 Updating app CI Build info..."
echo "🧱 🧱 commit: ${git_source}"
echo "🧱 💻 build source: ${ci_run_source}"


cat << EOF > ${ci_build_info_file}
object CIBuildInfo {
    val info: List<Pair<String, String>> = listOf(
        "🧱 commit:" to "${git_source}",
        "💻 build source:" to "${ci_run_source}",
    )
}
EOF

echo "✅ CI Build info updated successfully."
