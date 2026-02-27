# Contributing to committing-android-changes

## Development

This skill provides git commit conventions and workflow for the Bitwarden Android project. It consists of one content file:

- **SKILL.md** - Commit message format, pre-commit checklists, staging best practices

## Making Changes

This skill follows [Semantic Versioning](https://semver.org/):

- **Patch** (0.1.x): Typo fixes, minor clarifications, checklist corrections
- **Minor** (0.x.0): New checklist sections, expanded conventions, new workflow guidance
- **Major** (x.0.0): Structural changes, convention overhauls, breaking reorganizations

When making changes:

1. Update the relevant content in `SKILL.md`
2. Bump the `version` field in the SKILL.md YAML frontmatter
3. Add an entry to `CHANGELOG.md` under the appropriate version heading

## Testing Locally

To test the skill locally with Claude Code:

```bash
# From the repository root, invoke Claude Code and trigger the skill
claude "How do I write a commit message?"
```

Verify that:
- The skill triggers on expected phrases
- Conventions match the team's current commit standards
- Checklist items are accurate and actionable

## Pull Requests

All pull requests that modify skill content must include:

1. A version bump in the SKILL.md frontmatter
2. A corresponding CHANGELOG.md entry
3. Verification that conventions align with current team practices