# Contributing to refining-android-requirements

## Development

This skill provides requirements gap analysis and structured specification for the Bitwarden Android project. It consists of one content file:

- **SKILL.md** - Gap analysis rubric, question templates, specification output format

## Making Changes

This skill follows [Semantic Versioning](https://semver.org/):

- **Patch** (0.1.x): Typo fixes, minor clarifications, rubric wording improvements
- **Minor** (0.x.0): New rubric categories, expanded question templates, new output sections
- **Major** (x.0.0): Structural changes, rubric overhauls, breaking reorganizations

When making changes:

1. Update the relevant content in `SKILL.md`
2. Bump the `version` field in the SKILL.md YAML frontmatter
3. Add an entry to `CHANGELOG.md` under the appropriate version heading

## Testing Locally

To test the skill locally with Claude Code:

```bash
# From the repository root, invoke Claude Code and trigger the skill
claude "Refine requirements for PM-12345"
```

Verify that:
- The skill triggers on expected phrases
- Gap analysis rubric covers all 5 categories
- Questions are specific and actionable (not generic)
- Output specification uses correct numbered ID format (FR, TR, SR, UX)

## Pull Requests

All pull requests that modify skill content must include:

1. A version bump in the SKILL.md frontmatter
2. A corresponding CHANGELOG.md entry
3. Verification that rubric items align with current project architecture