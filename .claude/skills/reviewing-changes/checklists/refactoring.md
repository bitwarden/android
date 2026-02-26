# Refactoring Review Checklist

## Multi-Pass Strategy

### First Pass: Understand the Refactoring

<thinking>
Analyze the refactoring scope:
1. What pattern is being improved?
2. Why is this refactoring needed?
3. Does this change behavior or just structure?
4. What's the scope? (files affected, migration completeness)
5. What are the risks if something breaks?
</thinking>

**1. Understand the goal:**
- What pattern is being improved?
- Why is this refactoring needed?
- What's the scope of changes?

**2. Assess completeness:**
- Are all instances refactored or just some?
- Are there related areas that should also change?
- Is the migration complete or partial?

**3. Risk assessment:**
- Does this change behavior?
- How many files affected?
- Are tests updated to reflect changes?

### Second Pass: Verify Consistency

<thinking>
Verify refactoring quality:
1. Is the new pattern applied consistently throughout?
2. Are there missed instances of the old pattern?
3. Do tests still pass with same behavior?
4. Is the migration complete or partial?
5. Does this introduce any new issues?
</thinking>

**4. Pattern consistency:**
- Is the new pattern applied consistently throughout?
- Are there missed instances of the old pattern?
- Does this match established project patterns?

**5. Migration completeness:**
- Old pattern fully removed or deprecated?
- All usages updated?
- Documentation updated?

**6. Test coverage:**
- Do tests still pass?
- Are tests refactored to match?
- Does behavior remain unchanged?

## What to CHECK

✅ **Pattern Consistency**
- New pattern applied consistently across all touched code
- Follows established project patterns (MVVM, DI, error handling)
- No mix of old and new patterns

✅ **Migration Completeness**
- All instances of old pattern updated?
- Deprecated methods removed or marked @Deprecated?
- Related code also updated (tests, docs)?

✅ **Behavior Preservation**
- Refactoring doesn't change behavior
- Tests still pass
- Edge cases still handled

✅ **Deprecation Strategy** (if applicable)
- Old APIs marked @Deprecated with migration guidance
- Replacement clearly documented
- Timeline for removal specified

## What to SKIP

❌ **Suggesting Additional Refactorings** - Unless directly related to current changes
❌ **Scope Creep** - Don't request refactoring of untouched code
❌ **Perfection** - Better code is better than perfect code

## Red Flags

🚩 **Incomplete migration** - Mix of old and new patterns
🚩 **Behavior changes** - Refactoring shouldn't change behavior
🚩 **Broken tests** - Tests should be updated to match refactoring
🚩 **Undocumented pattern** - New pattern should be clear to team

## Key Questions to Ask

Use `reference/review-psychology.md` for phrasing:

- "I see the old pattern still used in [file:line] - should that be updated too?"
- "Can we add @Deprecated to the old method with migration guidance?"
- "How do we ensure this behavior remains the same?"
- "Should this pattern be documented in ARCHITECTURE.md?"

## Common Refactoring Patterns

### Extract Interface/Repository

```kotlin
// ✅ GOOD - Complete migration
interface FeatureRepository {
    suspend fun getData(): Result<Data>
}

class FeatureRepositoryImpl @Inject constructor(
    private val apiService: FeatureApiService
) : FeatureRepository {
    override suspend fun getData(): Result<Data> = runCatching {
        apiService.fetchData()
    }
}

// All usages updated to inject interface
class FeatureViewModel @Inject constructor(
    private val repository: FeatureRepository  // Interface
) : ViewModel()

// ❌ BAD - Incomplete migration
// Some files still inject FeatureRepositoryImpl directly
```

### Modernize Error Handling

```kotlin
// ✅ GOOD - Complete migration
// Old exception-based removed
suspend fun fetchData(): Result<Data> = runCatching {
    apiService.getData()
}

// All call sites updated
repository.fetchData().fold(
    onSuccess = { /* handle */ },
    onFailure = { /* handle */ }
)

// ❌ BAD - Mixed patterns
// Some functions use Result, others still throw exceptions
```

### Extract Reusable Component

```kotlin
// ✅ GOOD - Complete extraction
// Component moved to :ui module
@Composable
fun BitwardenButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)

// All usages updated to use new component
// Old inline button implementations removed

// ❌ BAD - Incomplete extraction
// Some screens use new component, others still have inline implementation
```

## Prioritizing Findings

Use `reference/priority-framework.md` to classify findings as Critical/Important/Suggested/Optional.

## Output Format

Follow the format guidance from `SKILL.md` Step 5 (concise summary with critical issues only, detailed inline comments with `<details>` tags).

```markdown
**Overall Assessment:** APPROVE / REQUEST CHANGES

**Critical Issues** (if any):
- [One-line summary of each critical blocking issue with file:line reference]

See inline comments for all issue details.
```

## Example Reviews

### Example 1: Refactoring with Incomplete Migration

**Context**: Refactoring authentication to Repository pattern, but one ViewModel still uses old pattern

**Summary Comment:**
```markdown
**Overall Assessment:** REQUEST CHANGES

**Critical Issues:**
- Incomplete migration (app/vault/VaultViewModel.kt:89)

See inline comments for details.
```

**Inline Comment 1** (on `app/vault/VaultViewModel.kt:89`):
```markdown
**IMPORTANT**: Incomplete migration

<details>
<summary>Details and fix</summary>

This ViewModel still injects AuthManager directly. Should it use AuthRepository like the other 11 ViewModels?

\```kotlin
// Current (old pattern)
class VaultViewModel @Inject constructor(
    private val authManager: AuthManager
)

// Should be (new pattern)
class VaultViewModel @Inject constructor(
    private val authRepository: AuthRepository
)
\```

This is the only ViewModel still using the old pattern.
</details>
```

**Inline Comment 2** (on `data/auth/AuthManager.kt:1`):
```markdown
**SUGGESTED**: Add deprecation notice

<details>
<summary>Details</summary>

Can we add @Deprecated to AuthManager to guide future development?

\```kotlin
@Deprecated(
    message = "Use AuthRepository interface instead",
    replaceWith = ReplaceWith("AuthRepository"),
    level = DeprecationLevel.WARNING
)
class AuthManager @Inject constructor(...)
\```

This helps prevent new code from using the old pattern.
</details>
```

---

### Example 2: Clean Refactoring (No Issues)

**Context**: Refactoring with complete migration, all patterns followed correctly, tests passing

**Review Comment:**
```markdown
**Overall Assessment:** APPROVE

Clean refactoring moving ExitManager to :ui module. Follows established patterns, eliminates duplication, tests updated correctly.
```

**Token count:** ~30 tokens (vs ~800 for verbose format)

**Why this works:**
- 3 lines total
- Clear approval decision
- Briefly notes what was done
- No elaborate sections, checkmarks, or excessive praise
- Author gets immediate green light to merge

**What NOT to do for clean refactorings:**
```markdown
❌ DO NOT create these sections:

## Summary
This PR successfully refactors ExitManager into shared code...

## Key Strengths
- ✅ Follows established module organization patterns
- ✅ Removes code duplication between apps
- ✅ Improves test coverage
- ✅ Maintains consistent behavior
[...20 more checkmarks...]

## Code Quality & Architecture
**Architectural Compliance:** ✅
- Correctly places manager in :ui module
- Follows established pattern for UI-layer managers
[...detailed analysis...]

## Changes
- ✅ Moved ExitManager interface from app → ui module
- ✅ Moved ExitManagerImpl from app → ui module
[...listing every file...]
```

This is excessive. **For clean PRs: 2-3 lines maximum.**
