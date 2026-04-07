# Bug Fix Review Checklist

## Multi-Pass Strategy

### First Pass: Understand the Bug

**1. Understand root cause:**
- What was the broken behavior?
- What caused it?
- How does this fix address the root cause?

**2. Assess scope:**
- How many files changed?
- Is this a targeted fix or broader refactoring?
- Does this affect multiple features?

**3. Check for side effects:**
- Could this break other features?
- Are there edge cases not considered?

### Second Pass: Verify the Fix

**4. Code changes:**
- Does the fix make sense?
- Is it the simplest solution?
- Any unnecessary changes included?

**5. Testing:**
- Is there a regression test?
- Does test verify the bug is fixed?
- Are edge cases covered?

**6. Related code:**
- Same pattern in other places that might have same bug?
- Should other similar code be fixed too?

## What to CHECK

✅ **Root Cause Analysis**
- Does the fix address the root cause or just symptoms?
- Is the explanation in PR/commit clear?

✅ **Regression Testing**
- Is there a new test that would fail without this fix?
- Does test cover the reported bug scenario?
- Are related edge cases tested?

✅ **Side Effects**
- Could this break existing functionality?
- Are there similar code paths that need checking?
- Does this change behavior in unexpected ways?

✅ **Fix Scope**
- Is the fix appropriately scoped (not too broad, not too narrow)?
- Are all instances of the bug fixed?
- Any related bugs discovered during investigation?

## What to SKIP

❌ **Full Architecture Review** - Unless fix reveals architectural problems
❌ **Comprehensive Testing Review** - Focus on regression tests, not entire test suite
❌ **Major Refactoring Suggestions** - Unless directly related to preventing similar bugs

## Red Flags

🚩 **No test for the bug** - How will we prevent regression?
🚩 **Fix doesn't match root cause** - Is this fixing symptoms?
🚩 **Broad changes beyond the bug** - Should this be split into separate PRs?
🚩 **Similar patterns elsewhere** - Should those be fixed too?

## Key Questions to Ask

Use `reference/review-psychology.md` for phrasing:

- "Can we add a test that would fail without this fix?"
- "I see this pattern in [other file] - does it have the same issue?"
- "Is this fixing the root cause or masking the symptom?"
- "Could this change affect [related feature]?"

## Prioritizing Findings

Use `reference/priority-framework.md` to classify findings as Critical/Important/Suggested/Optional.

## Output Format

See `examples/review-outputs.md` for the required output format and inline comment structure.

## Example Review

```markdown
**Overall Assessment:** APPROVE

See inline comments for suggested improvements.
```

**Inline comment examples:**

```
**data/auth/BiometricRepository.kt:120** - SUGGESTED: Extract null handling

<details>
<summary>Details</summary>

Root cause analysis: BiometricPrompt result was nullable but code assumed non-null, causing crash on cancellation (PM-12345).

Consider extracting null handling pattern:

\```kotlin
private fun handleBiometricResult(result: BiometricPrompt.AuthenticationResult?): AuthResult {
    return result?.let { AuthResult.Success(it) } ?: AuthResult.Cancelled
}
\```

This pattern could be reused if we add other biometric auth points.
</details>
```

```
**app/auth/BiometricViewModel.kt:89** - SUGGESTED: Add regression test

<details>
<summary>Details</summary>

Add test for cancellation scenario to prevent regression:

\```kotlin
@Test
fun `when biometric cancelled then returns cancelled state`() = runTest {
    coEvery { repository.authenticate() } returns Result.failure(CancelledException())
    viewModel.onBiometricAuth()
    assertEquals(AuthState.Cancelled, viewModel.state.value)
}
\```

This prevents regression of the bug just fixed.
</details>
```
