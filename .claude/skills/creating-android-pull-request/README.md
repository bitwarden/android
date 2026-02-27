# creating-android-pull-request

Pull request creation workflow for Bitwarden Android. Provides PR title formatting, body template
guidance, pre-PR checklists, and branch management for creating clean pull requests.

## Features

- **PR Title Format** - Jira ticket prefix, length limits, imperative mood
- **PR Body Template** - References to repo's PR template with guidance
- **Pre-PR Checklist** - Tests, lint, self-review, diff verification, branch freshness
- **Branch Management** - Push and target branch guidance

## Skill Structure

```
creating-android-pull-request/
├── SKILL.md          # PR creation workflow, title format, checklists
├── README.md         # This file
├── CHANGELOG.md      # Version history
└── CONTRIBUTING.md   # Contribution guidelines
```

## Usage

Claude triggers this skill automatically when conversations involve creating pull requests, writing
PR descriptions, or preparing branches for review.

**Example trigger phrases:**

- "Create PR"
- "Pull request"
- "Open PR"
- "gh pr create"
- "PR description"

## Content Summary

| Section           | Description                                              |
|-------------------|----------------------------------------------------------|
| PR Title Format   | Jira prefix, length limits, imperative mood guidelines   |
| PR Body Template  | Reference to `.github/PULL_REQUEST_TEMPLATE.md`          |
| Pre-PR Checklist  | Tests, lint, self-review, diff check, branch freshness   |
| Creating the PR   | Git push and `gh pr create` commands                     |
| Base Branch       | Default target branch and feature branch guidance        |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development guidelines, versioning, and pull request
requirements.

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for version history.

## License

This skill is part of the [Bitwarden Android](https://github.com/bitwarden/android) project and
follows its licensing terms.

## Maintainers

- Bitwarden Android team

## Support

For issues or questions, open an issue in
the [bitwarden/android](https://github.com/bitwarden/android) repository.