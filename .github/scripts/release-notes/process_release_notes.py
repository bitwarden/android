import re
import sys
import subprocess
import json
import argparse
from typing import List, Tuple

def extract_jira_tickets(line: str) -> List[str]:
    """Find all Jira tickets in format ABC-123 (with any prefix/suffix)"""
    return re.findall(r'[A-Z]+-\d+', line)

def extract_pr_numbers(line: str) -> List[str]:
    """Match PR numbers from GitHub format (#123)"""
    return re.findall(r'#(\d+)', line)

def extract_pr_url(line: str) -> str:
    """Match PR URL from GitHub format https://github.com/foo/bar/pull/123

    Returns:
        The first PR URL found in the line, or empty string if no URL is found
    """
    matches = re.findall(r'https://github\.com/[\w-]+/[\w.-]+/pull/\d+', line)
    return matches[0] if matches else ""

def fetch_labels(github_pr_url: str) -> List[str]:
    """Fetch labels from a GitHub PR using the GitHub CLI."""
    result = subprocess.run(
        ['gh', 'pr', 'view', github_pr_url, '--json', 'labels', '--jq', '.labels[].name'],
        capture_output=True,
        text=True,
        check=True
    )
    return [label.strip() for label in result.stdout.strip().split('\n') if label.strip()]

def should_skip_pr(release_app_label: str, pr_labels: List[str]) -> bool:
    """Check if the PR should be skipped based on app labels.

    Skip if there's at least one label that starts with "app:" but release_app_label isn't found.

    Args:
        release_app_label: The app label to look for (e.g., "app:password-manager")
        pr_labels: List of labels from the PR

    Returns:
        True if the PR should be skipped, False otherwise
    """
    pr_app_labels = [label for label in pr_labels if label.startswith('app:')]
    # Skip if there are app labels but release_app_label is not among them
    return len(pr_app_labels) > 0 and release_app_label not in pr_app_labels

def process_line(line: str) -> str:
    """Process a single line from release notes by removing Jira tickets, conventional commit prefixes and other common patterns.

    Args:
        line: A single line from release notes

    Returns:
        Processed line with tickets and prefixes removed

    Example:
        >>> process_line("[ABC-123] feat(ui): Add new button")
        "Add new button"
    """
    original = line

    # Remove Jira ticket patterns:
    line = re.sub(r'\[[A-Z]+-\d+\]', '', line) # [ABC-123] -> ""
    line = re.sub(r'[A-Z]+-\d+:\s', '', line) # ABC-123: -> ""
    line = re.sub(r'[A-Z]+-\d+\s-\s', '', line) # ABC-123 - -> ""

    # Remove keywords and their variations
    patterns = [
        r'🍒',                      # 🍒 -> ""
        r'BACKPORT',                # BACKPORT -> ""
        r'[deps]:',                 # [deps]: -> ""
        r'feat(?:\([^)]*\))?:',     # feat: or feat(ui): -> ""
        r'bug(?:\([^)]*\))?:',      # bug: or bug(core): -> ""
        r'ci(?:\([^)]*\))?:'        # ci: or ci(workflow): -> ""
    ]
    for pattern in patterns:
        line = re.sub(pattern, '', line)

    # Replace multiple consecutive spaces with a single space
    line = re.sub(r'\s+', ' ', line)

    cleaned = line.strip()
    original_stripped = original.strip()
    if cleaned != original_stripped:
        print(f"Processed: {original_stripped} -> {cleaned}")
    return cleaned

def process_file(input_file: str, release_app_label: str) -> Tuple[List[str], List[str], List[str]]:
    jira_tickets: List[str] = []
    pr_numbers: List[str] = []
    processed_lines: List[str] = []
    debug_lines: List[str] = []
    #community_highlights: List[str] = []

    print("Processing file: ", input_file)

    with open(input_file, 'r') as f:
        for line in f:
            line = line.strip()
            should_process = line and line.startswith('* ')

            if should_process:
                pr_url = extract_pr_url(line)
                pr_labels = []

                # Fetch labels from PR URL if available
                if pr_url:
                    pr_labels = fetch_labels(pr_url)
                    if should_skip_pr(release_app_label, pr_labels):
                        debug_lines.append(f"{line} | skipped - labels: {pr_labels}")
                        continue # skip the PR if it is not labeled with the app label

                tickets = extract_jira_tickets(line)
                jira_tickets.extend(tickets)

                prs = extract_pr_numbers(line)
                pr_numbers.extend(prs)
                processed_lines.append(process_line(line))
                debug_lines.append(f"{line} | labels: {pr_labels}")
            else:
                processed_lines.append(line)
                if line == "":
                    debug_lines.append("")
                else:
                    debug_lines.append(f"{line} | skipped - processing")


    # Remove duplicates while preserving order
    jira_tickets = list(dict.fromkeys(jira_tickets))
    pr_numbers = list(dict.fromkeys(pr_numbers))

    print("Jira tickets:", ",".join(jira_tickets))
    print("PR numbers:", ",".join(pr_numbers))
    print("Finished processing file: ", input_file)
    return jira_tickets, pr_numbers, processed_lines, debug_lines

def save_results(jira_tickets: List[str], pr_numbers: List[str], processed_lines: List[str], debug_lines: List[str],
                jira_file: str = 'jira_tickets.txt',
                pr_file: str = 'pr_numbers.txt',
                processed_file: str = 'processed_notes.txt',
                debug_file: str = 'processed_notes_debug.txt'
                ) -> None:
    with open(jira_file, 'w') as f:
        f.write('\n'.join(jira_tickets))

    with open(pr_file, 'w') as f:
        f.write('\n'.join(pr_numbers))

    with open(processed_file, 'w') as f:
        f.write('\n'.join(processed_lines))

    with open(debug_file, 'w') as f:
        f.write('\n'.join(debug_lines))

def parse_args():
    """Parse command line arguments.

    Returns:
        Parsed arguments namespace
    """
    parser = argparse.ArgumentParser(
        description='Process release notes by extracting Jira tickets and PR numbers, and cleaning up the text.'
    )
    parser.add_argument(
        'release_app_label',
        help='Filter PRs by app label (e.g., app:password-manager)'
    )
    parser.add_argument(
        'input_file',
        default='release_notes.txt',
        help='Input file containing release notes (default: release_notes.txt)'
    )
    parser.add_argument(
        '--processed-filepath',
        default='processed_notes.txt',
        help='Output file for processed notes (default: processed_notes.txt)'
    )
    parser.add_argument(
        '--jira-filepath',
        default='jira_tickets.txt',
        help='Output file for Jira tickets (default: jira_tickets.txt)'
    )
    parser.add_argument(
        '--pr-filepath',
        default='pr_numbers.txt',
        help='Output file for PR numbers (default: pr_numbers.txt)'
    )

    parser.add_argument(
        '--debug-filepath',
        default='processed_notes_debug.txt',
        help='Output file for debug notes (default: processed_notes_debug.txt)'
    )
    return parser.parse_args()

if __name__ == '__main__':
    args = parse_args()

    jira_tickets, pr_numbers, processed_lines, debug_lines = process_file(
        args.input_file,
        args.release_app_label
    )
    save_results(
        jira_tickets,
        pr_numbers,
        processed_lines,
        debug_lines,
        args.jira_filepath,
        args.pr_filepath,
        args.processed_filepath,
        args.debug_filepath
    )
