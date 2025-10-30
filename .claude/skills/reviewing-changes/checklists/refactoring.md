# Refactoring Review Checklist

**Review Depth**: Pattern-focused (consistency, completeness, behavior preservation)
**Risk Level**: MEDIUM-HIGH (depends on scope)

## Inline Comment Requirement

Create separate inline comment for EACH specific issue on the exact line (`file:line_number`).
Do NOT create one large summary comment. Do NOT update existing comments.
After inline comments, provide one summary comment.

---

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

```markdown
## Summary
Refactors [what] to use [new pattern] for [reason]

Scope: [X files changed, Y pattern instances updated]

## Critical Issues

List blocking issues with file:line references and specific solutions.

## Suggested Improvements

**[file:line]** - Old pattern still used here
I see the old pattern still in use. Should this be updated to match the refactoring?
```kotlin
// Old pattern at this location
OldClass().doSomething()

// Should use new pattern
newRepository.doSomething()
```

**[file:line]** - Add @Deprecated with migration guidance
```kotlin
@Deprecated(
    message = "Use FeatureRepository interface instead",
    replaceWith = ReplaceWith("featureRepository"),
    level = DeprecationLevel.WARNING
)
class OldFeatureManager
```
This helps other developers understand the migration path.

## Good Practices
[List 2-3 if applicable]
- Pattern applied consistently
- Tests updated to match refactoring
- Behavior preserved

## Action Items
1. Update remaining instances at [files]
2. Add @Deprecated to old pattern
3. Update ARCHITECTURE.md to document new pattern
```

## Example Review

```markdown
## Summary
Refactors authentication flow to use Repository pattern instead of direct Manager access

Scope: 12 files changed, 8 ViewModels updated, Repository interface extracted

## Critical Issues
None - behavior preserved, tests passing

## Suggested Improvements

**app/vault/VaultViewModel.kt:89** - Old pattern still used
This ViewModel still injects AuthManager directly. Should it use AuthRepository like the others?
```kotlin
// Current
class VaultViewModel @Inject constructor(
    private val authManager: AuthManager  // Old pattern
)

// Should be
class VaultViewModel @Inject constructor(
    private val authRepository: AuthRepository  // New pattern
)
```

**data/auth/AuthManager.kt:1** - Add deprecation notice
Can we add @Deprecated to AuthManager to guide future development?
```kotlin
@Deprecated(
    message = "Use AuthRepository interface instead",
    replaceWith = ReplaceWith("AuthRepository"),
    level = DeprecationLevel.WARNING
)
class AuthManager
```

**docs/ARCHITECTURE.md** - Document the new pattern
Should we update the architecture docs to reflect this Repository pattern?
The current docs still reference AuthManager as the recommended approach.

## Good Practices
- Repository interface clearly defined
- All data access methods use Result types
- Tests updated to match new pattern

## Action Items
1. Update VaultViewModel to use AuthRepository
2. Add @Deprecated to AuthManager with migration guidance
3. Update ARCHITECTURE.md to document Repository pattern
```
