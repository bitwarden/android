# build-test-verify

Build, test, lint, and deploy commands for the Bitwarden Android project. Provides all commands
needed to build APKs/AABs, run unit tests, execute lint/detekt checks, deploy via fastlane, and
discover codebase structure.

## Features

- **Build Commands** - Gradle tasks for assembling debug/release APKs and AABs across app flavors
- **Test Commands** - Unit test execution for all modules with correct flavor variants
- **Lint/Detekt** - Static analysis and code quality checks
- **Deploy/Fastlane** - Release deployment workflows and fastlane lane references
- **Codebase Structure** - Module layout, key directories, and project organization

## Skill Structure

```
build-test-verify/
├── SKILL.md          # Build, test, lint, deploy commands and codebase structure
├── README.md         # This file
├── CHANGELOG.md      # Version history
└── CONTRIBUTING.md   # Contribution guidelines
```

## Usage

Claude triggers this skill automatically when conversations involve building, testing, linting,
deploying, or exploring the structure of the Bitwarden Android project.

**Example trigger phrases:**

- "Run tests"
- "Build the app"
- "Gradle command"
- "Lint check"
- "Detekt"
- "Deploy"
- "Fastlane"
- "Assemble"
- "Verify"
- "Coverage"

## Content Summary

| Section            | Description                                                  |
|--------------------|--------------------------------------------------------------|
| Environment Setup  | Required environment variables and tool versions             |
| Build Commands     | Gradle assemble/bundle tasks for all flavors and build types |
| Test Commands      | Unit test execution per module with correct flavor variants  |
| Lint/Detekt        | Static analysis commands and configuration                   |
| Deploy/Fastlane    | Release deployment workflows and fastlane lanes              |
| Codebase Structure | Module layout, key directories, package organization         |

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
