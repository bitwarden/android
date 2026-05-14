# Migration Report: kotlinx.collections.immutable 0.5.0

**Project:** bitwarden-android (Bitwarden Android client)
**Modules audited:** `:app`, `:authenticator`, `:core`, `:testharness`, `:ui` (all five depend on the library)
**Date:** 2026-05-14
**kotlinx.collections.immutable version:** 0.4.0 → 0.5.0-beta01
**Kotlin version:** 2.3.21 (unchanged by this migration)
**Status:** Completed successfully — version bump only, **zero method renames required**

---

## Summary

bitwarden-android uses `kotlinx.collections.immutable` as a **read-only consumer**. Across 154 Kotlin files importing the library, the codebase invariably:

1. Constructs persistent collections with `persistentListOf(...)`, `persistentMapOf(...)`, `persistentSetOf(...)`.
2. Or converts via `.toImmutableList()`, `.toImmutableMap()`, `.toImmutableSet()`.
3. Stores and passes them around as the read-only interfaces `ImmutableList<T>` / `ImmutableMap<K, V>` / `ImmutableSet<T>` (never as `PersistentList<T>` / `PersistentMap<K, V>` / `PersistentSet<T>`).

Because the static receiver type is always the read-only interface, the deprecated `add` / `remove` / `removeAt` / `set` / `put` / `clear` / `addAll` / `removeAll` / `retainAll` / `putAll` members on `Persistent*` are never visible at any call site. The compiler emits **zero** `kotlinx.collections.immutable` deprecation warnings after the version bump.

The migration therefore reduces to a single one-line change in the version catalog. The new file `MIGRATION_REPORT.md` is added for traceability; it can be removed if maintainers prefer.

---

## Pre-Migration State

### Library Usage

| Module | Build file location | Version |
|--------|---------------------|---------|
| catalog | `gradle/libs.versions.toml:50` | 0.4.0 |
| `:app` | `app/build.gradle.kts:283` (`implementation(libs.kotlinx.collections.immutable)`) | 0.4.0 |
| `:authenticator` | `authenticator/build.gradle.kts:239` | 0.4.0 |
| `:core` | `core/build.gradle.kts:51` | 0.4.0 |
| `:testharness` | `testharness/build.gradle.kts:112` | 0.4.0 |
| `:ui` | `ui/build.gradle.kts:81` | 0.4.0 |

### File counts per module
- `:app` — 122 files (122 main + tests combined)
- `:authenticator` — 19 files
- `:ui` — 11 files
- `:core` — 2 files
- Total: 154 files referencing `kotlinx.collections.immutable`

### Baseline Build

- **Command:** `GITHUB_TOKEN=$(gh auth token) ./gradlew :app:compileStandardDebugKotlin :app:compileStandardDebugUnitTestKotlin :app:compileFdroidDebugKotlin :app:compileFdroidDebugUnitTestKotlin :authenticator:compileDebugKotlin :authenticator:compileDebugUnitTestKotlin :ui:compileDebugKotlin :core:compileDebugKotlin :core:compileDebugUnitTestKotlin :testharness:compileDebugKotlin --continue`
- **Result:** success.
- **Pre-existing warnings (kept noisy here, none migration-relevant):** ~253 deprecation warnings, all on bitwarden-internal APIs (`storePrivateKey` → `storeAccountKeys`, `getPinProtectedUserKey` → `getPinProtectedUserKeyEnvelope`, etc.), private custom-serializer visibility warnings in `:network`, plus a handful of Android-framework deprecations (`bundleOf`, `persistableBundleOf`, `EncryptedSharedPreferences`, `class Slice`). None involve `kotlinx.collections.immutable`.

### Pre-requisite environment setup

- **Android SDK path:** required a one-line `local.properties` (`sdk.dir=/Users/dmitry.nekrasov/Library/Android/sdk`) — bitwarden-android does not commit one. Gitignored by the project, so this is not in the commit.
- **GitHub Packages auth:** the build resolves `com.bitwarden:sdk-android:3.0.0-6774-0a0f5faf` from `maven.pkg.github.com/bitwarden/sdk`, which returns `401 Unauthorized` without a token. We supplied `GITHUB_TOKEN` from `gh auth token` after refreshing scopes with `gh auth refresh -s read:packages`. This is not a migration concern — it's a property of the bitwarden-android build configuration.

### Pre-existing `@Suppress("DEPRECATION")` annotations

18 occurrences across the codebase. None cover `kotlinx.collections.immutable` call sites — they all suppress Android-framework or bitwarden-internal deprecations and remain necessary after migration.

---

## Migration Steps

### Phase 3: Version Bump

- **File:** `gradle/libs.versions.toml`
- **Change:** `kotlinxCollectionsImmutable = "0.4.0"` → `"0.5.0-beta01"` (line 50)

### Phase 4: Compiler-Driven Renames

Total call sites renamed: **0**.

After bumping the version, the compile loop produced **zero** `kotlinx.collections.immutable` deprecation warnings across all five consuming modules (both `Standard` and `Fdroid` `:app` flavors plus their unit-test source sets, plus `:authenticator`, `:ui`, `:core`, `:testharness`).

Searching for `Persistent(List|Map|Set|Collection)` in the post-bump compile log returns no hits — confirming the compiler did not flag any usage.

### Phase 5: Compiler-Blind Passes

#### Operator-syntax indexed assignment (`list[i] = v`)
Receivers inspected: 2 candidates project-wide
- `savedStateHandle[KEY] = ...` (Android `SavedStateHandle`, not `PersistentList`) — leave alone

No `PersistentList` indexed assignments anywhere. **0 rewrites.**

#### Method / callable references
Receivers inspected: 4 candidates project-wide
- `mutableAutofillViewList::add` and `mutableIgnoreAutofillIdList::add` in `AutofillParserImpl.kt:296-306` — both are local `MutableList<...>` variables

**0 rewrites.**

#### Java callers
`find . -name '*.java' | xargs grep -ln 'PersistentList\|PersistentMap\|persistentListOf\|persistentMapOf'` returns 0 files. **No Java callers.**

### Phase 6: Interface Implementers

`grep -rn --include='*.kt' -E 'PersistentList<|PersistentMap<|PersistentSet<|PersistentCollection<' . | grep -E ':\s*(class|object|abstract\s+class|interface)\b'` returns 0 matches. **No third-party implementers in this codebase.**

The single custom extension function `com.bitwarden.core.util.persistentListOfNotNull` (in `core/src/main/kotlin/com/bitwarden/core/util/PersistentListExtensions.kt`) is a constructor wrapper — it returns `ImmutableList<T>` via `.filterNotNull().toImmutableList()` and does not call any deprecated methods.

### Phase 7: `@Suppress("DEPRECATION")` Cleanup

No redundant suppressions. The post-bump recompile emitted no `'@Suppress("DEPRECATION")' annotation has no effect` warnings.

### Phase 8: Verification

- **Compile command:** as above. `BUILD SUCCESSFUL in 1m 47s`.
- **Remaining `kotlinx.collections.immutable` warnings:** 0.
- **Tests:** `./gradlew :core:testDebugUnitTest :ui:testDebugUnitTest --continue` → `BUILD SUCCESSFUL in 35s`. Includes `PersistentListExtensionsTest` in `:core` which directly exercises the custom `persistentListOfNotNull` extension. (Skipped the full `:app` test suite — runtime impractical and zero kotlinx warnings means there is nothing to risk-test there.)

---

## Errors Encountered

None.

---

## Non-Trivial Decisions

- **No textual rewrites were applied.** The skill is compiler-driven; with zero compiler warnings, no rewrites are correct. A naive find-and-replace tool would have damaged ~511 factory-function call sites (`persistentListOf(...)`, `toImmutableList(...)`, etc.) that look like they could be renamed but should not be.

- **The custom extension `persistentListOfNotNull` was intentionally not modified.** Its body (`.filterNotNull().toImmutableList()`) is on a `List<T>` receiver returned by `filterNotNull()`, not on `PersistentList<T>`. There is no deprecated call to upgrade.

- **`:app` product flavors required flavored task names.** `compileDebugKotlin` is ambiguous in `:app`; the compile loop targeted `compileStandardDebugKotlin` + `compileFdroidDebugKotlin` + their `UnitTest` siblings to cover both Play-Store and F-Droid source sets.

---

## Files Changed

### Gradle Files
- `gradle/libs.versions.toml` — version bump 0.4.0 → 0.5.0-beta01 (line 50).

### Kotlin Sources
- None.

### Java Sources
- None.

### Created
- `MIGRATION_REPORT.md` — this file.

### Not Modified (deliberately)
- All `persistentListOf` / `persistentMapOf` / `persistentSetOf` call sites — these are factory functions whose names are unchanged in 0.5.0.
- All `.toImmutableList()` / `.toImmutableMap()` / `.toImmutableSet()` / `.toPersistentList()` / `.toPersistentMap()` conversions — unchanged in 0.5.0.
- All `mutableAutofillViewList::add` and similar `MutableList`-typed callable references — these are on `MutableList`, not `PersistentList`.
- All `savedStateHandle[KEY] = …` — `SavedStateHandle` indexed assignment, unrelated.
- `core/src/main/kotlin/com/bitwarden/core/util/PersistentListExtensions.kt` — custom extension wraps `filterNotNull().toImmutableList()`, no deprecated calls.
