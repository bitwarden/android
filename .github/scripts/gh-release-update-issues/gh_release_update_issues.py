#!/usr/bin/env python3
# Requires Python 3.9+
"""
Comment GitHub issues linked to Pull Requests mentioned in a given release.

Usage:
    python gh_release_update_issues.py <release_url> [--dry-run]

Arguments:
    release-url: The URL of the release to comment on
    --dry-run: Run without actually updating issues

Examples:
    python gh_release_update_issues.py https://github.com/owner/repo/releases/tag/v1.0.0
    python gh_release_update_issues.py https://github.com/owner/repo/releases/tag/v1.0.0 --dry-run
"""

import re
import subprocess
import json
import argparse
from collections import defaultdict
from typing import List, Tuple, Dict

def parse_release_url(release_url: str) -> Tuple[str, str, str]:
    """Extract owner, repo name, and tag from a GitHub release URL.

    Returns:
        Tuple of (owner, repo_name, release_tag)
    """
    match = re.search(r'github\.com/([\w-]+)/([\w.-]+)/releases/tag/(.+)$', release_url)
    if not match:
        raise ValueError(f"Cannot parse release URL: {release_url}")
    return match.group(1), match.group(2), match.group(3)

def extract_pr_numbers(release_notes: str) -> List[int]:
    return [int(n) for n in re.findall(r'/pull/(\d+)', release_notes)]

def build_issue_comment(repo: str, release_name: str, release_link: str, pr_numbers: List[int]) -> str:
    if len(pr_numbers) == 0:
        return ""

    pr_links = [f"* https://github.com/{repo}/pull/{pr_number}" for pr_number in pr_numbers]

    return f":shipit: Pull Request(s) linked to this issue released in [{release_name}]({release_link}):\n\n"+ "\n".join(pr_links)

def gh_fetch_release(repo: str, release_tag: str) -> Tuple[str, str]:
    result = subprocess.run(
        ['gh', 'release', 'view', release_tag, '--repo', repo, '--json', 'name,body'],
        capture_output=True, text=True, check=True
    )
    data = json.loads(result.stdout)
    return data['name'], data['body']

def gh_comment_issue(repo: str, issue_number: int, comment: str) -> None:
    """Use GitHub CLI to comment on an issue.
    """
    subprocess.run([
        'gh', 'issue', 'comment', str(issue_number), '--body', comment, '--repo', repo
    ], check=True)

def gh_fetch_linked_issues_batched(owner: str, repo_name: str, pr_numbers: List[int]) -> Dict[int, List[int]]:
    """Batch-fetch linked issues for all PRs in a single GraphQL call.

    Returns:
        Dict mapping each PR number to its list of linked issue numbers.
    """
    if not pr_numbers:
        return {}

    tmpl = 'pr_%d: pullRequest(number: %d) { closingIssuesReferences(first: 100) { nodes { number } } }'
    pr_fragments = "\n".join(tmpl % (pr, pr) for pr in pr_numbers)
    query = """
    query ($owner: String!, $repo: String!) {
        repository(owner: $owner, name: $repo) {
            %s
        }
    }
    """ % pr_fragments

    try:
        result = subprocess.run(
            [
                'gh', 'api', 'graphql',
                '-F', f'owner={owner}',
                '-F', f'repo={repo_name}',
                '-f', f'query={query}',
            ],
            capture_output=True, text=True, check=True,
        )
        data = json.loads(result.stdout)
        repo_data = data['data']['repository']

        pr_issues_map: Dict[int, List[int]] = {}
        for pr_number in pr_numbers:
            nodes = repo_data.get(f'pr_{pr_number}', {}).get('closingIssuesReferences', {}).get('nodes', [])
            pr_issues = [node['number'] for node in nodes]
            pr_issues_map[pr_number] = pr_issues
        return pr_issues_map

    except subprocess.CalledProcessError as e:
        print(f"Error batch-fetching linked issues: {e.stderr}")
        return {}

def map_issues_to_prs(pr_issues_map: Dict[int, List[int]]) -> Dict[int, List[int]]:
    """Invert a PR->issues map into an issue->PRs map."""
    issue_pr_map: Dict[int, List[int]] = defaultdict(list)
    for pr_number, issue_numbers in pr_issues_map.items():
        for issue_number in issue_numbers:
            issue_pr_map[issue_number].append(pr_number)
    return dict(issue_pr_map)

def comment_issues(repo: str, issue_pr_map: Dict[int, List[int]], release_name: str, release_url: str, dry_run: bool) -> None:
    for issue_number, linked_prs in issue_pr_map.items():
        comment = build_issue_comment(repo, release_name, release_url, linked_prs)
        print(f"{'Dry run - ' if dry_run else ''}Commenting on issue {issue_number}:\n{comment}\n")
        if not dry_run and comment:
            gh_comment_issue(repo, issue_number, comment)

def parse_args():
    parser = argparse.ArgumentParser(
        description='Comment GitHub issues linked to Pull Requests mentioned in a given release.'
    )
    parser.add_argument(
        'release_url',
        help='Release URL (e.g. https://github.com/owner/repo/releases/tag/v1.0.0)'
    )
    parser.add_argument(
        '--dry-run',
        action='store_true',
        help='Run without actually commenting issues'
    )
    return parser.parse_args()

if __name__ == '__main__':
    args = parse_args()

    owner, repo_name, release_tag = parse_release_url(args.release_url)
    repo = f"{owner}/{repo_name}"
    print(f"📋 Release URL: {args.release_url}")

    release_name, release_notes = gh_fetch_release(repo, release_tag)
    print(f"📋 Release Name: {release_name}")

    pr_numbers = extract_pr_numbers(release_notes)
    print(f"📋 PR Numbers parsed from release notes: {pr_numbers}\n")
    pr_issues_map = gh_fetch_linked_issues_batched(owner, repo_name, pr_numbers)
    issue_pr_map = map_issues_to_prs(pr_issues_map)
    comment_issues(repo, issue_pr_map, release_name, args.release_url, args.dry_run)
