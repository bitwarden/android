# Get Release Notes from Jira script

Fetches release notes from Jira issues.

## Prerequisites

- Jira API token. Generate one at: https://id.atlassian.com/manage-profile/security/api-tokens
- Jira cloud ID. Can be retrieved from the `tenant_info` endpoint, e.g.: `https://<my-site-name>.atlassian.net/_edge/tenant_info`

## Usage

```bash
./jira_release_notes.py RELEASE-1762 jira-cloud-id example@example.com T0k3n123
```

# Output Format

The script retrieves the content from a custom field and handles two types of Jira release notes formats:

1. Bullet Points:
```
• Point 1
• Point 2
• Point 3
```

2. Single Line:
```
Single line of release notes text
```

## Jira JSON format example

### Single line

```json
...
"customfield_9999": {
    "type": "doc",
    "version": 1,
    "content": [
        {
            "type": "paragraph",
            "content": [
                {
                    "type": "text",
                    "text": "Single line release notes"
                }
            ]
        }
    ]
},
...
```

### Bullet points

```json
...
"customfield_9999": {
    "type": "doc",
    "version": 1,
    "content": [
        {
            "type": "bulletList",
            "content": [
                {
                    "type": "listItem",
                    "content": [
                        {
                            "type": "paragraph",
                            "content": [
                                {
                                    "type": "text",
                                    "text": "Release notes list item 1"
                                }
                            ]
                        }
                    ]
                },
                {
                    "type": "listItem",
                    "content": [
                        {
                            "type": "paragraph",
                            "content": [
                                {
                                    "type": "text",
                                    "text": "Release notes list item 2"
                                }
                            ]
                        }
                    ]
                },
                {
                    "type": "listItem",
                    "content": [
                        {
                            "type": "paragraph",
                            "content": [
                                {
                                    "type": "text",
                                    "text": "Release notes list item 3"
                                }
                            ]
                        }
                    ]
                },
                {
                    "type": "listItem",
                    "content": [
                        {
                            "type": "paragraph",
                            "content": [
                                {
                                    "type": "text",
                                    "text": "Release notes list item 4"
                                }
                            ]
                        }
                    ]
                }
            ]
        }
    ]
},
...
```
