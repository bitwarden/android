---
name: build-test-verify
description: Build, test, lint, and deploy commands for the Bitwarden Android project. Use when running tests, building APKs/AABs, running lint/detekt, deploying, using fastlane, or discovering codebase structure. Triggered by "run tests", "build", "gradle", "lint", "detekt", "deploy", "fastlane", "assemble", "verify", "coverage".
---

# Build, Test & Verify

## Environment Setup

| Variable | Required | Description |
|----------|----------|-------------|
| `GITHUB_TOKEN` | Yes (CI) | GitHub Packages auth for SDK (`read:packages` scope) |
| Build flavors | - | `standard` (Play Store), `fdroid` (no Google services) |
| Build types | - | `debug`, `beta`, `release` |

If builds fail resolving the Bitwarden SDK, verify `GITHUB_TOKEN` in `ci.properties` or environment and check connectivity to `maven.pkg.github.com`.

---

## Building

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

---

## Running Tests

**IMPORTANT**: The app module uses the `standard` flavor. Always use `testStandardDebugUnitTest`, NOT `testDebugUnitTest`.

```bash
# App module tests (correct flavor!)
./gradlew app:testStandardDebugUnitTest

# Run all unit tests across all modules
./gradlew test

# Individual shared modules (no flavor needed)
./gradlew :core:test
./gradlew :data:test
./gradlew :network:test
./gradlew :ui:test

# Authenticator module
./gradlew authenticator:testStandardDebugUnitTest
```

### Test Structure

```
app/src/test/                    # App unit tests
app/src/testFixtures/            # App test utilities
core/src/testFixtures/           # Core test utilities (FakeDispatcherManager)
data/src/testFixtures/           # Data test utilities (FakeSharedPreferences)
network/src/testFixtures/        # Network test utilities (BaseServiceTest)
ui/src/testFixtures/             # UI test utilities (BaseViewModelTest, BaseComposeTest)
```

### Test Quick Reference

- **Dispatcher Control**: `FakeDispatcherManager` from `:core:testFixtures`
- **MockK**: `mockk<T> { every { } returns }`, `coEvery { }` for suspend
- **Flow Testing**: Turbine with `stateEventFlow()` helper from `BaseViewModelTest`
- **Time Control**: Inject `Clock` for deterministic time testing

---

## Lint & Static Analysis

```bash
# Detekt (static analysis)
./gradlew detekt

# Android Lint
./gradlew lint

# Full validation suite (detekt + lint + tests + coverage)
./fastlane check
```

---

## Codebase Discovery

```bash
# Find existing Bitwarden UI components
find ui/src/main/kotlin/com/bitwarden/ui/platform/components/ -name "Bitwarden*.kt" | sort

# Find all ViewModels
grep -rl "BaseViewModel<" app/src/main/kotlin/ --include="*.kt"

# Find all Navigation files with @Serializable routes
find app/src/main/kotlin/ -name "*Navigation.kt" | sort

# Find all Hilt modules
find app/src/main/kotlin/ -name "*Module.kt" -path "*/di/*" | sort

# Find all repository interfaces
find app/src/main/kotlin/ -name "*Repository.kt" -not -name "*Impl.kt" -path "*/repository/*" | sort

# Find encrypted disk source examples
grep -rl "EncryptedPreferences" app/src/main/kotlin/ --include="*.kt"

# Find Clock injection usage
grep -rl "private val clock: Clock" app/src/main/kotlin/ --include="*.kt"

# Search existing strings before adding new ones
grep -n "search_term" ui/src/main/res/values/strings.xml
```

---

## Deployment & Versioning

**Version location**: `gradle/libs.versions.toml`
```toml
appVersionCode = "1"
appVersionName = "2025.11.1"
```
Pattern: `YEAR.MONTH.PATCH`

**Publishing channels**:
- **Play Store**: GitHub Actions workflow with signed AAB
- **F-Droid**: Dedicated workflow with F-Droid signing keys
- **Firebase App Distribution**: Beta testing
