---
name: reviewing-changes
version: 3.0.0
description: Guides Android code reviews with type-specific checklists and MVVM/Compose pattern validation. Use when reviewing Android PRs, pull requests, diffs, or local changes involving Kotlin, ViewModel, Composable, Repository, or Gradle files. Triggered by "review PR", "review changes", "check this code", "Android review", or code review requests mentioning bitwarden/android. Loads specialized checklists for feature additions, bug fixes, UI refinements, refactoring, dependency updates, and infrastructure changes.
---

# Reviewing Changes - Android Additions

This skill provides Android-specific workflow additions that complement the base `bitwarden-code-reviewer` agent standards.

## Instructions

**IMPORTANT**: Use structured thinking throughout your review process. Plan your analysis in `<thinking>` tags before providing final feedback.

### Step 1: Retrieve Additional Details

<thinking>
Determine if more context is available for the changes:
1. Are there JIRA tickets or GitHub Issues mentioned in the PR title or body?
2. Are there other GitHub pull requests mentioned in the PR title or body?
</thinking>

Retrieve any additional information linked to the pull request using available tools (JIRA MCP, GitHub API).

If pull request title and message do not provide enough context, request additional details from the reviewer:
- Link a JIRA ticket
- Associate a GitHub issue
- Link to another pull request
- Add more detail to the PR title or body

### Step 2: Detect Change Type with Android Refinements

<thinking>
Analyze the changeset systematically:
1. What files were modified? (code vs config vs docs)
2. What is the PR/commit title indicating?
3. Is there new functionality or just modifications?
4. What's the risk level of these changes?
</thinking>

Use the base change type detection from the agent, with Android-specific refinements:

**Android-specific patterns:**
- **Feature Addition**: New `ViewModel`, new `Repository`, new `@Composable` functions, new `*Screen.kt` files
- **UI Refinement**: Changes only in `*Screen.kt`, `*Composable.kt`, `ui/` package files
- **Infrastructure**: Changes to `.github/workflows/`, `gradle/`, `build.gradle.kts`, `libs.versions.toml`
- **Dependency Update**: Changes only to `libs.versions.toml` or `build.gradle.kts` with version bumps

### Step 3: Load Appropriate Checklist

Based on detected type, read the relevant checklist file:

- **Dependency Update** → `checklists/dependency-update.md` (expedited review)
- **Bug Fix** → `checklists/bug-fix.md` (focused review)
- **Feature Addition** → `checklists/feature-addition.md` (comprehensive review)
- **UI Refinement** → `checklists/ui-refinement.md` (design-focused review)
- **Refactoring** → `checklists/refactoring.md` (pattern-focused review)
- **Infrastructure** → `checklists/infrastructure.md` (tooling-focused review)

The checklist provides:
- Multi-pass review strategy
- Type-specific focus areas
- What to check and what to skip
- Structured thinking guidance

### Step 4: Execute Review Following Checklist

<thinking>
Before diving into details:
1. What are the highest-risk areas of this change?
2. Which architectural patterns need verification?
3. What security implications exist?
4. How should I prioritize my findings?
5. What tone is appropriate for this feedback?
</thinking>

Follow the checklist's multi-pass strategy, thinking through each pass systematically.

### Step 5: Consult Android Reference Materials As Needed

Load reference files only when needed for specific questions:

- **Issue prioritization** → `reference/priority-framework.md` (Critical vs Suggested vs Optional)
- **Phrasing feedback** → `reference/review-psychology.md` (questions vs commands, I-statements)
- **Architecture questions** → `reference/architectural-patterns.md` (MVVM, Hilt DI, module org, error handling)
- **Security questions (quick reference)** → `reference/security-patterns.md` (common patterns and anti-patterns)
- **Security questions (comprehensive)** → `docs/ARCHITECTURE.md#security` (full zero-knowledge architecture)
- **Testing questions** → `reference/testing-patterns.md` (unit tests, mocking, null safety)
- **UI questions** → `reference/ui-patterns.md` (Compose patterns, theming)
- **Style questions** → `docs/STYLE_AND_BEST_PRACTICES.md`

## Core Principles

- **Appropriate depth**: Match review rigor to change complexity and risk
- **Specific references**: Always use `file:line_number` format for precise location
- **Actionable feedback**: Say what to do and why, not just what's wrong
- **Efficient reviews**: Use multi-pass strategy, skip what's not relevant
- **Android patterns**: Validate MVVM, Hilt DI, Compose conventions, Kotlin idioms
