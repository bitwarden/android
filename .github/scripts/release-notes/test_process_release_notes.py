import unittest
import tempfile
import os
from process_release_notes import *

class TestProcessReleaseNotes(unittest.TestCase):
    def setUp(self):
        self.test_file = tempfile.NamedTemporaryFile(delete=False)

    def tearDown(self):
        os.unlink(self.test_file.name)

    def test_extract_jira_tickets(self):
        test_cases = [
            ("[ABC-123] Some text", ["ABC-123"]),
            ("DEF-456: Some text", ["DEF-456"]),
            ("GHI-789 - Some text", ["GHI-789"]),
            ("Multiple [ABC-123] and DEF-456: tickets", ["ABC-123", "DEF-456"]),
            ("No tickets here", []),
            ("Mixed formats ABC-123 [DEF-456] GHI-789:", ["ABC-123", "DEF-456", "GHI-789"])
        ]
        for input_text, expected in test_cases:
            with self.subTest(input_text=input_text):
                result = extract_jira_tickets(input_text)
                self.assertEqual(result, expected)

    def test_extract_pr_numbers(self):
        test_cases = [
            ("PR #123 text", ["123"]),
            ("Multiple PRs #456 and #789", ["456", "789"]),
            ("No PR numbers", [])
        ]
        for input_text, expected in test_cases:
            with self.subTest(input_text=input_text):
                result = extract_pr_numbers(input_text)
                self.assertEqual(result, expected)

    def test_process_line(self):
        test_cases = [
            ("* [ABC-123] BACKPORT Some text", "* Some text"),
            ("* DEF-456: feat(component): Some text", "* Some text"),
            ("* GHI-789 - bug(fix): Some text", "* Some text"),
            ("* ci: Some text", "* Some text"),
            ("* ci(workflow): Some text", "* Some text"),
            ("* feat: Direct feature", "* Direct feature"),
            ("* bug: Simple bugfix", "* Simple bugfix"),
            ("* Normal text", "* Normal text"),
            ("* 🍒 Cherry picked PR", "* Cherry picked PR"),
        ]
        for input_text, expected in test_cases:
            with self.subTest(input_text=input_text):
                result = process_line(input_text)
                self.assertEqual(result, expected)

    def test_extract_pr_url(self):
        test_cases = [
            ("* Update SDK to 1.0.0-3436-2a00b727 by @bw-ghapp[bot] in https://github.com/bitwarden/android/pull/6042", "https://github.com/bitwarden/android/pull/6042"),
            ("* Bump JUnit from 6.0.0 to 6.0.1 by SaintPatrick in https://github.com/bitwarden/android/pull/6149", "https://github.com/bitwarden/android/pull/6149"),
            ("* [PM-26986] Hide select other account button if user has no other account by @aj-rosado in https://github.com/bitwarden/android/pull/6041", "https://github.com/bitwarden/android/pull/6041"),
            ("* No PR URL here", ""),
        ]
        for input_text, expected in test_cases:
            with self.subTest(input_text=input_text):
                result = extract_pr_url(input_text)
                self.assertEqual(result, expected)

    def test_fetch_labels(self):
        test_cases = [
            ("https://github.com/bitwarden/android/pull/6042", ["automated-pr", "t:ci"]),
            ("https://github.com/bitwarden/android/pull/6149", ["app:authenticator","app:password-manager","t:deps"]),
            ("https://github.com/bitwarden/android/pull/6041", []),
        ]
        for pr_url, expected in test_cases:
            with self.subTest(pr_url=pr_url):
                result = fetch_labels(pr_url)
                self.assertEqual(sorted(result), sorted(expected))

    def test_should_skip_pr(self):
        test_cases = [
            # (release_app_label, pr_labels, expected_skip)
            # skip - there's at least one label that starts with "app:" but release_app_label isn't found.
            ("app:password-manager", ["app:authenticator", "bug"], True),
            ("app:authenticator", ["app:password-manager", "t:deps"], True),

            # don't skip - app label is found or there are no app labels.
            ("app:password-manager", ["app:password-manager", "app:authenticator", "t:bug"], False),
            ("app:password-manager", ["app:password-manager", "t:tech-debt"], False),
            ("app:password-manager", ["automated-pr", "t:ci"], False),
            ("app:password-manager", [], False),
        ]
        for release_app_label, pr_labels, expected_skip in test_cases:
            with self.subTest(app_label=release_app_label, pr_labels=pr_labels):
                result = should_skip_pr(release_app_label, pr_labels)
                self.assertEqual(result, expected_skip)

    def test_process_file(self):
        content = """
### Features:
[ABC-123] feat(comp): Feature 1 #123
DEF-456: bug(fix): Bug fix #456
GHI-789 - BACKPORT Some text #789

### Bug Fixes:
Another line without changes
"""
        with open(self.test_file.name, 'w') as f:
            f.write(content)

        jira_tickets, pr_numbers, processed_lines = process_file(self.test_file.name)

        self.assertEqual(jira_tickets, ["ABC-123", "DEF-456", "GHI-789"])
        self.assertEqual(pr_numbers, ["123", "456", "789"])
        self.assertEqual(processed_lines, [
            '',
            '### Features:',
            'Feature 1 #123',
            'Bug fix #456',
            'Some text #789',
            '',
            '### Bug Fixes:',
            'Another line without changes'
        ])

    # def test_get_linked_issues(self):
    #     test_cases = [
    #         ("bitwarden", "android", 4696, [4659]),
    #         ("bitwarden", "android", 4809, [])
    #     ]

    #     for owner, repo, pr_id, expected_linked_issues in test_cases:
    #         with self.subTest(msg=f"Testing PR #{pr_id} for {owner}/{repo}"):
    #             result = get_linked_issues(owner, repo, pr_id)
    #             self.assertEqual(sorted(result), sorted(expected_linked_issues))


if __name__ == '__main__':
    unittest.main()
