# Finding Priority Framework

Use this framework to classify findings during code review. Clear prioritization helps authors triage and address issues effectively.

## Table of Contents

**Severity Categories:**
- [❌ CRITICAL (Blocker - Must Fix Before Merge)](#critical-blocker---must-fix-before-merge)
- [⚠️ IMPORTANT (Should Fix)](#important-should-fix)
- [♻️ DEBT (Technical Debt)](#debt-technical-debt)
- [🎨 SUGGESTED (Nice to Have)](#suggested-nice-to-have)
- [💭 QUESTION (Seeking Clarification)](#question-seeking-clarification)
- [Optional (Acknowledge But Don't Require)](#optional-acknowledge-but-dont-require)

**Guidelines:**
- [Classification Guidelines](#classification-guidelines)
  - [When Something is Between Categories](#when-something-is-between-categories)
  - [Context Matters](#context-matters)
- [Examples by Change Type](#examples-by-change-type)
- [Special Cases](#special-cases)
- [Summary](#summary)

---

## ❌ **CRITICAL** (Blocker - Must Fix Before Merge)

These issues **must** be addressed before the PR can be merged. They pose immediate risks to security, stability, or architecture integrity.

### Security
- Data leaks or plaintext sensitive data (passwords, keys, tokens)
- Weak encryption or insecure key storage
- Missing authentication or authorization checks
- Input injection vulnerabilities (SQL, XSS, command injection)
- Sensitive data in logs or error messages

**Example**:
```
**data/vault/VaultRepository.kt:145** - CRITICAL: PIN stored without encryption
PIN must be encrypted using Android Keystore, not stored in plaintext SharedPreferences.
Reference: docs/ARCHITECTURE.md#security
```

### Stability
- Compilation errors or warnings
- Null pointer exceptions in production paths
- Resource leaks (file handles, network connections, memory)
- Crashes or unhandled exceptions in critical paths
- Thread safety violations

**Example**:
```
**app/auth/BiometricRepository.kt:120** - CRITICAL: Missing null safety check
biometricPrompt result can be null. Add explicit null check to prevent crash.
```

### Architecture
- Mutable state exposure in ViewModels (violates MVVM)
- Exception-based error handling in business logic (should use Result)
- Circular dependencies between modules
- Violation of zero-knowledge principles
- Direct dependency instantiation (should use DI)

**Example**:
```
**app/login/LoginViewModel.kt:45** - CRITICAL: Exposes mutable state
Change MutableStateFlow to StateFlow in public API to prevent external state mutation.
This violates MVVM encapsulation pattern.
```

---

## ⚠️ **IMPORTANT** (Should Fix)

These issues should be addressed but don't block merge if there's a compelling reason. They improve code quality, maintainability, or robustness.

### Testing
- Missing tests for critical paths (authentication, encryption, data sync)
- Missing tests for new public APIs
- Tests that don't verify actual behavior (test implementation, not behavior)
- Missing test coverage for error scenarios

**Example**:
```
**data/auth/BiometricRepository.kt** - IMPORTANT: Missing test for cancellation
Add test for user cancellation scenario to prevent regression.
```

### Architecture
- Inconsistent patterns within PR (mixing error handling approaches)
- Poor separation of concerns
- Tight coupling between components
- Not following established project patterns

**Example**:
```
**app/vault/VaultViewModel.kt:89** - IMPORTANT: Business logic in ViewModel
Encryption logic should be in Repository, not ViewModel.
Reference: docs/ARCHITECTURE.md#mvvm-pattern
```

### Documentation
- Undocumented public APIs (missing KDoc)
- Missing documentation for complex algorithms
- Unclear naming or confusing interfaces

**Example**:
```
**core/crypto/EncryptionManager.kt:34** - IMPORTANT: Missing KDoc
Public encryption method should document parameters, return value, and exceptions.
```

### Performance
- Inefficient algorithms in hot paths (with evidence from profiling)
- Blocking main thread with I/O operations
- Memory inefficient data structures (with evidence)

**Example**:
```
**app/vault/VaultListViewModel.kt:78** - IMPORTANT: N+1 query pattern
Fetching items one-by-one in loop. Consider batch fetch to reduce database queries.
```

---

## ♻️ **DEBT** (Technical Debt)

Code that duplicates existing patterns, violates established conventions, or will require rework within 6 months. Introduces technical debt that should be tracked for future cleanup.

### Duplication
- Copy-pasted code blocks across files
- Repeated validation or business logic
- Multiple implementations of same pattern
- Data transformation duplicated in multiple places

**Example**:
```
**app/vault/VaultListViewModel.kt:156** - DEBT: Duplicates encryption logic
Same encryption pattern exists in VaultRepository.kt:234 and SyncManager.kt:89.
Extract to shared EncryptionUtil to reduce maintenance burden.
```

### Convention Violations
- Inconsistent error handling approaches within same module
- Mixing architectural patterns (MVVM + MVC)
- Not following established DI patterns
- Deviating from project code style significantly

**Example**:
```
**data/auth/AuthRepository.kt:78** - DEBT: Exception-based error handling
Project standard is Result<T> for error handling. This uses try-catch with throws.
Creates inconsistency and makes testing harder.
Reference: docs/ARCHITECTURE.md#error-handling
```

### Future Rework Required
- Hardcoded values that should be configurable
- Temporary workarounds without TODO/FIXME
- Code that will need changes when planned features arrive
- Tight coupling that prevents future extensibility

**Example**:
```
**app/settings/SettingsViewModel.kt:45** - DEBT: Hardcoded feature flags
Feature flags should come from remote config for A/B testing.
Will require rework when experimentation framework launches.
```

---

## 🎨 **SUGGESTED** (Nice to Have)

These are improvement opportunities but not required. Consider the effort vs. benefit before requesting changes.

### Code Quality
- Minor style inconsistencies (if not caught by linter)
- Opportunities for DRY improvements
- Better variable naming for clarity
- Simplification opportunities

**Example**:
```
**app/vault/VaultScreen.kt:145** - SUGGESTED: Consider extracting helper function
This 20-line block appears in 3 places. Consider extracting to reduce duplication.
```

### Testing
- Additional test coverage for edge cases (beyond critical paths)
- More comprehensive integration tests
- Performance tests for non-critical paths

**Example**:
```
**app/login/LoginViewModelTest.kt** - SUGGESTED: Add test for concurrent login attempts
Not critical, but would increase confidence in edge case handling.
```

### Refactoring
- Extracting reusable patterns
- Modernizing old patterns (if touching related code)
- Improving testability

**Example**:
```
**data/vault/VaultRepository.kt:200** - SUGGESTED: Consider extracting validation logic
Could be extracted to separate validator class for reusability and testing.
```

---

## 💭 **QUESTION** (Seeking Clarification)

Questions about requirements, unclear intent, or potential conflicts that require human knowledge to answer. Open inquiries that cannot be resolved through code inspection alone.

### Requirements Clarification
- Ambiguous acceptance criteria
- Multiple valid implementation approaches
- Unclear business rules or edge case handling
- Conflicting requirements between specs and implementation

**Example**:
```
**app/vault/ItemListViewModel.kt:67** - QUESTION: Expected sort behavior for equal timestamps?
When items have identical timestamps, should secondary sort be by:
- Name (alphabetical)
- Creation order
- Item type priority

Spec doesn't specify tie-breaking logic.
```

### Design Decisions
- Architecture choices that could go multiple ways
- Trade-offs between approaches without clear winner
- Feature flag strategy or rollout approach
- API design with multiple valid patterns

**Example**:
```
**data/sync/SyncManager.kt:134** - QUESTION: Should sync failures retry automatically?
Current implementation fails immediately. Options:
- Exponential backoff (3 retries)
- User-triggered retry only
- Background retry on network restore

What's the expected UX?
```

### System Integration
- Unclear contracts with external systems
- Potential conflicts with other features/modules
- Assumptions about third-party API behavior
- Cross-team coordination needs

**Example**:
```
**app/auth/BiometricPrompt.kt:89** - QUESTION: Compatibility with pending device credentials PR?
PR #1234 is refactoring device credentials. Should this:
- Merge first and adapt later
- Wait for #1234 to land
- Coordinate with that author

Timing unclear.
```

### Testing Strategy
- Uncertainty about test scope or approach
- Questions about mocking external dependencies
- Edge cases that need product input
- Performance testing requirements

**Example**:
```
**data/vault/EncryptionTest.kt:45** - QUESTION: Should we test against real Keystore?
Currently using mocked Keystore. Real Keystore testing would:
+ Catch hardware-specific issues
- Slow down CI significantly
- Require API 23+ emulators

What's the priority?
```

---

## Optional (Acknowledge But Don't Require)

Note good practices to reinforce positive patterns. Keep these **brief** - list only, no elaboration.

### Good Practices

**Format**: Simple bullet list, no explanation

```markdown
## Good Practices
- Proper Hilt DI usage throughout
- Comprehensive unit test coverage
- Clear separation of concerns
- Well-documented public APIs
```

**Don't do this** (too verbose):
```markdown
## Good Practices
- Proper Hilt DI usage throughout: Great job using @Inject constructor and injecting interfaces! This follows our established patterns perfectly and makes the code very testable. Really excellent work here! 👍
```

---

## Classification Guidelines

### When Something is Between Categories

**If unsure between Critical and Important**:
- Ask: "Could this cause production incidents, data loss, or security breaches?"
- If yes → Critical
- If no → Important

**If unsure between Important and Debt**:
- Ask: "Is this a bug/defect or just duplication/inconsistency?"
- If bug/defect → Important
- If duplication/inconsistency → Debt

**If unsure between Important and Suggested**:
- Ask: "Would I block merge over this?"
- If yes → Important
- If no → Suggested

**If unsure between Debt and Suggested**:
- Ask: "Will this require rework within 6 months?"
- If yes → Debt
- If no → Suggested

**If unsure between Suggested and Question**:
- Ask: "Am I requesting a change or asking for clarification?"
- If requesting change → Suggested
- If seeking clarification → Question

**If unsure between Suggested and Optional**:
- Ask: "Is this actionable feedback or just acknowledgment?"
- If actionable → Suggested
- If acknowledgment → Optional

### Context Matters

**Same issue, different contexts**:

```
// Critical for production code
Missing null safety check in auth flow → CRITICAL

// Suggested for internal test utility
Missing null safety check in test helper → SUGGESTED
```

**Same pattern, different risk levels**:

```
// Critical for new feature
Missing tests for new auth method → CRITICAL

// Important for bug fix
Missing regression test → IMPORTANT

// Suggested for refactoring
Missing tests for refactored helper → SUGGESTED
```

---

## Examples by Change Type

### Dependency Update
- **Critical**: Known CVEs in old version not addressed
- **Important**: Breaking changes that need migration
- **Suggested**: Beta/alpha version stability concerns

### Bug Fix
- **Critical**: Fix doesn't address root cause
- **Important**: Missing regression test
- **Suggested**: Similar bugs in related code

### Feature Addition
- **Critical**: Security vulnerabilities, architecture violations
- **Important**: Missing tests for critical paths
- **Suggested**: Additional test coverage, minor refactoring

### UI Refinement
- **Critical**: Missing accessibility for key actions
- **Important**: Not using theme (hardcoded colors)
- **Suggested**: Minor spacing/alignment improvements

### Refactoring
- **Critical**: Changes behavior (should be behavior-preserving)
- **Important**: Incomplete migration (mix of old/new patterns)
- **Suggested**: Additional instances that could be refactored

### Infrastructure
- **Critical**: Hardcoded secrets, no rollback plan
- **Important**: Performance regression in build times
- **Suggested**: Further optimization opportunities

---

## Special Cases

### Technical Debt
- Acknowledge existing tech debt but don't require fixing in unrelated PR
- Exception: If change makes tech debt worse, it's Important to address

### Scope Creep
- Don't request changes outside PR scope
- Can note as "Future consideration" but not required for this PR

### Linter-Catchable Issues
- Don't flag issues that automated tools handle
- Exception: If linter is misconfigured and missing real issues

### Personal Preferences
- Don't flag unless grounded in project standards or architectural principles
- Use "I-statements" if suggesting alternative approaches

---

## Summary

**Critical**: Block merge, must fix (security, stability, architecture)
**Important**: Should fix before merge (testing, quality, performance)
**Debt**: Technical debt introduced, track for future cleanup
**Suggested**: Nice to have, consider effort vs benefit
**Question**: Seeking clarification on requirements or design
**Optional**: Acknowledge good practices, keep brief
