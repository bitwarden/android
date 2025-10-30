---
name: reviewing-changes
description: Comprehensive code reviews for Bitwarden Android. Detects change type (dependency update, bug fix, feature, UI, refactoring, infrastructure) and applies appropriate review depth. Validates MVVM patterns, Hilt DI, security requirements, and test coverage per project standards. Use when reviewing pull requests, checking commits, analyzing code changes, or evaluating architectural compliance.
---

# Reviewing Changes

## Instructions

### Step 1: Detect Change Type

Analyze the changeset to determine the primary change type:

**Detection Rules:**
- **Dependency Update**: Only gradle files changed (`libs.versions.toml`, `build.gradle.kts`) with version number modifications
- **Bug Fix**: PR/commit title contains "fix", "bug", or issue ID; addresses existing broken behavior
- **Feature Addition**: New files, new ViewModels, significant new functionality
- **UI Refinement**: Only UI/Compose files changed, layout/styling focus
- **Refactoring**: Code restructuring without behavior change, pattern improvements
- **Infrastructure**: CI/CD files, Gradle config, build scripts, tooling changes

If changeset spans multiple types, use the most complex type's checklist.

### Step 2: Load Appropriate Checklist

Based on detected type, read the relevant checklist file:

- **Dependency Update** → `checklists/dependency-update.md` (15-20 min expedited review)
- **Bug Fix** → `checklists/bug-fix.md` (20-30 min focused review)
- **Feature Addition** → `checklists/feature-addition.md` (45-60 min comprehensive review)
- **UI Refinement** → `checklists/ui-refinement.md` (25-35 min design-focused review)
- **Refactoring** → `checklists/refactoring.md` (30-40 min pattern-focused review)
- **Infrastructure** → `checklists/infrastructure.md` (30-40 min tooling-focused review)

The checklist provides:
- Multi-pass review strategy
- Type-specific focus areas
- What to check and what to skip
- Time estimates and risk assessment

### Step 3: Consult Reference Materials As Needed

Load reference files only when needed for specific questions:

- **Issue prioritization** → `reference/priority-framework.md` (Critical vs Suggested vs Optional)
- **Phrasing feedback** → `reference/review-psychology.md` (questions vs commands, I-statements)
- **Architecture questions** → `reference/android-patterns.md` (MVVM, Hilt DI, module org)
- **Security questions** → `docs/ARCHITECTURE.md#security`
- **Style questions** → `docs/STYLE_AND_BEST_PRACTICES.md`

### Step 4: Document Findings

**CRITICAL**: Use inline comments on specific lines, NOT a single large review comment.

**Inline Comment Rules**:
- Create separate comment for EACH specific issue on the exact line
- Do NOT create one large summary comment with all issues
- Do NOT update existing comments - always create new comments
- This ensures history is retained for other reviewers

**Comment Format**:
```
**[Severity]**: [Issue description]

[Code example or specific fix if applicable]

[Rationale explaining why]

Reference: [docs link if applicable]
```

**Example inline comment**:
```
**CRITICAL**: Exposes mutable state

Change `MutableStateFlow<State>` to `StateFlow<State>`:

\```kotlin
private val _state = MutableStateFlow<State>()
val state: StateFlow<State> = _state.asStateFlow()
\```

Exposing MutableStateFlow allows external mutation, violating MVVM unidirectional data flow.

Reference: docs/ARCHITECTURE.md#mvvm-pattern
```

**When to use inline vs summary**:
- **Inline comment**: Specific code issue, recommendation, or question (use `file:line_number` format)
- **Summary comment**: Overall assessment, high-level observations, approval/request changes decision

See `examples/review-outputs.md` for examples.

## Core Principles

- **Appropriate depth**: Match review rigor to change complexity and risk
- **Specific references**: Always use `file:line_number` format for precise location
- **Actionable feedback**: Say what to do and why, not just what's wrong
- **Constructive tone**: Ask questions for design decisions, explain rationale, focus on code not people
- **Efficient reviews**: Use multi-pass strategy, time-box reviews, skip what's not relevant
