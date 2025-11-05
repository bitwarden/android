# Review Output Examples

Examples of well-structured code reviews for different change types. Each example demonstrates appropriate depth, tone, and formatting.

---

## CRITICAL: Inline Comments vs Summary Format

### ✅ PREFERRED: Inline Comments on Specific Lines

Create separate inline comment for each specific issue directly on the relevant line:

**Inline Comment 1** (on `app/login/LoginViewModel.kt:78`):
```
**CRITICAL**: Exposes mutable state

Change `MutableStateFlow<State>` to `StateFlow<State>` in public API:

\```kotlin
private val _state = MutableStateFlow<State>()
val state: StateFlow<State> = _state.asStateFlow()
\```

Exposing MutableStateFlow allows external mutation, violating MVVM unidirectional data flow.

Reference: docs/ARCHITECTURE.md#mvvm-pattern
```

**Inline Comment 2** (on `data/auth/BiometricRepository.kt:120`):
```
**CRITICAL**: Missing null safety check

biometricPrompt result can be null. Add explicit null check:

\```kotlin
val result = biometricPrompt.authenticate()
if (result != null) {
    processAuth(result)
} else {
    handleCancellation()
}
\```

Prevents NPE crash when user cancels biometric prompt.
```

**Summary Comment** (general PR-level comment):
```
## Summary
Implements PIN unlock feature for vault access.

## Overall Assessment
- 2 critical architecture issues found (mutable state, null safety)
- 3 suggestions for improvement (tests, component reuse)
- Good separation of concerns and DI usage

## Recommendation
**REQUEST CHANGES** - Address 2 critical issues before merge.
```

**Why This is Better**:
- Each issue appears directly on the problematic line
- Easy for author to see context and fix
- History preserved when creating new comments
- Other reviewers can see individual discussions
- Cleaner PR conversation thread

---

### ❌ DISCOURAGED: Single Large Summary Comment

Do NOT create one large comment with all issues:

```markdown
## Summary
Implements PIN unlock feature

## Critical Issues
- **app/login/LoginViewModel.kt:78** - Exposes mutable state (entire explanation...)
- **data/auth/BiometricRepository.kt:120** - Missing null safety (entire explanation...)
- **app/vault/UnlockScreen.kt:134** - Should use BitwardenTextField (entire explanation...)

## Suggested Improvements
- **app/vault/UnlockViewModel.kt:92** - Missing test (entire explanation...)
- **app/vault/UnlockViewModel.kt:105** - Consider rate limiting (entire explanation...)

## Good Practices
- Proper Hilt DI
- Clear separation of concerns

## Action Items
1. Fix mutable state exposure
2. Add null safety check
... (20 more items)
```

**Why This is Problematic**:
- All issues in one massive comment
- Author must scroll through entire comment to find each issue
- Hard to track which issues are addressed
- Loses context when jumping between files
- If comment is updated, history is lost
- Other reviewers can't comment on specific issues

---

## Format Guidelines

### Inline Comment Structure

```
**[SEVERITY]**: [One-line issue description]

[Detailed explanation if needed]

[Code example showing problem and/or solution]

[Rationale explaining why this matters]

Reference: [docs link if applicable]
```

**Severity Levels**:
- `**CRITICAL**`: Must fix before merge
- `**IMPORTANT**`: Should fix before merge
- `**SUGGESTED**`: Nice to have
- `**QUESTION**`: Asking for clarification

**Examples**:

```
**CRITICAL**: PIN stored without encryption

Must encrypt using Android Keystore:

\```kotlin
val encrypted = keystoreManager.encrypt(pin.toByteArray())
encryptedPrefs.putBytes(KEY_PIN, encrypted)
\```

Plaintext storage exposes PIN to backups and rooted devices.

Reference: docs/ARCHITECTURE.md#security
```

```
**SUGGESTED**: Consider using BitwardenTextField

Can we use `ui/components/BitwardenTextField.kt:67` instead?
This would maintain consistency across the app.
```

```
**QUESTION**: Is rate limiting needed for PIN attempts?

Currently allows unlimited attempts. Should we add lockout after N failed attempts?
```

### Summary Comment Structure

One summary comment per PR with overall assessment:

```markdown
## Summary
[1-2 sentence description of changes]

## Review Complete
- X critical issues found (see inline comments)
- Y suggestions provided (see inline comments)
- Z good practices noted

## Recommendation
**[APPROVE | REQUEST CHANGES | COMMENT]** - [Reason]
```

---

## Example 1: Dependency Update (Expedited Review)

**Context**: Simple patch version update, no breaking changes

```markdown
## Summary
Updates androidx.credentials from 1.5.0 to 1.6.0-beta03

## Analysis
- **Compilation**: ✓ No breaking changes detected in changelog
- **Security**: ✓ No CVEs addressed
- **Testing**: ✓ No test API changes
- **Changelog**: Adds support for additional credential types, internal bug fixes

## Findings
⚠️ **Beta version** - Monitor for stability issues in production environment

## Recommendation
**APPROVE** - Low-risk minor version bump. Beta status is noted but no blocking concerns identified.
```

**Key Features**:
- Expedited format (no detailed sections)
- Clear analysis checklist
- Single finding noted
- Direct recommendation

---

## Example 2: Dependency Update with Breaking Changes

**Context**: Major version update requiring code migration

```markdown
## Summary
Updates Retrofit from 2.9.0 to 3.0.0

## Analysis
- **Compilation**: ❌ Breaking changes in API
- **Security**: ✓ No security issues
- **Testing**: ⚠️ Test utilities need updates
- **Changelog**: Major rewrite, new Kotlin coroutines API, removed deprecated methods

## Critical Issues

**network/api/BitwardenApiService.kt:34** - API migration required
```kotlin
// Current (deprecated in Retrofit 3.0)
@GET("api/accounts/profile")
fun getProfile(): Call<ProfileResponse>

// Must migrate to
@GET("api/accounts/profile")
suspend fun getProfile(): Response<ProfileResponse>
```
Retrofit 3.0 removes the `Call<T>` return type. All 12 API methods in this file need migration.

**network/api/VaultApiService.kt:45** - Same migration needed
All 8 API methods need to migrate from `Call<T>` to `suspend fun` returning `Response<T>`.

## Suggested Improvements

**Consider separate PR for migration** - Given the scope (20 API methods across 2 files + all call sites + tests), this might be better as a dedicated migration PR with comprehensive testing.

## Recommendation
**REQUEST CHANGES** - Major version requires code migration:
1. Update all API service interfaces to use suspend functions
2. Update all call sites to use coroutines instead of enqueue/execute
3. Update tests to handle new suspend function APIs
4. Consider creating separate PR for this migration due to scope

## Migration Resources
- Retrofit 3.0 migration guide: https://square.github.io/retrofit/changelogs/changelog-3.x/
- Example migration: https://github.com/square/retrofit/tree/master/samples
```

**Key Features**:
- Specific file:line references for each issue
- Code examples showing current vs required
- Scope assessment
- Migration guidance

---

## Example 3: Bug Fix (Focused Review)

**Context**: Fixes crash when biometric prompt is cancelled

```markdown
## Summary
Fixes crash when biometric prompt is cancelled (PM-12345)

Root cause: BiometricPrompt result was nullable but code assumed non-null

## Critical Issues
None

## Suggested Improvements

**data/auth/BiometricRepository.kt:120** - Add regression test
Can we add a test that verifies this specific bug doesn't reoccur?
```kotlin
@Test
fun `when biometric cancelled then returns cancelled state`() = runTest {
    coEvery { biometricPrompt.authenticate() } returns null

    val result = repository.authenticate()

    assertEquals(AuthResult.Cancelled, result)
}
```
This prevents regression of the crash you just fixed.

**data/auth/BiometricRepository.kt:134** - Consider extracting null handling
```kotlin
private fun handleBiometricResult(result: BiometricPrompt.AuthenticationResult?): AuthResult {
    return result?.let { AuthResult.Success(it) } ?: AuthResult.Cancelled
}
```
This pattern could be reused if we add other biometric auth points in the future.

**app/auth/BiometricViewModel.kt:89** - Check similar pattern in FaceIdRepository
I see similar biometric handling in `data/auth/FaceIdRepository.kt:78`. Does it have the same issue?

## Good Practices
- Added null safety check for prompt result
- Proper error state propagation through sealed class
- Clear commit message explaining root cause

## Action Items
1. Add regression test for cancellation scenario
2. Check FaceIdRepository for similar null safety issue
3. Consider extracting null handling pattern if reusable
```

**Key Features**:
- Root cause explained
- Specific line references for each suggestion
- Code examples provided
- Questions about related code
- Concise good practices (no elaboration)

---

## Example 4: Feature Addition (Comprehensive Review)

**Context**: Implements PIN unlock for vault access

```markdown
## Summary
Implements PIN unlock feature for vault access (PM-54321)

Adds new authentication option allowing users to unlock vault with 4-6 digit PIN instead of master password. Includes PIN creation, validation, and secure storage.

## Critical Issues

**app/vault/unlock/UnlockViewModel.kt:78** - Exposes mutable state
```kotlin
// Current (problematic)
val unlockState: MutableStateFlow<UnlockState>

// Should be
private val _unlockState = MutableStateFlow<UnlockState>()
val unlockState: StateFlow<UnlockState> = _unlockState.asStateFlow()
```
Exposing MutableStateFlow allows external code to modify ViewModel state directly, violating MVVM's unidirectional data flow principle.

Reference: `docs/ARCHITECTURE.md#mvvm-pattern`

**data/vault/UnlockRepository.kt:145** - SECURITY: PIN stored without encryption
```kotlin
// Current (CRITICAL SECURITY ISSUE)
sharedPreferences.edit {
    putString(KEY_PIN, pin)
}

// Must use Android Keystore encryption
suspend fun storePin(pin: String): Result<Unit> = runCatching {
    val encrypted = keystoreManager.encrypt(pin.toByteArray())
    encryptedPrefs.putBytes(KEY_PIN, encrypted)
}
```
Storing PIN in plaintext SharedPreferences exposes it to backup systems and rooted devices.
Must use Android Keystore encryption or EncryptedSharedPreferences.

Reference: `docs/ARCHITECTURE.md#security`

## Suggested Improvements

**app/vault/unlock/UnlockViewModel.kt:92** - Missing error handling test
```kotlin
@Test
fun `when incorrect PIN entered then returns error state`() = runTest {
    val viewModel = UnlockViewModel(mockRepository)
    coEvery { mockRepository.validatePin("1234") } returns Result.failure(InvalidPinException())

    viewModel.onPinEntered("1234")

    assertEquals(UnlockState.Error("Invalid PIN"), viewModel.state.value)
}
```
Add test to prevent regression if error handling changes.

**app/vault/unlock/UnlockViewModel.kt:105** - Consider rate limiting
Can we add rate limiting for PIN attempts? Currently allows unlimited attempts, which could enable brute force attacks.
```kotlin
private var attemptCount = 0
private var lockoutUntil: Instant? = null

fun onPinEntered(pin: String) {
    if (isLockedOut()) {
        _state.value = UnlockState.LockedOut(lockoutUntil!!)
        return
    }
    // ... validate PIN ...
    if (invalid) {
        attemptCount++
        if (attemptCount >= MAX_ATTEMPTS) {
            lockoutUntil = clock.millis() + 15.minutes
        }
    }
}
```

**app/vault/unlock/UnlockScreen.kt:134** - Can we use BitwardenTextField?
This custom PIN input field looks similar to `ui/components/BitwardenTextField.kt:67`.
Would using the existing component maintain consistency?

**data/vault/UnlockRepository.kt:178** - Add test for PIN length validation
```kotlin
@Test
fun `when PIN is less than 4 digits then returns validation error`() = runTest {
    val result = repository.createPin("123")

    assertTrue(result.isFailure)
    assertIs<ValidationException>(result.exceptionOrNull())
}
```

## Good Practices
- Proper Hilt DI usage throughout
- Clear separation of UI/ViewModel/Repository layers
- Sealed classes for state management
- Comprehensive happy-path unit tests

## Action Items
1. **MUST FIX**: Encrypt PIN using Android Keystore (security issue)
2. **MUST FIX**: Expose immutable StateFlow in ViewModel (architecture violation)
3. **SHOULD ADD**: Test for incorrect PIN error flow
4. **SHOULD ADD**: Test for PIN length validation
5. **CONSIDER**: Rate limiting for PIN attempts (security enhancement)
6. **CONSIDER**: Evaluate BitwardenTextField for consistency
```

**Key Features**:
- Comprehensive review with multiple sections
- Critical security issues clearly flagged
- Specific code examples for fixes
- Mix of prescriptive fixes and collaborative questions
- Test examples provided
- Clear prioritization (MUST FIX vs SHOULD ADD vs CONSIDER)

---

## Example 5: UI Refinement (Design-Focused Review)

**Context**: Updates login screen layout for improved visual hierarchy

```markdown
## Summary
Updates login screen layout for improved visual hierarchy and touch target sizes

Adjusts spacing, increases button sizes to meet accessibility guidelines, and improves visual flow.

## Critical Issues
None

## Suggested Improvements

**app/auth/LoginScreen.kt:67** - Can we use BitwardenTextField?
This custom text field implementation looks very similar to `ui/components/BitwardenTextField.kt:89`.
```kotlin
// Current (custom implementation)
OutlinedTextField(
    value = email,
    onValueChange = onEmailChange,
    colors = OutlinedTextFieldDefaults.colors(...)
)

// Consider using existing component
BitwardenTextField(
    value = email,
    onValueChange = onEmailChange,
    label = "Email"
)
```
Using the existing component would maintain consistency across the app and reduce code duplication.

**app/auth/LoginScreen.kt:123** - Add contentDescription for accessibility
```kotlin
Icon(
    painter = painterResource(R.drawable.ic_visibility),
    contentDescription = "Show password",  // Add this
    modifier = Modifier.clickable { onToggleVisibility() }
)
```
Screen readers need contentDescription to announce the icon's purpose to visually impaired users.

**app/auth/LoginScreen.kt:145** - Use theme spacing
```kotlin
// Current (non-standard spacing)
Spacer(modifier = Modifier.height(17.dp))

// Should use standard theme spacing
Spacer(modifier = Modifier.height(16.dp))
```
Project uses standard spacing values (4dp, 8dp, 16dp, 24dp) for consistency.

**app/auth/LoginScreen.kt:178** - Use theme color
```kotlin
// Current (hardcoded color)
color = Color(0xFF0066FF)

// Should use theme
color = BitwardenTheme.colorScheme.primary
```

## Good Practices
- Proper state hoisting to ViewModel
- Preview composables included for development
- Touch targets meet 48dp minimum

## Action Items
1. Evaluate using BitwardenTextField for consistency
2. Add contentDescription for visibility icon
3. Use standard 16dp spacing instead of 17dp
4. Use theme color instead of hardcoded value
```

**Key Features**:
- Design and accessibility focused
- Specific line references
- Shows current vs recommended code
- Explains rationale (consistency, accessibility)
- Notes good practices briefly

---

## Example 6: Refactoring (Pattern Consistency Review)

**Context**: Refactors authentication to use Repository pattern

```markdown
## Summary
Refactors authentication flow to use Repository pattern instead of direct Manager access

Scope: 12 files changed, 8 ViewModels updated, AuthRepository interface extracted

## Critical Issues
None - behavior preserved, all tests passing

## Suggested Improvements

**app/vault/VaultViewModel.kt:89** - Old pattern still used here
This ViewModel still injects AuthManager directly. Should it use AuthRepository like the others?
```kotlin
// Current (old pattern)
class VaultViewModel @Inject constructor(
    private val authManager: AuthManager
)

// Should be (new pattern)
class VaultViewModel @Inject constructor(
    private val authRepository: AuthRepository
)
```
This is the only ViewModel still using the old pattern.

**data/auth/AuthManager.kt:1** - Add deprecation notice
Can we add @Deprecated to AuthManager to guide future development?
```kotlin
@Deprecated(
    message = "Use AuthRepository interface instead. AuthManager will be removed in v3.0.",
    replaceWith = ReplaceWith("AuthRepository"),
    level = DeprecationLevel.WARNING
)
class AuthManager @Inject constructor(...)
```
This helps other developers understand the migration path and prevents new code from using the old pattern.

**docs/ARCHITECTURE.md:145** - Document the new pattern
Should we update the architecture docs to reflect this Repository pattern?
The current documentation (line 145-167) still shows AuthManager as the recommended approach.

Suggest adding section:
```markdown
## Authentication Architecture

Use `AuthRepository` interface for all authentication operations:
- Login/logout
- Session management
- Token refresh

The repository pattern provides abstraction and makes testing easier.
```

## Good Practices
- Repository interface clearly defined with Result return types
- All ViewModels except one successfully migrated
- Tests updated to match new pattern
- Behavior preserved (all existing tests pass)

## Action Items
1. Update VaultViewModel to use AuthRepository
2. Add @Deprecated annotation to AuthManager with timeline
3. Update ARCHITECTURE.md to document Repository pattern
4. Consider deprecation timeline (suggest v3.0 removal)
```

**Key Features**:
- Focuses on migration completeness
- Identifies incomplete migration (one missed file)
- Suggests deprecation strategy
- Notes documentation needs
- Acknowledges good practices briefly

---

## Key Patterns Across All Examples

### Consistent Elements

1. **File:Line References**: Every specific issue includes `file:line_number` format
2. **Code Examples**: Complex issues show current code and suggested fix
3. **Rationale**: Explains **why** change is needed, not just **what**
4. **Prioritization**: Clear distinction between Critical, Suggested, Optional
5. **Actionable**: Specific steps the author should take
6. **Constructive Tone**: Questions for design decisions, prescriptive for violations

### Format Structure

```markdown
## Summary
[1-2 sentence description]

## Critical Issues (if any)
**file:line** - Issue description
[Code example showing problem and solution]
[Rationale explaining why this matters]

## Suggested Improvements
**file:line** - Suggestion with question or explanation
[Code example if helpful]
[Benefits of the suggestion]

## Good Practices (brief)
- Item 1
- Item 2
- Item 3

## Action Items
1. Required action
2. Recommended action
3. Optional consideration
```

---

## Anti-Patterns to Avoid

### ❌ Too Vague
```
The state management could be better.
This code has issues.
Consider improving the architecture.
```

### ❌ Too Verbose
```
## Good Practices
- Excellent Hilt DI usage! I really appreciate how you've implemented dependency injection here. The use of @HiltViewModel is exactly what we want to see, and injecting interfaces instead of implementations is a best practice that really shines in this PR. The constructor injection pattern is clean and testable. Great work! This is a perfect example of how to structure ViewModels in our codebase. Keep up the fantastic work! 👍🎉
```

### ❌ No Specifics
```
There are some null safety issues in the ViewModel.
Tests are missing for some scenarios.
```

### ✅ Good Specificity
```
**app/login/LoginViewModel.kt:78** - Missing null safety check
biometricPrompt result can be null. Add explicit check to prevent NPE.
```

---

## Concise Review Format (Recommended)

### Key Principles

1. **Minimal summary**: Only verdict + critical issues
2. **Collapsible inline comments**: Severity + one-line description visible, details collapsed
3. **No duplication**: Don't repeat inline issues in summary
4. **No redundant sections**: No "Action Items" or "Good Practices" in summary

### Example 1: Feature Review with Multiple Issues

**Summary Comment:**
```markdown
**Overall Assessment:** REQUEST CHANGES

**Critical Issues:**
- Exposes mutable state (app/vault/VaultViewModel.kt:45)
- Missing null safety check (app/vault/VaultRepository.kt:123)

See inline comments for all issue details.
```

**Inline Comments:**

```markdown
**app/vault/VaultViewModel.kt:45** - CRITICAL: Exposes mutable state

<details>
<summary>Details and fix</summary>

Change to private backing field pattern:

\```kotlin
private val _vaultState = MutableStateFlow<VaultState>(VaultState.Loading)
val vaultState: StateFlow<VaultState> = _vaultState.asStateFlow()
\```

Exposing MutableStateFlow allows external mutation, violating MVVM unidirectional data flow.

Reference: docs/ARCHITECTURE.md#mvvm-pattern
</details>
```

```markdown
**app/vault/VaultRepository.kt:123** - CRITICAL: Missing null safety check

<details>
<summary>Details and fix</summary>

Add null check before accessing cipher:

\```kotlin
val cipher = getCipher(id) ?: return Result.failure(CipherNotFoundException())
\```

Without null safety, this will crash when cipher ID is invalid.
</details>
```

```markdown
**app/vault/VaultViewModel.kt:89** - IMPORTANT: Missing test coverage

<details>
<summary>Details</summary>

Add test for error state handling:

\```kotlin
@Test
fun `when load fails then shows error state`() = runTest {
    coEvery { repository.getVaultItems() } returns Result.failure(Exception())
    viewModel.loadVault()
    assertEquals(VaultState.Error, viewModel.vaultState.value)
}
\```

Error paths should be tested to prevent regressions.

Reference: reference/testing-patterns.md
</details>
```

**Why this works:**
- Summary is 4 lines (vs 30+ lines with verbose format)
- Severity + issue visible immediately
- Full details available on expansion
- Zero duplication between summary and inline comments
- Token-efficient while preserving all information

---

### Example 2: Dependency Update (No Critical Issues)

**Summary Comment:**
```markdown
**Overall Assessment:** APPROVE

See inline comments for suggested improvements.
```

**Inline Comment:**

```markdown
**gradle/libs.versions.toml:45** - SUGGESTED: Beta version in production

<details>
<summary>Details</summary>

Updated androidx.credentials from 1.5.0 to 1.6.0-beta03.

Monitor for stability issues - beta releases may have unexpected behavior in production.

Changelog: Adds support for additional credential types, internal bug fixes.
</details>
```

**Why this works:**
- Immediate approval visible (no critical issues)
- Suggestion collapsed to reduce noise
- All context preserved for interested reviewers

---

### Example 3: Bug Fix Review

**Summary Comment:**
```markdown
**Overall Assessment:** APPROVE

See inline comments for suggested improvements.
```

**Inline Comments:**

```markdown
**data/auth/BiometricRepository.kt:120** - SUGGESTED: Extract null handling

<details>
<summary>Details</summary>

Root cause: BiometricPrompt result was nullable but code assumed non-null, causing crash on cancellation (PM-12345).

Consider extracting pattern for reuse:

\```kotlin
private fun handleBiometricResult(result: BiometricPrompt.AuthenticationResult?): AuthResult {
    return result?.let { AuthResult.Success(it) } ?: AuthResult.Cancelled
}
\```
</details>
```

```markdown
**app/auth/BiometricViewModel.kt:89** - SUGGESTED: Add regression test

<details>
<summary>Details</summary>

\```kotlin
@Test
fun `when biometric cancelled then returns cancelled state`() = runTest {
    coEvery { repository.authenticate() } returns Result.failure(CancelledException())
    viewModel.onBiometricAuth()
    assertEquals(AuthState.Cancelled, viewModel.state.value)
}
\```

Prevents regression of the bug just fixed.
</details>
```

**Why this works:**
- Approval decision immediately visible
- Root cause analysis preserved but collapsed
- Suggestions don't overwhelm the fix
- Test recommendations available but not blocking

---

## Example 7: Clean Refactoring (No Issues Found)

**Note**: This example was added to demonstrate proper handling of clean refactorings after PR #6117 received an overly verbose review. See the "Anti-Pattern" section below for the actual problematic output that motivated these changes.

**Context**: Moving shared code to common module, complete migration, all patterns followed

**Review Comment:**
```markdown
**Overall Assessment:** APPROVE

Clean refactoring that moves ExitManager to :ui module, eliminating duplication between apps.
```

**Token count:** ~30 tokens
**Review time:** < 1 minute

**Why this works:**
- Immediate approval visible
- One sentence summary acknowledging the work
- No unnecessary sections or elaborate praise
- Author gets quick feedback and can proceed

---

### ❌ Anti-Pattern: Excessive Praise for Clean PRs

**DO NOT do this for clean PRs:**

```markdown
### PR Review: PM-27756 - Create common ExitManager

This PR successfully refactors `ExitManager` into shared code in the `:ui` module, eliminating duplication between the Password Manager and Authenticator apps. The changes follow established architectural patterns and improve code quality.

---

### Summary

**Overall Assessment:** ✅ **Approved** - This is a well-executed refactoring that improves code organization and maintainability.

**Key Strengths:**
- Follows established module organization patterns (moving to `:ui` module alongside `IntentManager` and `QrCodeAnalyzer`)
- Removes code duplication between apps (deleted 28 lines of duplicate code)
- Improves test coverage by removing `@OmitFromCoverage` and adding comprehensive unit tests
- Maintains consistent behavior across both applications
- All imports and references updated correctly

**Changes:**
- ✅ Moved `ExitManager` interface from `app` → `ui` module
- ✅ Moved `ExitManagerImpl` from `app` → `ui` module
- ✅ Added `LocalExitManager` to common UI providers
- ✅ Deleted duplicate authenticator implementations
- ✅ Added unit test for `ExitManagerImpl`
- ✅ Updated all imports across 10 files

---

### Code Quality & Architecture

**Architectural Compliance:** ✅
- Correctly places manager in `:ui` module per `docs/ARCHITECTURE.md` guidelines
- Follows established pattern for UI-layer managers exposed via CompositionLocal
- Maintains proper module dependencies and separation of concerns

**Code Style:** ✅
- Follows Kotlin coding conventions
- Proper KDoc documentation on all public APIs
- Consistent with existing code patterns

**Testing:** ✅
- Added comprehensive unit test covering `exitApplication()` behavior
- Properly uses MockK for mocking Activity
- Test verifies `finishAndRemoveTask()` is called

---

### Specific Findings

**Test Coverage Note:**
The Codecov report shows 50% patch coverage with 1 missing line. This is expected and acceptable...

**Implementation Improvement:**
The common implementation correctly uses `activity.finishAndRemoveTask()`...

---

### Recommendations

No blocking issues found. The PR is ready to merge.
```

**Problems with above (actual PR #6117 review):**
- 800+ tokens for a PR with no issues
- Multiple redundant sections
- Excessive checkmarks listing what was done
- Detailed analysis of things that are correct
- "Key Strengths" section unnecessary
- "Specific Findings" section with non-issues

**Impact:**
- Wastes reviewer and author time
- Adds noise to PR conversation
- Makes it harder to identify PRs that actually need attention
- Excessive praise can feel condescending or insincere
- Burns tokens unnecessarily

---

### ✅ Correct Approach

For the same PR:

```markdown
**Overall Assessment:** APPROVE

Clean refactoring that moves ExitManager to :ui module, eliminating duplication between apps.
```

**Benefits:**
- 30 tokens vs 800+ tokens (96% reduction)
- Clear immediate approval
- Acknowledges the work without excessive detail
- Author can proceed without wading through unnecessary commentary
- Saves time for everyone

---

### Comparison: Verbose vs Concise

**Verbose Format (Old):**
```markdown
## Summary
Adds vault item encryption feature

Root cause: Feature implements client-side encryption for vault items

## Critical Issues

**app/vault/VaultViewModel.kt:45** - Exposes mutable state
Change MutableStateFlow to StateFlow:
\```kotlin
private val _state = MutableStateFlow()
val state = _state.asStateFlow()
\```
Prevents external mutation, enforces unidirectional data flow.
Reference: docs/ARCHITECTURE.md

**app/vault/VaultRepository.kt:123** - Missing null safety
Add null check: cipher ?: return Result.failure()

## Important Issues

[3 more issues with full details]

## Suggested Improvements

[5 more issues with full details]

## Good Practices
- Clean MVVM separation
- Proper Hilt DI usage
- Comprehensive test coverage

## Action Items
1. Fix mutable state exposure (app/vault/VaultViewModel.kt:45)
2. Add null safety (app/vault/VaultRepository.kt:123)
3. [8 more action items duplicating the issues above]
```

**Token count:** ~800-1000 tokens
**Issues:** Heavy duplication, verbose praise, action items redundant with inline comments

**Concise Format (New):**
```markdown
**Overall Assessment:** REQUEST CHANGES

**Critical Issues:**
- Exposes mutable state (app/vault/VaultViewModel.kt:45)
- Missing null safety (app/vault/VaultRepository.kt:123)

See inline comments for all issue details.
```

Plus inline comments with `<details>` tags.

**Token count:** ~200-300 tokens visible, ~600-800 total (expandable)
**Benefits:**
- 60-70% token reduction
- Zero duplication
- Faster scanning
- All details preserved

---

### Implementation Notes

**When to use which format:**

**Use Concise Format for:**
- All reviews going forward (new default)
- High token efficiency needed
- Multiple issues to report
- When details would overwhelm

**Visible Content (Not Collapsed):**
- Severity level
- One-line issue description
- File:line reference

**Collapsed Content (In `<details>`):**
- Code examples (before/after)
- Detailed rationale
- References to documentation
- Implementation suggestions

**Never Include in Summary:**
- Issue details (those are in inline comments)
- Good Practices section (eliminates noise)
- Action Items (duplicates inline comments)
