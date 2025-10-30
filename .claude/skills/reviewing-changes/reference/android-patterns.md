# Android Patterns Quick Reference

Quick reference for common Bitwarden Android patterns during code reviews. For comprehensive details, read `docs/ARCHITECTURE.md` and `docs/STYLE_AND_BEST_PRACTICES.md`.

## Inline Comment Requirement

Create separate inline comment for EACH specific issue on the exact line (`file:line_number`).
Do NOT create one large summary comment. Do NOT update existing comments.
Use code examples from this reference in your inline comments.

---

## MVVM + UDF Pattern

### ViewModel Structure

**✅ GOOD - Proper state encapsulation**:
```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val repository: FeatureRepository
) : ViewModel() {
    // Private mutable state
    private val _state = MutableStateFlow<FeatureState>(FeatureState.Initial)

    // Public immutable state
    val state: StateFlow<FeatureState> = _state.asStateFlow()

    // Actions as functions
    fun onActionClicked() {
        viewModelScope.launch {
            repository.performAction().fold(
                onSuccess = { data -> _state.value = FeatureState.Success(data) },
                onFailure = { error -> _state.value = FeatureState.Error(error) }
            )
        }
    }
}
```

**❌ BAD - Common violations**:
```kotlin
class FeatureViewModel : ViewModel() {
    // ❌ Exposes mutable state
    val state: MutableStateFlow<FeatureState>

    // ❌ Business logic in ViewModel
    fun onSubmit() {
        val encrypted = encryptionManager.encrypt(data)  // Should be in Repository
        _state.value = FeatureState.Success
    }

    // ❌ Direct Android framework dependency
    fun onCreate(context: Context) {  // ViewModels shouldn't depend on Context
        // ...
    }
}
```

**Key Rules**:
- Expose `StateFlow<T>`, never `MutableStateFlow<T>`
- Delegate business logic to Repository/Manager
- No direct Android framework dependencies (except ViewModel, SavedStateHandle)
- Use `viewModelScope` for coroutines

Reference: `docs/ARCHITECTURE.md#mvvm-pattern`

---

### UI Layer (Compose)

**✅ GOOD - Stateless, observes only**:
```kotlin
@Composable
fun FeatureScreen(
    state: FeatureState,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        when (state) {
            is FeatureState.Loading -> LoadingIndicator()
            is FeatureState.Success -> SuccessContent(state.data)
            is FeatureState.Error -> ErrorMessage(state.error)
        }

        BitwardenButton(
            text = "Action",
            onClick = onActionClick  // Sends event to ViewModel
        )
    }
}
```

**❌ BAD - Stateful, modifies state**:
```kotlin
@Composable
fun FeatureScreen(viewModel: FeatureViewModel) {
    var localState by remember { mutableStateOf(...) }  // ❌ State in UI

    Button(onClick = {
        viewModel._state.value = FeatureState.Loading  // ❌ Directly modifying ViewModel state
    })
}
```

**Key Rules**:
- Compose screens observe state, never modify
- User actions passed as events/callbacks to ViewModel
- No business logic in UI layer
- Use existing components from `:ui` module

---

## Hilt Dependency Injection

### ViewModels

**✅ GOOD - Interface injection**:
```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val repository: FeatureRepository,  // Interface, not implementation
    private val authManager: AuthManager,
    savedStateHandle: SavedStateHandle
) : ViewModel()
```

**❌ BAD - Common violations**:
```kotlin
// ❌ No @HiltViewModel annotation
class FeatureViewModel @Inject constructor(...)

// ❌ Injecting implementation instead of interface
class FeatureViewModel @Inject constructor(
    private val repository: FeatureRepositoryImpl  // Should inject interface
)

// ❌ Manual instantiation
class FeatureViewModel : ViewModel() {
    private val repository = FeatureRepositoryImpl()  // Should use @Inject
}
```

**Key Rules**:
- Annotate with `@HiltViewModel`
- Use `@Inject constructor`
- Inject interfaces, not implementations
- Use `SavedStateHandle` for process death survival

Reference: `docs/ARCHITECTURE.md#dependency-injection`

---

### Repositories and Managers

**✅ GOOD - Implementation with @Inject**:
```kotlin
interface FeatureRepository {
    suspend fun fetchData(): Result<Data>
}

class FeatureRepositoryImpl @Inject constructor(
    private val apiService: FeatureApiService,
    private val database: FeatureDao
) : FeatureRepository {
    override suspend fun fetchData(): Result<Data> = runCatching {
        apiService.getData()
    }
}
```

**Module provides interface**:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindFeatureRepository(
        impl: FeatureRepositoryImpl
    ): FeatureRepository
}
```

**Key Rules**:
- Define interface for abstraction
- Implementation uses `@Inject constructor`
- Module binds implementation to interface
- Appropriate scoping (`@Singleton`, `@ViewModelScoped`)

---

## Module Organization

```
android/
├── core/           # Shared utilities (cryptography, analytics, logging)
├── data/           # Repositories, database, domain models
├── network/        # API clients, network utilities
├── ui/             # Reusable Compose components, theme
├── app/            # Application, feature screens, ViewModels
└── authenticator/  # Authenticator app (separate from password manager)
```

**Correct Placement**:
- UI screens and ViewModels → `:app`
- Reusable Compose components → `:ui`
- Data models and Repositories → `:data`
- API services → `:network`
- Cryptography, logging → `:core`

**Check for**:
- No circular dependencies
- Correct module placement
- Proper visibility (internal vs public)

Reference: `docs/ARCHITECTURE.md#module-structure`

---

## Error Handling

### Use Result Types, Not Exceptions

**✅ GOOD - Result-based**:
```kotlin
// Repository
suspend fun fetchData(): Result<Data> = runCatching {
    apiService.getData()
}

// ViewModel
fun onFetch() {
    viewModelScope.launch {
        repository.fetchData().fold(
            onSuccess = { data -> _state.value = State.Success(data) },
            onFailure = { error -> _state.value = State.Error(error) }
        )
    }
}
```

**❌ BAD - Exception-based in business logic**:
```kotlin
// ❌ Don't throw in business logic
suspend fun fetchData(): Data {
    try {
        return apiService.getData()
    } catch (e: Exception) {
        throw FeatureException(e)  // Don't throw in repositories
    }
}

// ❌ Try-catch in ViewModel
fun onFetch() {
    viewModelScope.launch {
        try {
            val data = repository.fetchData()
            _state.value = State.Success(data)
        } catch (e: Exception) {
            _state.value = State.Error(e)
        }
    }
}
```

**Key Rules**:
- Use `Result<T>` return types in repositories
- Use `runCatching { }` to wrap API calls
- Handle results with `.fold()` in ViewModels
- Don't throw exceptions in business logic

Reference: `docs/ARCHITECTURE.md#error-handling`

---

## Security Patterns

### Encryption and Key Storage

**✅ GOOD - Android Keystore**:
```kotlin
// Sensitive data encrypted with Keystore
class SecureStorage @Inject constructor(
    private val keystoreManager: KeystoreManager
) {
    suspend fun storePin(pin: String): Result<Unit> = runCatching {
        val encrypted = keystoreManager.encrypt(pin.toByteArray())
        securePreferences.putBytes(KEY_PIN, encrypted)
    }
}

// Or use EncryptedSharedPreferences
val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

**❌ BAD - Plaintext or weak encryption**:
```kotlin
// ❌ CRITICAL - Plaintext storage
sharedPreferences.edit {
    putString("pin", userPin)  // Never store sensitive data in plaintext
}

// ❌ CRITICAL - Weak encryption
val cipher = Cipher.getInstance("DES")  // Use AES-256-GCM

// ❌ CRITICAL - Hardcoded keys
val key = "my_secret_key_123"  // Use Android Keystore
```

**Key Rules**:
- Use Android Keystore for encryption keys
- Use EncryptedSharedPreferences for simple key-value storage
- Use AES-256-GCM for encryption
- Never store sensitive data in plaintext
- Never hardcode encryption keys

Reference: `docs/ARCHITECTURE.md#security`

---

### Logging Sensitive Data

**✅ GOOD - No sensitive data**:
```kotlin
Log.d(TAG, "Authentication attempt for user")
Log.d(TAG, "Vault sync completed with ${items.size} items")
```

**❌ BAD - Logs sensitive data**:
```kotlin
// ❌ CRITICAL
Log.d(TAG, "Password: $password")
Log.d(TAG, "Auth token: $token")
Log.d(TAG, "PIN: $pin")
Log.d(TAG, "Encryption key: ${key.encoded}")
```

**Key Rules**:
- Never log passwords, PINs, tokens, keys
- Never log encryption keys or sensitive data
- Be careful with error messages (don't include sensitive context)

---

## Testing Patterns

### ViewModel Tests

**✅ GOOD - Tests behavior**:
```kotlin
@Test
fun `when login succeeds then state updates to success`() = runTest {
    // Arrange
    val viewModel = LoginViewModel(mockRepository)
    coEvery { mockRepository.login(any(), any()) } returns Result.success(User())

    // Act
    viewModel.onLoginClicked("user@example.com", "password")

    // Assert
    viewModel.state.test {
        assertEquals(LoginState.Loading, awaitItem())
        assertEquals(LoginState.Success, awaitItem())
    }
}
```

**❌ BAD - Tests implementation**:
```kotlin
@Test
fun `repository is called with correct parameters`() {
    // ❌ This tests implementation details, not behavior
    viewModel.onLoginClicked("user", "pass")
    coVerify { mockRepository.login("user", "pass") }
}
```

**Key Rules**:
- Test behavior, not implementation
- Use `runTest` for coroutine tests
- Use Turbine for Flow testing
- Use MockK for mocking

---

### Repository Tests

**✅ GOOD - Tests data transformations**:
```kotlin
@Test
fun `fetchItems maps API response to domain model`() = runTest {
    // Arrange
    val apiResponse = listOf(ApiItem(id = "1", name = "Test"))
    coEvery { apiService.getItems() } returns apiResponse

    // Act
    val result = repository.fetchItems()

    // Assert
    assertTrue(result.isSuccess)
    assertEquals(
        listOf(DomainItem(id = "1", name = "Test")),
        result.getOrThrow()
    )
}
```

**Key Rules**:
- Test data transformations
- Test error handling (network failures, API errors)
- Test caching behavior if applicable
- Mock API services and databases

Reference: Project uses JUnit 5, MockK, Turbine, kotlinx-coroutines-test

---

## Null Safety

**✅ GOOD - Safe handling**:
```kotlin
// Safe call with elvis operator
val result = apiService.getData() ?: return State.Error("No data")

// Let with safe call
intent?.getStringExtra("key")?.let { value ->
    processValue(value)
}

// Require with message
val data = requireNotNull(response.data) { "Response data must not be null" }
```

**❌ BAD - Unsafe assertions**:
```kotlin
// ❌ Unsafe - can crash
val result = apiService.getData()!!

// ❌ Platform type unchecked
val intent: Intent = getIntent()  // Could be null from Java
val value = intent.getStringExtra("key")  // Potential NPE
```

**Key Rules**:
- Avoid `!!` unless safety is guaranteed (rare)
- Handle platform types with explicit nullability
- Use safe calls (`?.`), elvis operator (`?:`), or explicit checks
- Use `requireNotNull` with descriptive message if crash is acceptable

---

## Compose UI Patterns

### Component Reuse

**✅ GOOD - Uses existing components**:
```kotlin
BitwardenButton(
    text = "Submit",
    onClick = onSubmit
)

BitwardenTextField(
    value = text,
    onValueChange = onTextChange,
    label = "Email"
)
```

**❌ BAD - Duplicates existing components**:
```kotlin
// ❌ Recreating BitwardenButton
Button(
    onClick = onSubmit,
    colors = ButtonDefaults.buttonColors(
        containerColor = BitwardenTheme.colorScheme.primary
    )
) {
    Text("Submit")
}
```

**Key Rules**:
- Check `:ui` module for existing components before creating custom ones
- Use BitwardenButton, BitwardenTextField, etc. for consistency
- Place new reusable components in `:ui` module

---

### Theme Usage

**✅ GOOD - Uses theme**:
```kotlin
Text(
    text = "Title",
    style = BitwardenTheme.typography.titleLarge,
    color = BitwardenTheme.colorScheme.primary
)

Spacer(modifier = Modifier.height(16.dp))  // Standard spacing
```

**❌ BAD - Hardcoded values**:
```kotlin
Text(
    text = "Title",
    style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),  // Use theme
    color = Color(0xFF0066FF)  // Use theme color
)

Spacer(modifier = Modifier.height(17.dp))  // Non-standard spacing
```

**Key Rules**:
- Use `BitwardenTheme.colorScheme` for colors
- Use `BitwardenTheme.typography` for text styles
- Use standard spacing (4.dp, 8.dp, 16.dp, 24.dp)

---

## Quick Checklist

Use this when reviewing code:

### Architecture
- [ ] ViewModels expose StateFlow, not MutableStateFlow?
- [ ] Business logic in Repository, not ViewModel?
- [ ] Using Hilt DI (@HiltViewModel, @Inject constructor)?
- [ ] Injecting interfaces, not implementations?
- [ ] Correct module placement?

### Error Handling
- [ ] Using Result types, not exceptions in business logic?
- [ ] Errors handled with .fold() in ViewModels?

### Security
- [ ] Sensitive data encrypted with Keystore?
- [ ] No plaintext passwords/keys?
- [ ] No sensitive data in logs?

### Testing
- [ ] ViewModels have unit tests?
- [ ] Tests verify behavior, not implementation?
- [ ] Edge cases covered?

### Code Quality
- [ ] Null safety handled properly (no `!!` without guarantee)?
- [ ] Public APIs have KDoc?
- [ ] Following naming conventions?

---

For comprehensive details, always refer to:
- `docs/ARCHITECTURE.md` - Full architecture patterns
- `docs/STYLE_AND_BEST_PRACTICES.md` - Complete style guide
