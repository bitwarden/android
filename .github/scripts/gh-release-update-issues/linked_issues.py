import sys
import re
import subprocess
from typing import List, Dict, DefaultDict
from collections import defaultdict

def extract_pr_numbers(line: str) -> List[str]:
    """Match PR numbers from GitHub format (#123)"""
    return re.findall(r'#(\d+)', line)

def extract_pr_url(release_notes: str) -> List[str]:
    """Match PR URLs from GitHub format https://github.com/foo/bar/pull/123

    Returns:
        A list of PR URLs found in the release notes, or empty list if no URLs are found
    """
    matches = re.findall(r'https://github\.com/[\w-]+/[\w.-]+/pull/\d+', release_notes)
    return matches if matches else []

def create_linked_issue_comment(repo_owner: str, repo_name: str, release_name: str, release_link: str, pr_numbers: List[int]) -> str:
    if len(pr_numbers) == 0:
        return ""

    pr_links = [f"* https://github.com/{repo_owner}/{repo_name}/pull/{pr_number}" for pr_number in pr_numbers]

    return f":shipit: Pull Request(s) linked to this issue released in [{release_name}]({release_link}):\n\n"+ "\n".join(pr_links)

def comment_linked_issues_in_prs(owner: str, repo: str, pr_numbers: List[int], release_name: str, release_link: str, dry_run: bool = False) -> None:
    issue_pr_map: DefaultDict[int, List[int]] = defaultdict(list)

    for pr_number in pr_numbers:
        linked_issues = get_linked_issues(owner, repo, pr_number)
        for issue_number in linked_issues:
            issue_pr_map[issue_number].append(pr_number)

    for issue_number, linked_prs in issue_pr_map.items():
        comment = create_linked_issue_comment(owner, repo, release_name, release_link, linked_prs)
        print(f"Commenting on issue {issue_number}:\n{comment}\n")
        if not dry_run and comment:
            comment_github_issue(owner, repo, issue_number, comment)

def comment_github_issue(owner: str, repo: str, issue_number: int, comment: str) -> None:
    """Use GitHub CLI to comment on an issue.
    """
    subprocess.run([
        'gh', 'issue', 'comment', str(issue_number), '--body', comment, '--repo', f'{owner}/{repo}'
    ], check=True)

def get_linked_issues(owner: str, repo: str, pr_number: int) -> List[int]:
    """Use GitHub CLI to retrieve linked issue numbers for a PR.
    """

    query = """
    query ($owner: String!, $repo: String!, $pr: Int!) {
        repository(owner: $owner, name: $repo) {
            pullRequest(number: $pr) {
                closingIssuesReferences(first: 100) {
                    nodes {
                        number
                    }
                }
            }
        }
    }
    """

    try:
        result = subprocess.run([
            'gh', 'api', 'graphql',
            '-F', f'owner={owner}',
            '-F', f'repo={repo}',
            '-F', f'pr={pr_number}',
            '-f', f'query={query}',
            '--jq', '.data.repository.pullRequest.closingIssuesReferences.nodes[].number'
        ], capture_output=True, text=True, check=True)

        # Split output into lines and convert to integers
        if result.stdout.strip():
            return [int(num) for num in result.stdout.strip().split('\n')]
        return []

    except subprocess.CalledProcessError:
        print(f"Error fetching linked issues for PR #{pr_number}")
        return []
