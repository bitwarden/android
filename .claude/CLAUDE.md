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

### Code Reviews

Use the `reviewing-changes` skill for structured code review checklists covering MVVM/Compose patterns, security validation, and type-specific review guidance.

---

## Data Models

Key types used throughout the codebase:

- **`UserState`** (`data/auth/`) - Active user ID, accounts list, pending account state
- **`VaultUnlockData`** (`data/vault/repository/model/`) - User ID and vault unlock status
- **`NetworkResult<T>`** (`network/`) - HTTP operation result: Success or Failure
- **`BitwardenError`** (`network/`) - Error classification: Http, Network, Other

---

## Security & Configuration

### Security Rules

**MANDATORY - These rules have no exceptions:**

1. **Zero-Knowledge Architecture**: Never transmit unencrypted vault data or master passwords to the server. All encryption happens client-side via the Bitwarden SDK.

2. **No Plaintext Key Storage**: Encryption keys must be stored using Android Keystore (biometric unlock) or encrypted with PIN/master password.

3. **Sensitive Data Cleanup**: On logout, all sensitive data must be cleared from memory and storage via `UserLogoutManager.logout()`.

4. **Input Validation**: Validate all user inputs before processing, especially URLs and credentials.

5. **SDK Isolation**: Use scoped SDK sources (`ScopedVaultSdkSource`) to prevent cross-user crypto context leakage.

### Security Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `BiometricsEncryptionManager` | `data/platform/manager/` | Android Keystore integration for biometric unlock |
| `VaultLockManager` | `data/vault/manager/` | Vault lock/unlock operations |
| `AuthDiskSource` | `data/auth/datasource/disk/` | Secure token and key storage |
| `BaseEncryptedDiskSource` | `data/datasource/disk/` | EncryptedSharedPreferences base class |

---

## Quick Reference

- **Code style**: Use `core-conventions` skill | Full rules: `docs/STYLE_AND_BEST_PRACTICES.md`
- **Building/testing**: Use `build-test-verify` skill | App tests: `./gradlew app:testStandardDebugUnitTest`
- **Troubleshooting**: See `docs/TROUBLESHOOTING.md`
- **Architecture**: `docs/ARCHITECTURE.md` | [Bitwarden SDK](https://github.com/bitwarden/sdk) | [Jetpack Compose](https://developer.android.com/jetpack/compose) | [Hilt DI](https://dagger.dev/hilt/)
