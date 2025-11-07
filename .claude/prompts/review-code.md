Use the `reviewing-changes` skill to review this pull request.

The PR branch is already checked out in the current working directory.

## CRITICAL OUTPUT REQUIREMENTS

**Summary Format (REQUIRED):**
- **Clean PRs (no issues)**: 2-3 lines MAXIMUM
  - Format: `**Overall Assessment:** APPROVE\n[One sentence]`
  - Example: "Clean refactoring following established patterns"

- **PRs with issues**: Verdict + critical issues list (5-10 lines MAX)
  - Format: `**Overall Assessment:** APPROVE/REQUEST CHANGES\n**Critical Issues:**\n- issue 1\nSee inline comments`
  - All details go in inline comments with `<details>` tags, NOT in summary

**NEVER create:**
- ❌ Praise sections ("Strengths", "Good Practices", "Excellent X")
- ❌ Verbose analysis sections (Architecture Assessment, Technical Review, Code Quality, etc.)
- ❌ Tables, statistics, or detailed breakdowns in summary
- ❌ Multiple summary sections
- ❌ Checkmarks listing everything done correctly

**Inline Comments:**
- Create separate inline comment for each specific issue/recommendation
- Use collapsible `<details>` sections for code examples and explanations
- Only severity + one-line description visible; all other content collapsed
- Track status of previously identified issues if this is a subsequent review
