---
description: Guided requirements refinement and implementation planning for Bitwarden Android
argument-hint: <Jira ticket (PM-12345), Confluence URL, or free-text description>
---

# Android Planning Workflow

You are guiding the developer through requirements refinement and implementation planning for the Bitwarden Android project. The input to plan from is:

**Input**: $ARGUMENTS

## Workflow Phases

Work through each phase sequentially. **Confirm with the user before advancing to the next phase.** The user may skip phases that are not applicable. If starting from a partially completed plan, skip to the appropriate phase.

### Phase 1: Ingest Requirements

Parse the input to detect and fetch all available sources:

**Source Detection Rules:**
- **Jira tickets** (patterns like `PM-\d+`, `BWA-\d+`, `EC-\d+`): Fetch via `get_issue` and `get_issue_comments`. Also fetch linked issue summaries (parent epic, sub-tasks, blockers) for context.
- **Confluence URLs** (containing `atlassian.net/wiki` or confluence page IDs): Extract page ID and fetch via `get_confluence_page`. If the page is a parent page, fetch child pages via `get_child_pages` and ask the user which are relevant.
- **Free text**: Treat as direct requirements — no fetching needed.
- **Multiple inputs**: All are first-class sources. Fetch each independently and consolidate.

**Present a structured summary:**
1. Sources identified and fetched (with links)
2. Raw requirements extracted from each source
3. Initial scope assessment (small / medium / large)

**Edge cases:**
- Jira ticket with no description → flag as critical gap that Phase 2 must address
- Multiple tickets → fetch all, consolidate, flag any contradictions
- Ticket + free text → both treated as first-class; free text supplements ticket

**Gate**: User confirms the summary is complete and may add additional sources or context before proceeding.

### Phase 2: Refine Requirements

Invoke the `refining-android-requirements` skill and use it to perform gap analysis on the raw requirements from Phase 1.

The skill will:
1. Consolidate all sources into a working document
2. Evaluate requirements against a structured rubric (functional, technical, security, UX, cross-cutting)
3. Present categorized gaps as blocking or non-blocking questions
4. After user answers, produce a structured specification with numbered IDs

**Gate**: User approves the refined specification. They may request changes or provide additional answers.

### Phase 3: Plan Implementation

Invoke the `planning-android-implementation` skill and use it to design the implementation approach based on the refined spec from Phase 2.

The skill will:
1. Classify the change type
2. Explore the codebase for reference implementations and integration points
3. Design the architecture with component relationships
4. Produce a file inventory and phased implementation plan
5. Assess risks and define verification criteria

**Gate**: User reviews the implementation plan and may request changes to architecture, phasing, or scope.

### Phase 4: Finalize & Save

Merge the outputs from Phase 2 (specification) and Phase 3 (implementation plan) into a single design document using this template:

```markdown
# [Feature Name] — Design Document

**Feature**: [concise description]
**Date**: [current date]
**Status**: Ready for Implementation
**Jira**: [ticket ID if available]
**Sources**: [list of all input sources with links]

---

## Requirements Specification

[Full output from Phase 2 — the refined specification with numbered IDs]

---

## Implementation Plan

[Full output from Phase 3 — architecture, file inventory, phases, risks]

---

## Executing This Plan

To implement this plan, run:

    /work-on-android [ticket or feature reference]

Reference this design document during implementation for architecture decisions,
file locations, and phase ordering.
```

**Save the document:**
- With ticket: `.claude/outputs/plans/PM-XXXXX-FEATURE-NAME-PLAN.md`
- Without ticket: `.claude/outputs/plans/FEATURE-NAME-PLAN.md`
- Feature name should be uppercase with hyphens (e.g., `BIOMETRIC-TIMEOUT-CONFIG-PLAN.md`)
- Create the output directory if it does not exist

**On completion**: Present the saved file path and remind the user they can execute the plan with `/work-on-android`.

## Guidelines

- Be explicit about which phase you are in at all times.
- If the user wants to skip a phase, acknowledge and move to the next applicable phase.
- When fetching from Jira/Confluence, summarize what was found rather than dumping raw content.
- Questions in Phase 2 should be specific and actionable, not generic.
- The implementation plan in Phase 3 should reference concrete files in the codebase, not abstract descriptions.