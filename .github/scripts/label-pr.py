#!/usr/bin/env python3

import json
import sys
import subprocess

CATCH_ALL_LABEL = "t:misc"

LABEL_TITLE_PATTERNS = {
    "t:new-feature": ["feat", "feature"],
    "t:bug": ["fix", "bug", "bugfix"],
    "t:tech-debt": ["refactor", "chore", "cleanup", "revert", "debt", "test", "perf"],
    "t:docs": ["docs"],
    "t:ci": ["ci", "build", "chore(ci)"],
    "t:deps": ["deps"],
    "t:breaking-change": ["breaking", "breaking-change"],
    "t:misc": ["misc"],
}

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
    "t:deps": [
        "gradle/",
    ],
    CATCH_ALL_LABEL: [
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
        return None

def gh_get_pr_title(pr_number: str) -> str:
    """Get the title of a pull request."""
    try:
        result = subprocess.run(
            ["gh", "pr", "view", pr_number, "--json", "title", "--jq", ".title"],
            capture_output=True,
            text=True,
            check=True
        )
        return result.stdout.strip()
    except subprocess.CalledProcessError as e:
        print(f"Error getting PR title: {e}")
        return None

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

    for label, patterns in LABEL_PATH_PATTERNS.items():
        for file in changed_files:
            if any(file.startswith(pattern) for pattern in patterns):
                print(f"👀 File '{file}' matches pattern for label '{label}'")
                labels_to_apply.add(label)
                break

    if "app:shared" in labels_to_apply:
        labels_to_apply.add("app:password-manager")
        labels_to_apply.add("app:authenticator")
        labels_to_apply.remove("app:shared")

    if not labels_to_apply:
        print("::warning::No matching file paths found, no labels applied.")

    return list(labels_to_apply)

def label_title(pr_title: str) -> list[str]:
    """Check PR title against patterns and return labels to apply."""
    labels_to_apply = set()

    title_lower = pr_title.lower()

    for label, patterns in LABEL_TITLE_PATTERNS.items():
        for pattern in patterns:
            # Check for pattern with : or ( suffix (conventional commit format)
            if f"{pattern}:" in title_lower or f"{pattern}(" in title_lower:
                print(f"📝 Title matches pattern '{pattern}' for label '{label}'")
                labels_to_apply.add(label)
                break

    if not labels_to_apply:
        print("::warning::No matching title patterns found, no labels applied.")

    return list(labels_to_apply)

def main():
    if len(sys.argv) < 2:
        print("Error: PR_NUMBER is required")
        print(f"Usage: {sys.argv[0]} <pr-number>")
        sys.exit(1)

    pr_number = sys.argv[1]

    print(f"🔍 Checking PR #{pr_number}...")

    pr_title = gh_get_pr_title(pr_number)
    print(f"📋 PR Title: {pr_title}\n")

    changed_files = gh_get_changed_files(pr_number)
    print("👀 Changed files:")
    for file in changed_files:
        print(file)
    print()

    filepath_labels = label_filepaths(changed_files)
    title_labels = label_title(pr_title)

    all_labels = set(filepath_labels + title_labels)

    if not any(label.startswith("t:") for label in all_labels):
        all_labels.add(CATCH_ALL_LABEL)

    if all_labels:
        labels_str = ', '.join(sorted(all_labels))
        print(f"🏷️  Applying labels: {labels_str}")
        gh_replace_labels(pr_number, list(all_labels))
    else:
        print("ℹ️  No matching patterns found, no labels applied.")

    print("✅ Done")


if __name__ == "__main__":
    main()
