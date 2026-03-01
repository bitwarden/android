# Contributing to build-test-verify

## Development

This skill provides build, test, lint, and deploy commands for the Bitwarden Android project. It consists of one content file:

- **SKILL.md** - All build, test, lint, deploy commands and codebase structure reference

## Making Changes

This skill follows [Semantic Versioning](https://semver.org/):

- **Patch** (0.1.x): Typo fixes, minor clarifications, command corrections
- **Minor** (0.x.0): New command sections, expanded coverage areas, new tool integrations
- **Major** (x.0.0): Structural changes, command overhauls, breaking reorganizations

When making changes:

1. Update the relevant content in `SKILL.md`
2. Bump the `version` field in the SKILL.md YAML frontmatter
3. Add an entry to `CHANGELOG.md` under the appropriate version heading

## Testing Locally

To test the skill locally with Claude Code:

```bash
# From the repository root, invoke Claude Code and trigger the skill
claude "How do I run tests?"
```

Verify that:
- The skill triggers on expected phrases
- Commands are accurate against the current build system
- Gradle task names match the current project configuration

## Pull Requests

All pull requests that modify skill content must include:

1. A version bump in the SKILL.md frontmatter
2. A corresponding CHANGELOG.md entry
3. Verification that commands work against the current project configuration
