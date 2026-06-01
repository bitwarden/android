# Test Base Classes Reference

Bitwarden Android provides specialized base classes that configure test environments and provide helper utilities.

## BaseViewModelTest

**Location:** `ui/src/testFixtures/kotlin/com/bitwarden/ui/platform/base/BaseViewModelTest.kt`

### Purpose
Provides essential setup for testing ViewModels with proper coroutine dispatcher configuration and Flow testing helpers.

### Automatic Configuration
- Registers `MainDispatcherExtension` for `UnconfinedTestDispatcher`
- Ensures deterministic coroutine execution in tests
- All coroutines complete immediately without real delays

### Key Feature: stateEventFlow() Helper

**Use Case:** When you need to test both StateFlow and EventFlow simultaneously.

```kotlin
@Test
fun `complex action should update state and emit event`() = runTest {
    val viewModel = ExampleViewModel(savedStateHandle, mockRepository)

    viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
        // Verify initial state
        assertEquals(INITIAL_STATE, stateFlow.awaitItem())

        // No events yet
        eventFlow.expectNoEvents()

        // Trigger action
        viewModel.trySendAction(ExampleAction.ComplexAction)

        // Verify state updated
        assertEquals(LOADING_STATE, stateFlow.awaitItem())

        // Verify event emitted
        assertEquals(ExampleEvent.ShowToast, eventFlow.awaitItem())
    }
}
```

### Usage Pattern

```kotlin
class MyViewModelTest : BaseViewModelTest() {
    private val mockRepository: MyRepository = mockk()
    private val savedStateHandle = SavedStateHandle(
        mapOf(KEY_STATE to INITIAL_STATE)
    )

    @Test
    fun `test action`() = runTest {
        val viewModel = MyViewModel(
            savedStateHandle = savedStateHandle,
            repository = mockRepository
        )

        // Test with automatic dispatcher setup
        viewModel.stateFlow.test {
            assertEquals(INITIAL_STATE, awaitItem())
        }
    }
}
```

## BitwardenComposeTest

**Location:** `app/src/test/kotlin/com/x8bit/bitwarden/ui/platform/base/BitwardenComposeTest.kt`

### Purpose
Pre-configured test class for Compose UI tests with all Bitwarden managers and theme setup.

### Automatic Configuration
- All Bitwarden managers pre-configured (FeatureFlags, AuthTab, Biometrics, etc.)
- Wraps content in `BitwardenTheme` and `LocalManagerProvider`
- Provides fixed `Clock` for deterministic time-based tests
- Extends `BaseComposeTest` for Robolectric and dispatcher setup

### Key Features

**Pre-configured Managers:**
- `FeatureFlagManager` - Controls feature flag behavior
- `AuthTabManager` - Manages auth tab state
- `BiometricsManager` - Handles biometric authentication
- `ClipboardManager` - Clipboard operations
- `NotificationManager` - Notification display

**Fixed Clock:**
All tests use a fixed clock for deterministic time-based testing:
```kotlin
// Tests use consistent time: 2023-10-27T12:00:00Z
val fixedClock: Clock
```

### Usage Pattern

```kotlin
class MyScreenTest : BitwardenComposeTest() {
    private var haveCalledNavigateBack = false
    private val mutableEventFlow = bufferedMutableSharedFlow<MyEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<MyViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        setContent {
            MyScreen(
                onNavigateBack = { haveCalledNavigateBack = true },
                viewModel = viewModel
            )
        }
    }

    @Test
    fun `on back click should send action`() {
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify { viewModel.trySendAction(MyAction.BackClick) }
    }

    @Test
    fun `loading state should show progress`() {
        mutableStateFlow.value = DEFAULT_STATE.copy(isLoading = true)
        composeTestRule.onNode(isProgressBar).assertIsDisplayed()
    }
}
```

### Important: bufferedMutableSharedFlow for Events

In Compose tests, use `bufferedMutableSharedFlow` instead of regular `MutableSharedFlow` (default replay is 0):

```kotlin
// Correct for Compose tests
private val mutableEventFlow = bufferedMutableSharedFlow<MyEvent>()

// This allows triggering events and having the UI react
mutableEventFlow.tryEmit(MyEvent.NavigateBack)
```

## BaseServiceTest

**Location:** `network/src/testFixtures/kotlin/com/bitwarden/network/base/BaseServiceTest.kt`

### Purpose
Provides MockWebServer setup for testing API service implementations.

### Automatic Configuration
- `server: MockWebServer` - Auto-started before each test, stopped after
- `retrofit: Retrofit` - Pre-configured with:
  - JSON converter (kotlinx.serialization)
  - NetworkResultCallAdapter for Result<T> responses
  - Base URL pointing to MockWebServer
- `json: Json` - kotlinx.serialization JSON instance

### Usage Pattern

```kotlin
class MyServiceTest : BaseServiceTest() {
    private val api: MyApi = retrofit.create()
    private val service = MyServiceImpl(api)

    @Test
    fun `getConfig should return success when API succeeds`() = runTest {
        // Enqueue mock response
        server.enqueue(MockResponse().setBody(EXPECTED_JSON))

        // Call service
        val result = service.getConfig()

        // Verify result
        assertEquals(EXPECTED_RESPONSE.asSuccess(), result)
    }

    @Test
    fun `getConfig should return failure when API fails`() = runTest {
        // Enqueue error response
        server.enqueue(MockResponse().setResponseCode(500))

        // Call service
        val result = service.getConfig()

        // Verify failure
        assertTrue(result.isFailure)
    }
}
```

### MockWebServer Patterns

**Enqueue successful response:**
```kotlin
server.enqueue(MockResponse().setBody("""{"key": "value"}"""))
```

**Enqueue error response:**
```kotlin
server.enqueue(MockResponse().setResponseCode(404))
server.enqueue(MockResponse().setResponseCode(500))
```

**Enqueue delayed response:**
```kotlin
server.enqueue(
    MockResponse()
        .setBody("""{"key": "value"}""")
        .setBodyDelay(1000, TimeUnit.MILLISECONDS)
)
```

**Verify request details:**
```kotlin
val request = server.takeRequest()
assertEquals("/api/config", request.path)
assertEquals("GET", request.method)
assertEquals("Bearer token", request.getHeader("Authorization"))
```

## BaseComposeTest

**Location:** `ui/src/testFixtures/kotlin/com/bitwarden/ui/platform/base/BaseComposeTest.kt`

### Purpose
Base class for Compose tests that extends `BaseRobolectricTest` and provides `setTestContent()` helper.

### Features
- Robolectric configuration for Compose
- Proper dispatcher setup
- `composeTestRule` for UI testing
- `setTestContent()` helper wraps content in theme

### Usage
Typically you'll extend `BitwardenComposeTest` which extends this class. Use `BaseComposeTest` directly only for tests that don't need Bitwarden-specific manager configuration.

## When to Use Each Base Class

| Test Type | Base Class | Use When |
|-----------|------------|----------|
| ViewModel tests | `BaseViewModelTest` | Testing ViewModel state and events |
| Compose screen tests | `BitwardenComposeTest` | Testing Compose UI with Bitwarden components |
| API service tests | `BaseServiceTest` | Testing network layer with MockWebServer |
| Repository tests | None (manual setup) | Testing repository logic with mocked dependencies |
| Utility/helper tests | None (manual setup) | Testing pure functions or utilities |

## Complete Examples

**ViewModel Test:**
`../examples/viewmodel-test-example.md`

**Compose Screen Test:**
`../examples/compose-screen-test-example.md`

**Repository Test:**
`../examples/repository-test-example.md`
