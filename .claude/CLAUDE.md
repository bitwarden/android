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

### Adding New Feature Screen

Use the `implementing-android-code` skill for Bitwarden-specific patterns, gotchas, and templates. Steps:

1. **Define State/Event/Action** - `@Parcelize` state, sealed event/action classes with `Internal` subclass
2. **Implement ViewModel** - Extend `BaseViewModel<S, E, A>`, persist state via `SavedStateHandle`
3. **Implement Screen** - Stateless `@Composable`, use `EventsEffect` for navigation
4. **Define Navigation** - `@Serializable` route, `NavGraphBuilder`/`NavController` extensions
5. **Write Tests** - Use the `testing-android-code` skill for test patterns and templates

### Other Skills

- `reviewing-changes` - Code review checklists for MVVM/Compose patterns
- `build-test-verify` - Build, test, lint, deploy commands and codebase discovery
- `git-commit` - Commit message format and pre-commit workflow
- `create-pull-request` - PR creation workflow and templates
- `core-conventions` - Code style, naming, anti-patterns quick reference
- `self-review-checklist` - Quality gate before committing or opening a PR

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
- **Building/testing**: Use `build-test-verify` skill | App tests: `./gradlew app:testStandardDebugUnitTest`
- **Before writing code**: Use `implementing-android-code` skill for Bitwarden-specific patterns, gotchas, and templates
- **Before writing tests**: Use `testing-android-code` skill for test patterns and templates
- **Troubleshooting**: See `docs/TROUBLESHOOTING.md`
- **Architecture**: `docs/ARCHITECTURE.md` | [Bitwarden SDK](https://github.com/bitwarden/sdk) | [Jetpack Compose](https://developer.android.com/jetpack/compose) | [Hilt DI](https://dagger.dev/hilt/)
