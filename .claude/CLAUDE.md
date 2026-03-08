# Bitwarden Android - Claude Code Configuration

Official Android application for Bitwarden Password Manager and Bitwarden Authenticator, providing secure password management, two-factor authentication, and credential autofill services with zero-knowledge encryption.

## Overview

- Multi-module Android application: `:app` (Password Manager), `:authenticator` (2FA TOTP generator)
- Zero-knowledge architecture: encryption/decryption happens client-side via Bitwarden SDK
- Target users: End-users via Google Play Store and F-Droid

### Key Concepts

- **Zero-Knowledge Architecture**: Server never has access to unencrypted vault data or encryption keys
- **Bitwarden SDK**: Rust-based cryptographic SDK handling all encryption/decryption operations
- **DataState**: Wrapper for streaming data states (Loading, Loaded, Pending, Error, NoNetwork)
- **Result Types**: Custom sealed classes for operation results (never throw exceptions from data layer)
- **UDF (Unidirectional Data Flow)**: State flows down, actions flow up through ViewModels

---

## Architecture

```
User Request (UI Action)
         |
    Screen (Compose)
         |
    ViewModel (State/Action/Event)
         |
    Repository (Business Logic)
         |
    +----+----+----+
    |    |    |    |
  Disk  Network  SDK
   |      |      |
 Room  Retrofit  Bitwarden
  DB    APIs     Rust SDK
```

### Key Principles

1. **No Exceptions from Data Layer**: All suspending functions return `Result<T>` or custom sealed classes
2. **State Hoisting to ViewModel**: All state that affects behavior must live in the ViewModel's state
3. **Interface-Based DI**: All implementations use interface/`...Impl` pairs with Hilt injection
4. **Encryption by Default**: All sensitive data encrypted via SDK before storage

### Core Patterns

- **BaseViewModel**: Enforces UDF with State/Action/Event pattern. See `ui/src/main/kotlin/com/bitwarden/ui/platform/base/BaseViewModel.kt`.
- **Repository Result Pattern**: Type-safe error handling using custom sealed classes for discrete operations and `DataState<T>` wrapper for streaming data.
- **Common Patterns**: Flow collection via `Internal` actions, error handling via `when` branches, `DataState` streaming with `.map { }` and `.stateIn()`.

> For complete architecture patterns, code templates, and module organization, see `docs/ARCHITECTURE.md`.

---

## Development Guide

### Workflow Skills

> **Quick start**: Use `/plan-android-work <task>` to refine requirements and plan,
> then `/work-on-android <task>` for implementation.

**Planning Phase:**

1. `refining-android-requirements` - Gap analysis and structured spec from any input source
2. `planning-android-implementation` - Architecture design and phased task breakdown

**Implementation Phase:**

3. `implementing-android-code` - Patterns, gotchas, and templates for writing code
4. `testing-android-code` - Test patterns and templates for verifying code
5. `build-test-verify` - Build, test, lint, and deploy commands
6. `perform-android-preflight-checklist` - Quality gate before committing
7. `committing-android-changes` - Commit message format and pre-commit workflow
8. `reviewing-changes` - Code review checklists for MVVM/Compose patterns
9. `creating-android-pull-request` - PR creation workflow and templates

---

## Security Rules

**MANDATORY - These rules have no exceptions:**

1. **Zero-Knowledge Architecture**: Never transmit unencrypted vault data or master passwords to the server. All encryption happens client-side via the Bitwarden SDK.
2. **No Plaintext Key Storage**: Encryption keys must be stored using Android Keystore (biometric unlock) or encrypted with PIN/master password.
3. **Sensitive Data Cleanup**: On logout, all sensitive data must be cleared from memory and storage via `UserLogoutManager.logout()`.
4. **Input Validation**: Validate all user inputs before processing, especially URLs and credentials.
5. **SDK Isolation**: Use scoped SDK sources (`ScopedVaultSdkSource`) to prevent cross-user crypto context leakage.

---

## Code Style & Standards

- **Formatter**: Android Studio with `bitwarden-style.xml` | **Line Limit**: 100 chars | **Detekt**: Enabled
- **Naming**: `camelCase` (vars/fns), `PascalCase` (classes), `SCREAMING_SNAKE_CASE` (constants), `...Impl` (implementations)
- **KDoc**: Required for all public APIs
- **String Resources**: Add new strings to `:ui` module (`ui/src/main/res/values/strings.xml`). Use typographic quotes/apostrophes (`"` `"` `'`) not escaped ASCII (`\"` `\'`)

> For complete style rules (imports, formatting, documentation, Compose conventions), see `docs/STYLE_AND_BEST_PRACTICES.md`.

---

## Anti-Patterns

In addition to the Key Principles above, follow these rules:

### DO
- Use `remember(viewModel)` for lambdas passed to composables
- Map async results to internal actions before updating state
- Inject `Clock` for time-dependent operations
- Return early to reduce nesting

### DON'T
- Update state directly inside coroutines (use internal actions)
- Use `any` types or suppress null safety
- Catch generic `Exception` (catch specific types)
- Use `e.printStackTrace()` (use Timber logging)
- Create new patterns when established ones exist
- Skip KDoc for public APIs

---

## Quick Reference

- **Code style**: Full rules: `docs/STYLE_AND_BEST_PRACTICES.md`
- **Before writing code**: Use `implementing-android-code` skill for Bitwarden-specific patterns, gotchas, and templates
- **Before writing tests**: Use `testing-android-code` skill for test patterns and templates
- **Building/testing**: Use `build-test-verify` skill | App tests: `./gradlew app:testStandardDebugUnitTest`
- **Before committing**: Use `perform-android-preflight-checklist` skill, then `committing-android-changes` skill for message format
- **Code review**: Use `reviewing-changes` skill for MVVM/Compose review checklists
- **Creating PRs**: Use `creating-android-pull-request` skill for PR workflow and templates
- **Troubleshooting**: See `docs/TROUBLESHOOTING.md`
- **Architecture**: `docs/ARCHITECTURE.md` | [Bitwarden SDK](https://github.com/bitwarden/sdk) | [Jetpack Compose](https://developer.android.com/jetpack/compose) | [Hilt DI](https://dagger.dev/hilt/)
