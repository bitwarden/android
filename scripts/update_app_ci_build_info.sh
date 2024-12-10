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
    echo "E.g: $0 bitwarden/android main abc123 https://github.com/bitwarden/android/actions/runs/123"
    exit 1
fi

set -euo pipefail

repository=$1
branch=$2
commit_hash=$3
ci_run_url=$4

ci_build_info_file="../app/src/main/java/com/x8bit/bitwarden/ui/platform/feature/settings/about/utils/CIBuildInfo.kt"
git_source="${repository}/${branch}@${commit_hash}"

echo "🧱 Updating app CI Build info..."
echo "🧱 CI run url: ${ci_run_url}"


cat << EOF > ${ci_build_info_file}
object CIBuildInfo {
    val info: List<Pair<String, String>> = listOf(
        ":seedling:" to "${git_source}",
        ":octocat:" to "${ci_run_url}",
    )
}
EOF

echo "✅ CI Build info updated successfully."
