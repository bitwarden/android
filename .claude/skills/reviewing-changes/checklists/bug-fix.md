# Bug Fix Review Checklist

## Inline Comment Requirement

Create separate inline comment for EACH specific issue on the exact line (`file:line_number`).
Do NOT create one large summary comment. Do NOT update existing comments.
After inline comments, provide one summary comment.

---

## Multi-Pass Strategy

### First Pass: Understand the Bug

<thinking>
Before evaluating the fix:
1. What was the original bug/broken behavior?
2. What is the expected correct behavior?
3. What was the root cause?
4. How was the bug discovered? (user report, test, production)
5. What's the severity? (crash, data loss, UI glitch, minor annoyance)
</thinking>

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

<thinking>
Evaluate the fix systematically:
1. Does this fix address the root cause or just symptoms?
2. Are there edge cases not covered?
3. Could this break other functionality?
4. Is the fix localized or does it ripple through the codebase?
5. How do we prevent this bug from returning?
</thinking>

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

```markdown
## Summary
Fixes [bug description] by [solution approach]

Root cause: [Brief explanation]

## Critical Issues

List blocking issues with file:line references and specific solutions.

## Suggested Improvements

**[file:line]** - Add regression test
Can we add a test that verifies this specific bug doesn't reoccur?
```kotlin
@Test
fun `test that verifies bug is fixed`() {
    // Test the exact scenario that was broken
}
```

**[file:line]** - Consider similar code paths
The pattern in [other file:line] looks similar - does it have the same issue?

## Good Practices
- Targeted fix addresses root cause
- Includes regression test

## Action Items
1. Add regression test for the fixed bug
2. Check [related code] for similar issues
```

## Example Review

```markdown
## Summary
Fixes crash when biometric prompt is cancelled (PM-12345)

Root cause: BiometricPrompt result was nullable but code assumed non-null

## Critical Issues
None

## Suggested Improvements

**data/auth/BiometricRepository.kt:120** - Consider extracting null handling
```kotlin
private fun handleBiometricResult(result: BiometricPrompt.AuthenticationResult?): AuthResult {
    return result?.let { AuthResult.Success(it) }
        ?: AuthResult.Cancelled
}
```
This pattern could be reused if we add other biometric auth points.

**app/auth/BiometricViewModel.kt:89** - Add test for cancellation scenario
```kotlin
@Test
fun `when biometric cancelled then returns cancelled state`() = runTest {
    coEvery { repository.authenticate() } returns Result.failure(CancelledException())

    viewModel.onBiometricAuth()

    assertEquals(AuthState.Cancelled, viewModel.state.value)
}
```
This prevents regression of the bug you just fixed.

## Good Practices
- Added null safety check
- Proper error state propagation

## Action Items
1. Consider adding test for cancellation scenario (prevents regression)
2. Evaluate if null handling helper would benefit other auth flows
```
