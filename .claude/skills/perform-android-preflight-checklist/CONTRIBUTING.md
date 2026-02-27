# Contributing to perform-android-preflight-checklist

## Development

This skill provides a quality gate checklist for the Bitwarden Android project. It consists of one content file:

- **SKILL.md** - Pre-commit/pre-PR checklist covering tests, quality, security, patterns, and files

## Making Changes

This skill follows [Semantic Versioning](https://semver.org/):

- **Patch** (0.1.x): Typo fixes, minor clarifications, checklist item corrections
- **Minor** (0.x.0): New checklist categories, expanded validation criteria, new check items
- **Major** (x.0.0): Structural changes, checklist overhauls, breaking reorganizations

When making changes:

1. Update the relevant content in `SKILL.md`
2. Bump the `version` field in the SKILL.md YAML frontmatter
3. Add an entry to `CHANGELOG.md` under the appropriate version heading

## Testing Locally

To test the skill locally with Claude Code:

```bash
# From the repository root, invoke Claude Code and trigger the skill
claude "Check my work before I commit"
```

Verify that:
- The skill triggers on expected phrases
- Checklist items are accurate for current project patterns
- Security checks align with current Bitwarden security requirements

## Pull Requests

All pull requests that modify skill content must include:

1. A version bump in the SKILL.md frontmatter
2. A corresponding CHANGELOG.md entry
3. Verification that checklist items reflect current project standards