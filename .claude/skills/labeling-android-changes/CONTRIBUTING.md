# Contributing to labeling-android-changes

## Development

This skill provides the single source of truth for conventional commit type keywords in the
Bitwarden Android project. It consists of one content file:

- **SKILL.md** - Type keyword table, format reference, and selection guidance

## Making Changes

This skill follows [Semantic Versioning](https://semver.org/):

- **Patch** (0.1.x): Typo fixes, minor clarifications
- **Minor** (0.x.0): New type keywords, expanded guidance
- **Major** (x.0.0): Format changes, keyword renames, breaking reorganizations

When making changes:

1. Update the relevant content in `SKILL.md`
2. Bump the `version` field in the SKILL.md YAML frontmatter
3. Add an entry to `CHANGELOG.md` under the appropriate version heading

## Important

The type keywords in this skill must stay in sync with `.github/label-pr.json`. If CI labeling
patterns change, this skill must be updated to match.

## Testing Locally

To test the skill locally with Claude Code:

```bash
# From the repository root, invoke Claude Code and trigger the skill
claude "What type should this commit be?"
```

Verify that:
- The skill triggers on expected phrases
- Type keywords match `.github/label-pr.json` title_patterns
- Guidance is clear and actionable

## Pull Requests

All pull requests that modify skill content must include:

1. A version bump in the SKILL.md frontmatter
2. A corresponding CHANGELOG.md entry
3. Verification that type keywords align with `.github/label-pr.json`