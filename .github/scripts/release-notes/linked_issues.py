import sys
import subprocess
from typing import List

def create_linked_issue_comment(repo_owner: str, repo_name: str, release_name: str, release_link: str, pr_numbers: List[int]) -> str:
    if len(pr_numbers) == 0:
        return ""

    pr_links = [f"* https://github.com/{repo_owner}/{repo_name}/pull/{pr_number}" for pr_number in pr_numbers]

    return f":shipit: Pull Request(s) linked to this issue released in [{release_name}]({release_link}):\n\n"+ "\n".join(pr_links)

def comment_linked_issues_in_pr(owner: str, repo: str, pr_number: int) -> None:
    """Use GitHub CLI to comment all issues linked to a PR.
    """


    linked_issues = get_linked_issues(owner, repo, pr_number)
    for issue_number in linked_issues:
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
