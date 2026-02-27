# labeling-android-changes

Conventional commit type keywords for PR titles and commit messages in the Bitwarden Android project.
Provides the single source of truth for type keywords that drive automatic `t:` label assignment via CI.

## Features

- **Type Keyword Table** - Maps conventional commit prefixes to CI labels
- **Format Reference** - Shows where the type keyword appears in titles/messages
- **Selection Guidance** - How to infer the correct type from changes

## Skill Structure

```
labeling-android-changes/
├── SKILL.md          # Type keywords, format, and selection guidance
├── README.md         # This file
├── CHANGELOG.md      # Version history
└── CONTRIBUTING.md   # Contribution guidelines
```

## Usage

Users can invoke this skill directly with `/labeling-android-changes`, or Claude triggers it automatically
when determining the change type for commits or PRs. It is also referenced by the `committing-android-changes` and
`creating-android-pull-request` skills.

**Example trigger phrases:**

- `/labeling-android-changes`
- "What type should this be?"
- "Label"
- "Change type"
- "Conventional commit"

## Content Summary

| Section           | Description                                              |
|-------------------|----------------------------------------------------------|
| Format            | Where the type keyword appears in the title/message      |
| Type Keywords     | Full mapping of types to CI labels                       |
| Selecting a Type  | Guidance on inferring the correct type                   |

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
