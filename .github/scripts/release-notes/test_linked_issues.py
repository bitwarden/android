import unittest
from linked_issues import get_linked_issues, create_linked_issue_comment

class TestLinkedIssues(unittest.TestCase):
    def test_create_linked_issue_comment(self):
        test_cases = [
            ("bitwarden", "android", "v2025.1.0", "https://github.com/bitwarden/android/releases/tag/v2025.1.0", [4696]),
            ("bitwarden", "android", "v2025.2.0", "https://github.com/bitwarden/android/releases/tag/v2025.2.0", [4809, 1, 2, 3]),
            ("bitwarden", "android", "v2025.3.0", "https://github.com/bitwarden/android/releases/tag/v2025.3.0", []),
        ]

        for owner, repo, release_name, release_link, pr_numbers in test_cases:
            with self.subTest(msg=f"Creating comment for issue in release {release_name}"):
                comment = create_linked_issue_comment(owner, repo, release_name, release_link, pr_numbers)
                print(comment + "\n")

    def test_get_linked_issues(self):
        test_cases = [
            ("bitwarden", "android", 4696, [4659]),
            ("bitwarden", "android", 4809, [])
        ]

        for owner, repo, pr_id, expected_linked_issues in test_cases:
            with self.subTest(msg=f"Testing PR #{pr_id} for {owner}/{repo}"):
                result = get_linked_issues(owner, repo, pr_id)
                self.assertEqual(sorted(result), sorted(expected_linked_issues))


if __name__ == '__main__':
    unittest.main()
