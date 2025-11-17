import re
import sys
import subprocess
import json
from typing import List, Tuple

def extract_jira_tickets(line: str) -> List[str]:
    # Find all Jira tickets in format ABC-123 (with any prefix/suffix)
    return re.findall(r'[A-Z]+-\d+', line)

def extract_pr_numbers(line: str) -> List[str]:
    # Match PR numbers from GitHub format (#123)
    return re.findall(r'#(\d+)', line)

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
        r'BACKPORT',              # BACKPORT -> ""
        r'[deps]:',               # [deps]: -> ""
        r'feat(?:\([^)]*\))?:',   # feat: or feat(ui): -> ""
        r'bug(?:\([^)]*\))?:',    # bug: or bug(core): -> ""
        r'ci(?:\([^)]*\))?:'      # ci: or ci(workflow): -> ""
    ]
    for pattern in patterns:
        line = re.sub(pattern, '', line)

    cleaned = line.strip()
    if cleaned != original.strip():
        print(f"Processed: {original.strip()} -> {cleaned}")
    return cleaned

def process_file(input_file: str) -> Tuple[List[str], List[str], List[str]]:
    jira_tickets: List[str] = []
    pr_numbers: List[str] = []
    processed_lines: List[str] = []

    print("Processing file: ", input_file)

    with open(input_file, 'r') as f:
        for line in f:
            line = line.strip()
            should_process = line and not line.endswith(':')

            if should_process:
                tickets = extract_jira_tickets(line)
                jira_tickets.extend(tickets)

                prs = extract_pr_numbers(line)
                pr_numbers.extend(prs)
                processed_lines.append(process_line(line))
            else:
                processed_lines.append(line)


    # Remove duplicates while preserving order
    jira_tickets = list(dict.fromkeys(jira_tickets))
    pr_numbers = list(dict.fromkeys(pr_numbers))

    print("Jira tickets:", ",".join(jira_tickets))
    print("PR numbers:", ",".join(pr_numbers))
    print("Finished processing file: ", input_file)
    return jira_tickets, pr_numbers, processed_lines

def save_results(jira_tickets: List[str], pr_numbers: List[str], processed_lines: List[str],
                jira_file: str = 'jira_tickets.txt',
                pr_file: str = 'pr_numbers.txt',
                processed_file: str = 'processed_notes.txt') -> None:
    with open(jira_file, 'w') as f:
        f.write('\n'.join(jira_tickets))

    with open(pr_file, 'w') as f:
        f.write('\n'.join(pr_numbers))

    with open(processed_file, 'w') as f:
        f.write('\n'.join(processed_lines))

if __name__ == '__main__':
    input_file = 'release_notes.txt'
    jira_file = 'jira_tickets.txt'
    pr_file = 'pr_numbers.txt'
    processed_file = 'processed_notes.txt'

    if len(sys.argv) >= 2:
        input_file = sys.argv[1]
    if len(sys.argv) >= 3:
        jira_file = sys.argv[2]
    if len(sys.argv) >= 4:
        pr_file = sys.argv[3]
    if len(sys.argv) >= 5:
        processed_file = sys.argv[4]

    jira_tickets, pr_numbers, processed_lines = process_file(input_file)
    save_results(jira_tickets, pr_numbers, processed_lines, jira_file, pr_file, processed_file)
