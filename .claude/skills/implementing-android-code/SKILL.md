---
name: implementing-android-code
version: 0.1.4
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
- ViewModels that need navigation arguments receive their route via **assisted injection**
  (`@HiltViewModel(assistedFactory = ...)`, `@AssistedInject constructor(@Assisted route: XGraph.XRoute)`)
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
    initialState = savedStateHandle[KEY_STATE] ?: ExampleState(),
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

**ViewModel with navigation arguments (assisted injection):**
```kotlin
@HiltViewModel(assistedFactory = ExampleViewModel.Factory::class)
class ExampleViewModel @AssistedInject constructor(
    @Assisted route: ExampleGraph.ExampleRoute,
    savedStateHandle: SavedStateHandle,
    private val repository: ExampleRepository,
) : BaseViewModel<ExampleState, ExampleEvent, ExampleAction>(
    initialState = savedStateHandle[KEY_STATE] ?: ExampleState(userId = route.userId),
) {
    /**
     * Factory for creating an [ExampleViewModel] for a specific [ExampleGraph.ExampleRoute].
     */
    @AssistedFactory
    interface Factory {
        /**
         * Creates an [ExampleViewModel] for the given [route].
         */
        fun create(route: ExampleGraph.ExampleRoute): ExampleViewModel
    }
    // ...
}
```

**Reference:**
- `ui/src/main/kotlin/com/bitwarden/ui/platform/base/BaseViewModel.kt` (see `handleAction` method)
- `app/src/main/kotlin/com/x8bit/bitwarden/ui/auth/feature/login/LoginViewModel.kt` (see class declaration)
- `app/src/main/kotlin/com/x8bit/bitwarden/ui/vault/feature/item/VaultItemViewModel.kt` (assisted injection of the route)

**Critical Gotchas:**
- ❌ **NEVER** update `mutableStateFlow` directly inside coroutines
- ✅ **ALWAYS** post internal actions from coroutines, update state in `handleAction()`
- ❌ **NEVER** forget `@IgnoredOnParcel` for sensitive data (causes security leak)
- ✅ **ALWAYS** use `@Parcelize` on state classes for process death recovery
- ✅ State restoration happens automatically if properly saved to `SavedStateHandle`

---

### B. Navigation Implementation (Navigation 3, Type-Safe)

All navigation uses **Navigation 3** with **type-safe `@Serializable` routes**. Every `NavDisplay`
displays one **navigation graph**: a `@Serializable sealed class {Name}Graph : NavKey` whose nested
subclasses are its routes (e.g. `AuthGraph.LoginRoute`). The back stack is owned by a
`NavigationController<{Name}Graph>` (`ui/.../platform/navigation/NavigationController.kt`); never
touch a `NavBackStack` directly. A route may host a nested graph (e.g. `RootNavGraph.AuthRoute`
hosts the `AuthGraph`, each nav bar tab hosts its own graph).

**Pattern Structure:**
1. `@Serializable` route nested in the graph of the `NavDisplay` that shows the screen, holding every
   argument (any `@Serializable` type)
2. `EntryProviderScope<{Name}Graph>.{screen}Destination()` extension that adds the `entry`, declares
   its transitions in metadata, and creates the ViewModel for the route (assisted injection)
3. `NavigationController<{Name}Graph>.navigateTo{Screen}()` extension for navigation calls
4. Groups of related destinations (a "flow") are added with
   `EntryProviderScope<{Name}Graph>.{flow}Destinations(navigationController)`, and a route hosting a
   nested graph is added with a `{flow}Graph()` extension that displays a `BitwardenNavDisplay`
5. A screen shown by multiple graphs gets a route and a `...Destination` per graph; its ViewModel
   takes the arguments it needs (e.g. `create(isPreAuth = true)`) instead of a route

**Template**: See [Navigation template](templates.md#navigation-template-navigation-3)

**Pattern Summary:**
```kotlin
@Serializable
sealed class ExampleGraph : NavKey {
    @Serializable
    data class ExampleRoute(val userId: String, val isEditMode: Boolean = false) : ExampleGraph()
}

fun NavigationController<ExampleGraph>.navigateToExample(userId: String, isEditMode: Boolean = false) {
    navigate(route = ExampleGraph.ExampleRoute(userId = userId, isEditMode = isEditMode))
}

fun EntryProviderScope<ExampleGraph>.exampleDestination(onNavigateBack: () -> Unit) {
    slideUpEntry<ExampleGraph.ExampleRoute> { route ->
        ExampleScreen(
            viewModel = hiltViewModel<ExampleViewModel, ExampleViewModel.Factory>(
                creationCallback = { factory -> factory.create(route = route) },
            ),
            onNavigateBack = onNavigateBack,
        )
    }
}
```

**Using a `NavigationController`** (`ui/.../platform/navigation/`; see the KDoc there for exact
semantics):

1. **Own it where the graph is displayed.** The `entry` (or screen) that shows a graph remembers its
   controller, hands it to `BitwardenNavDisplay`, and passes it to the graph's destinations. A route
   hosting a nested graph seeds the controller from its own `backStack` argument, so the parent can
   choose where the nested flow starts:
   ```kotlin
   fun EntryProviderScope<RootNavGraph>.authGraph() {
       entry<RootNavGraph.AuthRoute> { route ->
           val navigationController = rememberNavigationController(
               routes = route.backStack.toTypedArray(),
           )
           BitwardenNavDisplay(
               navigationController = navigationController,
               entryProvider = entryProvider {
                   authDestinations(navigationController = navigationController)
               },
           )
       }
   }
   ```
2. **Screens never see the controller.** `...Destination()` functions take `onNavigate...` lambdas,
   and the graph's `...Destinations(navigationController)` function wires each lambda to a
   `navigateTo{Screen}()` extension or a controller call:
   ```kotlin
   checkEmailDestination(onNavigateBack = { navigationController.navigateBack() })
   landingDestination(
       onNavigateToLogin = { navigationController.navigateToLogin(emailAddress = it) },
   )
   ```
3. **Pick the call that matches the user-visible behavior:**

   | When the user should… | Use | Example |
   |-----------------------|-----|---------|
   | Open a new screen on top | `navigateTo{Screen}()` (wraps `navigate(route)`) | most destinations |
   | Go back / close the screen | `navigateBack()` | every `onNavigateBack` |
   | Open a screen that may already be showing (overlays, debug menu) | `addSingleTop(route)` | `UpdateBiometricsNavigation.kt` |
   | Swap the current screen for a sibling, so back skips it | `replaceLast(route)` | QR scan ↔ manual entry in `VaultUnlockedNavigation.kt` |
   | Return to an earlier screen in the flow | `popUpTo<Route>()` | `popUpToCompleteRegistration()` |
   | Return to an earlier screen with new arguments | `navigatePoppingUpTo(route) { it is Route }` | login after registration in `AuthNavigation.kt` |
   | Start the graph over from its first screen | `popToRoot()` | re-selecting the current tab |
   | Follow app state rather than user input | `replaceAll(routes)` | `RootNavScreen.kt` (state → root route) |
4. **Navigating in another graph** goes through that graph's controller. Nested graphs cannot reach
   their parent's controller, so the parent passes callbacks down (e.g. a tab destination receives
   `onNavigateToImportLogins = { vaultUnlockedNavigationController.navigateToImportLoginsScreen() }`
   to open a full-screen flow above the nav bar).
5. **Tabs** use a `BottomNavigationController` that bundles the nav bar's controller with one
   controller per tab (`VaultUnlockedNavBarNavigationControllers.kt`). Nav bar clicks call
   `navigateToTabOrPopToRoot(tab.rootRoute)` and system back calls its `navigateBack()`, which pops
   within the selected tab before falling back to the start tab.
6. **Reacting to back stack changes:** in composition, just read `navigationController.backStack`.
   Outside of composition, collect `backStackSnapshot` from a `LaunchedEffect` keyed on the
   controller (see `AuthenticatorNavigation.kt`).
7. **Tests** pass `navigationControllerOf(routes)` (unsaved, `@VisibleForTesting`) into the screen in
   place of the remembered one, then assert on `navigationController.backStack.toList()` (see
   `RootNavScreenTest.kt`).

**Transitions** live in entry metadata (`ui/.../theme/EntryTransitions.kt`): `EntryTransitions.slideUp`,
`.push`, `.rootPush` (root of a push flow, e.g. a tab root), `.stay`, and
`EntryTransitions.rootTransition(RootTransition.X)` for a section route of the `RootNavGraph`.

**Reference:**
- `app/src/main/kotlin/com/x8bit/bitwarden/ui/vault/feature/item/VaultItemNavigation.kt`
- `app/src/main/kotlin/com/x8bit/bitwarden/ui/platform/feature/vaultunlocked/VaultUnlockedGraph.kt` (a graph)
- `app/src/main/kotlin/com/x8bit/bitwarden/ui/platform/feature/vaultunlocked/VaultUnlockedNavigation.kt` (section hosting a nested graph)
- `app/src/main/kotlin/com/x8bit/bitwarden/ui/platform/feature/vaultunlockednavbar/VaultUnlockedNavBarNavigation.kt` (tabs, each hosting its own graph)

**Key Benefits:**
- ✅ Type safety: Compile-time errors for missing parameters
- ✅ No string literals in navigation code
- ✅ Back stack is owned, observable state that is saved across process death
- ✅ ViewModels receive their route directly (no `SavedStateHandle` argument parsing)

**Gotchas:**
- ❌ **NEVER** register the same route class twice in one entry provider (Navigation 3 throws)
- ❌ **NEVER** read navigation arguments from `SavedStateHandle`; inject the route instead
- ❌ **NEVER** mutate a `NavBackStack` directly (e.g. `removeLast()` on the last entry empties it and crashes `NavDisplay`); go through the `NavigationController`, whose pop operations always keep the root entry
- ❌ **NEVER** chain `popUpTo` + `navigate` (or clear + push) when an atomic call exists; use `navigatePoppingUpTo`, `replaceAll`, or `replaceLast` so `NavDisplay` never sees an intermediate stack

---

### C. Screen/Compose Implementation

All screens follow consistent Compose patterns.

**Template**: See [Screen/Compose template](templates.md#screencompose-template)

**Key Patterns:**
```kotlin
@Composable
fun ExampleScreen(
    onNavigateBack: () -> Unit,
    // Created by the navigation entry (assisted injection). Screens whose ViewModel needs no
    // navigation arguments may default this to `hiltViewModel()` instead.
    viewModel: ExampleViewModel,
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
                onNavigationIconClick = { viewModel.trySendAction(ExampleAction.BackClick) },
            )
        },
    ) {
        // UI content
    }
}
```

**Reference:** `app/src/main/kotlin/com/x8bit/bitwarden/ui/auth/feature/login/LoginScreen.kt` (see `LoginScreen` composable)

**Essential Requirements:**
- ✅ Use `hiltViewModel()` for dependency injection (`hiltViewModel<VM, VM.Factory>(creationCallback = ...)` in the navigation entry for ViewModels with navigation arguments)
- ✅ Use `collectAsStateWithLifecycle()` for state (not `collectAsState()`)
- ✅ Use `EventsEffect(viewModel)` for one-shot events
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

**KDoc on Interfaces vs. Implementations**

KDoc on an interface member describes the **contract** — what the caller can rely on. It must not describe **how** the implementation fulfills that contract. Implementation details (which data source is consulted, what is cached, what is logged, which manager is delegated to, ordering of internal calls, etc.) belong on the `...Impl` member — and only when the *why* is non-obvious from the code itself.

This applies to every interface in the codebase: repositories, managers, data sources, validators, and UI-layer interfaces alike. The rule is the same one CLAUDE.md states for comments in general — describe the contract or the non-obvious *why*, never the *what* a well-named identifier already conveys.

❌ **Wrong** — interface KDoc leaks the implementation:
```kotlin
interface ExampleRepository {
    /**
     * Fetches data by reading from [ExampleDiskSource] first, falling back to
     * [ExampleService] on cache miss and writing the network result back to disk.
     */
    suspend fun fetchData(id: String): ExampleResult
}
```

✅ **Right** — interface KDoc describes the contract; implementation details (if needed at all) live on the override:
```kotlin
interface ExampleRepository {
    /** Returns the [ExampleData] for [id], or an error result on failure. */
    suspend fun fetchData(id: String): ExampleResult
}

class ExampleRepositoryImpl(...) : ExampleRepository {
    // No KDoc needed — the disk-then-network pattern is visible in the code.
    override suspend fun fetchData(id: String): ExampleResult { ... }
}
```

Red flags that an interface KDoc has drifted into implementation territory:
- Names a concrete collaborator (`...DiskSource`, `...Service`, `...Manager`, `...Impl`)
- Describes ordering of internal calls ("first ... then ...", "falls back to ...")
- Mentions caching, retries, logging, or threading behavior that callers don't depend on
- Restates the method signature in prose ("Suspends until X returns Y")

If callers genuinely depend on a behavior (e.g., "this method is safe to call before vault unlock", "result is cached for the session"), that *is* part of the contract and belongs on the interface. The test: would a different valid implementation be free to change this? If yes, it's implementation detail — strip it.

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
Search `ui/src/main/kotlin/com/bitwarden/ui/platform/components/` for existing `Bitwarden*` components. For build, test, and codebase discovery commands, use the **`build-test-verify`** skill.

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
    ZoneOffset.UTC,
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

### I. Kotlin Style Rules

Project-specific style conventions enforced in code review. These supplement (not replace) `docs/STYLE_AND_BEST_PRACTICES.md`.

**`when` branches with wrapped right-hand side require curly braces.**

When a `when` branch's expression is too long to fit on the same line as the arrow and is wrapped to the next line, wrap the body in `{ }`. A bare `->` followed by an indented expression on its own line is rejected in review.

❌ **Wrong** — wrapped body without braces:
```kotlin
when (type) {
    VaultItemCipherType.LOGIN -> VaultAddEditState.ViewState.Content.ItemType.Login()
    VaultItemCipherType.BANK_ACCOUNT ->
        VaultAddEditState.ViewState.Content.ItemType.BankAccount()
    VaultItemCipherType.DRIVERS_LICENSE ->
        VaultAddEditState.ViewState.Content.ItemType.DriversLicense()
}
```

✅ **Right** — wrapped body with braces:
```kotlin
when (type) {
    VaultItemCipherType.LOGIN -> VaultAddEditState.ViewState.Content.ItemType.Login()
    VaultItemCipherType.BANK_ACCOUNT -> {
        VaultAddEditState.ViewState.Content.ItemType.BankAccount()
    }
    VaultItemCipherType.DRIVERS_LICENSE -> {
        VaultAddEditState.ViewState.Content.ItemType.DriversLicense()
    }
}
```

Single-line branches (body fits on the same line as `->`) do **not** need braces.

---

## Bitwarden-Specific Anti-Patterns

**General anti-patterns are documented in CLAUDE.md.** This section covers violations specific to Bitwarden's State-Action-Event, navigation, and data layer patterns:

❌ **NEVER update ViewModel state directly in coroutines**
- Post internal actions, update state synchronously in `handleAction()`

❌ **NEVER inject `...Impl` classes**
- Only inject interfaces via Hilt

❌ **NEVER create navigation without `@Serializable` routes nested in a `...Graph` sealed class**
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

❌ **NEVER put implementation details in interface KDoc**
- Interface KDoc describes the contract; concrete collaborators, call ordering, caching, and fallback behavior belong on the `...Impl` override (and only when the *why* is non-obvious). See section D for examples.

---

## Quick Reference

For build, test, and codebase discovery commands, use the **`build-test-verify`** skill.

**File Reference Format:**
When pointing to specific code, use: `file_path:line_number`

Example: `ui/src/main/kotlin/com/bitwarden/ui/platform/base/BaseViewModel.kt` (see `handleAction` method)
