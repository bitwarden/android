# refining-android-requirements

Requirements gap analysis and structured specification for Bitwarden Android. Takes raw
requirements from any source (Jira tickets, Confluence pages, free-text descriptions) and
produces implementation-ready specifications through systematic gap analysis.

## Features

- **Source Consolidation** - Combines multiple input sources with provenance tracking
- **5-Category Gap Analysis** - Functional, Technical, Security, UX, and Cross-cutting evaluation
- **Blocking vs Non-Blocking Classification** - Prioritizes questions by implementation impact
- **Structured Specification Output** - Numbered requirements (FR, TR, SR, UX) with source tracing

## Skill Structure

```
refining-android-requirements/
├── SKILL.md          # Gap analysis rubric, question templates, spec output format
├── README.md         # This file
├── CHANGELOG.md      # Version history
└── CONTRIBUTING.md   # Contribution guidelines
```

## Usage

Claude triggers this skill automatically when conversations involve refining requirements,
analyzing specifications, or identifying gaps in feature descriptions.

**Example trigger phrases:**

- "Refine requirements"
- "Gap analysis"
- "Spec review"
- "Requirements analysis"
- "What's missing from this spec"
- "Analyze this ticket"

## Content Summary

| Section              | Description                                              |
|----------------------|----------------------------------------------------------|
| Source Consolidation | Combine inputs with provenance tracking                  |
| Gap Analysis         | 5-category rubric with specific question templates       |
| Present Gaps         | Blocking vs non-blocking classification with defaults    |
| Produce Specification| Numbered IDs, source tracing, structured tables          |

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