---
name: refining-android-requirements
version: 0.1.0
description: Requirements gap analysis and structured specification for Bitwarden Android. Use when refining requirements, analyzing specs, identifying gaps, or producing structured specifications from tickets or descriptions. Triggered by "refine requirements", "gap analysis", "spec review", "requirements analysis", "what's missing from this spec", "analyze this ticket".
---

# Requirements Refinement

This skill takes raw requirements (from Jira tickets, Confluence pages, or free-text descriptions) and produces a structured, implementation-ready specification through systematic gap analysis.

**Key principle**: This skill identifies gaps and produces specifications. It does NOT propose solutions or architecture — that is the responsibility of the `planning-android-implementation` skill.

---

## Step 1: Source Consolidation

Combine all input sources into a single working document. For each requirement, note its source:

```
- [Source: PM-12345] User must be able to configure timeout
- [Source: Confluence] Timeout range is 1-60 minutes
- [Source: User] Default timeout should be 15 minutes
```

Flag any contradictions between sources for immediate resolution.

---

## Step 2: Gap Analysis

Evaluate the consolidated requirements against the following 5-category rubric. For each category, check every item and note whether it is **covered**, **partially covered**, or **missing**.

### A. Functional Requirements

| Check | Question to Ask If Missing |
|-------|---------------------------|
| User actions defined? | What specific user actions trigger this feature? |
| All states covered? (empty, loading, error, success) | What should the user see in [empty/loading/error] state? |
| Edge cases identified? | What happens when [boundary condition]? |
| Cancellation/back navigation flows? | Can the user cancel mid-flow? What happens to partial data? |
| Input validation rules? | What are the valid ranges/formats for [input]? |
| Success/failure criteria? | How does the user know the operation succeeded or failed? |
| Offline behavior? | What happens if this is attempted offline? |

### B. Technical Requirements

| Check | Question to Ask If Missing |
|-------|---------------------------|
| Module scope identified? (`:app`, `:authenticator`, shared) | Which module(s) does this feature belong to? |
| SDK dependencies? | Does this require Bitwarden SDK operations? Which ones? |
| Data storage approach? (Room, DataStore, in-memory) | Where is the data for this feature persisted? |
| Network API endpoints? | Which API endpoints are involved? Are they existing or new? |
| Process death handling? | What state needs to survive process death? |
| Migration requirements? | Does existing data need migration? |
| Feature flag needed? | Should this be behind a feature flag for staged rollout? |
| Product flavors (standard vs fdroid)? | Does this feature depend on Google Play Services? Available on F-Droid? |
| Data layer tier? | Does this need a new Manager (single-responsibility) or only Repository/DataSource? Consult `docs/ARCHITECTURE.md` Data Layer section. |
| Streaming vs discrete data? | Is data continuously observed (`DataState<T>` + `StateFlow`) or a one-shot operation (custom sealed class)? See `docs/ARCHITECTURE.md` Repositories section. |

### C. Security Requirements

| Check | Question to Ask If Missing |
|-------|---------------------------|
| Data sensitivity classified? | What sensitivity level does this data have? (vault-level, account-level, non-sensitive) |
| Storage encryption required? | Must this data be encrypted at rest? Via SDK or Android Keystore? |
| Logout cleanup behavior? | What must be cleared when the user logs out? |
| Auth-gating? | Does accessing this feature require active authentication? |
| Input sanitization? | Are there URL or credential inputs that need validation? |
| Sensitive data in ViewModel state? | Will passwords, tokens, or keys appear in state? Must use `@IgnoredOnParcel`. See `implementing-android-code` skill Section F. |
| SDK crypto context isolation? | Does this use vault encryption? Must use `ScopedVaultSdkSource` for multi-account safety. See CLAUDE.md Security Rules. |

### D. UX/UI Requirements

| Check | Question to Ask If Missing |
|-------|---------------------------|
| UI copy/strings defined? | What text should appear for [label/button/message]? |
| Error messages specified? | What should the error message say when [failure case]? |
| Loading states designed? | Should loading show a spinner, skeleton, or shimmer? |
| Navigation flow clear? | Where does the user go after [action]? Back stack behavior? |
| Accessibility considerations? | Are there content descriptions or focus order requirements? |
| Toast/snackbar/dialog for feedback? | What feedback mechanism for [action result]? |

### E. Cross-Cutting Concerns

| Check | Question to Ask If Missing |
|-------|---------------------------|
| Multi-account behavior? | How does this behave with multiple accounts? Per-account or global? |
| Backwards compatibility? | Does this affect existing users? Migration path? |
| Feature flag strategy? | Is this behind a server-side or local feature flag? |
| Analytics/logging? | Are there analytics events to track? |
| Bitwarden Authenticator impact? | Does this affect the `:authenticator` module? |
| F-Droid compatibility? | Does this degrade gracefully without Google Play Services (no push notifications, no Play Integrity)? |

---

## Step 3: Present Gaps

Organize all identified gaps into two categories:

### Blocking Questions

Questions that **must** be answered before implementation can begin because they change the architecture, data model, or core flow.

Format each question as:

```
**G[N]** ([Category]) — [Question text]
  Context: [Why this matters / what depends on the answer]
```

### Non-Blocking Questions

Questions that have **reasonable defaults** and can be resolved during implementation. Note the assumed default.

Format each question as:

```
**G[N]** ([Category]) — [Question text]
  Default assumption: [What we'll assume if not answered]
  Context: [Why this matters]
```

---

## Step 4: Produce Specification

After the user answers blocking questions (and optionally non-blocking ones), produce a structured specification:

```markdown
## Overview

[1-2 paragraph summary of the feature, its purpose, and scope]

## Functional Requirements

| ID | Requirement | Source | Notes |
|----|------------|--------|-------|
| FR1 | [requirement] | [source] | [any notes] |
| FR2 | ... | ... | ... |

## Technical Requirements

| ID | Requirement | Source | Notes |
|----|------------|--------|-------|
| TR1 | [requirement] | [source] | [any notes] |
| TR2 | ... | ... | ... |

## Security Requirements

| ID | Requirement | Source | Notes |
|----|------------|--------|-------|
| SR1 | [requirement] | [source] | [any notes] |

## UX Requirements

| ID | Requirement | Source | Notes |
|----|------------|--------|-------|
| UX1 | [requirement] | [source] | [any notes] |

## Open Items

Non-blocking items with assumed defaults that may be revisited:

| ID | Question | Assumed Default | Category |
|----|----------|----------------|----------|
| G[N] | [question] | [default] | [category] |

## Source Documentation

| Source | Type | Link |
|--------|------|------|
| [name] | Jira / Confluence / User-provided | [link if available] |
```

### Output Guidelines

- Requirements use numbered IDs (FR1, TR1, SR1, UX1) for traceability through implementation
- Each requirement cites its source (ticket, page, or user-provided)
- Technical requirements use table format for structured key/value data
- Interface signatures are included as fenced code blocks when applicable
- Open items preserve the gap ID (G[N]) for cross-referencing