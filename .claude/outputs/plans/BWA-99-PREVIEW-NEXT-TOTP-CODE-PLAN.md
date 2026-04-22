# Preview Next TOTP Code — Design Document

**Feature**: Add a user-controlled setting that always displays the upcoming TOTP code below the current code in the authenticator item list.
**Date**: 2026-04-22
**Status**: Ready for Implementation
**Jira**: BWA-99
**Sources**:
- [BWA-99](https://bitwarden.atlassian.net/browse/BWA-99) — Parent story
- [BWA-249](https://bitwarden.atlassian.net/browse/BWA-249) — QA Test Cases subtask
- Figma design node `4081-6476` (Bitwarden Authenticator Phase 1)
- Ente Auth design reference (attached to BWA-99)

---

## Requirements Specification

### Overview

This feature adds a user-controlled toggle in Settings that, when enabled, always displays the next TOTP code directly below the current code for each item in the authenticator list. The goal is to let users see the upcoming code before the current one expires, avoiding the need to wait through the countdown. The feature is purely additive and scoped to the `:authenticator` module. HOTP (counter-based) items are out of scope and receive no next-code display.

### Functional Requirements

| ID | Requirement | Source | Notes |
|----|-------------|--------|-------|
| FR1 | Users can toggle "show next TOTP code" on or off via a setting | BWA-99 / user | Setting persists across sessions |
| FR2 | When the setting is **enabled**, the next TOTP code is displayed below the current code for each TOTP item in the list | BWA-99 / user | Always visible — not threshold-triggered |
| FR3 | When the setting is **disabled**, no next code is shown (list items appear as today) | BWA-99 / user | Default state is off |
| FR4 | HOTP (counter-based) items never show a next code, regardless of the setting | User | Next code for HOTP requires advancing the counter — out of scope |
| FR5 | The next code is computed as the TOTP code valid at `issueTime + periodSeconds` | BWA-99 / codebase | Pure computation, no counter mutation |
| FR6 | The next code display uses the same formatting as the current code (spaces every 3 characters) | Codebase convention | Consistency with current code rendering |
| FR7 | If next code generation fails (SDK error), the next code is silently omitted — no error shown | Default | TOTP math failure is extremely unlikely |

### Technical Requirements

| ID | Requirement | Source | Notes |
|----|-------------|--------|-------|
| TR1 | Module scope: `:authenticator` only | Codebase | No changes to `:app` or shared modules |
| TR2 | New boolean user setting `showNextTotpCode` persisted via SharedPreferences (`SettingsDiskSource`) | User | Default: `false` |
| TR3 | `TotpCodeManagerImpl` must generate a `nextCode: String?` for TOTP items using `generateTotp(uri, issueTime + periodSeconds)` | Codebase | `null` for HOTP items |
| TR4 | `VerificationCodeItem` data class must gain a `nextCode: String?` field | Codebase | `null` when HOTP or SDK call fails |
| TR5 | `VerificationCodeDisplayItem` UI model must gain a `nextAuthCode: String?` field | Codebase | `null` when setting is off or item is HOTP |
| TR6 | `VaultVerificationCodeItem` composable renders next code when `nextAuthCode != null` | Codebase | No label — code value only |
| TR7 | Settings screen gains a new toggle row for this feature | User | Existing `BitwardenSwitch` pattern |
| TR8 | No feature flag required | User | — |
| TR9 | Setting change must reactively update the item list display without requiring app restart | User | Setting observed as `Flow<Boolean>` combined in `ItemListingViewModel` |
| TR10 | F-Droid compatible — no Play Services dependency | Codebase | Pure local computation |

### Security Requirements

| ID | Requirement | Source | Notes |
|----|-------------|--------|-------|
| SR1 | Next TOTP code has the same sensitivity level as the current code — displayed in plaintext in the list view | Codebase | Already accepted pattern for current code |
| SR2 | No new storage encryption required — next code is computed on demand, never persisted | Codebase | — |
| SR3 | Setting value (`showNextTotpCode`) is non-sensitive — no encryption needed | User | SharedPreferences plaintext is acceptable |

### UX Requirements

| ID | Requirement | Source | Notes |
|----|-------------|--------|-------|
| UX1 | The next code appears inline below the current code within the list item composable | User / Figma | Figma design (node 4081-6476) is the layout authority |
| UX2 | No label prefix for the next code — code value only | User | Distinguishable by position alone |
| UX3 | Accessibility content description: `"Next code, [code]"` (e.g. `"Next code, 123 456"`) | User | Follows TalkBack pattern of current code |
| UX4 | Settings toggle: label `"Show next code"`, sublabel `"See incoming codes in the list"` | User | In Settings screen |
| UX5 | No analytics events for this feature | Default | Feature is passive display, no new user action |

### String Resources (`:ui` module `strings.xml`)

```xml
<string name="show_next_code">Show next code</string>
<string name="see_incoming_codes_in_the_list">See incoming codes in the list</string>
```

### Open Items

| ID | Question | Assumed Default | Category |
|----|----------|----------------|----------|
| G5 | Exact Settings section to place the toggle | "Other" or existing display group — follow `SettingsScreen.kt` structure at time of implementation | UX |

---

## Implementation Plan

### Change Classification

**Enhancement** — Extending an existing feature. No new screens or navigation. All changes are additive modifications to existing files.

### Architecture

```
┌─────────────────────────────┐
│       SettingsScreen        │  ← New BitwardenSwitch: "Show next code"
└─────────────┬───────────────┘
              │ ShowNextTotpCodeToggle action
┌─────────────▼───────────────┐
│      SettingsViewModel      │  ← Handles toggle, updates state + repository
└─────────────┬───────────────┘
              │ write: settingsRepository.showNextTotpCode
┌─────────────▼───────────────┐
│      SettingsRepository     │◄──────────────────────────────────────────────┐
│  + showNextTotpCode: Boolean │                                               │
│  + showNextTotpCodeStateFlow │                                               │ read: Flow<Boolean>
└─────────────┬───────────────┘                                               │
              │ write                                                          │
┌─────────────▼───────────────┐        ┌──────────────────────────────┐      │
│     SettingsDiskSource      │        │   ItemListingViewModel        │──────┘
│  + getShowNextTotpCode()    │        │ observes showNextTotpCodeFlow │
│  + storeShowNextTotpCode()  │        └──────────────┬───────────────┘
│  + getShowNextTotpCodeFlow()│                       │ toDisplayItem(showNextCode = ...)
└─────────────────────────────┘        ┌──────────────▼───────────────┐
                                        │ VerificationCodeItemExtensions│
                                        │ toDisplayItem(..., showNextCode)
                                        └──────────────┬───────────────┘
                                                       │
                              ┌────────────────────────▼───────────────────────┐
                              │            TotpCodeManagerImpl                  │
                              │  On code generation: also call generateTotp(    │
                              │    uri, issueTime + periodSeconds               │
                              │  ) → VerificationCodeItem(nextCode = ...)       │
                              └────────────────────────┬───────────────────────┘
                                                       │
                              ┌────────────────────────▼───────────────────────┐
                              │           AuthenticatorSdkSource               │
                              │   generateTotp(uri, time: Instant)             │
                              └────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│           VaultVerificationCodeItem                  │
│   renders nextAuthCode when non-null (below authCode)│
└─────────────────────────────────────────────────────┘
```

### Design Decisions

| Decision | Resolution | Rationale |
|----------|-----------|-----------|
| Where to apply the setting | `toDisplayItem()` receives `showNextCode: Boolean`; `nextAuthCode` is `null` when off | Manager stays pure; ViewModel combines the setting flow with code flow |
| When to generate next code | Only during code regeneration (on expiry), not on every 1s tick | Next code doesn't change until current code expires; avoids redundant SDK calls |
| HOTP exclusion mechanism | Check `item.otpUri.contains("otpauth://hotp/", ignoreCase = true)` → `nextCode = null` | URI is the source of truth; no explicit type field on `AuthenticatorItem` |
| `showNextTotpCode` in ViewModel | Added to `ItemListingState` top-level, observed via `settingsRepository.showNextTotpCodeStateFlow` | Reactive — setting change auto-rebuilds list items without restart |
| Storage layer | SharedPreferences via `SettingsDiskSource` | Consistent with every other boolean setting in the authenticator |

### Pattern Anchors

1. `authenticator/.../datasource/disk/SettingsDiskSourceImpl.kt` — SharedPreferences boolean pattern (`getBoolean` / `putBoolean` / Flow)
2. `authenticator/.../repository/SettingsRepositoryImpl.kt` — `StateFlow<Boolean>` wrapping disk source with `unconfinedScope`
3. `authenticator/.../feature/settings/SettingsViewModel.kt` — toggle action handling and `mutableStateFlow.update { it.copy(...) }` pattern

### File Inventory

#### Files to Modify

| File Path | Change Description | Risk |
|-----------|-------------------|------|
| `authenticator/.../datasource/disk/SettingsDiskSource.kt` | Add `getShowNextTotpCode()`, `storeShowNextTotpCode()`, `getShowNextTotpCodeFlow()` | Low |
| `authenticator/.../datasource/disk/SettingsDiskSourceImpl.kt` | Implement with SharedPreferences key `"showNextTotpCode"` | Low |
| `authenticator/.../repository/SettingsRepository.kt` | Add `var showNextTotpCode: Boolean` + `showNextTotpCodeStateFlow: StateFlow<Boolean>` | Low |
| `authenticator/.../repository/SettingsRepositoryImpl.kt` | Implement with disk source + `stateIn(unconfinedScope, SharingStarted.Lazily)` | Low |
| `authenticator/.../manager/model/VerificationCodeItem.kt` | Add `val nextCode: String? = null` | Medium |
| `authenticator/.../components/listitem/model/VerificationCodeDisplayItem.kt` | Add `val nextAuthCode: String? = null` | Medium |
| `authenticator/.../manager/TotpCodeManagerImpl.kt` | On code regeneration: call `generateTotp(uri, nextPeriodInstant)` for non-HOTP items | Medium |
| `authenticator/.../feature/util/VerificationCodeItemExtensions.kt` | Add `showNextCode: Boolean` param; map `nextCode` → `nextAuthCode` conditionally | Medium |
| `authenticator/.../feature/itemlisting/ItemListingViewModel.kt` | Observe `showNextTotpCodeStateFlow`; add to `ItemListingState`; pass to `toDisplayItem()` | Medium |
| `authenticator/.../feature/settings/SettingsViewModel.kt` | Add `ShowNextTotpCodeToggle` action, `showNextTotpCode` to state, handler | Low |
| `authenticator/.../feature/settings/SettingsScreen.kt` | Add `BitwardenSwitch` with label/sublabel | Low |
| `authenticator/.../components/listitem/VaultVerificationCodeItem.kt` | Add `nextAuthCode: String?` param; render below `authCode` when non-null | Low |
| `ui/.../res/values/strings.xml` | Add `show_next_code`, `see_incoming_codes_in_the_list` | Low |
| Test fakes (`FakeSettingsDiskSource`, `FakeSettingsRepository`, `FakeTotpCodeManager`) | Add new property stubs | Low |
| Existing test files for each modified class | Update for new parameters/fields | Low |

### Implementation Phases

#### Phase 1: Setting Foundation

**Goal**: Persist and expose `showNextTotpCode` through all layers (disk → repository).

**Files**: `SettingsDiskSource.kt`, `SettingsDiskSourceImpl.kt`, `SettingsRepository.kt`, `SettingsRepositoryImpl.kt`, `FakeSettingsDiskSource.kt`, `FakeSettingsRepository.kt`

**Tasks**:
1. Add `getShowNextTotpCode(): Boolean?`, `storeShowNextTotpCode(value: Boolean?)`, `getShowNextTotpCodeFlow(): Flow<Boolean?>` to `SettingsDiskSource` interface
2. Implement in `SettingsDiskSourceImpl` using SharedPreferences key `"showNextTotpCode"`
3. Add `var showNextTotpCode: Boolean` and `val showNextTotpCodeStateFlow: StateFlow<Boolean>` to `SettingsRepository`
4. Implement in `SettingsRepositoryImpl` — getter defaults to `false`, StateFlow uses `unconfinedScope` + `SharingStarted.Lazily`
5. Update fakes with matching stubs

**Verification**:
```bash
./gradlew authenticator:testStandardDebugUnitTest --tests "*SettingsDiskSource*"
./gradlew authenticator:testStandardDebugUnitTest --tests "*SettingsRepository*"
```

---

#### Phase 2: Data Model Extension

**Goal**: Add `nextCode` / `nextAuthCode` fields to the data and UI model layers.

**Files**: `VerificationCodeItem.kt`, `VerificationCodeDisplayItem.kt`

**Tasks**:
1. Add `val nextCode: String? = null` to `VerificationCodeItem`
2. Add `val nextAuthCode: String? = null` to `VerificationCodeDisplayItem` (verify `@Parcelize` compiles)
3. Audit all `VerificationCodeItem(...)` constructors in test files; add `nextCode = null` where needed

**Verification**:
```bash
./gradlew authenticator:compileStandardDebugUnitTestKotlin
```

---

#### Phase 3: TOTP Manager — Next Code Generation

**Goal**: `TotpCodeManagerImpl` generates next code for TOTP items during code refresh.

**Files**: `TotpCodeManagerImpl.kt`, `TotpCodeManagerTest.kt`

**Tasks**:
1. In the code-regeneration branch (when `isExpired`), after a successful `generateTotp` result:
   - TOTP check: `!item.otpUri.contains("otpauth://hotp/", ignoreCase = true)`
   - If TOTP: call `generateTotp(item.otpUri, Instant.ofEpochMilli(clock.millis() + response.period * 1000L))`
   - Assign `nextCode = nextResponse.code` on success, `null` on failure or HOTP
2. In the time-update-only branch, preserve existing `nextCode` via `.copy(timeLeftSeconds = ..., nextCode = verificationCodeItem.nextCode)`
3. Write tests: TOTP item → non-null `nextCode`; HOTP item → null `nextCode`; SDK failure on next code → null `nextCode`

**Verification**:
```bash
./gradlew authenticator:testStandardDebugUnitTest --tests "*TotpCodeManager*"
```

---

#### Phase 4: Conversion & ViewModel Wiring

**Goal**: Thread the setting and `nextCode` from repository through ViewModel to display items.

**Files**: `VerificationCodeItemExtensions.kt`, `ItemListingViewModel.kt`, `ItemListingViewModelTest.kt`

**Tasks**:
1. Add `showNextCode: Boolean` parameter to `toDisplayItem()`; set `nextAuthCode = if (showNextCode) nextCode else null`
2. Verify `settingsRepository` is injected in `ItemListingViewModel`; add if absent
3. Add `showNextTotpCode: Boolean` to `ItemListingState`
4. Combine `settingsRepository.showNextTotpCodeStateFlow` into the existing `combine(...)` block; update state when it changes
5. Pass `showNextCode = state.showNextTotpCode` to all `toDisplayItem()` call sites
6. Test: setting enabled → `nextAuthCode` non-null; setting disabled → `nextAuthCode` null

**Verification**:
```bash
./gradlew authenticator:testStandardDebugUnitTest --tests "*ItemListingViewModel*"
./gradlew authenticator:testStandardDebugUnitTest --tests "*VerificationCodeItemExtensions*"
```

---

#### Phase 5: Settings UI Toggle

**Goal**: Expose the setting to users in the Settings screen.

**Files**: `strings.xml`, `SettingsViewModel.kt`, `SettingsScreen.kt`, `SettingsViewModelTest.kt`

**Tasks**:
1. Add `show_next_code` and `see_incoming_codes_in_the_list` to `strings.xml`
2. Add `ShowNextTotpCodeToggle(val enabled: Boolean)` to `SettingsAction`
3. Add `val showNextTotpCode: Boolean` to `SettingsState`; initialize from `settingsRepository.showNextTotpCode`
4. Add handler: update `settingsRepository.showNextTotpCode` + `mutableStateFlow.update { it.copy(showNextTotpCode = action.enabled) }`
5. Add `BitwardenSwitch` to `SettingsScreen` with `testTag("ShowNextTotpCodeSwitch")`
6. Tests: toggle on stores `true`; toggle off stores `false`; state reflects repository initial value

**Verification**:
```bash
./gradlew authenticator:testStandardDebugUnitTest --tests "*SettingsViewModel*"
./gradlew authenticator:lintStandardDebug
```

---

#### Phase 6: List Item UI

**Goal**: Render the next code in `VaultVerificationCodeItem` when present.

**Files**: `VaultVerificationCodeItem.kt`, `VaultVerificationCodeItemTest.kt`

**Tasks**:
1. Add `nextAuthCode: String? = null` to both overloads of `VaultVerificationCodeItem`
2. When `nextAuthCode != null`, render below `authCode` using same text style and 3-character spacing utility
3. Apply `semantics { contentDescription = "Next code, $nextAuthCode" }` to the next code element
4. Add test tag `"NextVerificationCode"` to the next code `Text`
5. Pass `nextAuthCode = displayItem.nextAuthCode` in the `displayItem` overload
6. Tests: next code rendered when non-null; absent when null; content description correct

**Verification**:
```bash
./gradlew authenticator:testStandardDebugUnitTest --tests "*VaultVerificationCodeItem*"
./gradlew authenticator:compileStandardDebugKotlin
```

---

### Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| `@Parcelize` incompatibility with new nullable `String?` field | Low | Medium | Kotlin `@Parcelize` handles `String?` natively — verify at Phase 2 compile step |
| Extra SDK call per TOTP item per period causes performance impact | Low | Low | One call per 30s per item; SDK is pure Rust math, negligible overhead |
| HOTP detection via URI string check is fragile | Low | Low | OTP URI scheme is standardized; add explicit HOTP test case |
| `settingsRepository` not yet injected in `ItemListingViewModel` | Unknown | Low | Verify in Phase 4 before assuming; add injection if absent |
| Settings section placement conflicts with existing screen structure | Low | Low | Read `SettingsScreen.kt` in Phase 5 before inserting the toggle |

### Final Verification Checklist

```bash
# Full authenticator unit tests
./gradlew authenticator:testStandardDebugUnitTest

# Lint + detekt
./gradlew authenticator:lintStandardDebug
./gradlew detekt

# Build
./gradlew authenticator:assembleStandardDebug
```

**Manual scenarios**:
1. Settings → "Show next code" toggle appears with correct label and sublabel
2. Toggle ON → list items each show a second code below the current code
3. Toggle OFF → next code disappears from all items
4. Wait for code rollover → old next code becomes new current code; new next code appears
5. HOTP items (if any) → no next code shown regardless of setting
6. Kill app with "Don't keep activities" ON → restore → setting persists
7. TalkBack: focus list item with next code → "Next code, [code]" announced

---

## Executing This Plan

To implement this plan, run:

    /work-on-android BWA-99

Reference this design document during implementation for architecture decisions,
file locations, and phase ordering.
