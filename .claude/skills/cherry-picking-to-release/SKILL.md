---
name: cherry-picking-to-release
description: Cherry-pick workflow for Bitwarden Android release branches. Guides through identifying commits on main, creating cherry-pick branches, resolving conflicts, and opening PRs with correct conventions. Use when user says "cherry-pick", "backport", "cp to release", or needs to port a fix to a release candidate branch. Accepts target release branch as argument.
---

# Cherry-Pick to Release Branch

Sequential workflow for cherry-picking commits from `main` into a release branch.

**Inputs:**
- `$0`: Original PR number on `main`
- `$1`: Target release branch (e.g., `release/2026.3-rc48`). If omitted, infer from current branch context below.

## Pre-loaded Context

**Current branch:** !`git branch --show-current`
**Latest RC branch:** !`git branch -r --sort=-committerdate | grep 'origin/release/.*-rc' | head -1 | tr -d ' '`
**Source PR metadata:** !`gh pr view $0 --json title,body,labels,mergeCommit,number 2>/dev/null || echo "No PR number provided as argument — ask user for PR number or commit SHA"`

---

## Step 1: Identify Source Commit & PR

Extract from the pre-loaded PR metadata above:
- **Ticket prefix**: `[PM-XXXXX]` from the title
- **Type keyword**: `feat`, `fix`, `debt`, `refactor`, etc. from the title
- **Description**: The imperative summary after the type keyword
- **PR number**: `#<number>` for back-reference
- **Labels**: To replicate on the cherry-pick PR
- **Merge commit SHA**: The commit to cherry-pick

If no PR metadata was pre-loaded (user provided a commit SHA instead), find the associated PR:

```bash
gh pr list --search "<SHA>" --state merged --json number,title
```

---

## Step 2: Validate Target Release Branch

Use `$1` if provided. Otherwise, if the current branch (from pre-loaded context) is already a `release/` branch, offer it as the default target.

Verify the target branch exists using the pre-loaded release branch list. If not found:
- Show the available release branches from context above
- Ask the user to confirm or correct the target branch
- Do NOT proceed until a valid branch is confirmed

---

## Step 3: Create Cherry-Pick Branch

**Branch naming convention:**

```
<release-branch>_cp-<ticket-number>-<short-desc>
```

**Examples:**
- `release/2026.3-rc48_cp-33394-sync-on-unlock`
- `release/2026.4-rc1_cp-41200-fix-autofill-crash`

**Commands:**

```bash
git checkout -b <branch-name> origin/<target-release-branch>
```

---

## Step 4: Execute Cherry-Pick

```bash
git cherry-pick <sha>
```

### Handling Conflicts

If the cherry-pick fails with conflicts:

1. Review conflicting files: `git status`
2. Resolve each conflict manually or with editor
3. Stage resolved files: `git add <resolved-files>`
4. Continue: `git cherry-pick --continue`

**If conflicts are too complex to resolve:**

```bash
git cherry-pick --abort
```

Then inform the user and discuss alternative approaches (manual port, partial cherry-pick, etc.).

### Multiple Commits

When cherry-picking multiple related commits, apply them sequentially in chronological order:

```bash
git cherry-pick <oldest-sha>
git cherry-pick <next-sha>
# ... and so on
```

---

## Step 5: Amend Commit Message

Cherry-picked commits must be prefixed with the cherry emoji (🍒) directly before the ticket prefix, with no space between them.

**Format:**

```
🍒[PM-XXXXX] <type>: <original description>
```

**Examples from this repo:**
- `🍒[PM-33394] debt: Add userFriendlyMessage extension and errorMessage to result types (#6644)`
- `🍒[PM-33227] feat: Add Clear SSO Cookies button to debug menu (#6632)`
- `🍒[PM-32123] feat: Propagate informative cookie redirect error message (#6631)`

**Rules:**
- No space between `🍒` and `[PM-`
- Keep the original PR number reference in parentheses at the end
- Body remains empty (no cherry-pick trailer, no Co-Authored-By)

Amend if needed:

```bash
git commit --amend -m "$(cat <<'EOF'
🍒[PM-XXXXX] <type>: <original description> (#<original-PR-number>)
EOF
)"
```

---

## Step 6: Push & Create PR

### Push the branch:

```bash
git push -u origin <branch-name>
```

### Create PR:

```bash
gh pr create \
  --base <target-release-branch> \
  --title "$(cat <<'EOF'
🍒[PM-XXXXX] <type>: <original description> (#<original-PR-number>)
EOF
)" \
  --body "$(cat <<'EOF'
## Tracking
PM-XXXXX
Cherry-picked from #<original-PR-number>

## Objective
<copied from original PR objective/summary>
EOF
)" \
  --label "<label1>" --label "<label2>" \
  --draft
```

**PR rules:**
- **Base branch**: Must be the target release branch, NOT `main`
- **Title**: Same cherry emoji format as the commit message
- **Labels**: Copy from the original PR
- **Draft**: Default to draft PR unless user requests otherwise

---

## Step 7: Post-Creation Checklist

After creating the PR, verify:

- [ ] PR targets the correct release branch (not `main`)
- [ ] Title has `🍒` prefix with no space before `[PM-`
- [ ] PR body references the original PR number
- [ ] Labels match the original PR
- [ ] CI checks are passing (or expected failures noted)

```bash
gh pr view --json baseRefName,title,labels,url
```

Report the PR URL to the user and link to the original PR for reviewer context.

---

## Error Reference

| Error | Cause | Resolution |
|-------|-------|------------|
| `fatal: bad object` | SHA not found locally | Run `git fetch origin main` first |
| `error: could not apply` | Cherry-pick conflicts | Resolve conflicts per Step 4 |
| `fatal: bad revision` | Branch doesn't exist | Verify branch name per Step 2 |