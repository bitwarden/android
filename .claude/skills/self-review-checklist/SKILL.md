---
name: self-review-checklist
description: Quality gate checklist to run before committing or creating a PR. Use when finishing implementation, checking work quality, or preparing to commit. Triggered by "self review", "check my work", "ready to commit", "done implementing", "review checklist", "quality check".
---

# Self-Review Checklist

Run through this checklist before committing or opening a PR.

## Tests
- [ ] Tests pass with correct flavor: `./gradlew app:testStandardDebugUnitTest`
- [ ] New code has corresponding test coverage
- [ ] Tests for affected modules also pass (`:core:test`, `:data:test`, etc.)

## Code Quality
- [ ] Lint/detekt clean: `./gradlew detekt`
- [ ] No unintended file changes (`git diff` review)
- [ ] KDoc on all new public APIs
- [ ] No TODO comments left behind (or they reference a ticket)

## Security
- [ ] No plaintext keys, tokens, or secrets in code
- [ ] User input validated before processing
- [ ] Sensitive data uses encrypted storage patterns
- [ ] No logging of sensitive data (passwords, keys, tokens)

## Bitwarden Patterns
- [ ] String resources in `:ui` module with typographic quotes
- [ ] Navigation route is `@Serializable` and registered in graph
- [ ] New implementations have Hilt `@Binds` or `@Provides` in a module
- [ ] ViewModel extends `BaseViewModel<S, E, A>` with proper state persistence
- [ ] Async results mapped through internal actions (not direct state updates)

## Files
- [ ] No accidental `.idea/`, build output, or generated files staged
- [ ] No credential files or `.env` files included
