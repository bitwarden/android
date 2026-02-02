---
name: testing-android-code
description: This skill should be used when writing or reviewing tests for Android code in Bitwarden. Triggered by "BaseViewModelTest", "BitwardenComposeTest", "BaseServiceTest", "stateEventFlow", "bufferedMutableSharedFlow", "FakeDispatcherManager", "expectNoEvents", "assertCoroutineThrows", "createMockCipher", "createMockSend", "asSuccess", "Why is my Bitwarden test failing?", or testing questions about ViewModels, repositories, Compose screens, or data sources in Bitwarden.
version: 1.0.0
---

# Testing Android Code - Bitwarden Testing Patterns

**This skill provides tactical testing guidance for Bitwarden-specific patterns.** For comprehensive architecture and testing philosophy, consult `docs/ARCHITECTURE.md`.

## Test Framework Configuration

**Required Dependencies:**
- **JUnit 5** (jupiter), **MockK**, **Turbine** (app.cash.turbine)
- **kotlinx.coroutines.test**, **Robolectric**, **Compose Test**

**Critical Note:** Tests run with en-US locale for consistency. Don't assume other locales.

---

## A. ViewModel Testing Patterns

### Base Class: BaseViewModelTest

**Always extend `BaseViewModelTest` for ViewModel tests.**

**Location:** `ui/src/testFixtures/kotlin/com/bitwarden/ui/platform/base/BaseViewModelTest.kt`

**Benefits:**
- Automatically registers `MainDispatcherExtension` for `UnconfinedTestDispatcher`
- Provides `stateEventFlow()` helper for simultaneous StateFlow/EventFlow testing

**Pattern:**
```kotlin
class ExampleViewModelTest : BaseViewModelTest() {
    private val mockRepository: ExampleRepository = mockk()
    private val savedStateHandle = SavedStateHandle(mapOf(KEY_STATE to INITIAL_STATE))

    @Test
    fun `ButtonClick should fetch data and update state`() = runTest {
        coEvery { mockRepository.fetchData(any()) } returns Result.success("data")

        val viewModel = ExampleViewModel(savedStateHandle, mockRepository)

        viewModel.stateFlow.test {
            assertEquals(INITIAL_STATE, awaitItem())
            viewModel.trySendAction(ExampleAction.ButtonClick)
            assertEquals(INITIAL_STATE.copy(data = "data"), awaitItem())
        }

        coVerify { mockRepository.fetchData(any()) }
    }
}
```

**For complete examples:** See `references/test-base-classes.md`

### StateFlow vs EventFlow (Critical Distinction)

| Flow Type | Replay | First Action | Pattern |
|-----------|--------|--------------|---------|
| StateFlow | Yes (1) | `awaitItem()` gets current state | Expect initial → trigger → expect new |
| EventFlow | No | `expectNoEvents()` first | expectNoEvents → trigger → expect event |

**For detailed patterns:** See `references/flow-testing-patterns.md`

---

## B. Compose UI Testing Patterns

### Base Class: BitwardenComposeTest

**Always extend `BitwardenComposeTest` for Compose screen tests.**

**Location:** `app/src/test/kotlin/com/x8bit/bitwarden/ui/platform/base/BitwardenComposeTest.kt`

**Benefits:**
- Pre-configures all Bitwarden managers (FeatureFlags, AuthTab, Biometrics, etc.)
- Wraps content in `BitwardenTheme` and `LocalManagerProvider`
- Provides fixed Clock for deterministic time-based tests

**Pattern:**
```kotlin
class ExampleScreenTest : BitwardenComposeTest() {
    private var haveCalledNavigateBack = false
    private val mutableEventFlow = bufferedMutableSharedFlow<ExampleEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<ExampleViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }

    @Before
    fun setup() {
        setContent {
            ExampleScreen(
                onNavigateBack = { haveCalledNavigateBack = true },
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `on back click should send BackClick action`() {
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify { viewModel.trySendAction(ExampleAction.BackClick) }
    }
}
```

**Note:** Use `bufferedMutableSharedFlow` for event testing in Compose tests. Default replay is 0; pass `replay = 1` if needed.

**For complete base class details:** See `references/test-base-classes.md`

---

## C. Repository and Service Testing

### Service Testing with MockWebServer

**Base Class:** `BaseServiceTest` (`network/src/testFixtures/`)

```kotlin
class ExampleServiceTest : BaseServiceTest() {
    private val api: ExampleApi = retrofit.create()
    private val service = ExampleServiceImpl(api)

    @Test
    fun `getConfig should return success when API succeeds`() = runTest {
        server.enqueue(MockResponse().setBody(EXPECTED_JSON))
        val result = service.getConfig()
        assertEquals(EXPECTED_RESPONSE.asSuccess(), result)
    }
}
```

### Repository Testing Pattern

```kotlin
class ExampleRepositoryTest {
    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )
    private val dispatcherManager = FakeDispatcherManager()
    private val mockDiskSource: ExampleDiskSource = mockk()
    private val mockService: ExampleService = mockk()

    private val repository = ExampleRepositoryImpl(
        clock = fixedClock,
        exampleDiskSource = mockDiskSource,
        exampleService = mockService,
        dispatcherManager = dispatcherManager,
    )

    @Test
    fun `fetchData should return success when service succeeds`() = runTest {
        coEvery { mockService.getData(any()) } returns expectedData.asSuccess()
        val result = repository.fetchData(userId)
        assertTrue(result.isSuccess)
    }
}
```

**Key patterns:** Use `FakeDispatcherManager`, fixed Clock, and `.asSuccess()` helpers.

---

## D. Test Data Builders

### Builder Pattern with Number Parameter

**Location:** `network/src/testFixtures/kotlin/com/bitwarden/network/model/`

```kotlin
fun createMockCipher(
    number: Int,
    id: String = "mockId-$number",
    name: String? = "mockName-$number",
    // ... more parameters with defaults
): SyncResponseJson.Cipher

// Usage:
val cipher1 = createMockCipher(number = 1)  // mockId-1, mockName-1
val cipher2 = createMockCipher(number = 2)  // mockId-2, mockName-2
val custom = createMockCipher(number = 3, name = "Custom")
```

**Available Builders (35+):**
- **Cipher:** `createMockCipher()`, `createMockLogin()`, `createMockCard()`, `createMockIdentity()`, `createMockSecureNote()`, `createMockSshKey()`, `createMockField()`, `createMockUri()`, `createMockFido2Credential()`, `createMockPasswordHistory()`, `createMockCipherPermissions()`
- **Sync:** `createMockSyncResponse()`, `createMockFolder()`, `createMockCollection()`, `createMockPolicy()`, `createMockDomains()`
- **Send:** `createMockSend()`, `createMockFile()`, `createMockText()`, `createMockSendJsonRequest()`
- **Profile:** `createMockProfile()`, `createMockOrganization()`, `createMockProvider()`, `createMockPermissions()`
- **Attachments:** `createMockAttachment()`, `createMockAttachmentJsonRequest()`, `createMockAttachmentResponse()`

See `network/src/testFixtures/kotlin/com/bitwarden/network/model/` for full list.

---

## E. Result Type Testing

**Locations:**
- `.asSuccess()`, `.asFailure()`: `core/src/main/kotlin/com/bitwarden/core/data/util/ResultExtensions.kt`
- `assertCoroutineThrows`: `core/src/testFixtures/kotlin/com/bitwarden/core/data/util/TestHelpers.kt`

```kotlin
// Create results
"data".asSuccess()              // Result.success("data")
throwable.asFailure()           // Result.failure<T>(throwable)

// Assertions
assertTrue(result.isSuccess)
assertEquals(expectedValue, result.getOrNull())
```

---

## F. Test Utilities and Helpers

### Fake Implementations

| Fake | Location | Purpose |
|------|----------|---------|
| `FakeDispatcherManager` | `core/src/testFixtures/` | Deterministic coroutine execution |
| `FakeConfigDiskSource` | `data/src/testFixtures/` | In-memory config storage |
| `FakeSharedPreferences` | `data/src/testFixtures/` | Memory-backed SharedPreferences |

### Exception Testing (CRITICAL)

```kotlin
// CORRECT - Call directly, NOT inside runTest
@Test
fun `test exception`() {
    assertCoroutineThrows<IllegalStateException> {
        repository.throwingFunction()
    }
}
```

**Why:** `runTest` catches exceptions and rethrows them, breaking the assertion pattern.

---

## G. Critical Gotchas

Common testing mistakes in Bitwarden. **For complete details and examples:** See `references/critical-gotchas.md`

**Core Patterns:**
- **assertCoroutineThrows + runTest** - Never wrap in `runTest`; call directly
- **Static mock cleanup** - Always `unmockkStatic()` in `@After`
- **StateFlow vs EventFlow** - StateFlow: `awaitItem()` first; EventFlow: `expectNoEvents()` first
- **FakeDispatcherManager** - Always use instead of real `DispatcherManagerImpl`
- **Coroutine test wrapper** - Use `runTest { }` for all Flow/coroutine tests

**Assertion Patterns:**
- **Complete state assertions** - Assert entire state objects, not individual fields
- **JUnit over Kotlin** - Use `assertTrue()`, not Kotlin's `assert()`
- **Use Result extensions** - Use `asSuccess()` and `asFailure()` for Result type assertions

**Test Design:**
- **Fake vs Mock strategy** - Use Fakes for happy paths, Mocks for error paths
- **DI over static mocking** - Extract interfaces (like UuidManager) instead of mockkStatic
- **Null stream testing** - Test null returns from ContentResolver operations
- **bufferedMutableSharedFlow** - Use with `.onSubscription { emit(state) }` in Fakes
- **Test factory methods** - Accept domain state types, not SavedStateHandle

---

## H. Test File Organization

### Directory Structure

```
module/src/test/kotlin/com/bitwarden/.../
├── ui/*ScreenTest.kt, *ViewModelTest.kt
├── data/repository/*RepositoryTest.kt
└── network/service/*ServiceTest.kt

module/src/testFixtures/kotlin/com/bitwarden/.../
├── util/TestHelpers.kt
├── base/Base*Test.kt
└── model/*Util.kt
```

### Test Naming

- Classes: `*Test.kt`, `*ScreenTest.kt`, `*ViewModelTest.kt`
- Functions: `` `given state when action should result` ``

---

## Summary

Key Bitwarden-specific testing patterns:

1. **BaseViewModelTest** - Automatic dispatcher setup with `stateEventFlow()` helper
2. **BitwardenComposeTest** - Pre-configured with all managers and theme
3. **BaseServiceTest** - MockWebServer setup for network testing
4. **Turbine Flow Testing** - StateFlow (replay) vs EventFlow (no replay)
5. **Test Data Builders** - Consistent `number: Int` parameter pattern
6. **Fake Implementations** - FakeDispatcherManager, FakeConfigDiskSource
7. **Result Type Testing** - `.asSuccess()`, `.asFailure()`

**Always consult:** `docs/ARCHITECTURE.md` and existing test files for reference implementations.

---

## Reference Documentation

For detailed information, see:

- `references/test-base-classes.md` - Detailed base class documentation and usage patterns
- `references/flow-testing-patterns.md` - Complete Turbine patterns for StateFlow/EventFlow
- `references/critical-gotchas.md` - Full anti-pattern reference and debugging tips

**Complete Examples:**
- `examples/viewmodel-test-example.md` - Full ViewModel test with StateFlow/EventFlow
- `examples/compose-screen-test-example.md` - Full Compose screen test
- `examples/repository-test-example.md` - Full repository test with mocks and fakes
