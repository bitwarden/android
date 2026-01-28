# Bitwarden Android - Claude Code Configuration

Official Android application for Bitwarden Password Manager and Bitwarden Authenticator, providing secure password management, two-factor authentication, and credential autofill services with zero-knowledge encryption.

## Overview

### What This Project Does
- Multi-module Android application providing secure password management and TOTP code generation
- Implements zero-knowledge architecture where encryption/decryption happens client-side
- Key entry points: `:app` (Password Manager), `:authenticator` (2FA TOTP generator)
- Target users: End-users via Google Play Store and F-Droid

### Key Concepts
- **Zero-Knowledge Architecture**: Server never has access to unencrypted vault data or encryption keys
- **Bitwarden SDK**: Rust-based cryptographic SDK handling all encryption/decryption operations
- **DataState**: Wrapper for streaming data states (Loading, Loaded, Pending, Error, NoNetwork)
- **Result Types**: Custom sealed classes for operation results (never throw exceptions from data layer)
- **UDF (Unidirectional Data Flow)**: State flows down, actions flow up through ViewModels

---

## Architecture & Patterns

### System Architecture

```
User Request (UI Action)
         |
    Screen (Compose)
         |
    ViewModel (State/Action/Event)
         |
    Repository (Business Logic)
         |
    +----+----+----+
    |    |    |    |
  Disk  Network  SDK
   |      |      |
 Room  Retrofit  Bitwarden
  DB    APIs     Rust SDK
```

### Code Organization

```
android/
├── app/                    # Password Manager application
│   └── src/main/kotlin/com/x8bit/bitwarden/
│       ├── data/           # Repositories, managers, data sources
│       │   ├── auth/       # Authentication domain
│       │   ├── vault/      # Vault/cipher domain
│       │   ├── platform/   # Platform services
│       │   └── tools/      # Generator, export tools
│       └── ui/             # ViewModels, Screens, Navigation
│           ├── auth/       # Login, registration screens
│           ├── vault/      # Vault screens
│           └── platform/   # Settings, debug menu
├── authenticator/          # Authenticator 2FA application
├── core/                   # Shared utilities, dispatcher management
├── data/                   # Shared data layer (disk sources, models)
├── network/                # Network layer (Retrofit services, models)
├── ui/                     # Shared UI components, theming
├── authenticatorbridge/    # IPC bridge between apps
├── cxf/                    # Credential Exchange integration
└── annotation/             # Custom annotations for code generation
```

### Key Principles

1. **No Exceptions from Data Layer**: All suspending functions return `Result<T>` or custom sealed classes
2. **State Hoisting to ViewModel**: All state that affects behavior must live in the ViewModel's state
3. **Interface-Based DI**: All implementations use interface/`...Impl` pairs with Hilt injection
4. **Encryption by Default**: All sensitive data encrypted via SDK before storage

### Core Patterns

#### BaseViewModel Pattern

**Purpose**: Enforces unidirectional data flow with State, Actions, and Events

**Implementation** (see `ui/src/main/kotlin/com/bitwarden/ui/platform/base/BaseViewModel.kt`):
```kotlin
abstract class BaseViewModel<S, E, A>(initialState: S) : ViewModel() {
    protected val mutableStateFlow: MutableStateFlow<S>
    val stateFlow: StateFlow<S>      // UI reads state
    val eventFlow: Flow<E>           // One-shot events (navigation)

    protected abstract fun handleAction(action: A)
    fun trySendAction(action: A)     // UI sends actions
}
```

**Usage**:
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MyRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<MyState, MyEvent, MyAction>(
    initialState = savedStateHandle[KEY_STATE] ?: MyState()
) {
    override fun handleAction(action: MyAction) {
        when (action) {
            is MyAction.ButtonClick -> handleButtonClick()
            is MyAction.Internal.DataReceived -> handleDataReceived(action)
        }
    }
}
```

#### Repository Result Pattern

**Purpose**: Type-safe error handling without exceptions

**Implementation**:
```kotlin
// For discrete operations - use custom sealed classes
sealed class CreateCipherResult {
    data class Success(val cipher: CipherView) : CreateCipherResult()
    data class Error(val errorMessage: String?, val error: Throwable?) : CreateCipherResult()
}

// For streaming data - use DataState wrapper
sealed class DataState<out T> {
    data object Loading : DataState<Nothing>()
    data class Loaded<T>(override val data: T) : DataState<T>()
    data class Error<T>(val error: Throwable, override val data: T?) : DataState<T>()
}
```

---

## Development Guide

### Adding New Feature Screen

**1. Define State/Event/Action** (`app/src/main/kotlin/.../ui/feature/MyFeatureViewModel.kt`)
```kotlin
@Parcelize
data class MyFeatureState(
    val data: String = "",
    val dialogState: DialogState? = null,
) : Parcelable {
    sealed class DialogState : Parcelable {
        @Parcelize data object Loading : DialogState()
    }
}

sealed class MyFeatureEvent {
    data class NavigateToNext(val id: String) : MyFeatureEvent()
}

sealed class MyFeatureAction {
    data object BackClick : MyFeatureAction()
    data class ItemClick(val id: String) : MyFeatureAction()
    sealed class Internal : MyFeatureAction() {
        data class DataReceived(val data: String) : Internal()
    }
}
```

**2. Implement ViewModel** (`app/src/main/kotlin/.../ui/feature/MyFeatureViewModel.kt`)
```kotlin
private const val KEY_STATE = "state"

@HiltViewModel
class MyFeatureViewModel @Inject constructor(
    private val repository: MyRepository,
    private val savedStateHandle: SavedStateHandle,
) : BaseViewModel<MyFeatureState, MyFeatureEvent, MyFeatureAction>(
    initialState = savedStateHandle[KEY_STATE] ?: MyFeatureState()
) {
    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)

        repository.dataFlow
            .map { MyFeatureAction.Internal.DataReceived(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: MyFeatureAction) {
        when (action) {
            is MyFeatureAction.BackClick -> sendEvent(MyFeatureEvent.NavigateBack)
            is MyFeatureAction.ItemClick -> handleItemClick(action)
            is MyFeatureAction.Internal -> handleInternalAction(action)
        }
    }
}
```

**3. Implement Screen** (`app/src/main/kotlin/.../ui/feature/MyFeatureScreen.kt`)
```kotlin
@Composable
fun MyFeatureScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNext: (String) -> Unit,
    viewModel: MyFeatureViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is MyFeatureEvent.NavigateBack -> onNavigateBack()
            is MyFeatureEvent.NavigateToNext -> onNavigateToNext(event.id)
        }
    }

    // Render UI based on state
    BitwardenScaffold(
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(R.string.my_feature),
                navigationIcon = NavigationIcon.Back(
                    onNavigationIconClick = remember(viewModel) {
                        { viewModel.trySendAction(MyFeatureAction.BackClick) }
                    }
                ),
            )
        }
    ) {
        // Content
    }
}
```

**4. Define Navigation** (`app/src/main/kotlin/.../ui/feature/MyFeatureNavigation.kt`)
```kotlin
@Serializable
data class MyFeatureRoute(val itemId: String)

data class MyFeatureArgs(val itemId: String)

fun SavedStateHandle.toMyFeatureArgs(): MyFeatureArgs {
    val route = this.toRoute<MyFeatureRoute>()
    return MyFeatureArgs(itemId = route.itemId)
}

fun NavGraphBuilder.myFeatureDestination(
    onNavigateBack: () -> Unit,
    onNavigateToNext: (String) -> Unit,
) {
    composableWithSlideTransitions<MyFeatureRoute> {
        MyFeatureScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToNext = onNavigateToNext,
        )
    }
}

fun NavController.navigateToMyFeature(itemId: String, navOptions: NavOptions? = null) {
    this.navigate(route = MyFeatureRoute(itemId = itemId), navOptions = navOptions)
}
```

**5. Write Tests** (`app/src/test/kotlin/.../ui/feature/MyFeatureViewModelTest.kt`)
```kotlin
class MyFeatureViewModelTest : BaseViewModelTest() {
    private val repository = mockk<MyRepository> {
        every { dataFlow } returns MutableStateFlow("initial")
    }

    @Test
    fun `ItemClick sends NavigateToNext event`() = runTest {
        val viewModel = createViewModel()

        viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
            viewModel.trySendAction(MyFeatureAction.ItemClick("123"))

            assertEquals(
                MyFeatureEvent.NavigateToNext("123"),
                eventFlow.awaitItem(),
            )
        }
    }

    private fun createViewModel() = MyFeatureViewModel(
        repository = repository,
        savedStateHandle = SavedStateHandle(),
    )
}
```

### Common Patterns

#### Flow Collection in ViewModel
```kotlin
// Always map async results to internal actions
repository.dataStateFlow
    .map { MyAction.Internal.DataReceived(it) }
    .onEach(::sendAction)
    .launchIn(viewModelScope)

// Never update state directly in coroutines
viewModelScope.launch {
    val result = repository.fetchData()
    sendAction(MyAction.Internal.FetchComplete(result))
}
```

#### Error Handling
```kotlin
// Repository returns sealed result
when (val result = repository.createCipher(cipher)) {
    is CreateCipherResult.Success -> handleSuccess(result.cipher)
    is CreateCipherResult.Error -> mutableStateFlow.update {
        it.copy(dialogState = DialogState.Error(result.errorMessage))
    }
}
```

#### Response Formatting
```kotlin
// DataState for streaming data
val ciphersStateFlow: StateFlow<DataState<List<CipherView>>> =
    vaultRepository.ciphersStateFlow
        .map { dataState -> dataState.map { /* transform */ } }
        .stateIn(viewModelScope, SharingStarted.Lazily, DataState.Loading)
```

---

## Data Models

### Core Types

```kotlin
// User authentication state
data class UserState(
    val activeUserId: String,
    val accounts: List<Account>,
    val hasPendingAccountAddition: Boolean,
)

// Vault unlock data
data class VaultUnlockData(
    val userId: String,
    val status: VaultUnlockStatus,
)

// DataState wrapper for async data
sealed class DataState<out T> {
    abstract val data: T?
    data object Loading : DataState<Nothing>()
    data class Loaded<T>(override val data: T) : DataState<T>()
    data class Pending<T>(override val data: T) : DataState<T>()
    data class Error<T>(val error: Throwable, override val data: T?) : DataState<T>()
    data class NoNetwork<T>(override val data: T?) : DataState<T>()
}
```

### Network Result Types

```kotlin
// NetworkResult for HTTP operations
sealed class NetworkResult<out T> {
    data class Success<T>(val value: T) : NetworkResult<T>()
    data class Failure(val throwable: Throwable) : NetworkResult<Nothing>()
}

// BitwardenError for error classification
sealed class BitwardenError {
    abstract val throwable: Throwable
    data class Http(override val throwable: HttpException) : BitwardenError()
    data class Network(override val throwable: IOException) : BitwardenError()
    data class Other(override val throwable: Throwable) : BitwardenError()
}
```

---

## Security & Configuration

### Security Rules

**MANDATORY - These rules have no exceptions:**

1. **Zero-Knowledge Architecture**: Never transmit unencrypted vault data or master passwords to the server. All encryption happens client-side via the Bitwarden SDK.

2. **No Plaintext Key Storage**: Encryption keys must be stored using Android Keystore (biometric unlock) or encrypted with PIN/master password.

3. **Sensitive Data Cleanup**: On logout, all sensitive data must be cleared from memory and storage via `UserLogoutManager.logout()`.

4. **Input Validation**: Validate all user inputs before processing, especially URLs and credentials.

5. **SDK Isolation**: Use scoped SDK sources (`ScopedVaultSdkSource`) to prevent cross-user crypto context leakage.

### Security Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `BiometricsEncryptionManager` | `data/platform/manager/` | Android Keystore integration for biometric unlock |
| `VaultLockManager` | `data/vault/manager/` | Vault lock/unlock operations |
| `AuthDiskSource` | `data/auth/datasource/disk/` | Secure token and key storage |
| `BaseEncryptedDiskSource` | `data/datasource/disk/` | EncryptedSharedPreferences base class |

### Environment Configuration

| Variable | Required | Description |
|----------|----------|-------------|
| `GITHUB_TOKEN` | Yes (CI) | GitHub Packages authentication for SDK |
| Build flavors | - | `standard` (Play Store), `fdroid` (no Google services) |
| Build types | - | `debug`, `beta`, `release` |

### Authentication & Authorization

- **Login Methods**: Email/password, SSO (OAuth 2.0 + PKCE), trusted device, passwordless auth request
- **Vault Unlock**: Master password, PIN, biometric, trusted device key
- **Token Management**: JWT access tokens with automatic refresh via `AuthTokenManager`
- **Key Derivation**: PBKDF2-SHA256 or Argon2id via `KdfManager`

---

## Testing

### Test Structure

```
app/src/test/                    # App unit tests
app/src/testFixtures/            # App test utilities
core/src/testFixtures/           # Core test utilities (FakeDispatcherManager)
data/src/testFixtures/           # Data test utilities (FakeSharedPreferences)
network/src/testFixtures/        # Network test utilities (BaseServiceTest)
ui/src/testFixtures/             # UI test utilities (BaseViewModelTest, BaseComposeTest)
```

### Writing Tests

**ViewModel Test Template** (using Turbine):
```kotlin
class MyViewModelTest : BaseViewModelTest() {
    private val repository = mockk<MyRepository> {
        every { dataFlow } returns MutableStateFlow(initialData)
    }

    @Test
    fun `action updates state correctly`() = runTest {
        val viewModel = createViewModel()

        viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
            // Initial state
            assertEquals(expectedInitialState, stateFlow.awaitItem())

            // Send action
            viewModel.trySendAction(MyAction.SomeAction)

            // Verify state update
            assertEquals(expectedUpdatedState, stateFlow.awaitItem())
        }
    }
}
```

**Screen/Compose Test Template**:
```kotlin
class MyScreenTest : BaseComposeTest() {
    @Test
    fun `displays loading state`() {
        composeTestRule.setTestContent {
            MyScreen(state = MyState(isLoading = true), onAction = {})
        }

        composeTestRule
            .onNodeWithTag("LoadingIndicator")
            .assertIsDisplayed()
    }
}
```

### Running Tests

```bash
./gradlew test                    # Run all unit tests
./gradlew app:testDebugUnitTest   # Run app module tests
./gradlew :core:test              # Run core module tests
./fastlane check                  # Run full validation (detekt, lint, tests, coverage)
```

### Test Environment

- **Dispatcher Control**: Use `FakeDispatcherManager` from `:core:testFixtures`
- **MockK Patterns**: `mockk<T> { every { } returns }`, `coEvery { }` for suspend
- **Flow Testing**: Turbine with `stateEventFlow()` helper from `BaseViewModelTest`
- **Time Control**: Inject `Clock` for deterministic time testing

---

## Code Style & Standards

### Formatting
- **Tool**: Android Studio formatter with `bitwarden-style.xml`
- **Line Limit**: 100 characters (enforced)
- **Detekt**: Enabled with auto-correction

### Naming Conventions
- `camelCase`: Variables, functions, parameters
- `PascalCase`: Classes, interfaces, sealed classes, type aliases
- `SCREAMING_SNAKE_CASE`: Constants, enum values
- `...Impl`: Implementation classes (e.g., `AuthRepositoryImpl`)

### Imports
- Ordered by: Android, third-party, project
- No wildcard imports
- Group related imports together

### Documentation
- **KDoc**: Required for all public classes, functions, properties
- **@property**: Document constructor properties (3+ properties)
- **Inline comments**: Imperative voice, capitalize first word

### Pre-commit Hooks
- Detekt runs on staged files
- Lint checks via Fastlane

---

## Anti-Patterns

### DO

- Use `Result<T>` or sealed classes for operations that can fail
- Hoist state to ViewModel when it affects business logic
- Use `remember(viewModel)` for lambdas passed to composables
- Map async results to internal actions before updating state
- Use interface-based DI with Hilt
- Inject `Clock` for time-dependent operations
- Return early to reduce nesting

### DON'T

- Throw exceptions from data layer functions
- Update state directly inside coroutines (use internal actions)
- Use `any` types or suppress null safety
- Store static data (use DI singletons instead)
- Catch generic `Exception` (catch specific types)
- Use `e.printStackTrace()` (use Timber logging)
- Create new patterns when established ones exist
- Skip KDoc for public APIs

---

## Deployment

### Building

```bash
# Debug builds
./gradlew app:assembleDebug
./gradlew authenticator:assembleDebug

# Release builds (requires signing keys)
./gradlew app:assembleStandardRelease
./gradlew app:bundleStandardRelease

# F-Droid builds
./gradlew app:assembleFdroidRelease
```

### Versioning

**Location**: `gradle/libs.versions.toml`
```toml
appVersionCode = "1"
appVersionName = "2025.11.1"
```

Follow semantic versioning pattern: `YEAR.RELEASE.PATCH`

### Publishing

- **Play Store**: Via GitHub Actions workflow with signed AAB
- **F-Droid**: Via dedicated workflow with F-Droid signing keys
- **Firebase App Distribution**: For beta testing

---

## Troubleshooting

### Common Issues

#### Build fails with SDK dependency error

**Problem**: Cannot resolve Bitwarden SDK from GitHub Packages

**Solution**:
1. Ensure `GITHUB_TOKEN` is set in `ci.properties` or environment
2. Verify token has `read:packages` scope
3. Check network connectivity to `maven.pkg.github.com`

#### Tests fail with dispatcher issues

**Problem**: Tests hang or fail with "Module with Main dispatcher had failed to initialize"

**Solution**:
1. Extend `BaseViewModelTest` for ViewModel tests
2. Use `@RegisterExtension val mainDispatcherExtension = MainDispatcherExtension()`
3. Ensure `runTest { }` wraps test body

#### Compose preview not rendering

**Problem**: @Preview functions show "Rendering problem"

**Solution**:
1. Check for missing theme wrapper: `BitwardenTheme { YourComposable() }`
2. Verify no ViewModel dependency in preview (use state-based preview)
3. Clean and rebuild project

#### ProGuard/R8 stripping required classes

**Problem**: Release build crashes with missing class errors

**Solution**:
1. Add keep rules to `proguard-rules.pro`
2. Check `consumer-rules.pro` in library modules
3. Verify kotlinx.serialization rules are present

### Debug Tips

- **Timber Logging**: Enabled in debug builds, check Logcat with tag filter
- **Debug Menu**: Available in debug builds via Settings > About > Debug Menu
- **Network Inspector**: Use Android Studio Network Profiler or Charles Proxy
- **SDK Debugging**: Check `BaseSdkSource` for wrapped exceptions

---

## References

### Internal Documentation
- `docs/ARCHITECTURE.md` - Complete architecture patterns and principles
- `docs/STYLE_AND_BEST_PRACTICES.md` - Kotlin and Compose code style

### External Documentation
- [Bitwarden SDK](https://github.com/bitwarden/sdk) - Cryptographic SDK
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - UI framework
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-guide.html) - Async programming
- [Hilt DI](https://dagger.dev/hilt/) - Dependency injection
- [Turbine](https://github.com/cashapp/turbine) - Flow testing

### Tools & Libraries
- [MockK](https://mockk.io/) - Kotlin mocking library
- [Retrofit](https://square.github.io/retrofit/) - HTTP client
- [Room](https://developer.android.com/training/data-storage/room) - Database
- [Detekt](https://detekt.dev/) - Static analysis
