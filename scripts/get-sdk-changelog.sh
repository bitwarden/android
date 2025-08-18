#!/bin/bash

# Script to get changelog from sdk-internal repo between two git refs
# Usage: ./scripts/get-sdk-changelog.sh <current-ref> <new-ref>

set -euo pipefail

# Check arguments
if [ $# -lt 2 ]; then
    echo "Usage: $0 <current-ref> <new-ref>"
    echo "Example: $0 9fe3aeda fix-wasm-import"
    echo "Example: $0 2450-9fe3aeda 2577-fix-wasm-import"
    exit 1
fi

CURRENT_REF="$1"
NEW_REF="$2"
REPO="bitwarden/sdk-internal"

echo "📝 Getting changelog from $CURRENT_REF to $NEW_REF in $REPO"

# Get commits between the two refs
CHANGELOG=$(gh api "repos/$REPO/compare/$CURRENT_REF...$NEW_REF" \
    --jq '.commits[] | "- \(.commit.message | split("\n")[0]) (\(.sha[0:7]))"' | head -20)

if [ -z "$CHANGELOG" ]; then
    echo "No changes found between $CURRENT_REF and $NEW_REF"
    exit 0
fi

echo "$CHANGELOG"
