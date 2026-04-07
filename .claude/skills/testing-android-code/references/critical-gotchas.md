# Critical Gotchas and Anti-Patterns

Common mistakes and pitfalls when writing tests in the Bitwarden Android codebase.

## ❌ NEVER wrap assertCoroutineThrows in runTest

### The Problem

`runTest` catches exceptions and rethrows them, which breaks the `assertCoroutineThrows` assertion pattern.

### Wrong

```kotlin
@Test
fun `test exception`() = runTest {
    assertCoroutineThrows<Exception> {
        repository.throwingFunction()
    }  // Won't work - exception is caught by runTest!
}
```

### Correct

```kotlin
@Test
fun `test exception`() {
    assertCoroutineThrows<Exception> {
        repository.throwingFunction()
    }  // Works correctly
}
```

### Why This Happens

`runTest` provides a coroutine scope and catches exceptions to provide better error messages. However, `assertCoroutineThrows` needs to catch the exception itself to verify it was thrown. When wrapped in `runTest`, the exception is caught twice, breaking the assertion.

## ❌ ALWAYS unmock static functions

### The Problem

MockK's static mocking persists across tests. Forgetting to clean up causes mysterious failures in subsequent tests.

### Wrong

```kotlin
@Before
fun setup() {
    mockkStatic(::isBuildVersionAtLeast)
    every { isBuildVersionAtLeast(any()) } returns true
}

// Forgot @After - subsequent tests will fail mysteriously!
```

### Correct

```kotlin
@Before
fun setup() {
    mockkStatic(::isBuildVersionAtLeast)
    every { isBuildVersionAtLeast(any()) } returns true
}

@After
fun tearDown() {
    unmockkStatic(::isBuildVersionAtLeast)  // CRITICAL
}
```

### Common Static Functions to Watch

```kotlin
// Platform version checks
mockkStatic(::isBuildVersionAtLeast)
unmockkStatic(::isBuildVersionAtLeast)

// URI parsing
mockkStatic(Uri::class)
unmockkStatic(Uri::class)

// Static utility functions
mockkStatic(MyUtilClass::class)
unmockkStatic(MyUtilClass::class)
```

### Debugging Tip

If tests pass individually but fail when run together, suspect static mocking cleanup issues.

## ❌ Don't confuse StateFlow and EventFlow testing

### StateFlow (replay = 1)

```kotlin
// CORRECT - StateFlow always has current value
viewModel.stateFlow.test {
    val initial = awaitItem()  // Gets current state immediately
    viewModel.trySendAction(action)
    val updated = awaitItem()  // Gets new state
}
```

### EventFlow (no replay)

```kotlin
// CORRECT - EventFlow has no initial value
viewModel.eventFlow.test {
    expectNoEvents()  // MUST do this first
    viewModel.trySendAction(action)
    val event = awaitItem()  // Gets emitted event
}
```

### Common Mistake

```kotlin
// WRONG - Forgetting expectNoEvents() on EventFlow
viewModel.eventFlow.test {
    viewModel.trySendAction(action)  // May cause flaky tests
    assertEquals(event, awaitItem())
}
```

## ❌ Don't mix real and test dispatchers

### Wrong

```kotlin
private val repository = ExampleRepositoryImpl(
    dispatcherManager = DispatcherManagerImpl(),  // Real dispatcher!
)

@Test
fun `test repository`() = runTest {
    // Test will have timing issues - real dispatcher != test dispatcher
}
```

### Correct

```kotlin
private val repository = ExampleRepositoryImpl(
    dispatcherManager = FakeDispatcherManager(),  // Test dispatcher
)

@Test
fun `test repository`() = runTest {
    // Test runs deterministically
}
```

### Why This Matters

Real dispatchers use actual thread pools and delays. Test dispatchers (UnconfinedTestDispatcher) execute immediately and deterministically. Mixing them causes:
- Non-deterministic test failures
- Real delays in tests (slow test suite)
- Race conditions

### Always Use

- `FakeDispatcherManager()` for repositories
- `UnconfinedTestDispatcher()` when manually creating dispatchers
- `runTest` for coroutine tests (provides TestDispatcher automatically)

## ❌ Don't forget to use runTest for coroutine tests

### Wrong

```kotlin
@Test
fun `test coroutine`() {
    viewModel.stateFlow.test { /* ... */ }  // Missing runTest!
}
```

This causes:
- Test completes before coroutines finish
- False positives (test passes but assertions never run)
- Mysterious failures

### Correct

```kotlin
@Test
fun `test coroutine`() = runTest {
    viewModel.stateFlow.test { /* ... */ }
}
```

### When runTest is Required

- Testing ViewModels (they use `viewModelScope`)
- Testing Flows with Turbine `.test {}`
- Testing repositories with suspend functions
- Any test calling suspend functions

### Exception: assertCoroutineThrows

As noted above, `assertCoroutineThrows` should NOT be wrapped in `runTest`.

## ❌ Don't forget relaxed = true for complex mocks

### Without relaxed

```kotlin
private val viewModel = mockk<ExampleViewModel>()  // Must mock every method!

// Error: "no answer found for: stateFlow"
```

### With relaxed

```kotlin
private val viewModel = mockk<ExampleViewModel>(relaxed = true) {
    // Only mock what you care about
    every { stateFlow } returns mutableStateFlow
    every { eventFlow } returns mutableEventFlow
}
```

### When to Use relaxed

- Mocking ViewModels in Compose tests
- Mocking complex objects with many methods
- When you only care about specific method calls

### When NOT to Use relaxed

- Mocking repository interfaces (be explicit about behavior)
- When you want to verify NO unexpected calls
- Testing error paths (want test to fail if unexpected method called)

## ❌ Don't assert individual fields when complete state is available

### The Problem

Asserting individual state fields can miss unintended side effects on other fields.

### Wrong

```kotlin
@Test
fun `action should update state`() = runTest {
    viewModel.trySendAction(SomeAction.DoThing)

    val state = viewModel.stateFlow.value
    assertEquals(null, state.dialog)  // Only checks one field!
}
```

### Correct

```kotlin
@Test
fun `action should update state`() = runTest {
    viewModel.trySendAction(SomeAction.DoThing)

    val expected = SomeState(
        isLoading = false,
        data = "result",
        dialog = null,
    )
    assertEquals(expected, viewModel.stateFlow.value)  // Checks all fields
}
```

### Why This Matters

- Catches unintended mutations to other state fields
- Makes expected state explicit and readable
- Prevents silent regressions when state structure changes

---

## ❌ Don't use Kotlin assert() for boolean checks

### The Problem

Kotlin's `assert()` doesn't follow JUnit conventions and provides poor failure messages.

### Wrong

```kotlin
@Test
fun `event should trigger callback`() {
    mutableEventFlow.tryEmit(SomeEvent.Navigate)

    assert(onNavigateCalled)  // Kotlin assert - bad failure messages
}
```

### Correct

```kotlin
@Test
fun `event should trigger callback`() {
    mutableEventFlow.tryEmit(SomeEvent.Navigate)

    assertTrue(onNavigateCalled)  // JUnit assertTrue - proper assertion
}
```

### Always Use JUnit Assertions

- `assertTrue()` / `assertFalse()` for booleans
- `assertEquals()` for value comparisons
- `assertNotNull()` / `assertNull()` for nullability
- `assertThrows<T>()` for exceptions

---

## ❌ Don't pass SavedStateHandle to test factory methods

### The Problem

Exposing `SavedStateHandle` in test factory methods leaks Android framework details into test logic.

### Wrong

```kotlin
private fun createViewModel(
    savedStateHandle: SavedStateHandle = SavedStateHandle(),  // Framework type exposed
): MyViewModel = MyViewModel(
    savedStateHandle = savedStateHandle,
    repository = mockRepository,
)

@Test
fun `initial state from saved state`() = runTest {
    val savedState = MyState(isLoading = true)
    val savedStateHandle = SavedStateHandle(mapOf("state" to savedState))

    val viewModel = createViewModel(savedStateHandle = savedStateHandle)
    // ...
}
```

### Correct

```kotlin
private fun createViewModel(
    initialState: MyState? = null,  // Domain type only
): MyViewModel = MyViewModel(
    savedStateHandle = SavedStateHandle().apply { set("state", initialState) },
    repository = mockRepository,
)

@Test
fun `initial state from saved state`() = runTest {
    val savedState = MyState(isLoading = true)

    val viewModel = createViewModel(initialState = savedState)
    // ...
}
```

### Why This Matters

- Cleaner, more intuitive test code
- Hides SavedStateHandle implementation details
- Follows Bitwarden conventions

---

## ❌ Don't test SavedStateHandle persistence in unit tests

### The Problem

Testing whether state persists to SavedStateHandle is testing Android framework behavior, not your business logic.

### Wrong

```kotlin
@Test
fun `state should persist to SavedStateHandle`() = runTest {
    val savedStateHandle = SavedStateHandle()
    val viewModel = createViewModel(savedStateHandle = savedStateHandle)

    viewModel.trySendAction(SomeAction)

    val savedState = savedStateHandle.get<MyState>("state")
    assertEquals(expectedState, savedState)  // Testing framework, not logic!
}
```

### Correct

Focus on testing business logic and state transformations:

```kotlin
@Test
fun `action should update state correctly`() = runTest {
    val viewModel = createViewModel()

    viewModel.trySendAction(SomeAction)

    assertEquals(expectedState, viewModel.stateFlow.value)  // Test observable state
}
```

---

## ❌ Don't use static mocking when DI pattern is available

### The Problem

Static mocking (`mockkStatic`) is harder to maintain and less testable than dependency injection.

### Wrong

```kotlin
class ParserTest {
    @BeforeEach
    fun setup() {
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns mockk {
            every { toString() } returns "fixed-uuid"
        }
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(UUID::class)
    }
}
```

### Correct

Extract an interface and inject it:

```kotlin
// Production code
interface UuidManager {
    fun generateUuid(): String
}

class UuidManagerImpl : UuidManager {
    override fun generateUuid(): String = UUID.randomUUID().toString()
}

class Parser(private val uuidManager: UuidManager) { ... }

// Test code
class ParserTest {
    private val mockUuidManager = mockk<UuidManager>()

    @BeforeEach
    fun setup() {
        every { mockUuidManager.generateUuid() } returns "fixed-uuid"
    }

    // No tearDown needed - no static mocking!
}
```

### When to Use This Pattern

- UUID generation
- Timestamp/Clock operations
- System property access
- Any static function that needs deterministic testing

---

## ❌ Don't forget to test null stream returns from Android APIs

### The Problem

Android's `ContentResolver.openOutputStream()` and `openInputStream()` can return null, not just throw exceptions.

### Wrong

```kotlin
class FileManagerTest {
    @Test
    fun `stringToUri with exception should return false`() = runTest {
        every { mockContentResolver.openOutputStream(any()) } throws IOException()

        val result = fileManager.stringToUri(mockUri, "data")

        assertFalse(result)
    }
    // Missing: test for null return!
}
```

### Correct

```kotlin
class FileManagerTest {
    @Test
    fun `stringToUri with exception should return false`() = runTest {
        every { mockContentResolver.openOutputStream(any()) } throws IOException()

        val result = fileManager.stringToUri(mockUri, "data")
        assertFalse(result)
    }

    @Test
    fun `stringToUri with null stream should return false`() = runTest {
        every { mockContentResolver.openOutputStream(any()) } returns null

        val result = fileManager.stringToUri(mockUri, "data")
        assertFalse(result)  // CRITICAL: must handle null!
    }
}
```

### Common Android APIs That Return Null

- `ContentResolver.openOutputStream()` / `openInputStream()`
- `Context.getExternalFilesDir()`
- `PackageManager.getApplicationInfo()` (can throw)

---

## Bitwarden Mocking Guidelines

**Mock at architectural boundaries:**
- Repository → ViewModel (mock repository)
- Service → Repository (mock service)
- API → Service (use MockWebServer, not mocks)
- DiskSource → Repository (mock disk source)

**Fake vs Mock Strategy (IMPORTANT):**
- **Happy paths**: Use Fake implementations (`FakeAuthenticatorDiskSource`, `FakeVaultDiskSource`)
- **Error paths**: Use MockK with isolated repository instances

```kotlin
// Happy path - use Fake
private val fakeDiskSource = FakeAuthenticatorDiskSource()

@Test
fun `createItem should return Success`() = runTest {
    val result = repository.createItem(mockItem)
    assertEquals(CreateItemResult.Success, result)
}

// Error path - use isolated Mock
@Test
fun `createItem with exception should return Error`() = runTest {
    val mockDiskSource = mockk<AuthenticatorDiskSource> {
        coEvery { saveItem(any()) } throws RuntimeException()
    }
    val repository = RepositoryImpl(diskSource = mockDiskSource)

    val result = repository.createItem(mockItem)
    assertEquals(CreateItemResult.Error, result)
}
```

**Use Fakes for:**
- `FakeDispatcherManager` - deterministic coroutines
- `FakeConfigDiskSource` - in-memory config storage
- `FakeSharedPreferences` - memory-backed preferences
- `FakeAuthenticatorDiskSource` - in-memory authenticator storage

**Create real instances for:**
- Data classes, value objects (User, Config, CipherView)
- Test data builders (`createMockCipher(number = 1)`)

## ❌ Don't forget bufferedMutableSharedFlow with onSubscription for Fakes

### The Problem

Fake data sources using `MutableSharedFlow` won't emit cached state to new subscribers without explicit handling.

### Wrong

```kotlin
class FakeDataSource : DataSource {
    private val mutableFlow = MutableSharedFlow<List<Item>>()
    private val storedItems = mutableListOf<Item>()

    override fun getItems(): Flow<List<Item>> = mutableFlow

    override suspend fun saveItem(item: Item) {
        storedItems.add(item)
        mutableFlow.emit(storedItems)
    }
}

// Test: Initial collection gets nothing!
repository.dataFlow.test {
    // Hangs or fails - no initial emission
}
```

### Correct

```kotlin
class FakeDataSource : DataSource {
    private val mutableFlow = bufferedMutableSharedFlow<List<Item>>()
    private val storedItems = mutableListOf<Item>()

    override fun getItems(): Flow<List<Item>> = mutableFlow
        .onSubscription { emit(storedItems.toList()) }

    override suspend fun saveItem(item: Item) {
        storedItems.add(item)
        mutableFlow.emit(storedItems.toList())
    }
}

// Test: Initial collection receives current state
repository.dataFlow.test {
    assertEquals(emptyList(), awaitItem())  // Works!
}
```

### Key Points

- Use `bufferedMutableSharedFlow()` from `core/data/repository/util/`
- Add `.onSubscription { emit(currentState) }` for immediate state emission
- This ensures new collectors receive the current cached state

---

## ✅ Use Result extension functions for assertions

### The Pattern

Use `asSuccess()` and `asFailure()` extensions from `com.bitwarden.core.data.util` for cleaner Result assertions.

### Success Path

```kotlin
@Test
fun `getData should return success`() = runTest {
    val result = repository.getData()
    val expected = expectedData.asSuccess()

    assertEquals(expected.getOrNull(), result.getOrNull())
}
```

### Failure Path

```kotlin
@Test
fun `getData with error should return failure`() = runTest {
    val exception = IOException("Network error")
    coEvery { mockService.getData() } returns exception.asFailure()

    val result = repository.getData()

    assertTrue(result.isFailure)
    assertEquals(exception, result.exceptionOrNull())
}
```

### Avoid Redundant Assertions

```kotlin
// WRONG - redundant success checks
assertTrue(result.isSuccess)
assertTrue(expected.isSuccess)
assertArrayEquals(expected.getOrNull(), result.getOrNull())

// CORRECT - final assertion is sufficient
assertArrayEquals(expected.getOrNull(), result.getOrNull())
```

---

## Summary Checklist

Before submitting tests, verify:

**Core Patterns:**
- [ ] No `assertCoroutineThrows` inside `runTest`
- [ ] All static mocks have `unmockk` in `@After`
- [ ] EventFlow tests start with `expectNoEvents()`
- [ ] Using FakeDispatcherManager, not real dispatchers
- [ ] All coroutine tests use `runTest`

**Assertion Patterns:**
- [ ] Assert complete state objects, not individual fields
- [ ] Use JUnit `assertTrue()`, not Kotlin `assert()`
- [ ] Use `asSuccess()` for Result type assertions
- [ ] Avoid redundant assertion patterns

**Test Design:**
- [ ] Test factory methods accept domain types, not SavedStateHandle
- [ ] Use Fakes for happy paths, Mocks for error paths
- [ ] Prefer DI patterns over static mocking
- [ ] Test null returns from Android APIs (streams, files)
- [ ] Fakes use `bufferedMutableSharedFlow()` with `.onSubscription`

**General:**
- [ ] Tests don't depend on execution order
- [ ] Complex mocks use `relaxed = true`
- [ ] Test data is created fresh for each test
- [ ] Mocking behavior, not value objects
- [ ] Testing observable behavior, not implementation

When tests fail mysteriously, check these gotchas first.
