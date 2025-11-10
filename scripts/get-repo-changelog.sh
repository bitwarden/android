#!/bin/bash

# Script to get changelog from sdk-internal repo between two git refs
# Usage: ./scripts/get-sdk-changelog.sh <current-ref> <new-ref>

set -euo pipefail

if [ $# -lt 2 ]; then
    echo "Usage: $0 <repo> <current-ref> <new-ref>"
    echo "Example: $0 bitwarden/sdk-internal 9fe3aeda fix-wasm-import"
    echo "Example: $0 bitwarden/sdk-internal 2450-9fe3aeda 2577-fix-wasm-import"
    exit 1
fi

REPO="$1"
CURRENT_REF="$2"
NEW_REF="$3"

CHANGELOG=$(gh api "repos/$REPO/compare/$CURRENT_REF...$NEW_REF" \
    --jq '.commits[] | "- \(.commit.message | split("\n")[0])"')

if [ -z "$CHANGELOG" ]; then
    echo "No changes found between $CURRENT_REF and $NEW_REF"
    exit 0
fi


# GitHub renders org/repo#123 as a link to a PR, removing the commit message when a PR ID is found
# including the raw changelog in a collapsible section in case the pattern matching fails
CLEANED_CHANGELOG=$(echo "$CHANGELOG" | sed -E "s|.*\(#([0-9]+)\).*|- $REPO#\1|")

echo "$CLEANED_CHANGELOG"
echo
echo "<details>
<summary>Raw changelog</summary>

\`\`\`
$CHANGELOG
\`\`\`
</details>
"
