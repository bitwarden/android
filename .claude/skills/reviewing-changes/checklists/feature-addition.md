# Feature Addition Review Checklist

## Multi-Pass Strategy

### First Pass: High-Level Assessment

**1. Understand the feature:**
- Read PR description - what problem does this solve?
- Identify user-facing changes vs internal changes
- Note any security implications (auth, encryption, data handling)

**2. Scan file structure:**
- Which modules affected? (app, data, network, ui, core?)
- Are files organized correctly per module structure?
- Any new public APIs introduced?

**3. Initial risk assessment:**
- Does this touch sensitive data or security-critical paths?
- Does this affect existing features or only add new ones?
- Are there obvious compilation or null safety issues?

### Second Pass: Architecture Deep-Dive

**4. MVVM + UDF Pattern Compliance:**
- ViewModels properly structured?
- State management using StateFlow?
- Business logic in correct layer?

**5. Dependency Injection:**
- Hilt DI used correctly?
- Dependencies injected, not manually instantiated?
- Proper scoping applied?

**6. Module Organization:**
- Code placed in correct modules?
- No circular dependencies introduced?
- Proper separation of concerns?

**7. Error Handling:**
- Using Result types, not exception-based handling?
- Errors propagated correctly through layers?

### Third Pass: Details and Quality

**8. Testing:**
- Unit tests for ViewModels and repositories?
- Test coverage for edge cases and error scenarios?
- Tests verify behavior, not implementation?

**9. Code Quality:**
- Null safety handled properly?
- Public APIs have KDoc documentation?
- Naming follows project conventions?

**10. Security:**
- Sensitive data encrypted properly?
- Authentication/authorization handled correctly?
- Zero-knowledge architecture preserved?

## Architecture Review

Read `reference/architectural-patterns.md` for full patterns and code examples.

**Check these four areas:**
- **MVVM/UDF**: ViewModel exposes `StateFlow` (not `MutableStateFlow`), business logic in Repository, UI is stateless
- **Hilt DI**: `@HiltViewModel` + `@Inject constructor`, inject interfaces not implementations, no manual instantiation
- **Module placement**: UI in `:ui`/`:app`, data in `:data`, network in `:network`, no circular dependencies
- **Error handling**: `Result<T>` / `runCatching` throughout — no thrown exceptions from data layer

## Security Review

Reference: `docs/ARCHITECTURE.md#security`

**Critical Security Checks:**

- **Sensitive data encrypted**: Passwords, keys, tokens use Android Keystore or EncryptedSharedPreferences
- **No plaintext secrets**: No passwords/keys in logs, memory dumps, or SharedPreferences
- **Input validation**: All user-provided data validated and sanitized
- **Authentication tokens**: Securely stored and transmitted
- **Zero-knowledge architecture**: Encryption happens client-side, server never sees plaintext

**Red Flags:**
```kotlin
// ❌ CRITICAL - Plaintext storage
sharedPreferences.edit {
    putString("pin", userPin)  // Must use EncryptedSharedPreferences
}

// ❌ CRITICAL - Logging sensitive data
Log.d("Auth", "Password: $password")  // Never log sensitive data

// ❌ CRITICAL - Weak encryption
val cipher = Cipher.getInstance("DES")  // Use AES-256-GCM

// ✅ GOOD - Keystore encryption
val encryptedData = keystoreManager.encrypt(sensitiveData)
secureStorage.store(encryptedData)
```

**If security concerns found, classify as CRITICAL using `reference/priority-framework.md`**

## Testing Review

Reference: `reference/testing-patterns.md`

**Required Test Coverage:**

- **ViewModels**: Unit tests for state transitions, actions, error scenarios
- **Repositories**: Unit tests for data transformations, error handling
- **Business logic**: Unit tests for complex algorithms, calculations
- **Edge cases**: Null inputs, empty states, network failures, concurrent operations

**Test Quality:**
```kotlin
// ✅ GOOD - Tests behavior
@Test
fun `when login succeeds then state updates to success`() = runTest {
    val viewModel = LoginViewModel(mockRepository)

    coEvery { mockRepository.login(any(), any()) } returns Result.success(User())

    viewModel.onLoginClicked("user", "pass")

    viewModel.state.test {
        assertEquals(LoginState.Success, awaitItem())
    }
}

// ❌ BAD - Tests implementation
@Test
fun `repository is called with correct parameters`() {
    // This is testing internal implementation, not behavior
}
```

**Testing Frameworks:**
- JUnit 5 for test structure
- MockK for mocking
- Turbine for Flow testing
- Kotlinx-coroutines-test for coroutine testing

## Code Quality

### Null Safety

- No `!!` (non-null assertion) without clear safety guarantee
- Platform types (from Java) handled with explicit nullability
- Nullable types have proper null checks or use safe operators (`?.`, `?:`)

```kotlin
// ❌ BAD - Unsafe assertion
val result = apiService.getData()!!  // Could crash

// ✅ GOOD - Safe handling
val result = apiService.getData() ?: return State.Error("No data")

// ❌ BAD - Platform type unchecked
val intent: Intent = getIntent()  // Could be null from Java
intent.getStringExtra("key")  // Potential NPE

// ✅ GOOD - Explicit nullability
val intent: Intent? = getIntent()
intent?.getStringExtra("key")
```

### Documentation

- **Public APIs**: Have KDoc comments explaining purpose, parameters, return values
- **Complex algorithms**: Explained in comments
- **Non-obvious behavior**: Documented with rationale

```kotlin
// ✅ GOOD - Documented public API
/**
 * Encrypts the given data using AES-256-GCM with a key from Android Keystore.
 *
 * @param plaintext The data to encrypt
 * @return Result containing encrypted data or encryption error
 */
suspend fun encrypt(plaintext: ByteArray): Result<EncryptedData>
```

### Style Compliance

Reference: `docs/STYLE_AND_BEST_PRACTICES.md`

Only flag style issues if:
- Not caught by linters (Detekt, ktlint)
- Have architectural implications
- Significantly impact readability

Skip minor formatting (spaces, line breaks, etc.) - linters handle this.

## Prioritizing Findings

Use `reference/priority-framework.md` to classify findings as Critical/Important/Suggested/Optional.

## Providing Feedback

Use `reference/review-psychology.md` for phrasing guidance.

**Key principles:**
- **Ask questions** for design decisions: "Can we use the existing BitwardenTextField component here?"
- **Be prescriptive** for clear violations: "Change MutableStateFlow to StateFlow (MVVM pattern requirement)"
- **Explain rationale**: "This exposes mutable state, violating unidirectional data flow"
- **Use I-statements**: "It's hard for me to understand this logic without comments"
- **Avoid condescension**: Don't use "just", "simply", "obviously"

## Output Format

See `examples/review-outputs.md` for the required output format and inline comment structure.
