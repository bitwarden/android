---
description: Guided Android code review workflow through context gathering, Android-specific review, and output
argument-hint: [PR# | PR URL | "local"]
---

# Android Code Review Workflow

You are guiding the developer through a comprehensive Android code review for the Bitwarden Android project.

**Input**: $ARGUMENTS

## Prerequisites

- **Jira/Confluence access**: The `bitwarden-atlassian-tools@bitwarden-marketplace` MCP plugin is required to fetch linked Jira tickets. If unavailable, skip ticket context.
- **GitHub CLI**: Required for fetching PR metadata. Verify with `gh auth status`.

## Workflow Phases

Work through each phase sequentially. **Confirm with the user before advancing to the next phase.** The user may skip phases that are not applicable.

### Phase 1: Ingest

Parse the input to determine review context:

**Source Detection Rules:**
- **PR number** (`123`, `PR #123`, `https://github.com/.../pull/123`): Extract the numeric ID. Fetch PR metadata via `gh pr view <N> --json title,body,headRefName,baseRefName,author,files`. Fetch existing review threads to avoid duplicate comments via `gh api graphql` with `reviewThreads(first: 100)`.
- **"local"** or no argument: Review current branch changes via `git diff main...HEAD` and `git log main...HEAD --oneline --no-merges`.
- **No input**: Ask the user whether to review a PR (provide number/URL) or local branch changes.

**Additional context:**
- Detect Jira ticket references in PR title/body (patterns like `PM-\d+`, `BWA-\d+`). Fetch via `get_issue` if the MCP plugin is available.
- Summarize what was fetched rather than dumping raw content.

**Present a structured summary:**
1. What is being reviewed (PR title/number, branch, or local changes description)
2. Jira ticket context if found (summary and acceptance criteria)
3. Files changed (count and modules affected)
4. Existing review thread count (PR reviews only — avoids duplicate comments)

**Gate**: User confirms the summary is complete before proceeding.

### Phase 2: Review

Invoke the `reviewing-changes` skill and use it to perform the Android-specific code review. Use the PR context from Phase 1 (change type, files affected, modules, Jira requirements) to inform the skill's change type detection and checklist selection.

The skill will:
1. Detect the change type based on files and PR context from Phase 1
2. Load the appropriate type-specific checklist
3. Execute the multi-pass review strategy
4. Consult reference materials as needed

**Before advancing**: Share a summary of key findings (critical issues if any, overall assessment) and confirm the user is ready to output the review.

### Phase 3: Output

Write the completed review to local files:

- `review-summary.md` — Overall assessment (APPROVE / REQUEST CHANGES) plus critical issues list
- `review-inline-comments.md` — All inline findings with `<details>` tags

Follow the exact output format from `.claude/skills/reviewing-changes/examples/review-outputs.md`.

For PR reviews: offer to post the review to GitHub using `gh pr review <N> --comment -b "$(cat review-summary.md)"` for the summary. For inline comments, use the GitHub API or the `bitwarden-code-review` plugin if installed.

**Before advancing**: Confirm the files were written successfully and ask if the user wants to post to GitHub (PR reviews only).

## Guidelines

- Be explicit about which phase you are in at all times.
- Never proceed to another phase without user confirmation.
- If the user wants to skip a phase, acknowledge and move to the next applicable phase.
- If starting from a partially completed review (e.g., review already written), skip to the appropriate phase.
