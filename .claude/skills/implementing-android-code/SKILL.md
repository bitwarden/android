---
name: implementing-android-code
version: 0.1.1
description: This skill should be used when implementing Android code in Bitwarden. Covers critical patterns, gotchas, and anti-patterns unique to this codebase. Triggered by "How do I implement a ViewModel?", "Create a new screen", "Add navigation", "Write a repository", "BaseViewModel pattern", "State-Action-Event", "type-safe navigation", "@Serializable route", "SavedStateHandle persistence", "process death recovery", "handleAction", "sendAction", "Hilt module", "Repository pattern", "implementing a screen", "adding a data source", "handling navigation", "encrypted storage", "security patterns", "Clock injection", "DataState", or any questions about implementing features, screens, ViewModels, data sources, or navigation in the Bitwarden Android app.
---

# Implementing Android Code - Bitwarden Quick Reference

**This skill provides tactical guidance for Bitwarden-specific patterns.** For comprehensive architecture decisions and complete code style rules, consult `docs/ARCHITECTURE.md` and `docs/STYLE_AND_BEST_PRACTICES.md`.

---

## Critical Patterns Reference

### A. ViewModel Implementation (State-Action-Event Pattern)

All ViewModels follow the **State-Action-Event (SAE)** pattern via `BaseViewModel<State, Event, Action>`.

**Key Requirements:**
- Annotate with `@HiltViewModel`
- State class MUST be `@Parcelize data class : Parcelable`
- Implement `handleAction(action: A)` - MUST be synchronous
- Post internal actions from coroutines using `sendAction()`
- Save/restore state via `SavedStateHandle[KEY_STATE]`
- Private action handlers: `private fun handle*` naming convention

**Template**: See [ViewModel template](templates.md#viewmodel-template-state-action-event-pattern)

**Pattern Summary:**
```kotlin
@HiltViewModel
class ExampleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ExampleRepository,
) : BaseViewModel<ExampleState, ExampleEvent, ExampleAction>(
    initialState = savedStateHandle[KEY_STATE] ?: ExampleState()
) {
    init {
        stateFlow.onEach { savedStateHandle[KEY_STATE] = it }.launchIn(viewModelScope)
    }

    override fun handleAction(action: ExampleAction) {
        // Synchronous dispatch only
        when (action) {
            is Action.Click -> handleClick()
            is Action.Internal.DataReceived -> handleDataReceived(action)
        }
    }

    private fun handleClick() {
        viewModelScope.launch {
            val result = repository.fetchData()
            sendAction(Action.Internal.DataReceived(result))  // Post internal action
        }
    }

    private fun handleDataReceived(action: Action.Internal.DataReceived) {
        mutableStateFlow.update { it.copy(data = action.result) }
    }
}
```

**Reference:**
- `ui/src/main/kotlin/com/bitwarden/ui/platform/base/BaseViewModel.kt` (see `handleAction` method)
- `app/src/main/kotlin/com/x8bit/bitwarden/ui/auth/feature/login/LoginViewModel.kt` (see class declaration)

**Critical Gotchas:**
- ❌ **NEVER** update `mutableStateFlow` directly inside coroutines
- ✅ **ALWAYS** post internal actions from coroutines, update state in `handleAction()`
- ❌ **NEVER** forget `@IgnoredOnParcel` for sensitive data (causes security leak)
- ✅ **ALWAYS** use `@Parcelize` on state classes for process death recovery
- ✅ State restoration happens automatically if properly saved to `SavedStateHandle`

---

### B. Navigation Implementation (Type-Safe)

All navigation uses **type-safe routes** with kotlinx.serialization.

**Pattern Structure:**
1. `@Serializable` route data class with parameters
2. `...Args` helper class for extracting from `SavedStateHandle`
3. `NavGraphBuilder.{screen}Destination()` extension for adding screen to graph
4. `NavController.navigateTo{Screen}()` extension for navigation calls

**Template**: See [Navigation template](templates.md#navigation-template-type-safe-routes)

**Pattern Summary:**
```kotlin
@Serializable
data class ExampleRoute(val userId: String, val isEditMode: Boolean = false)

data class ExampleArgs(val userId: String, val isEditMode: Boolean)

fun SavedStateHandle.toExampleArgs(): ExampleArgs {
    val route = this.toRoute<ExampleRoute>()
    return ExampleArgs(userId = route.userId, isEditMode = route.isEditMode)
}

fun NavController.navigateToExample(userId: String, isEditMode: Boolean = false, navOptions: NavOptions? = null) {
    this.navigate(route = ExampleRoute(userId, isEditMode), navOptions = navOptions)
}

fun NavGraphBuilder.exampleDestination(onNavigateBack: () -> Unit) {
    composableWithSlideTransitions<ExampleRoute> {
        ExampleScreen(onNavigateBack = onNavigateBack)
    }
}
```

**Reference:** `app/src/main/kotlin/com/x8bit/bitwarden/ui/auth/feature/login/LoginNavigation.kt` (see `LoginRoute` and extensions)

**Key Benefits:**
- ✅ Type safety: Compile-time errors for missing parameters
- ✅ No string literals in navigation code
- ✅ Automatic serialization/deserialization
- ✅ Clear contract for screen dependencies

---

### C. Screen/Compose Implementation

All screens follow consistent Compose patterns.

**Template**: See [Screen/Compose template](templates.md#screencompose-template)

**Key Patterns:**
```kotlin
@Composable
fun ExampleScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExampleViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            ExampleEvent.NavigateBack -> onNavigateBack()
        }
    }

    BitwardenScaffold(
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(R.string.title),
                navigationIcon = rememberVectorPainter(BitwardenDrawable.ic_back),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(ExampleAction.BackClick) }
                },
            )
        },
    ) {
        // UI content
    }
}
```

**Reference:** `app/src/main/kotlin/com/x8bit/bitwarden/ui/auth/feature/login/LoginScreen.kt` (see `LoginScreen` composable)

**Essential Requirements:**
- ✅ Use `hiltViewModel()` for dependency injection
- ✅ Use `collectAsStateWithLifecycle()` for state (not `collectAsState()`)
- ✅ Use `EventsEffect(viewModel)` for one-shot events
- ✅ Use `remember(viewModel) { }` for stable callbacks to prevent recomposition
- ✅ Use `Bitwarden*` prefixed components from `:ui` module

**State Hoisting Rules:**
- **ViewModel state**: Data that needs to survive process death or affects business logic
- **UI-only state**: Temporary UI state (scroll position, text field focus) using `remember` or `rememberSaveable`

---

### D. Data Layer Implementation

The data layer follows strict patterns for repositories, managers, and data sources.

**Interface + Implementation Separation (ALWAYS)**

**Template**: See [Data Layer template](templates.md#data-layer-template-repository--hilt-module)

**Pattern Summary:**
```kotlin
// Interface (injected via Hilt)
interface ExampleRepository {
    suspend fun fetchData(id: String): ExampleResult
    val dataFlow: StateFlow<DataState<ExampleData>>
}

// Implementation (NOT directly injected)
class ExampleRepositoryImpl(
    private val exampleDiskSource: ExampleDiskSource,
    private val exampleService: ExampleService,
) : ExampleRepository {
    override suspend fun fetchData(id: String): ExampleResult {
        // NO exceptions thrown - return Result or sealed class
        return exampleService.getData(id).fold(
            onSuccess = { ExampleResult.Success(it.toModel()) },
            onFailure = { ExampleResult.Error(it.message) },
        )
    }
}

// Sealed result class (domain-specific)
sealed class ExampleResult {
    data class Success(val data: ExampleData) : ExampleResult()
    data class Error(val message: String?) : ExampleResult()
}

// Hilt Module
@Module
@InstallIn(SingletonComponent::class)
object ExampleRepositoryModule {
    @Provides
    @Singleton
    fun provideExampleRepository(
        exampleDiskSource: ExampleDiskSource,
        exampleService: ExampleService,
    ): ExampleRepository = ExampleRepositoryImpl(exampleDiskSource, exampleService)
}
```

**Reference:**
- `app/src/main/kotlin/com/x8bit/bitwarden/data/auth/repository/AuthRepository.kt`
- `app/src/main/kotlin/com/x8bit/bitwarden/data/tools/generator/repository/di/GeneratorRepositoryModule.kt`

**Three-Layer Data Architecture:**
1. **Data Sources** - Raw data access (network, disk, SDK). Return `Result<T>`, never throw.
2. **Managers** - Single responsibility business logic. Wrap OS/external services.
3. **Repositories** - Aggregate sources/managers. Return domain-specific sealed classes.

**Critical Rules:**
- ❌ **NEVER** throw exceptions in data layer
- ✅ **ALWAYS** use interface + `...Impl` pattern
- ✅ **ALWAYS** inject interfaces, never implementations
- ✅ Data sources return `Result<T>`, repositories return domain sealed classes
- ✅ Use `StateFlow` for continuously observed data

---

### E. UI Components

**Use Existing Components First:**

The `:ui` module provides reusable `Bitwarden*` prefixed components. Search before creating new ones.

**Common Components:**
- `BitwardenFilledButton` - Primary action buttons
- `BitwardenOutlinedButton` - Secondary action buttons
- `BitwardenTextField` - Text input fields
- `BitwardenPasswordField` - Password input with show/hide
- `BitwardenSwitch` - Toggle switches
- `BitwardenTopAppBar` - Toolbar/app bar
- `BitwardenScaffold` - Screen container with scaffold
- `BitwardenBasicDialog` - Simple dialogs
- `BitwardenLoadingDialog` - Loading indicators

**Component Discovery:**
Search `ui/src/main/kotlin/com/bitwarden/ui/platform/components/` for existing `Bitwarden*` components. See **Codebase Discovery** in `CLAUDE.md` for search commands.

**When to Create New Reusable Components:**
- Component used in 3+ places
- Component needs consistent theming across app
- Component has semantic meaning (accessibility)
- Component has complex state management

**New Component Requirements:**
- Prefix with `Bitwarden`
- Accept themed colors/styles from `BitwardenTheme`
- Include preview composables for testing
- Support accessibility (content descriptions, semantics)

**String Resources:**

New strings belong in the `:ui` module: `ui/src/main/res/values/strings.xml`

- Use typographic apostrophes and quotes to avoid escape characters: `you’ll` not `you\'ll`, `“word”` not `\"word\"`
- Reference strings via generated `BitwardenString` resource IDs
- Do not add strings to other modules unless explicitly instructed

---

### F. Security Patterns

**Encrypted vs Unencrypted Storage:**

**Template**: See [Security templates](templates.md#security-templates)

**Pattern Summary:**
```kotlin
class ExampleDiskSourceImpl(
    @EncryptedPreferences encryptedSharedPreferences: SharedPreferences,
    @UnencryptedPreferences sharedPreferences: SharedPreferences,
) : BaseEncryptedDiskSource(
    encryptedSharedPreferences = encryptedSharedPreferences,
    sharedPreferences = sharedPreferences,
),
    ExampleDiskSource {
    fun storeAuthToken(token: String) {
        putEncryptedString(KEY_TOKEN, token)  // Sensitive — uses base class method
    }

    fun storeThemePreference(isDark: Boolean) {
        putBoolean(KEY_THEME, isDark)  // Non-sensitive — uses base class method
    }
}
```

**Android Keystore (Biometric Keys):**
- User-scoped encryption keys: `BiometricsEncryptionManager`
- Keys stored in Android Keystore (hardware-backed when available)
- Integrity validation on biometric state changes

**Input Validation:**
```kotlin
// Validation returns boolean, NEVER throws
interface RequestValidator {
    fun validate(request: Request): Boolean
}

// Sanitization removes dangerous content
fun String?.sanitizeTotpUri(issuer: String?, username: String?): String? {
    if (this.isNullOrBlank()) return null
    // Sanitize and return safe value
}
```

**Security Checklist:**
- ✅ Use `@EncryptedPreferences` for credentials, keys, tokens
- ✅ Use `@UnencryptedPreferences` for UI state, preferences
- ✅ Use `@IgnoredOnParcel` for sensitive ViewModel state
- ❌ **NEVER** log sensitive data (passwords, tokens, vault items)
- ✅ Validate all user input before processing
- ✅ Use Timber for non-sensitive logging only

---

### G. Testing Patterns

**ViewModel Testing:**

**Template**: See [Testing templates](templates.md#testing-templates)

**Pattern Summary:**
```kotlin
class ExampleViewModelTest : BaseViewModelTest() {
    private val mockRepository: ExampleRepository = mockk()

    @Test
    fun `ButtonClick should fetch data and update state`() = runTest {
        val expectedResult = ExampleResult.Success(data = "test")
        coEvery { mockRepository.fetchData(any()) } returns expectedResult

        val viewModel = createViewModel()
        viewModel.trySendAction(ExampleAction.ButtonClick)

        viewModel.stateFlow.test {
            assertEquals(EXPECTED_STATE.copy(data = "test"), awaitItem())
        }
    }

    private fun createViewModel(): ExampleViewModel = ExampleViewModel(
        savedStateHandle = SavedStateHandle(mapOf(KEY_STATE to EXPECTED_STATE)),
        repository = mockRepository,
    )
}
```

**Reference:** `app/src/test/kotlin/com/x8bit/bitwarden/ui/tools/feature/generator/GeneratorViewModelTest.kt`

**Key Testing Patterns:**
- ✅ Extend `BaseViewModelTest` for proper dispatcher management
- ✅ Use `runTest` from `kotlinx.coroutines.test`
- ✅ Use Turbine's `.test { awaitItem() }` for Flow assertions
- ✅ Use MockK: `coEvery` for suspend functions, `every` for sync
- ✅ Test both state changes and event emissions
- ✅ Test both success and failure Result paths

**Flow Testing with Turbine:**
```kotlin
// Test state and events simultaneously
viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
    viewModel.trySendAction(ExampleAction.Submit)
    assertEquals(ExpectedState.Loading, stateFlow.awaitItem())
    assertEquals(ExampleEvent.ShowSuccess, eventFlow.awaitItem())
}
```

**MockK Quick Reference:**
```kotlin
coEvery { repository.fetchData(any()) } returns Result.success("data")  // Suspend
every { diskSource.getData() } returns "cached"  // Sync
coVerify { repository.fetchData("123") }  // Verify
```

---

### H. Clock/Time Handling

All code needing current time must inject `Clock` for testability.

**Key Requirements:**
- ✅ Inject `Clock` via Hilt in ViewModels
- ✅ Pass `Clock` as parameter in extension functions
- ✅ Use `clock.instant()` to get current time
- ❌ Never call `Instant.now()` or `DateTime.now()` directly
- ❌ Never use `mockkStatic` for datetime classes in tests

**Pattern Summary:**
```kotlin
// ViewModel with Clock
class MyViewModel @Inject constructor(
    private val clock: Clock,
) {
    val timestamp = clock.instant()
}

// Extension function with Clock parameter
fun State.getTimestamp(clock: Clock): Instant =
    existingTime ?: clock.instant()

// Test with fixed clock
val FIXED_CLOCK = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC
)
```

**Reference:**
- `docs/STYLE_AND_BEST_PRACTICES.md` (see Time and Clock Handling section)
- `core/src/main/kotlin/com/bitwarden/core/di/CoreModule.kt` (see `provideClock` function)

**Critical Gotchas:**
- ❌ `Instant.now()` creates hidden dependency, non-testable
- ❌ `mockkStatic(Instant::class)` is fragile, can leak between tests
- ✅ `Clock.fixed(...)` provides deterministic test behavior

---

## Bitwarden-Specific Anti-Patterns

**General anti-patterns are documented in CLAUDE.md.** This section covers violations specific to Bitwarden's State-Action-Event, navigation, and data layer patterns:

❌ **NEVER update ViewModel state directly in coroutines**
- Post internal actions, update state synchronously in `handleAction()`

❌ **NEVER inject `...Impl` classes**
- Only inject interfaces via Hilt

❌ **NEVER create navigation without `@Serializable` routes**
- No string-based navigation, always type-safe

❌ **NEVER use raw `Result<T>` in repositories**
- Use domain-specific sealed classes for better error handling

❌ **NEVER make state classes without `@Parcelize`**
- All ViewModel state must survive process death

❌ **NEVER skip `SavedStateHandle` persistence for ViewModels**
- Users lose form progress on process death

❌ **NEVER forget `@IgnoredOnParcel` for passwords/tokens**
- Causes security vulnerability (sensitive data in parcel)

❌ **NEVER use generic `Exception` catching**
- Catch specific exceptions only (`RemoteException`, `IOException`)

❌ **NEVER call `Instant.now()` or `DateTime.now()` directly**
- Inject `Clock` via Hilt, use `clock.instant()` for testability

---

## Quick Reference

For build, test, and codebase discovery commands, see the **Codebase Discovery**, **Testing**, and **Deployment** sections in `CLAUDE.md`.

**File Reference Format:**
When pointing to specific code, use: `file_path:line_number`

Example: `ui/src/main/kotlin/com/bitwarden/ui/platform/base/BaseViewModel.kt` (see `handleAction` method)

