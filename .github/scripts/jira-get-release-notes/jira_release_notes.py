#!/usr/bin/env python3

import sys
import base64
import json
import requests

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

def parse_release_notes(response_json):
    try:
        fields = response_json.get('fields', {})
        release_notes_field = fields.get('customfield_10309', {})

        if not release_notes_field or not release_notes_field.get('content'):
            return ''

        release_notes = extract_text_from_content(release_notes_field.get('content', []))
        return release_notes

    except Exception as e:
        print(f"Error parsing release notes: {str(e)}", file=sys.stderr)
        return ''

def main():
    if len(sys.argv) != 4:
        print(f"Usage: {sys.argv[0]} <issue_id> <jira_email> <jira_api_token>")
        sys.exit(1)

    jira_issue_id = sys.argv[1]
    jira_email = sys.argv[2]
    jira_api_token = sys.argv[3]
    jira_base_url = "https://bitwarden.atlassian.net"

    auth = base64.b64encode(f"{jira_email}:{jira_api_token}".encode()).decode()
    headers = {
        "Authorization": f"Basic {auth}",
        "Content-Type": "application/json"
    }

    response = requests.get(
        f"{jira_base_url}/rest/api/3/issue/{jira_issue_id}",
        headers=headers
    )

    if response.status_code != 200:
        print(f"Error fetching Jira issue: {response.status_code}", file=sys.stderr)
        sys.exit(1)

    release_notes = parse_release_notes(response.json())
    print(release_notes)

if __name__ == "__main__":
    main()
