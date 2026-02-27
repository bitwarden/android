---
name: committing-android-changes
version: 0.1.0
description: Git commit conventions and workflow for Bitwarden Android. Use when committing code, writing commit messages, or preparing changes for commit. Triggered by "commit", "git commit", "commit message", "prepare commit", "stage changes".
---

# Git Commit Conventions

## Commit Message Format

```
[PM-XXXXX] <type>: <imperative summary>

<optional body explaining why, not what>
```

### Rules

1. **Ticket prefix**: Always include `[PM-XXXXX]` matching the Jira ticket
2. **Type keyword**: Include a conventional commit type after the ticket prefix (see table below)
3. **Imperative mood**: "Add feature" not "Added feature" or "Adds feature"
4. **Short summary**: Under 72 characters for the first line
5. **Body**: Explain the "why" not the "what" — the diff shows the what

### Type Keywords

Invoke the `labeling-android-changes` skill for the full type keyword table and selection guidance.

### Example

```
[PM-12345] feat: Add biometric unlock timeout configuration

Users reported confusion about when biometric prompts appear.
This adds a configurable timeout setting to the security preferences.
```

### Followup Commits

Only the first commit on a branch needs the full format (ticket prefix, type keyword, body). Subsequent commits — whether addressing review feedback, making intermediate changes, or iterating locally — can use a short, descriptive summary with no prefix or body required.

```
Update error handling in login flow
```

---

## Pre-Commit Checklist

Run the `perform-android-preflight-checklist` skill for the full quality gate. At minimum, before staging and committing:

1. **Run affected module tests** (use `build-test-verify` skill for correct commands)
2. **Check lint**: `./gradlew detekt` on changed modules
3. **Review staged changes**: `git diff --staged` — verify no unintended modifications
4. **Verify no secrets**: No API keys, tokens, passwords, or `.env` files staged
5. **Verify no generated files**: No build outputs, `.idea/` changes, or generated code

---

## What NOT to Commit

- `.env` files or `user.properties` with real tokens
- Credential files or signing keystores
- Build outputs (`build/`, `*.apk`, `*.aab`)
- IDE-specific files (`.idea/` changes, `*.iml`)
- Large binary files

---

## Staging Best Practices

- **Stage specific files** by name rather than `git add -A` or `git add .`
- Put each file path on its own line for readability:
  ```bash
  git add \
    path/to/first/File.kt \
    path/to/second/File.kt \
    path/to/third/File.kt
  ```
- Review each file being staged to avoid accidentally including sensitive data
- Use `git status` (without `-uall` flag) to see the working tree state
