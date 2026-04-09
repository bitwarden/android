---
description: End-to-end pipeline orchestrating requirements analysis, architecture planning, implementation, and multi-agent code review via agent team
argument-hint: <task description, Jira ticket, or Confluence URL> [--confirm]
---

# Plan-Implement-Review Pipeline

You are the **team lead** for an end-to-end Android development pipeline. Use the **Claude Agent Teams** feature to create a team, define tasks with dependencies, and add teammates who self-organize around the task list. The task dependency chain drives execution order — teammates claim unblocked tasks, complete them, and check for newly available work.

**Input**: $ARGUMENTS

## Input Parsing

1. **Extract task description**: Strip the `--confirm` flag (if present) from `$ARGUMENTS` to get the raw task description.
2. **Detect `--confirm` flag**: If `$ARGUMENTS` contains `--confirm`, enable **gated mode** (you must present phase output and get user approval before unblocking the next phase). Otherwise, run in **autonomous mode** (the task dependency chain drives progression automatically).
3. **Handle empty input**: If `$ARGUMENTS` is empty or only contains `--confirm`, ask the user to provide a task description before proceeding.
4. **Derive team slug**: Convert the task description to a slug — lowercase, replace spaces and special characters with hyphens, truncate to 40 characters. Example: `"PM-12345 Add biometric timeout"` → `pm-12345-add-biometric-timeout`.
5. **Define output paths** (using the slug):
   - `.claude/outputs/plans/{slug}-REQUIREMENTS.md`
   - `.claude/outputs/plans/{slug}-IMPLEMENTATION-PLAN.md`
   - `.claude/outputs/plans/{slug}-WORK-BREAKDOWN.md`
   - `.claude/outputs/plans/{slug}-QA-HANDOFF.md`
   - `.claude/outputs/reviews/{slug}-REVIEW-REQUIREMENTS.md`
   - `.claude/outputs/reviews/{slug}-REVIEW-ARCHITECTURE.md`
   - `.claude/outputs/reviews/{slug}-REVIEW-SECURITY.md`
   - `.claude/outputs/reviews/{slug}-REVIEW-CODE.md`
   - `.claude/outputs/reviews/{slug}-REVIEW-SUMMARY.md`

## Pipeline Structure

The pipeline has two major phases: **Plan** (runs once) and **Implement+Review** (loops per phase from the work breakdown). Most features have multiple implementation phases, so looping is the norm.

```
Plan (once):
  Requirements → Architecture → Work Breakdown → QA Handoff

Implement+Review (per phase from WBD):
  Phase 1: Implement → Review → Fix cycle → ✓
  Phase 2: Implement → Review → Fix cycle → ✓
  ...
  Phase N: Implement → Review → Fix cycle → ✓

Shutdown
```

After the planning phase produces a work breakdown with multiple phases, the team lead drives the implement+review loop — creating new branch per phase group (if desired), dispatching the implementer with phase-specific scope, running the 4-reviewer gauntlet, and iterating until all phases are complete or the user halts.

## Prerequisites

The following marketplace plugins are required for the full pipeline. If a plugin is not installed, inform the user and offer to **skip that teammate** rather than blocking the entire pipeline.

| Plugin | Source | Required For |
|--------|--------|-------------|
| `bitwarden-product-analyst` | `bitwarden-marketplace` | Requirements analysis + requirements review |
| `bitwarden-security-engineer` | `bitwarden-marketplace` | Security review |
| `bitwarden-code-review` | `bitwarden-marketplace` | Code quality review |
| `bitwarden-architect` | `bitwarden-marketplace` | Architecture planning + architecture review |
| `bitwarden-atlassian-tools` | `bitwarden-marketplace` | Optional — Jira/Confluence fetching |

The `android-implementer` agent is local (defined in `.claude/agents/`) and always available. The `architect` agent is provided by the `bitwarden-architect` marketplace plugin.

## Step 1: Create Team

Use `TeamCreate` to create the team:
- **Team name**: `pir-{slug}`
- **Description**: Summary of the task being worked on

Create the output directories if they don't exist:
- `.claude/outputs/plans/`
- `.claude/outputs/reviews/`

## Step 2: Plan Phase (runs once)

Create **4 planning tasks** and **5 standing teammates** (1 implementer + 4 reviewers):

### Planning Tasks

| Task | Subject | Description | blockedBy |
|------|---------|-------------|-----------|
| 1 | Analyze requirements | Analyze requirements for: {task description}. Write the requirements specification to `.claude/outputs/plans/{slug}-REQUIREMENTS.md`. Include a high-level work breakdown: epics, user stories, and acceptance criteria. | [] |
| 2 | Plan architecture and implementation | Read the requirements spec at `.claude/outputs/plans/{slug}-REQUIREMENTS.md`. Design the architecture and produce an implementation plan with phased task breakdown. Write the plan to `.claude/outputs/plans/{slug}-IMPLEMENTATION-PLAN.md`. | ["1"] |
| 3 | Produce work breakdown | Read the requirements spec at `.claude/outputs/plans/{slug}-REQUIREMENTS.md` and the implementation plan at `.claude/outputs/plans/{slug}-IMPLEMENTATION-PLAN.md`. Consolidate the product analyst's high-level work breakdown (epics, stories, acceptance criteria) with the architect's technical task breakdown (phases, files, dependencies) into a single Jira-ready work breakdown document. Write to `.claude/outputs/plans/{slug}-WORK-BREAKDOWN.md`. | ["1","2"] |
| 4 | Produce QA handoff | Read the work breakdown at `.claude/outputs/plans/{slug}-WORK-BREAKDOWN.md` and the requirements spec at `.claude/outputs/plans/{slug}-REQUIREMENTS.md`. For each implementation phase/increment in the work breakdown, define: what becomes testable at that point, which acceptance criteria can be verified, test scenarios, regression scope, and any dependencies or environment requirements. Write to `.claude/outputs/plans/{slug}-QA-HANDOFF.md`. | ["3"] |

### Planning Teammates

Add these teammates to handle the planning tasks. They will be shut down after planning completes:

| Teammate Name | Agent Type | Task | Role |
|---------------|-----------|------|------|
| `product-analyst` | `bitwarden-product-analyst:product-analyst` | 1 | Analyze requirements and produce specification |
| `architect` | `bitwarden-architect:architect` | 2 | Design architecture and produce implementation plan |
| `wbd-author` | `bitwarden-architect:architect` | 3 | Consolidate high-level and technical breakdowns into Jira-ready WBD |
| `qa-handoff-author` | `bitwarden-product-analyst:product-analyst` | 4 | Produce QA handoff document with testable increments and scenarios |

### Standing Teammates

Also add these teammates at team creation time. They will persist across all implementation phases:

| Teammate Name | Agent Type | Role |
|---------------|-----------|------|
| `implementer` | `android-implementer` | Implement, test, build, preflight, and commit |
| `requirements-reviewer` | `bitwarden-product-analyst:product-analyst` | Requirements conformance and QA handoff coverage |
| `architecture-reviewer` | `bitwarden-architect:architect` | Architecture and pattern adherence |
| `security-reviewer` | `bitwarden-security-engineer:bitwarden-security-engineer` | Security posture and zero-knowledge compliance |
| `code-reviewer` | `bitwarden-code-review:bitwarden-code-reviewer` | Code quality and Bitwarden standards |

**Important**: Instruct all standing teammates (implementer + 4 reviewers) to **wait for explicit instructions from the team lead** before starting any work. They must NOT self-activate based on task dependencies — the team lead controls when each implementation phase begins and when reviewers should start.

### Planning Completion

Once all 4 planning tasks are complete:
1. **Shut down planning teammates** (`product-analyst`, `architect`, `wbd-author`, `qa-handoff-author`).
2. **Read the work breakdown** at `.claude/outputs/plans/{slug}-WORK-BREAKDOWN.md`.
3. **Identify the implementation phases** — extract the ordered list of phases and their scope.
4. **Present the phase plan** to the user: list all phases with brief descriptions, and ask whether to proceed with all phases sequentially or a subset.
5. **Proceed to Step 3** (Implementation Loop).

## Step 3: Implementation Loop (per phase)

For each implementation phase identified in the work breakdown, execute this cycle:

### 3a: Dispatch Implementer

1. **Create an implementation task** via `TaskCreate`: "Implement Phase {N}: {phase name}"
2. **Assign to `implementer`** via `TaskUpdate` with `owner`.
3. **Send the implementer** a message via `SendMessage` with:
   - Which phase(s) to implement from the work breakdown
   - The implementation plan path for reference
   - Instruction to commit when done and report back
4. **Wait** for the implementer to report completion.
5. **Verify** that new commits exist on the branch (check `git log`).

### 3b: Dispatch Reviewers

Once the implementer commits:

1. **Create 4 review tasks** via `TaskCreate`, one per reviewer.
2. **Assign each task** to its reviewer via `TaskUpdate`.
3. **Send each reviewer** a message via `SendMessage` instructing them to:
   - Review ONLY the changes from this phase's commit(s) — not previously reviewed code
   - Write findings to their output file (append the phase number if multiple phases, e.g., `{slug}-REVIEW-CODE-P{N}.md`, or overwrite for single-phase reviews)
   - Mark their task complete when done
4. **Wait** for all 4 reviews to complete.

**CRITICAL**: Do NOT allow reviewers to start before the implementer has committed. Reviewers who start early will review stale code and produce invalid findings.

### 3c: Consolidate and Fix

Follow the same consolidation and fix cycle as described in Steps 5 and 6 below:
1. Read all 4 review files, consolidate into a summary.
2. If critical/important issues exist, send to implementer for fixes (up to 3 rounds).
3. If clean, proceed to the next phase.

### 3d: Phase Transition

After a phase's review cycle is clean:
1. **Print phase completion status**: "Phase {N} complete — {summary of what was built}."
2. **Optionally create a new branch** for the next phase group (ask user or auto-continue based on mode).
3. **Proceed to next phase** or, if all phases are done, proceed to Step 7 (Shutdown).

### Phase Grouping

Phases can be grouped for a single implement+review cycle when they are closely related and small enough. The team lead should use judgment:
- Group tightly coupled phases (e.g., "Bank Account form" + "Bank Account view" = one cycle)
- Keep independent phases separate for cleaner reviews
- Never group more than 3 phases in a single cycle

## Step 4: Monitor Progress

As team lead, your role during execution is to monitor and coordinate:

1. **Receive automatic notifications** as teammates complete tasks or report issues.
2. **Print status updates** at each phase transition:
   - Planning: "Task {N} complete — {artifact} written to {path}"
   - Implementation: "Phase {N} implemented and committed."
   - Review: "Phase {N} reviews complete — {summary}."
3. **Handle `--confirm` mode**: If gated mode is active, present phase output summary and wait for user approval before proceeding to the next phase. In autonomous mode, the loop continues automatically.
4. **Handle failures**: If a teammate reports a failure, surface details to the user and offer: retry, skip, or abort.
5. **Handle missing plugins**: If an Agent tool call fails because a plugin is not installed, inform the user which plugin to install and offer to skip that teammate's task.

## Step 5: Consolidate Reviews

After all 4 review tasks for a phase are complete, **you (the team lead) consolidate** the findings:

1. Read all 4 review output files.
2. Write a consolidated review summary to `.claude/outputs/reviews/{slug}-REVIEW-SUMMARY.md`:

```markdown
# Review Summary: {feature name}

**Date**: {current date}
**Phase**: {phase number and name}
**Round**: {cycle number} of 3
**Reviewers**: Product Analyst, Android Architect, Security Engineer, Code Reviewer

## Critical Issues
[Issues that MUST be addressed before merging — from any reviewer]

## Important Issues
[Issues that SHOULD be addressed — from any reviewer]

## Suggestions
[Non-blocking improvements — from any reviewer]

## Per-Reviewer Summaries

### Requirements Conformance (Product Analyst)
[Brief summary of findings]

### Architecture & Patterns (Android Architect)
[Brief summary of findings]

### Security (Security Engineer)
[Brief summary of findings]

### Code Quality (Code Reviewer)
[Brief summary of findings]
```

After writing the summary:

1. Deduplicate overlapping findings — if multiple reviewers flagged the same issue, note which reviewers identified it.
2. Present the consolidated summary to the user.
3. If there are **no critical or important issues**, proceed to the next phase (Step 3d) or Step 7 (Shutdown) if all phases are done.
4. If there **are** critical or important issues, proceed to Step 6 (Review-Fix Cycle).

## Step 6: Review-Fix Cycle (up to 3 rounds per phase)

When reviews surface critical or important issues, the implementer must assess and address them. This cycle can repeat **up to 3 times** before escalating to the user for human intervention.

### 6a: Send Findings to Implementer

Send the consolidated review summary to the `implementer` teammate via `SendMessage`. Instruct the implementer to:

1. **Read** the consolidated review summary and all individual review files.
2. **Assess** each critical and important finding:
   - **Legitimate**: Fix the issue in the codebase.
   - **Disputed**: Send a message to the specific reviewer(s) who raised the finding, explaining why the implementer believes it is a false positive or not applicable. The reviewer should respond with either agreement (finding withdrawn) or a rebuttal with additional evidence.
3. **After all findings are assessed**: Re-run tests and preflight checks, then commit the fixes.
4. **Create a new task** via `TaskCreate` for this fix round: "Address review findings (Phase {P}, round {N})" and mark it complete when done.
5. **Report back** to the team lead with a summary of what was fixed, what was disputed, and the outcomes of any disputes.

### 6b: Re-Review

After the implementer completes the fix round:

1. **Create 4 new review tasks** via `TaskCreate`, one per reviewer.
2. **Assign and send each reviewer** a message via `SendMessage` instructing them to re-review. Their new review should:
   - Focus on whether their previous findings were addressed
   - Check that fixes did not introduce new issues
   - Write updated findings to the same output file (overwriting the previous round)
3. **Wait** for all 4 re-reviews to complete.
4. **Re-consolidate** by repeating Step 5 with the updated review files. Increment the round counter.

### 6c: Cycle Control

- **If no critical/important issues remain**: Exit the cycle and proceed to the next phase or Step 7.
- **If issues persist and round < 3**: Repeat from Step 6a.
- **If round reaches 3 and issues still remain**: **Escalate to the user.** Present the outstanding issues, what was attempted across all rounds, and which findings remain disputed. Ask the user to decide: resolve manually, override and proceed, or abort.

### Cycle Status Updates

At each cycle transition, print:
```
Review-Fix Cycle: Phase {P}, Round {N}/3
  - Findings addressed: {count}
  - Findings disputed: {count} ({count} resolved, {count} upheld)
  - Remaining critical/important: {count}
  - Status: {Proceeding to re-review | Escalating to user | All clear}
```

## Step 7: Shutdown

After all implementation phases are complete (or user decides to stop):

1. **Shutdown all remaining teammates** via `SendMessage` with `shutdown_request` to each by name.
2. **Delete the team** via `TeamDelete`.
3. **Present the final summary** listing:
   - All planning artifact paths (requirements, plan, WBD, QA handoff)
   - All review file paths (final round per phase)
   - Consolidated review summary path
   - Number of phases completed
   - Number of review-fix cycles per phase
   - Total commits on the branch
4. **Suggest next steps**:
   - If all issues resolved: create a PR via `Skill(creating-android-pull-request)`
   - If user overrode remaining issues: note them as known items for the PR description
   - Review findings can be used as PR description context

## Guidelines

- **Plan once, implement+review in a loop**: The planning phase produces artifacts that cover ALL phases. Do NOT re-plan for subsequent phases — go straight to implement+review using the existing plans.
- **Team lead controls reviewer activation**: Reviewers must ONLY start when the team lead explicitly sends them a message after verifying the implementer has committed. Never let reviewers self-activate — they will review stale code.
- **Use Teams properly**: Create the team, create tasks with dependencies, add all teammates, and let the task system track progress. The team lead manually drives the phase loop.
- **Standing teammates persist**: The implementer and 4 reviewers stay active across all phases. Only planning teammates are shut down after the plan phase.
- **Minimal prompts**: Provide teammates only their team name, task number, and output file path. Their AGENT.md definitions handle workflow details.
- **Status messages**: Print a brief status message when each task completes so the user can track progress.
- **Missing plugins**: If a marketplace plugin is not installed, tell the user which to install and offer to skip that teammate. Never block the entire pipeline.
- **Agent failures**: Surface details to the user and offer retry, skip, or abort.
- **Gated mode (`--confirm`)**: Present phase output summary and wait for user approval before allowing the next phase to proceed.
- **Autonomous mode (default)**: The loop continues automatically between phases. Only intervene on failures.
- **Phase grouping**: Use judgment to group closely related phases (max 3) into a single implement+review cycle for efficiency.