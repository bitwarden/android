---
description: Guided Android development workflow through all lifecycle phases
argument-hint: <task description, plan, or Jira ticket reference>
---

# Android Development Workflow

You are guiding the developer through a complete Android development lifecycle for the Bitwarden Android project. The task to work on is:

**Task**: $ARGUMENTS

## Workflow Phases

Work through each phase sequentially. **Confirm with the user before advancing to the next phase.** If a phase fails (tests fail, lint errors, etc.), loop on that phase until resolved before advancing. The user may skip phases that are not applicable.

### Phase 1: Implement

Invoke the `implementing-android-code` skill and use it to guide your implementation of the task. Understand what needs to be done, explore the relevant code, and write the implementation.

**Before advancing**: Summarize what was implemented and confirm the user is ready to move to testing.

### Phase 2: Test

Invoke the `testing-android-code` skill and use it to write tests for the changes made in Phase 1. Follow the project's test patterns and conventions.

**Before advancing**: Summarize what tests were written and confirm readiness for build verification.

### Phase 3: Build & Verify

Invoke the `build-test-verify` skill to run tests, lint, and detekt. Ensure everything passes.

**If failures occur**: Fix the issues and re-run verification. Do not advance until all checks pass.

**Before advancing**: Report build/test/lint results and confirm readiness for self-review.

### Phase 4: Self-Review

Invoke the `perform-android-preflight-checklist` skill to perform a quality gate check on all changes. Address any issues found.

**Before advancing**: Share the self-review results and confirm readiness to commit.

### Phase 5: Commit

Invoke the `committing-android-changes` skill to stage and commit the changes with a properly formatted commit message.

**Before advancing**: Confirm the commit was successful and ask if the user wants to proceed to review and PR creation, or stop here.

### Phase 6: Review

Invoke the `reviewing-changes` skill to perform a self-review of the committed diff. Address any issues found before proceeding.

**Before advancing**: Share review findings and confirm readiness for PR creation.

### Phase 7: Pull Request

Invoke the `creating-android-pull-request` skill to create the pull request with proper description and formatting. **Create as a draft PR by default** unless the user has explicitly requested a ready-for-review PR.

**On completion**: Share the PR URL with the user.

## Guidelines

- Be explicit about which phase you are in at all times.
- If the user wants to skip a phase, acknowledge and move to the next applicable phase.
- If starting from a partially completed task (e.g., code already written), skip to the appropriate phase.
