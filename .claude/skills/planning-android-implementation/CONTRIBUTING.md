# Contributing to planning-android-implementation

## Development

This skill provides architecture design and phased implementation planning for the Bitwarden Android project. It consists of one content file:

- **SKILL.md** - Planning steps, architecture templates, file inventory format, phase structure

## Making Changes

This skill follows [Semantic Versioning](https://semver.org/):

- **Patch** (0.1.x): Typo fixes, minor clarifications, template wording improvements
- **Minor** (0.x.0): New planning steps, expanded architecture templates, new risk categories
- **Major** (x.0.0): Structural changes, planning process overhauls, breaking reorganizations

When making changes:

1. Update the relevant content in `SKILL.md`
2. Bump the `version` field in the SKILL.md YAML frontmatter
3. Add an entry to `CHANGELOG.md` under the appropriate version heading

## Testing Locally

To test the skill locally with Claude Code:

```bash
# From the repository root, invoke Claude Code and trigger the skill
claude "Plan implementation for adding biometric timeout configuration"
```

Verify that:
- The skill triggers on expected phrases
- Change classification is accurate for the described work
- Pattern anchors reference real, existing files in the codebase
- Architecture diagrams use consistent ASCII box-drawing characters
- Implementation phases are independently testable and reasonably scoped

## Pull Requests

All pull requests that modify skill content must include:

1. A version bump in the SKILL.md frontmatter
2. A corresponding CHANGELOG.md entry
3. Verification that templates and examples align with current project architecture