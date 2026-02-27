---
name: creating-android-pull-request
version: 0.1.0
description: Pull request creation workflow for Bitwarden Android. Use when creating PRs, writing PR descriptions, or preparing branches for review. Triggered by "create PR", "pull request", "open PR", "gh pr create", "PR description".
---

# Create Pull Request

## PR Title Format

```
[PM-XXXXX] <type>: <short imperative summary>
```

**Examples:**
- `[PM-12345] feat: Add autofill support for passkeys`
- `[PM-12345] fix: Resolve crash during vault sync`
- `[PM-12345] refactor: Simplify authentication flow`

**Rules:**
- Include Jira ticket prefix
- Keep under 70 characters total
- Use imperative mood in the summary

**Type keywords** (triggers automatic `t:` label via CI):

Invoke the `labeling-android-changes` skill for the full type keyword table and selection guidance.

---

## PR Body Template

**IMPORTANT:** Always follow the repo's PR template at `.github/PULL_REQUEST_TEMPLATE.md`. Delete the Screenshots section entirely if there are no UI changes.

---

## Pre-PR Checklist

1. **All tests pass**: Run `./gradlew app:testStandardDebugUnitTest` (and other affected modules)
2. **Lint clean**: Run `./gradlew detekt`
3. **Self-review done**: Use `perform-android-preflight-checklist` skill
4. **No unintended changes**: Check `git diff main...HEAD` for unexpected files
5. **Branch up to date**: Rebase on `main` if needed

---

## Creating the PR

```bash
# Ensure branch is pushed
git push -u origin <branch-name>

# Create PR as draft by default (body follows .github/PULL_REQUEST_TEMPLATE.md)
gh pr create --draft --title "[PM-XXXXX] feat: Short summary" --body "<fill in from PR template>"
```

**Default to draft PRs.** Only create a non-draft (ready for review) PR if the user explicitly requests it.

---

## Base Branch

- Default target: `main`
- Check with team if targeting a feature branch instead
