# Contributing to creating-android-pull-request

## Development

This skill provides the pull request creation workflow for the Bitwarden Android project. It consists of one content file:

- **SKILL.md** - PR title format, body template guidance, pre-PR checklist, branch management

## Making Changes

This skill follows [Semantic Versioning](https://semver.org/):

- **Patch** (0.1.x): Typo fixes, minor clarifications, checklist corrections
- **Minor** (0.x.0): New checklist sections, expanded PR guidance, new workflow steps
- **Major** (x.0.0): Structural changes, workflow overhauls, breaking reorganizations

When making changes:

1. Update the relevant content in `SKILL.md`
2. Bump the `version` field in the SKILL.md YAML frontmatter
3. Add an entry to `CHANGELOG.md` under the appropriate version heading

## Testing Locally

To test the skill locally with Claude Code:

```bash
# From the repository root, invoke Claude Code and trigger the skill
claude "Create a PR for my changes"
```

Verify that:
- The skill triggers on expected phrases
- PR template references match the current `.github/PULL_REQUEST_TEMPLATE.md`
- Checklist items are accurate and actionable

## Pull Requests

All pull requests that modify skill content must include:

1. A version bump in the SKILL.md frontmatter
2. A corresponding CHANGELOG.md entry
3. Verification that the workflow aligns with current team PR practices