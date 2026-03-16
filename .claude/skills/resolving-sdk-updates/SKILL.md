---
name: resolving-sdk-updates
description: >
  Diagnose and resolve build failures from Bitwarden SDK updates (com.bitwarden:sdk-android).
  Investigates sdk-internal PRs, fixes exhaustive when expressions, and assesses behavioral
  impact. Use when reviewing SDK update PRs, fixing SDK build errors, encountering "when
  expression must be exhaustive" after SDK bump, updating sdk-android in libs.versions.toml,
  or when any PR from the sdlc/sdk-update branch has failing CI. Triggered by "fix the SDK
  update", "resolve SDK breaking changes", "check the SDK PR", "SDK version bump",
  "sdk-android", "sdk-internal". Do NOT use for general dependency updates unrelated to the
  Bitwarden SDK.
allowed-tools: Bash(git branch --show-current), Bash(git diff *), Bash(gh run list *), Bash(gh run view *), Bash(gh pr view *), Bash(gh pr diff *)
---

# Resolving SDK Updates

Sequential five-phase workflow for diagnosing and resolving build failures caused by Bitwarden SDK (`com.bitwarden:sdk-android`) version updates.

## Important

- **SDK repo**: `bitwarden/sdk-internal`
- **Artifact**: `com.bitwarden:sdk-android` published to GitHub Packages
- **Version catalog**: `gradle/libs.versions.toml` (key: `sdk-android`)
- **Update branch convention**: `sdlc/sdk-update`

CRITICAL: Before applying any fix, always read the `sdk-internal` PR diff to understand the author's intent. A compile fix alone may be insufficient if the SDK change introduces new behavior that requires a dedicated code path.

## Current State (preprocessed)

- **Current branch**: !`git branch --show-current`
- **SDK version diff vs main**: !`git diff main -- gradle/libs.versions.toml | grep sdk-android || echo "No SDK version change detected"`
- **Latest CI failure (sdlc/sdk-update)**: !`gh run list --branch sdlc/sdk-update --status failure --limit 1 --json databaseId,event,conclusion -q '.[0]' 2>/dev/null`

## Proactive Behavior

- If the preprocessed state above shows CI failures, skip Phase 1 and jump directly to Phase 2.
- If no CI failures exist, focus on Phase 1 (changelog review) and Phase 5 (impact assessment).
- If the SDK version diff above shows no change, confirm the branch and version catalog before proceeding.

---

## Phase 1: Identify the Update

Determine what changed in the SDK version bump. The preprocessed SDK version diff above may already provide this; verify and continue.

1. **Extract version diff** (if not already shown above) from `gradle/libs.versions.toml`:
   ```bash
   git diff main -- gradle/libs.versions.toml | grep sdk-android
   ```

2. **Parse PR body** for linked `sdk-internal` PRs and Jira tickets:
   - SDK PR pattern: `bitwarden/sdk-internal#NNN` or `#NNN` in context of SDK references
   - Jira ticket pattern: `PM-NNNNN`
   - If working from a GitHub PR: `gh pr view {PR_NUMBER} --json body -q .body`

3. **Record findings**: List each `sdk-internal` PR number and Jira ticket for subsequent phases.

---

## Phase 2: Diagnose Build Failures

Identify and categorize all compiler errors introduced by the SDK change.

1. **Extract CI errors** (if CI is failing):
   ```bash
   gh run list --branch {BRANCH} --status failure --limit 1 --json databaseId -q '.[0].databaseId'
   gh run view {RUN_ID} --log-failed | grep -E "e: |error:" | head -50
   ```

2. **Or build locally**:
   ```bash
   ./gradlew app:compileStandardDebugKotlin 2>&1 | grep -E "e: " | head -30
   ```

3. **Categorize each error** — see `references/common-fix-patterns.md` for the full error category table and fix strategies.

---

## Phase 3: Investigate SDK Changes

Understand the SDK author's intent behind each change. This determines whether a compile fix alone is sufficient or a new code path is needed.

1. **For each referenced `sdk-internal` PR**, retrieve details:
   ```bash
   gh pr view {N} --repo bitwarden/sdk-internal --json title,body,files
   ```

2. **Read the diff** for breaking changes:
   ```bash
   gh pr diff {N} --repo bitwarden/sdk-internal
   ```

3. **Focus on**:
   - New sealed class variants (will cause exhaustive `when` breaks)
   - Changed function signatures (new/removed/renamed parameters)
   - New error types in Result sealed classes
   - Behavioral changes described in PR body or commit messages
   - Deprecation notices or migration guidance

4. **Document** each change with: what changed, why it changed (from PR description), and expected Android impact.

---

## Phase 4: Apply Fixes

Resolve each compiler error using patterns from `references/common-fix-patterns.md`.

1. **Fix each error** according to its category. For exhaustive `when` expressions, the most common fix:
   ```kotlin
   is NewSealedVariant -> {
       // Handle new case appropriately based on SDK PR context
   }
   ```

2. **Search for non-exhaustive usages** the compiler won't catch (`when` used as statement not expression, or `when` without `else` on non-sealed types):
   ```bash
   grep -r "AffectedTypeName\." --include="*.kt" app/src/main/kotlin/
   grep -r "is AffectedTypeName" --include="*.kt" app/src/main/kotlin/
   ```

3. **Verify the fix compiles**:
   ```bash
   ./gradlew app:compileStandardDebugKotlin
   ```

4. **Run affected tests** to ensure no regressions:
   ```bash
   ./gradlew app:testStandardDebugUnitTest --tests "*.AffectedClassTest"
   ```

---

## Phase 5: Assess Behavioral Impact

Classify each SDK change and determine if follow-up work is needed beyond compile fixes.

1. **For each change, classify**:
   - **Compile-only fix**: New variant added but no feature work needed (e.g., new error type that maps to existing generic error handling)
   - **Feature-requiring**: New capability exposed by SDK that needs a new code path, UI, or business logic

2. **For feature-requiring changes**, trace Jira tickets.

   First, determine if the `bitwarden-atlassian-tools` MCP plugin is available by checking whether any tools starting with `mcp__plugin_bitwarden-atlassian-tools_` are listed in your available tools.

   **If the plugin IS available:**
   - Search for existing Android tickets: `project = PM AND text ~ "{keyword}" AND text ~ "Android"`
   - Check linked issues from SDK tickets: `issue in linkedIssues(PM-XXXXX)`
   - Document: ticket ID, status, assignee, whether Android work is already planned

   **If the plugin is NOT available:**
   - Inform the user: "Jira ticket tracing requires the `bitwarden-atlassian-tools@bitwarden-marketplace` plugin (github.com/bitwarden/ai-plugins). I can skip ticket tracing or pause for plugin installation."
   - Document the Jira ticket IDs (from Phase 1) that should be investigated manually
   - Note: behavioral impact assessment will be incomplete without ticket tracing

3. **Produce summary**:
   - What was fixed (files changed, error types resolved)
   - What needs separate feature work (with Jira ticket references)
   - Any risks or behavioral changes to call out in PR review

---

## Examples

### Example 1: SDK update PR with failing CI

User says: "Fix the build errors on PR #6615"

Actions:
1. Extract CI errors from failing run — identify exhaustive `when` breaks
2. Parse PR body for `sdk-internal` PR references
3. Read `sdk-internal` PR diff to understand new sealed variants
4. Add new branches to affected `when` expressions
5. Grep for other usages of affected types across codebase
6. Verify fix compiles and tests pass

Result: Two files fixed with new `when` branches, behavioral impact documented showing compile-only changes.

### Example 2: SDK update PR review (no failures)

User says: "Review the SDK changes in this PR"

Actions:
1. Parse PR body for changelog and `sdk-internal` PR numbers
2. Read each `sdk-internal` PR diff to understand changes
3. Classify each change as compile-only vs feature-requiring
4. Search Jira for tracking tickets on feature-requiring changes

Result: Summary of SDK changes with impact assessment and Jira ticket status.

---

## Performance Notes

- Take your time to thoroughly investigate each `sdk-internal` PR diff before applying fixes.
- Quality is more important than speed — a hasty compile fix that misses behavioral intent creates harder bugs later.
- Do not skip the codebase-wide grep in Phase 4 step 2. The compiler only catches exhaustive `when` expressions, not `when` statements.

---

## Troubleshooting

| Problem | Cause | Solution |
|---|---|---|
| 401 Unauthorized fetching SDK | Missing or expired GitHub token | Check `GITHUB_TOKEN` in `user.properties` or env; needs `read:packages` scope |
| Cannot find `sdk-internal` repo | GitHub CLI lacks access | Run `gh auth status` and verify org access; may need `gh auth refresh` |
| `sdk-internal` PR not found | PR number parsed incorrectly | Verify PR number from the update PR body; check `bitwarden/sdk-internal` directly |
| Build succeeds but tests fail | Behavioral change in SDK | Review SDK PR description for behavioral changes; may need test updates |

**Cross-references**:
- `build-test-verify` skill — build and test commands
- `reviewing-changes` skill — general dependency update review checklists
- `implementing-android-code` skill — patterns for new code paths