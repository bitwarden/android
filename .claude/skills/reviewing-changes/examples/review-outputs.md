# Review Output Examples

Well-structured code reviews demonstrating appropriate depth, tone, and formatting for different change types.

## Table of Contents

**Format Reference:**
- [Quick Format Reference](#quick-format-reference)
  - [Inline Comment Format](#inline-comment-format-required)
  - [Summary Comment Format](#summary-comment-format)

**Examples:**
- [Example 1: Clean PR (No Issues)](#example-1-clean-pr-no-issues)
- [Example 2: Dependency Update with Breaking Changes](#example-2-dependency-update-with-breaking-changes)
- [Example 3: Feature Addition with Critical Issues](#example-3-feature-addition-with-critical-issues)

**Anti-Patterns:**
- [❌ Anti-Patterns to Avoid](#-anti-patterns-to-avoid)
  - [Problem: Verbose Summary with Multiple Sections](#problem-verbose-summary-with-multiple-sections)
  - [Problem: Praise-Only Inline Comments](#problem-praise-only-inline-comments)
  - [Problem: Missing `<details>` Tags](#problem-missing-details-tags)

**Summary:**
- [Summary](#summary)

---

## Quick Format Reference

### Inline Comment Format (REQUIRED)

**MUST use `<details>` tags.** Only severity + description visible; all other content collapsed.

```
[emoji] **[SEVERITY]**: [One-line issue description]

<details>
<summary>Details and fix</summary>

[Code example or specific fix]

[Rationale explaining why]

Reference: [docs link if applicable]
</details>
```

**Severity Levels:**
- ❌ **CRITICAL** - Blocking, must fix (security, crashes, architecture violations)
- ⚠️ **IMPORTANT** - Should fix (missing tests, quality issues)
- ♻️ **DEBT** - Technical debt (duplication, convention violations, future rework needed)
- 🎨 **SUGGESTED** - Nice to have (refactoring, improvements)
- 💭 **QUESTION** - Seeking clarification (requirements, design decisions)

### Summary Comment Format

**Required format for ALL PRs:**
```
**Overall Assessment:** APPROVE / REQUEST CHANGES

**Critical Issues** (if any):
- [issue with file:line]

See inline comments for details.
```

All PRs use the same minimal format - no exceptions for size or complexity. Summary must be 5-10 lines maximum.

---

## Example 1: Clean PR (No Issues)

**Context**: Moving shared code to common module, complete migration, all patterns followed

**Review Comment:**
```markdown
**Overall Assessment:** APPROVE

Clean refactoring that moves ExitManager to :ui module, eliminating duplication between apps.
```

**Why this works:**
- Immediate approval visible (2-3 lines)
- One sentence acknowledging the work
- No unnecessary sections or elaborate praise
- Author gets quick feedback and can proceed

---

## Example 2: Dependency Update with Breaking Changes

**Context**: Major version update requiring code migration

**Summary Comment:**
```markdown
**Overall Assessment:** REQUEST CHANGES

**Critical Issues:**
- API migration required for Retrofit 3.0 breaking changes (network/api/BitwardenApiService.kt:34)

See inline comments for migration details.
```

**Inline Comment 1** (on `network/api/BitwardenApiService.kt:34`):
```markdown
❌ **CRITICAL**: API migration required for Retrofit 3.0

<details>
<summary>Details and fix</summary>

Retrofit 3.0 removes the `Call<T>` return type. All 12 API methods in this file need migration:

```kotlin
// Current (deprecated in Retrofit 3.0)
@GET("api/accounts/profile")
fun getProfile(): Call<ProfileResponse>

// Must migrate to
@GET("api/accounts/profile")
suspend fun getProfile(): Response<ProfileResponse>
```

Breaking API change affects:
- 12 methods in BitwardenApiService
- 8 methods in VaultApiService
- All call sites using enqueue/execute
- Test utilities

Consider creating separate PR for this migration given the scope.

Reference: [Retrofit 3.0 migration guide](https://square.github.io/retrofit/changelogs/changelog-3.x/)
</details>
```

**Key Features:**
- Minimal summary (2-3 lines)
- Full details in collapsed inline comment
- Specific file:line references
- Code examples in <details>
- Migration guidance and scope assessment

---

## Example 3: Feature Addition with Critical Issues

**Context**: Implements PIN unlock for vault access

**Summary Comment:**
```markdown
**Overall Assessment:** REQUEST CHANGES

**Critical Issues:**
- Exposes mutable state violating MVVM (UnlockViewModel.kt:78)
- PIN stored without encryption - SECURITY ISSUE (UnlockRepository.kt:145)

See inline comments for all issues and suggestions.
```

**Inline Comment 1** (on `app/vault/unlock/UnlockViewModel.kt:78`):
```markdown
❌ **CRITICAL**: Exposes mutable state

<details>
<summary>Details and fix</summary>

Change `MutableStateFlow<State>` to `StateFlow<State>`:

```kotlin
// Current (problematic)
val unlockState: MutableStateFlow<UnlockState>

// Should be
private val _unlockState = MutableStateFlow<UnlockState>()
val unlockState: StateFlow<UnlockState> = _unlockState.asStateFlow()
```

Exposing MutableStateFlow allows external mutation, violating MVVM unidirectional data flow.

Reference: docs/ARCHITECTURE.md#mvvm-pattern
</details>
```

**Inline Comment 2** (on `data/vault/UnlockRepository.kt:145`):
```markdown
❌ **CRITICAL**: PIN stored without encryption - SECURITY ISSUE

<details>
<summary>Details and fix</summary>

Storing PIN in plaintext SharedPreferences exposes it to backup systems and rooted devices.

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

Use Android Keystore encryption or EncryptedSharedPreferences per security architecture.

Reference: docs/ARCHITECTURE.md#security
</details>
```

**Inline Comment 3** (on `app/vault/unlock/UnlockViewModel.kt:92`):
```markdown
⚠️ **IMPORTANT**: Missing error handling test

<details>
<summary>Details and fix</summary>

Add test to prevent regression if error handling changes:

```kotlin
@Test
fun `when incorrect PIN entered then returns error state`() = runTest {
    val viewModel = UnlockViewModel(mockRepository)
    coEvery { mockRepository.validatePin("1234") }
        returns Result.failure(InvalidPinException())

    viewModel.onPinEntered("1234")

    assertEquals(UnlockState.Error("Invalid PIN"), viewModel.state.value)
}
```

Ensures error flow remains robust across refactorings.
</details>
```

**Inline Comment 4** (on `app/vault/unlock/UnlockViewModel.kt:105`):
```markdown
🎨 **SUGGESTED**: Consider rate limiting for PIN attempts

<details>
<summary>Details and fix</summary>

Currently allows unlimited attempts, which could enable brute force attacks.

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

Would add security layer against brute force. Consider discussing threat model with security team.
</details>
```

**Inline Comment 5** (on `app/vault/unlock/UnlockScreen.kt:134`):
```markdown
💭 **QUESTION**: Can we use BitwardenTextField?

<details>
<summary>Details</summary>

This custom PIN input field looks similar to `ui/components/BitwardenTextField.kt:67`.

Would using the existing component maintain consistency and reduce custom UI code?
</details>
```

**Key Features:**
- Minimal summary (3-4 lines) with critical issues only
- Each issue gets separate inline comment with `<details>` tag
- Multiple severity levels demonstrated (CRITICAL, IMPORTANT, SUGGESTED, QUESTION)
- Mix of prescriptive fixes and collaborative questions
- Code examples collapsed in <details>
- No "Good Practices" or "Action Items" sections

---

## ❌ Anti-Patterns to Avoid

### Problem: Verbose Summary with Multiple Sections

**What NOT to do:**
```markdown
### Review Complete ✅

## Summary
[Lengthy description of what the PR does]

### Strengths 👍
1. **Excellent documentation** - KDoc comments are comprehensive
2. **Proper fail-closed design** - Security defaults to rejection
3. **Defense in depth** - Multiple validation layers
[7 total items with elaboration]

### Critical Issues ⚠️
- Missing test coverage for security-critical code (with full details)
- [More issues with full explanations]

### Recommendations 🎨
- [Multiple recommendations]

### Test Coverage Status 📊
- [Analysis]

### Architecture Compliance ✅
- [Analysis]

## Recommendation
**Conditional approval** with follow-up...
```

**Why this is wrong:**
- 800+ tokens for a summary comment
- Multiple sections (Strengths, Recommendations, Test Coverage, Architecture)
- Elaborates on positive aspects ("Excellent documentation...")
- Duplicates critical issues (summary has details + inline comments have same details)
- Creates visual clutter in PR conversation

**Correct approach:**
```markdown
**Overall Assessment:** REQUEST CHANGES

**Critical Issues:**
- Missing test coverage for security-critical code (PasswordManagerSignatureVerifierImpl.kt:47)

See inline comments for details.
```

**Key differences:**
- 3-5 lines vs 800+ tokens
- Verdict + critical issues only
- All details belong in inline comments
- No positive commentary sections
- Scales with PR complexity, not analysis thoroughness

### Problem: Praise-Only Inline Comments

**What NOT to do:**

Creating inline comment on `AuthenticatorBridgeManagerImpl.kt:73`:
```markdown
👍 **Excellent integration of signature verification**

The signature verification is properly integrated into the connection flow:
- Checked during initialization (line 73)
- Checked before binding (line 134)
- Ensures only validated apps can connect

This is exactly the right approach for fail-safe security.
```

**Why this is wrong:**
- Entire comment is positive feedback with no actionable issue
- Takes up space in PR conversation
- Distracts from actual issues
- Violates "focus on actionable feedback" principle

**Correct approach:**
- Do not create this comment at all
- Reserve inline comments exclusively for issues requiring attention

### Problem: Missing `<details>` Tags

**What NOT to do:**

```markdown
❌ **CRITICAL**: Missing test coverage for security-critical code

The `@OmitFromCoverage` annotation excludes this entire class from test coverage.

**Problems:**
1. No validation that certificate hashes match actual Bitwarden certificates
2. No verification of fail-closed behavior on edge cases
3. No tests for multiple signer rejection logic
4. Certificate hash typos would go undetected until production

**Recommendation:**
Replace `@OmitFromCoverage` with proper unit tests.

Example test structure:
[long code block]

Security-critical code should have the highest test coverage, not be omitted.
```

**Why this is wrong:**
- All content visible immediately (code examples, problems list, rationale)
- Creates visual clutter in PR conversation
- Makes it hard to scan multiple issues quickly

**Correct approach:**
```markdown
❌ **CRITICAL**: Missing test coverage for security-critical code

<details>
<summary>Details and fix</summary>

The `@OmitFromCoverage` annotation excludes this entire class from test coverage.

**Problems:**
1. No validation that certificate hashes match actual Bitwarden certificates
2. No verification of fail-closed behavior on edge cases
3. No tests for multiple signer rejection logic
4. Certificate hash typos would go undetected until production

**Recommendation:**
Replace `@OmitFromCoverage` with proper unit tests.

Example test structure:
[code block]

Security-critical code should have the highest test coverage, not be omitted.
</details>
```

**Key difference:** Only severity + one-line description visible. All details collapsed.

---

## Summary

**Always use:**
- Minimal summary (verdict + critical issues)
- Separate inline comments with `<details>` tags
- Hybrid emoji + text severity prefixes
- Focus exclusively on actionable feedback

**Never use:**
- Multiple summary sections (Strengths, Recommendations, etc.)
- Praise-only inline comments
- Duplication between summary and inline comments
- Verbose analysis in summary (belongs in inline comments)
