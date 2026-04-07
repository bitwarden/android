# Architectural Patterns Quick Reference

Quick reference for Bitwarden Android architectural patterns during code reviews. For comprehensive details, read `docs/ARCHITECTURE.md` and `docs/STYLE_AND_BEST_PRACTICES.md`.

## Table of Contents

**Core Patterns:**
- [MVVM + UDF Pattern](#mvvm--udf-pattern)
  - [ViewModel Structure](#viewmodel-structure)
  - [UI Layer (Compose)](#ui-layer-compose)
- [Hilt Dependency Injection](#hilt-dependency-injection)
  - [ViewModels](#viewmodels)
  - [Repositories and Managers](#repositories-and-managers)
  - [Clock/Time Handling](#clocktime-handling)
- [Module Organization](#module-organization)
- [Error Handling](#error-handling)
  - [Use Result Types, Not Exceptions](#use-result-types-not-exceptions)
- [Quick Checklist](#quick-checklist)

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

    // Actions as functions, state updated via internal action
    fun onActionClicked() {
        viewModelScope.launch {
            val result = repository.performAction()
            sendAction(FeatureAction.Internal.ActionComplete(result))
        }
    }

    // The ViewModel has a handler that processes the internal action
    private fun handleInternalAction(action: FeatureAction.Internal) {
        when (action) {
            is FeatureAction.Internal.ActionComplete -> {
                // The action handler evaluates the result and updates state
                action.result.fold(
                    onSuccess = { _state.value = State.Success },
                    onFailure = { _state.value = State.Error(it) }
                )
            }
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

### Clock/Time Handling

Time-dependent code must use injected `Clock` rather than direct `Instant.now()` or `DateTime.now()` calls. This follows the same DI principle as other dependencies.

**✅ GOOD - Injected Clock**:
```kotlin
// ViewModel with Clock injection
class MyViewModel @Inject constructor(
    private val clock: Clock,
) {
    fun save() {
        val timestamp = clock.instant()
    }
}

// Extension function with Clock parameter
fun State.getTimestamp(clock: Clock): Instant =
    existingTime ?: clock.instant()
```

**❌ BAD - Static/direct calls**:
```kotlin
// Hidden dependency, non-testable
val timestamp = Instant.now()
val dateTime = DateTime.now()
```

**Key Rules**:
- Inject `Clock` via Hilt constructor (like other dependencies)
- Pass `Clock` as parameter to extension functions
- `Clock` is provided via `CoreModule` as singleton
- Enables deterministic testing with `Clock.fixed(...)`

Reference: `docs/STYLE_AND_BEST_PRACTICES.md#best-practices--time-and-clock-handling`

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
        val result = repository.fetchData()
        sendAction(FeatureAction.Internal.FetchComplete(result))
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
            sendAction(FeatureAction.Internal.FetchComplete(data))
        } catch (e: Exception) {
            sendAction(FeatureAction.Internal.FetchComplete(e))
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

## Quick Checklist

### Architecture
- [ ] ViewModels expose StateFlow, not MutableStateFlow?
- [ ] Business logic in Repository, not ViewModel?
- [ ] Using Hilt DI (@HiltViewModel, @Inject constructor)?
- [ ] Injecting interfaces, not implementations?
- [ ] Time-dependent code uses injected `Clock` (not `Instant.now()`)?
- [ ] Correct module placement?

### Error Handling
- [ ] Using Result types, not exceptions in business logic?
- [ ] Errors handled with .fold() in ViewModels?

---

For comprehensive details, always refer to:
- `docs/ARCHITECTURE.md` - Full architecture patterns
- `docs/STYLE_AND_BEST_PRACTICES.md` - Complete style guide
