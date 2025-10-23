---
name: reviewing-changes
description: Performs comprehensive code reviews for Bitwarden Android projects, verifying architecture compliance, style guidelines, compilation safety, test coverage, and security requirements. Use when reviewing pull requests, checking commits, analyzing code changes, verifying Bitwarden coding standards, evaluating MVVM patterns, checking Hilt DI usage, reviewing security implementations, or assessing test coverage. Automatically invoked by CI pipeline or manually for interactive code reviews.
---

# Reviewing Changes

## Instructions

Follow this process to review code changes for Bitwarden Android:

### Step 1: Understand Context

Start with high-level assessment of the change's purpose and approach. Read PR/commit descriptions and understand what problem is being solved.

### Step 2: Verify Compliance

Systematically check each area against Bitwarden standards documented in `CLAUDE.md`:

1. **Architecture**: Follow patterns in `docs/ARCHITECTURE.md`
   - MVVM + UDF (ViewModels with `StateFlow`, Compose UI)
   - Hilt DI (interface injection, `@HiltViewModel`)
   - Repository pattern and proper data flow

2. **Style**: Adhere to `docs/STYLE_AND_BEST_PRACTICES.md`
   - Naming conventions, code organization, formatting
   - Kotlin idioms (immutability, null safety, coroutines)

3. **Compilation**: Analyze for potential build issues
   - Import statements and dependencies
   - Type safety and null safety
   - API compatibility and deprecation warnings
   - Resource references and manifest requirements

4. **Testing**: Verify appropriate test coverage
   - Unit tests for business logic and utility functions
   - Integration tests for complex workflows
   - UI tests for user-facing features when applicable
   - Test coverage for edge cases and error scenarios

5. **Security**: Given Bitwarden's security-focused nature
   - Proper handling of sensitive data
   - Secure storage practices (Android Keystore)
   - Authentication and authorization patterns
   - Data encryption and decryption flows
   - Zero-knowledge architecture preservation

### Step 3: Document Findings

Identify specific violations with `file:line_number` references. Be precise about locations.

### Step 4: Provide Recommendations

Give actionable recommendations for improvements. Explain why changes are needed and suggest specific solutions.

### Step 5: Flag Critical Issues

Highlight issues that must be addressed before merge. Distinguish between blockers and suggestions.

### Step 6: Acknowledge Quality

Note well-implemented patterns (briefly, without elaboration). Keep positive feedback concise.

## Review Anti-Patterns (DO NOT)

- Be nitpicky about linter-catchable style issues
- Review without understanding context - ask for clarification first
- Focus only on new code - check surrounding context for issues
- Request changes outside the scope of this changeset

## Examples

### Good Review Format

```markdown
## Summary
This PR adds biometric authentication to the login flow, implementing MVVM pattern with proper state management.

## Critical Issues
- `app/login/LoginViewModel.kt:45` - Mutable state exposed; use `StateFlow` instead of `MutableStateFlow`
- `data/auth/BiometricRepository.kt:120` - Missing null safety check on `biometricPrompt` result

## Suggested Improvements
- Consider extracting biometric prompt logic to separate use case class
- Add integration tests for biometric failure scenarios
- `app/login/LoginScreen.kt:89` - Consider using existing `BitwardenButton` component

## Good Practices
- Proper Hilt DI usage throughout
- Comprehensive unit test coverage
- Clear separation of concerns

## Action Items
1. Fix mutable state exposure in `LoginViewModel`
2. Add null safety check in `BiometricRepository`
3. Consider adding integration tests for error flows
```

### What to Focus On

**DO focus on:**
- Architecture violations (incorrect patterns)
- Security issues (data handling, encryption)
- Missing tests for critical paths
- Compilation risks (type safety, null safety)

**DON'T focus on:**
- Minor formatting (handled by linters)
- Personal preferences without architectural basis
- Issues outside the changeset scope
