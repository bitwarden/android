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
    echo "## What's Changed"
    echo
    echo "No changes found between $CURRENT_REF and $NEW_REF"
    exit 0
fi


# GitHub renders org/repo#123 as a link to a PR, removing the commit message when a PR ID is found
# including the raw changelog in a collapsible section in case the pattern matching fails
CLEANED_CHANGELOG=$(echo "$CHANGELOG" | sed -E "s|.*\(#([0-9]+)\).*|- $REPO#\1|")

# Fetch the labels of every changelog PR in a single GraphQL call, aliasing each PR as pr<number>
PR_NUMBERS=$(echo "$CHANGELOG" | sed -nE 's|.*\(#([0-9]+)\).*|\1|p' | sort -u)
BREAKING_PR_NUMBERS=""
if [ -n "$PR_NUMBERS" ]; then
    PR_FIELDS=""
    for PR_NUMBER in $PR_NUMBERS; do
        PR_FIELDS+="pr$PR_NUMBER: pullRequest(number: $PR_NUMBER) { labels(first: 100) { nodes { name } } } "
    done

    LABELS_RESPONSE=$(gh api graphql \
        -f query="query(\$owner: String!, \$name: String!) { repository(owner: \$owner, name: \$name) { $PR_FIELDS } }" \
        -f owner="${REPO%%/*}" \
        -f name="${REPO#*/}") \
        || echo "::warning::Failed to fetch some $REPO PR labels; breaking-change detection may be incomplete." >&2
    BREAKING_PR_NUMBERS=$(jq -r '(.data.repository // {}) | to_entries[]
        | select(.value != null)
        | select(any(.value.labels.nodes[];
            .name == "breaking-change" or .name == "breaking-change-android"))
        | .key | ltrimstr("pr")' <<< "$LABELS_RESPONSE") || BREAKING_PR_NUMBERS=""
fi

if [ -n "$BREAKING_PR_NUMBERS" ]; then
    for PR_NUMBER in $BREAKING_PR_NUMBERS; do
        CLEANED_CHANGELOG=$(echo "$CLEANED_CHANGELOG" | sed -E "s|^- $REPO#$PR_NUMBER\$|& :feelsgood:|")
    done

    echo "> [!WARNING]"
    echo "> Breaking changes detected :feelsgood:"
    echo
fi

echo "## What's Changed"
echo
echo "$CLEANED_CHANGELOG"
echo
echo "<details>
<summary>Raw changelog</summary>

\`\`\`
$CHANGELOG
\`\`\`
</details>
"
