# Review Psychology: Constructive Feedback Phrasing

Effective code review feedback is clear, actionable, and constructive. This guide provides phrasing patterns for inline comments.

## Table of Contents

**Guidelines:**
- [Core Directives](#core-directives)
- [Phrasing Templates](#phrasing-templates)
  - [Critical Issues (Prescriptive)](#critical-issues-prescriptive)
  - [Suggested Improvements (Exploratory)](#suggested-improvements-exploratory)
  - [Questions (Collaborative)](#questions-collaborative)
  - [Test Suggestions](#test-suggestions)
- [When to Be Prescriptive vs Ask Questions](#when-to-be-prescriptive-vs-ask-questions)
- [Special Cases](#special-cases)

---

## Core Directives

- **Keep positive feedback minimal**: For clean PRs with no issues, use 2-3 line approval only. When acknowledging good practices in PRs with issues, use single bullet list with no elaboration. Never create elaborate sections praising correct implementations.
- Ask questions for design decisions, be prescriptive for clear violations
- Focus on code, not people ("This code..." not "You...")
- Use I-statements for subjective feedback ("Hard for me to understand...")
- Explain rationale with every recommendation
- Avoid: "just", "simply", "obviously", "easy"

---

## Phrasing Templates

### Critical Issues (Prescriptive)

**Pattern**: State problem + Provide solution + Explain why

```
**[file:line]** - CRITICAL: [Issue description]

[Specific fix with code example if applicable]

[Rationale explaining why this is critical]

Reference: [docs link if applicable]
```

**Example**:
```
**data/vault/VaultRepository.kt:145** - CRITICAL: PIN stored without encryption

PIN must be encrypted using Android Keystore, not stored in plaintext SharedPreferences.
Plaintext storage exposes the PIN to backup systems and rooted devices.

Reference: docs/ARCHITECTURE.md#security
```

---

### Suggested Improvements (Exploratory)

**Pattern**: Observe + Suggest + Explain benefit

```
**[file:line]** - Consider [alternative approach]

[Current observation]
Can we [specific suggestion]?

[Benefit or rationale]
```

**Example**:
```
**app/login/LoginScreen.kt:89** - Consider using existing BitwardenButton

This custom button implementation looks similar to `ui/components/BitwardenButton.kt:45`.
Can we use the existing component to maintain consistency across the app?
```

---

### Questions (Collaborative)

**Pattern**: Ask + Provide context (optional)

```
**[file:line]** - [Question about intent or approach]?

[Optional context or observation]
```

**Example**:
```
**data/sync/SyncManager.kt:234** - How does this handle concurrent sync attempts?

It looks like multiple coroutines could call `startSync()` simultaneously.
Is there a mechanism to prevent race conditions, or is that handled elsewhere?
```

---

### Test Suggestions

**Pattern**: Observe gap + Suggest specific test + Provide skeleton

```
**[file:line]** - Consider adding test for [scenario]

[Rationale]

```kotlin
@Test
fun `test description`() = runTest {
    // Test skeleton
}
```
```

**Example**:
```
**data/auth/BiometricRepository.kt** - Consider adding test for cancellation scenario

This would prevent regression of the bug you just fixed:

```kotlin
@Test
fun `when biometric cancelled then returns cancelled state`() = runTest {
    coEvery { biometricPrompt.authenticate() } returns null

    val result = repository.authenticate()

    assertEquals(AuthResult.Cancelled, result)
}
```
```

---

## When to Be Prescriptive vs Ask Questions

**Be Prescriptive** (Tell them what to do):
- Security issues
- Architecture pattern violations
- Null safety problems
- Compilation errors
- Documented project standards

**Ask Questions** (Seek explanation):
- Design decisions with multiple valid approaches
- Performance trade-offs without data
- Unclear intent or reasoning
- Scope decisions (this PR vs future work)
- Patterns not documented in project guidelines

---

## Special Cases

**Nitpicks** - For truly minor suggestions, use "Nit:" prefix:
```
**Nit**: Extra blank line at line 145
```

**Uncertainty** - If unsure, acknowledge it:
```
I'm not certain, but this might be called frequently.
Has this been profiled?
```

**Positive Feedback** - Brief list only, no elaboration:
```
## Good Practices
- Proper Hilt DI usage throughout
- Comprehensive unit test coverage
- Clear separation of concerns
```
