#!/usr/bin/env python3

import json
import sys
import subprocess

LABEL_PATH_PATTERNS = {
    "app:shared": [
        "annotation/",
        "core/",
        "data/",
        "network/",
        "ui/",
        "authenticatorbridge/",
        "gradle/"
    ],
    "app:password-manager": [
        "app/",
        "cxf/",
    ],
    "app:authenticator": [
        "authenticator/",
    ],
    "t:ci": [
        ".github/",
        "scripts/",
        "fastlane/",
        ".gradle/",
        ".claude/",
        "detekt-config.yml",
    ],
    "t:docs": [
        "docs/",
    ],
    "t:deps": [ #TODO test this
        "gradle/",
    ],
    "t:misc": [ # catch-all label case for changes that aren't captured by other labels
        "keystore/",
    ]
}

def gh_get_changed_files(pr_number: str) -> list[str]:
    """Get list of changed files in a pull request."""
    try:
        result = subprocess.run(
            ["gh", "pr", "diff", pr_number, "--name-only"],
            capture_output=True,
            text=True,
            check=True
        )
        changed_files = result.stdout.strip().split("\n")
        return list(filter(None, changed_files))
    except subprocess.CalledProcessError as e:
        print(f"Error getting changed files: {e}")
        sys.exit(1)

def gh_add_labels(pr_number: str, labels: list[str]) -> None:
    """Add labels to a pull request (doesn't remove existing labels)."""
    gh_labels = ','.join(labels)
    subprocess.run(
        ["gh", "pr", "edit", pr_number, "--add-label", gh_labels],
        check=True
    )

def gh_replace_labels(pr_number: str, labels: list[str]) -> None:
    """Replace all labels on a pull request with the specified labels."""
    payload = json.dumps({"labels": labels})
    subprocess.run(
        ["gh", "api", "repos/{owner}/{repo}/issues/" + pr_number, "-X", "PATCH", "--silent", "--input", "-"],
        input=payload,
        text=True,
        check=True
    )

def label_filepaths(changed_files: list[str]) -> list[str]:
    """Check changed files against path patterns and return labels to apply."""
    labels_to_apply = set()  # Use set to avoid duplicates

    for file in changed_files:
        for label, patterns in LABEL_PATH_PATTERNS.items():
            if any(file.startswith(pattern) for pattern in patterns):
                print(f"👀 File '{file}' matches pattern for label '{label}'")
                labels_to_apply.add(label)

    if "app:shared" in labels_to_apply:
        labels_to_apply.add("app:password-manager")
        labels_to_apply.add("app:authenticator")
        labels_to_apply.remove("app:shared")

    return list(labels_to_apply)

def main():
    if len(sys.argv) < 2:
        print("Error: PR_NUMBER is required")
        print(f"Usage: {sys.argv[0]} <pr-number>")
        sys.exit(1)

    pr_number = sys.argv[1]

    print(f"🔍 Checking files changed in PR #{pr_number}...")

    changed_files = gh_get_changed_files(pr_number)

    if not changed_files:
        print("ℹ️  No files changed in this PR")
        sys.exit(0)

    print("👀 Changed files:")
    for file in changed_files:
        print(file)
    print()

    filepath_labels = label_filepaths(changed_files)

    if filepath_labels:
        labels_str = ', '.join(filepath_labels)
        print(f"🏷️  Adding labels: {labels_str}")
        gh_replace_labels(pr_number, filepath_labels)
    else:
        print("ℹ️  No matching paths found, no labels applied.")

    print("✅ Done")


if __name__ == "__main__":
    main()
