# committing-android-changes

Git commit conventions and workflow for Bitwarden Android. Provides commit message formatting,
pre-commit checklists, staging best practices, and rules for what should never be committed.

## Features

- **Commit Message Format** - Jira ticket prefix, imperative mood, body conventions
- **Pre-Commit Checklist** - Tests, lint, staged diff review, secrets check
- **Staging Best Practices** - File-specific staging over bulk adds
- **Exclusion Rules** - Files and patterns that must never be committed

## Skill Structure

```
committing-android-changes/
├── SKILL.md          # Commit conventions, checklists, and staging workflow
├── README.md         # This file
├── CHANGELOG.md      # Version history
└── CONTRIBUTING.md   # Contribution guidelines
```

## Usage

Claude triggers this skill automatically when conversations involve committing code, writing commit
messages, or preparing changes for commit.

**Example trigger phrases:**

- "Commit"
- "Git commit"
- "Commit message"
- "Prepare commit"
- "Stage changes"

## Content Summary

| Section              | Description                                            |
|----------------------|--------------------------------------------------------|
| Commit Message Format | Ticket prefix, imperative mood, body guidelines       |
| Pre-Commit Checklist | Tests, lint, diff review, secrets verification         |
| What NOT to Commit   | Credentials, build outputs, IDE files, binaries        |
| Staging Best Practices | File-specific staging and review guidance             |

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