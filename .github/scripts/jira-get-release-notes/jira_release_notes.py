#!/usr/bin/env python3
# Requires Python 3.9+
"""Fetch release notes from a Jira issue."""

import argparse
import base64
import json
import sys
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen

SCRIPT_NAME = Path(__file__).name

def extract_text_from_content(content):
    if isinstance(content, list):
        texts = [extract_text_from_content(item) for item in content]
        return '\n'.join(text for text in texts if text.strip())

    if isinstance(content, dict):
        if content.get('type') == 'text':
            return content.get('text', '')
        elif content.get('type') == 'paragraph':
            return extract_text_from_content(content.get('content', []))
        elif content.get('type') == 'bulletList':
            return extract_text_from_content(content.get('content', []))
        elif content.get('type') == 'listItem':
            item_text = extract_text_from_content(content.get('content', []))
            return f"* {item_text.strip()}"

    return ''

def log_customfields_with_content(fields):
    """Log all customfield_* fields that have a 'content' key to help troubleshoot structure changes."""
    print(f"[{SCRIPT_NAME}] Available customfield_* fields with 'content':", file=sys.stderr)
    found = False
    for key, value in fields.items():
        if key.startswith('customfield_') and isinstance(value, dict) and 'content' in value:
            found = True
            print(f"[{SCRIPT_NAME}]   {key}: {json.dumps(value, indent=2)}", file=sys.stderr)
    if not found:
        print(f"[{SCRIPT_NAME}]   None found", file=sys.stderr)

def parse_release_notes(response_json):
    release_notes_field_name = 'customfield_10309'
    try:
        fields = response_json.get('fields')
        if not fields:
            print(f"[{SCRIPT_NAME}] 'fields' is empty or missing in response", file=sys.stderr)
            return ''

        release_notes_field = fields.get(release_notes_field_name)
        if not release_notes_field:
            print(f"[{SCRIPT_NAME}] Release notes field is empty or missing. Field name: {release_notes_field_name}", file=sys.stderr)
            log_customfields_with_content(fields)
            return ''

        content = release_notes_field.get('content', [])
        if not content:
            print(f"[{SCRIPT_NAME}] Release notes field was found but 'content' is empty or missing in {release_notes_field_name}", file=sys.stderr)
            log_customfields_with_content(fields)
            return ''

        release_notes = extract_text_from_content(content)
        return release_notes

    except Exception as e:
        print(f"[{SCRIPT_NAME}] Error parsing release notes: {str(e)}", file=sys.stderr)
        return ''

def parse_args():
    parser = argparse.ArgumentParser(
        description=__doc__,
    )
    parser.add_argument("issue_id", help="RELEASE issue ID to fetch release notes from")
    parser.add_argument("jira_cloud_id", help="Atlassian Cloud ID - Can be retrieved from the `tenant_info` endpoint, e.g.: `https://<my-site-name>.atlassian.net/_edge/tenant_info`")
    parser.add_argument("jira_email", help="Email used to create the API token")
    parser.add_argument("jira_api_token", help="Jira API token - Generate one at: https://id.atlassian.com/manage-profile/security/api-tokens")
    return parser.parse_args()

def main():
    args = parse_args()

    jira_issue_id = args.issue_id
    jira_cloud_id = args.jira_cloud_id
    jira_email = args.jira_email
    jira_api_token = args.jira_api_token
    jira_base_url = "https://api.atlassian.com/ex/jira"

    auth = base64.b64encode(f"{jira_email}:{jira_api_token}".encode()).decode()
    request = Request(
        f"{jira_base_url}/{jira_cloud_id}/rest/api/3/issue/{jira_issue_id}",
        headers={
            "Authorization": f"Basic {auth}",
            "Content-Type": "application/json"
        }
    )

    try:
        with urlopen(request) as response:
            response_json = json.loads(response.read().decode())
    except HTTPError as error:
        error_text = error.read().decode().replace(jira_cloud_id, "[REDACTED]")
        print(f"[{SCRIPT_NAME}] Error fetching Jira issue ({jira_issue_id}). Status code: {error.code}. Msg: {error_text}", file=sys.stderr)
        sys.exit(1)

    release_notes = parse_release_notes(response_json)
    print(release_notes)

if __name__ == "__main__":
    main()
