---
name: create-pull-request
description: Pull request creation workflow for Bitwarden Android. Use when creating PRs, writing PR descriptions, or preparing branches for review. Triggered by "create PR", "pull request", "open PR", "gh pr create", "PR description".
---

# Create Pull Request

## PR Title Format

```
[PM-XXXXX] <short imperative summary under 70 chars>
```

- Include Jira ticket prefix
- Keep under 70 characters total
- Use imperative mood: "Add", "Fix", "Update", "Remove"

---

## PR Body Template

**IMPORTANT:** Always follow the repo's PR template at `.github/PULL_REQUEST_TEMPLATE.md`. Delete the Screenshots section entirely if there are no UI changes.

---

## Pre-PR Checklist

1. **All tests pass**: Run `./gradlew app:testStandardDebugUnitTest` (and other affected modules)
2. **Lint clean**: Run `./gradlew detekt`
3. **Self-review done**: Use `self-review-checklist` skill
4. **No unintended changes**: Check `git diff main...HEAD` for unexpected files
5. **Branch up to date**: Rebase on `main` if needed

---

## Creating the PR

```bash
# Ensure branch is pushed
git push -u origin <branch-name>

# Create PR targeting main (body follows .github/PULL_REQUEST_TEMPLATE.md)
gh pr create --title "[PM-XXXXX] Short summary" --body "<fill in from PR template>"
```

---

## Base Branch

- Default target: `main`
- Check with team if targeting a feature branch instead
