# Contributing to implementing-android-code

## Development

This skill provides Bitwarden Android implementation patterns, gotchas, and code templates for Claude Code. It consists of two content files:

- **SKILL.md** - Quick reference for patterns, anti-patterns, and gotchas
- **templates.md** - Copy-pasteable code templates for all layer types

## Making Changes

This skill follows [Semantic Versioning](https://semver.org/):

- **Patch** (0.1.x): Typo fixes, minor clarifications, template corrections
- **Minor** (0.x.0): New patterns, new templates, expanded coverage areas
- **Major** (x.0.0): Structural changes, pattern overhauls, breaking reorganizations

When making changes:

1. Update the relevant content in `SKILL.md` and/or `templates.md`
2. Bump the `version` field in the SKILL.md YAML frontmatter
3. Add an entry to `CHANGELOG.md` under the appropriate version heading

## Testing Locally

To test the skill locally with Claude Code:

```bash
# From the repository root, invoke Claude Code and trigger the skill
claude "How do I implement a ViewModel?"
```

Verify that:
- The skill triggers on expected phrases
- Templates render correctly
- Pattern references are accurate against the current codebase

## Pull Requests

All pull requests that modify skill content must include:

1. A version bump in the SKILL.md frontmatter
2. A corresponding CHANGELOG.md entry
3. Verification that templates compile against the current codebase patterns