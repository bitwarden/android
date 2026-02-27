# perform-android-preflight-checklist

Quality gate checklist to run before committing or creating a PR. Covers tests, code quality,
security validation, Bitwarden-specific patterns, and file hygiene checks.

## Features

- **Test Verification** - Correct flavor test commands, coverage for new code
- **Code Quality** - Lint/detekt, unintended changes, KDoc, TODO hygiene
- **Security Checks** - No plaintext secrets, input validation, encrypted storage, no sensitive logging
- **Bitwarden Patterns** - String resources, navigation, Hilt DI, ViewModel, async patterns
- **File Hygiene** - No IDE files, build outputs, or credentials staged

## Skill Structure

```
perform-android-preflight-checklist/
├── SKILL.md          # Quality gate checklist with all review categories
├── README.md         # This file
├── CHANGELOG.md      # Version history
└── CONTRIBUTING.md   # Contribution guidelines
```

## Usage

Claude triggers this skill automatically when conversations involve reviewing work quality,
preparing to commit, or running through a pre-merge checklist.

**Example trigger phrases:**

- "Self review"
- "Check my work"
- "Ready to commit"
- "Done implementing"
- "Review checklist"
- "Quality check"

## Content Summary

| Section            | Description                                              |
|--------------------|----------------------------------------------------------|
| Tests              | Flavor-correct test runs, coverage for new code          |
| Code Quality       | Lint, detekt, unintended changes, KDoc, TODOs            |
| Security           | Secrets, input validation, encrypted storage, logging    |
| Bitwarden Patterns | String resources, navigation, Hilt, ViewModel, async     |
| Files              | No IDE files, build outputs, or credential files staged  |

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