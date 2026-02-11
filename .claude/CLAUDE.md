# Bitwarden Android - Claude Code Configuration

Official Android application for Bitwarden Password Manager and Bitwarden Authenticator, providing secure password management, two-factor authentication, and credential autofill services with zero-knowledge encryption.

## Overview

### What This Project Does
- Multi-module Android application providing secure password management and TOTP code generation
- Implements zero-knowledge architecture where encryption/decryption happens client-side
- Key entry points: `:app` (Password Manager), `:authenticator` (2FA TOTP generator)
- Target users: End-users via Google Play Store and F-Droid

### Key Concepts
- **Zero-Knowledge Architecture**: Server never has access to unencrypted vault data or encryption keys
- **Bitwarden SDK**: Rust-based cryptographic SDK handling all encryption/decryption operations
- **DataState**: Wrapper for streaming data states (Loading, Loaded, Pending, Error, NoNetwork)
- **Result Types**: Custom sealed classes for operation results (never throw exceptions from data layer)
- **UDF (Unidirectional Data Flow)**: State flows down, actions flow up through ViewModels

---

## Architecture & Patterns

### System Architecture

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

### Code Organization

```
android/
├── app/                    # Password Manager application
│   └── src/main/kotlin/com/x8bit/bitwarden/
│       ├── data/           # Repositories, managers, data sources
│       │   ├── auth/       # Authentication domain
│       │   ├── vault/      # Vault/cipher domain
│       │   ├── platform/   # Platform services
│       │   └── tools/      # Generator, export tools
│       └── ui/             # ViewModels, Screens, Navigation
│           ├── auth/       # Login, registration screens
│           ├── vault/      # Vault screens
│           └── platform/   # Settings, debug menu
├── authenticator/          # Authenticator 2FA application
├── core/                   # Shared utilities, dispatcher management
├── data/                   # Shared data layer (disk sources, models)
├── network/                # Network layer (Retrofit services, models)
├── ui/                     # Shared UI components, theming
├── authenticatorbridge/    # IPC bridge between apps
├── cxf/                    # Credential Exchange integration
└── annotation/             # Custom annotations for code generation
```

### Key Principles

1. **No Exceptions from Data Layer**: All suspending functions return `Result<T>` or custom sealed classes
2. **State Hoisting to ViewModel**: All state that affects behavior must live in the ViewModel's state
3. **Interface-Based DI**: All implementations use interface/`...Impl` pairs with Hilt injection
4. **Encryption by Default**: All sensitive data encrypted via SDK before storage

### Core Patterns

- **BaseViewModel**: Enforces UDF with State/Action/Event pattern. See `ui/src/main/kotlin/com/bitwarden/ui/platform/base/BaseViewModel.kt` and `docs/ARCHITECTURE.md` for full templates and usage examples.
- **Repository Result Pattern**: Type-safe error handling using custom sealed classes for discrete operations and `DataState<T>` wrapper for streaming data. See `docs/ARCHITECTURE.md` for implementation details.
- **Common Patterns**: Flow collection via `Internal` actions, error handling via `when` branches, `DataState` streaming with `.map { }` and `.stateIn()`.

> For complete architecture patterns, code templates, and examples, see `docs/ARCHITECTURE.md`.

---

## Development Guide

### Adding New Feature Screen

Follow these steps (see `docs/ARCHITECTURE.md` for full templates and patterns):

1. **Define State/Event/Action** - `@Parcelize` state, sealed event/action classes with `Internal` subclass
2. **Implement ViewModel** - Extend `BaseViewModel<S, E, A>`, persist state via `SavedStateHandle`, map Flow results to internal actions
3. **Implement Screen** - Stateless `@Composable`, use `EventsEffect` for navigation, `remember(viewModel)` for action lambdas
4. **Define Navigation** - `@Serializable` route, `NavGraphBuilder` extension with `composableWithSlideTransitions`, `NavController` extension
5. **Write Tests** - Use the `testing-android-code` skill for comprehensive test patterns and templates

### Code Reviews

Use the `reviewing-changes` skill for structured code review checklists covering MVVM/Compose patterns, security validation, and type-specific review guidance.

---

## Data Models

Key types used throughout the codebase (see source files and `docs/ARCHITECTURE.md` for full definitions):

- **`UserState`** (`data/auth/`) - Active user ID, accounts list, pending account state
- **`VaultUnlockData`** (`data/vault/`) - User ID and vault unlock status
- **`DataState<T>`** (`data/`) - Async data wrapper: Loading, Loaded, Pending, Error, NoNetwork
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

### Environment Configuration

| Variable | Required | Description |
|----------|----------|-------------|
| `GITHUB_TOKEN` | Yes (CI) | GitHub Packages authentication for SDK |
| Build flavors | - | `standard` (Play Store), `fdroid` (no Google services) |
| Build types | - | `debug`, `beta`, `release` |

### Authentication & Authorization

- **Login Methods**: Email/password, SSO (OAuth 2.0 + PKCE), trusted device, passwordless auth request
- **Vault Unlock**: Master password, PIN, biometric, trusted device key
- **Token Management**: JWT access tokens with automatic refresh via `AuthTokenManager`
- **Key Derivation**: PBKDF2-SHA256 or Argon2id via `KdfManager`

---

## Testing

### Test Structure

```
app/src/test/                    # App unit tests
app/src/testFixtures/            # App test utilities
core/src/testFixtures/           # Core test utilities (FakeDispatcherManager)
data/src/testFixtures/           # Data test utilities (FakeSharedPreferences)
network/src/testFixtures/        # Network test utilities (BaseServiceTest)
ui/src/testFixtures/             # UI test utilities (BaseViewModelTest, BaseComposeTest)
```

### Running Tests

```bash
./gradlew test                    # Run all unit tests
./gradlew app:testDebugUnitTest   # Run app module tests
./gradlew :core:test              # Run core module tests
./fastlane check                  # Run full validation (detekt, lint, tests, coverage)
```

### Test Quick Reference

- **Dispatcher Control**: `FakeDispatcherManager` from `:core:testFixtures`
- **MockK**: `mockk<T> { every { } returns }`, `coEvery { }` for suspend
- **Flow Testing**: Turbine with `stateEventFlow()` helper from `BaseViewModelTest`
- **Time Control**: Inject `Clock` for deterministic time testing

> For comprehensive test templates (ViewModel, Screen, Repository, DataSource, Network), use the `testing-android-code` skill.

---

## Code Style & Standards

- **Formatter**: Android Studio with `bitwarden-style.xml` | **Line Limit**: 100 chars | **Detekt**: Enabled
- **Naming**: `camelCase` (vars/fns), `PascalCase` (classes), `SCREAMING_SNAKE_CASE` (constants), `...Impl` (implementations)
- **KDoc**: Required for all public APIs

> For complete style rules (imports, formatting, documentation, Compose conventions), see `docs/STYLE_AND_BEST_PRACTICES.md`.

---

## Anti-Patterns

### DO
- Use `Result<T>` or sealed classes for operations that can fail
- Hoist state to ViewModel when it affects business logic
- Use `remember(viewModel)` for lambdas passed to composables
- Map async results to internal actions before updating state
- Use interface-based DI with Hilt
- Inject `Clock` for time-dependent operations
- Return early to reduce nesting

### DON'T
- Throw exceptions from data layer functions
- Update state directly inside coroutines (use internal actions)
- Use `any` types or suppress null safety
- Catch generic `Exception` (catch specific types)
- Use `e.printStackTrace()` (use Timber logging)
- Create new patterns when established ones exist
- Skip KDoc for public APIs

---

## Deployment

### Building

```bash
# Debug builds
./gradlew app:assembleDebug
./gradlew authenticator:assembleDebug

# Release builds (requires signing keys)
./gradlew app:assembleStandardRelease
./gradlew app:bundleStandardRelease

# F-Droid builds
./gradlew app:assembleFdroidRelease
```

### Versioning

**Location**: `gradle/libs.versions.toml`
```toml
appVersionCode = "1"
appVersionName = "2025.11.1"
```

Follow semantic versioning pattern: `YEAR.RELEASE.PATCH`

### Publishing

- **Play Store**: Via GitHub Actions workflow with signed AAB
- **F-Droid**: Via dedicated workflow with F-Droid signing keys
- **Firebase App Distribution**: For beta testing

---

## Troubleshooting

### Common Issues

#### Build fails with SDK dependency error

**Problem**: Cannot resolve Bitwarden SDK from GitHub Packages

**Solution**:
1. Ensure `GITHUB_TOKEN` is set in `ci.properties` or environment
2. Verify token has `read:packages` scope
3. Check network connectivity to `maven.pkg.github.com`

#### Tests fail with dispatcher issues

**Problem**: Tests hang or fail with "Module with Main dispatcher had failed to initialize"

**Solution**:
1. Extend `BaseViewModelTest` for ViewModel tests
2. Use `@RegisterExtension val mainDispatcherExtension = MainDispatcherExtension()`
3. Ensure `runTest { }` wraps test body

#### Compose preview not rendering

**Problem**: @Preview functions show "Rendering problem"

**Solution**:
1. Check for missing theme wrapper: `BitwardenTheme { YourComposable() }`
2. Verify no ViewModel dependency in preview (use state-based preview)
3. Clean and rebuild project

#### ProGuard/R8 stripping required classes

**Problem**: Release build crashes with missing class errors

**Solution**:
1. Add keep rules to `proguard-rules.pro`
2. Check `consumer-rules.pro` in library modules
3. Verify kotlinx.serialization rules are present

### Debug Tips

- **Timber Logging**: Enabled in debug builds, check Logcat with tag filter
- **Debug Menu**: Available in debug builds via Settings > About > Debug Menu
- **Network Inspector**: Use Android Studio Network Profiler or Charles Proxy
- **SDK Debugging**: Check `BaseSdkSource` for wrapped exceptions

---

## References

### Internal Documentation
- `docs/ARCHITECTURE.md` - Complete architecture patterns, BaseViewModel, Repository Result, DataState
- `docs/STYLE_AND_BEST_PRACTICES.md` - Kotlin and Compose code style, formatting, imports, documentation

### Skills & Tools
- `testing-android-code` - Comprehensive test templates and patterns (ViewModel, Screen, Repository, DataSource, Network)
- `reviewing-changes` - Structured code review checklists with MVVM/Compose pattern validation
- `bitwarden-code-review:code-review` - Automated GitHub PR review with inline comments
- `bitwarden-code-review:code-review-local` - Local change review written to files

### External Documentation
- [Bitwarden SDK](https://github.com/bitwarden/sdk) - Cryptographic SDK
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - UI framework
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-guide.html) - Async programming
- [Hilt DI](https://dagger.dev/hilt/) - Dependency injection
- [Turbine](https://github.com/cashapp/turbine) - Flow testing

### Tools & Libraries
- [MockK](https://mockk.io/) - Kotlin mocking library
- [Retrofit](https://square.github.io/retrofit/) - HTTP client
- [Room](https://developer.android.com/training/data-storage/room) - Database
- [Detekt](https://detekt.dev/) - Static analysis
