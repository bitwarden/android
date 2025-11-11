#!/usr/bin/env python3
"""
Label pull requests based on changed file paths and PR title patterns (conventional commit format).

Usage:
    python label-pr.py <pr-number> [-a|--add|-r|--replace] [-d|--dry-run]

Arguments:
    pr-number: The pull request number
    -a, --add: Add labels without removing existing ones (default)
    -r, --replace: Replace all existing labels
    -d, --dry-run: Run without actually applying labels

Examples:
    python label-pr.py 1234
    python label-pr.py 1234 -a
    python label-pr.py 1234 --replace
    python label-pr.py 1234 -r -d
"""

import argparse
import json
import sys
import subprocess

DEFAULT_MODE = "add"
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
    parser = argparse.ArgumentParser(
        description="Label pull requests based on changed file paths and PR title patterns."
    )
    parser.add_argument(
        "pr_number",
        help="The pull request number"
    )

    mode_group = parser.add_mutually_exclusive_group()
    mode_group.add_argument(
        "-a", "--add",
        action="store_true",
        help="Add labels without removing existing ones (default)"
    )
    mode_group.add_argument(
        "-r", "--replace",
        action="store_true",
        help="Replace all existing labels"
    )

    parser.add_argument(
        "-d", "--dry-run",
        action="store_true",
        help="Run without actually applying labels"
    )

    args = parser.parse_args()

    pr_number = args.pr_number
    mode = "add" if args.add else "replace"

    if args.dry_run:
        print("🔍 DRY RUN MODE - Labels will not be applied")

    print(f"🔍 Checking PR #{pr_number}...")
    print(f"📌 Labeling mode: {mode}")

    pr_title = gh_get_pr_title(pr_number)
    print(f"📋 PR Title: {pr_title}\n")

    changed_files = gh_get_changed_files(pr_number)
    print("👀 Changed files:" + "\n".join(changed_files) + "\n")

    filepath_labels = label_filepaths(changed_files)
    title_labels = label_title(pr_title)
    all_labels = set(filepath_labels + title_labels)

    if not any(label.startswith("t:") for label in all_labels):
        all_labels.add(CATCH_ALL_LABEL)

    if all_labels:
        labels_str = ', '.join(sorted(all_labels))
        if args.re:
            print(f"🏷️  Replacing labels with: {labels_str}")
            if not args.dry_run:
                gh_replace_labels(pr_number, list(all_labels))
        else:
            print(f"🏷️  Adding labels: {labels_str}")
            if not args.dry_run:
                gh_add_labels(pr_number, list(all_labels))
    else:
        print("ℹ️  No matching patterns found, no labels applied.")

    print("✅ Done")

if __name__ == "__main__":
    main()
