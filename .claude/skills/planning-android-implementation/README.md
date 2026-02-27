# planning-android-implementation

Architecture design and phased implementation planning for Bitwarden Android. Takes refined
specifications and produces actionable implementation plans with architecture diagrams, file
inventories, and risk assessments.

## Features

- **Change Classification** - Categorizes work type to guide planning depth
- **Codebase Exploration** - Finds pattern anchors and integration points in existing code
- **Architecture Design** - ASCII component diagrams and design decision tables
- **File Inventory** - Files to create (with pattern references) and modify (with risk levels)
- **Phased Implementation** - Sequential, independently testable phases with verification criteria
- **Risk Assessment** - Risk table with mitigations and automated/manual test plans

## Skill Structure

```
planning-android-implementation/
├── SKILL.md          # Planning steps, architecture templates, phase structure
├── README.md         # This file
├── CHANGELOG.md      # Version history
└── CONTRIBUTING.md   # Contribution guidelines
```

## Usage

Claude triggers this skill automatically when conversations involve planning implementations,
designing architecture, or breaking features into phases.

**Example trigger phrases:**

- "Plan implementation"
- "Architecture design"
- "Implementation plan"
- "Break this into phases"
- "What files do I need"
- "Design the architecture"

## Content Summary

| Section                | Description                                              |
|------------------------|----------------------------------------------------------|
| Classify Change        | Determine work type (feature, enhancement, fix, etc.)    |
| Codebase Exploration   | Find pattern anchors and integration points              |
| Architecture Design    | ASCII diagrams and design decision tables                |
| File Inventory         | Create/modify lists with pattern refs and risk levels    |
| Implementation Phases  | Sequential, testable phases with verification criteria   |
| Risk & Verification    | Risk table, automated tests, manual test scenarios       |

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