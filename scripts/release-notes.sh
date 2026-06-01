#!/bin/bash
# Release Notes Generator
#
# Generates release notes when GitHub's automated system fails, specifically for:
#   - Releases containing cherry-picked commits
#   - Changes without associated Pull Requests

# Prerequisites:
#   - GitHub CLI (gh) installed and authenticated
#   - Git command line tools installed

if [ $# -ne 2 ]; then
    echo "Usage: $0 <tag1|branch1> <tag2|branch2>"
    echo "E.g: $0 v2024.10.2 origin/release/hotfix-v2024.10.2"
    exit 1
fi

TAG1="$1"
TAG2="$2"

echo "## What's Changed"
echo

git log "$TAG1..$TAG2" --pretty=format:"%an|%ae|%s|%b" --reverse |
while IFS='|' read -r name email commit_title commit_body; do
    echo $name $email
    continue
    if [ -z "$email" ]; then
        continue
    fi

    # Extract GitHub username from email
    if [[ "$email" == *"@users.noreply.github.com" ]]; then
        author=${email##*+}
        author=${author%@*}
    else
        # For other emails, look up GitHub username using gh cli
        author=$(gh api -q '.items[0].login' "search/users?q=$email")
    fi

    cherry_picked_hash=$(echo "$commit_body" | grep 'cherry picked' | sed 's/(cherry picked from commit \(.*\))/\1/')
    changelog="* $commit_title by @$author"
    if [[ "$commit_body" == *"cherry picked"* ]]; then
        changelog="$changelog 🍒 $cherry_picked_hash"
    fi

    echo "$changelog"
done
