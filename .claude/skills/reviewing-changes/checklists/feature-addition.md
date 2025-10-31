# Feature Addition Review Checklist

## Multi-Pass Strategy

### First Pass: High-Level Assessment

<thinking>
Before diving into details:
1. What is this feature supposed to do?
2. How does it fit into the existing architecture?
3. What are the security implications?
4. What's the scope? (files touched, modules affected)
5. What are the highest-risk areas?
</thinking>

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

<thinking>
Verify architectural integrity:
1. Does this follow MVVM + UDF pattern?
2. Is Hilt DI used correctly?
3. Is state management proper (StateFlow, immutability)?
4. Are modules organized correctly?
5. Is error handling robust (Result types)?
</thinking>

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

<thinking>
Check quality and completeness:
1. Is code quality high? (null safety, documentation, naming)
2. Are tests comprehensive? (unit + integration)
3. Are there edge cases not covered?
4. Is documentation clear?
5. Are there any code smells or anti-patterns?
</thinking>

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

### MVVM Pattern Compliance

Read `reference/architectural-patterns.md` for detailed patterns.

**ViewModels must:**
- Use `@HiltViewModel` annotation
- Use `@Inject constructor`
- Expose `StateFlow<T>`, NOT `MutableStateFlow<T>` publicly
- Delegate business logic to Repository/Manager
- Avoid direct Android framework dependencies (except ViewModel, SavedStateHandle)

**Common Violations:**
```kotlin
// ❌ BAD - Exposes mutable state
class FeatureViewModel @Inject constructor() : ViewModel() {
    val state: MutableStateFlow<State> = MutableStateFlow(State.Initial)
}

// ✅ GOOD - Exposes immutable state
class FeatureViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow<State>(State.Initial)
    val state: StateFlow<State> = _state.asStateFlow()
}

// ❌ BAD - Business logic in ViewModel
fun onSubmit() {
    val encrypted = encryptionManager.encrypt(password) // Should be in Repository
    _state.value = State.Success
}

// ✅ GOOD - Business logic in Repository, state updated via internal event
fun onSubmit() {
    viewModelScope.launch {
        // The result of the async operation is captured
        val result = repository.submitData(password)
        // A single event is sent with the result, not updating state directly
        sendAction(FeatureAction.Internal.SubmissionComplete(result))
    }
}

// The ViewModel has a handler that processes the internal event
private fun handleInternalAction(action: FeatureAction.Internal) {
    when (action) {
        is FeatureAction.Internal.SubmissionComplete -> {
            // The event handler evaluates the result and updates state
            action.result.fold(
                onSuccess = { _state.value = State.Success },
                onFailure = { _state.value = State.Error(it) }
            )
        }
    }
}
```

**UI Layer must:**
- Only observe state, never modify
- Pass user actions as events to ViewModel
- Contain no business logic
- Use existing UI components from `:ui` module where possible

### Hilt Dependency Injection

Reference: `docs/ARCHITECTURE.md#dependency-injection`

**Required Patterns:**
- ViewModels: `@HiltViewModel` + `@Inject constructor`
- Repositories: `@Inject constructor` on implementation
- Inject interfaces, not concrete implementations
- Modules must provide proper scoping (`@Singleton`, `@ViewModelScoped`)

**Common Violations:**
```kotlin
// ❌ BAD - Manual instantiation
class FeatureViewModel : ViewModel() {
    private val repository = FeatureRepositoryImpl()
}

// ✅ GOOD - Injected interface
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val repository: FeatureRepository  // Interface, not implementation
) : ViewModel()

// ❌ BAD - Injecting implementation
class FeatureViewModel @Inject constructor(
    private val repository: FeatureRepositoryImpl  // Should inject interface
)

// ✅ GOOD - Interface injection
class FeatureViewModel @Inject constructor(
    private val repository: FeatureRepository  // Interface
)
```

### Module Organization

Reference: `docs/ARCHITECTURE.md#module-structure`

**Correct Placement:**
- `:core` - Shared utilities (cryptography, analytics, logging)
- `:data` - Repositories, database, domain models
- `:network` - API clients, network utilities
- `:ui` - Reusable Compose components, theme
- `:app` - Feature screens, ViewModels, navigation
- `:authenticator` - Authenticator app (separate from password manager)

**Check:**
- UI code in `:ui` or `:app` modules
- Data models in `:data`
- Network clients in `:network`
- No circular dependencies between modules

### Error Handling

Reference: `docs/ARCHITECTURE.md#error-handling`

**Required Pattern - Use Result types:**
```kotlin
// ✅ GOOD - Result type
suspend fun fetchData(): Result<Data> = runCatching {
    apiService.getData()
}

// ViewModel handles Result
repository.fetchData().fold(
    onSuccess = { data -> _state.value = State.Success(data) },
    onFailure = { error -> _state.value = State.Error(error) }
)

// ❌ BAD - Exception-based in business logic
suspend fun fetchData(): Data {
    try {
        return apiService.getData()
    } catch (e: Exception) {
        throw FeatureException(e)  // Don't throw in business logic
    }
}
```

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

Follow the format guidance from `SKILL.md` Step 5 (concise summary with critical issues only, detailed inline comments with `<details>` tags).

See `examples/review-outputs.md` for comprehensive feature review example.

```markdown
**Overall Assessment:** APPROVE / REQUEST CHANGES

**Critical Issues** (if any):
- [One-line summary of each critical blocking issue with file:line reference]

See inline comments for all issue details.
```
