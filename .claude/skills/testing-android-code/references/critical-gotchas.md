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

## Bitwarden Mocking Guidelines

**Mock at architectural boundaries:**
- Repository → ViewModel (mock repository)
- Service → Repository (mock service)
- API → Service (use MockWebServer, not mocks)
- DiskSource → Repository (mock disk source)

**Use Fakes for:**
- `FakeDispatcherManager` - deterministic coroutines
- `FakeConfigDiskSource` - in-memory config storage
- `FakeSharedPreferences` - memory-backed preferences

**Create real instances for:**
- Data classes, value objects (User, Config, CipherView)
- Test data builders (`createMockCipher(number = 1)`)

## Summary Checklist

Before submitting tests, verify:

- [ ] No `assertCoroutineThrows` inside `runTest`
- [ ] All static mocks have `unmockk` in `@After`
- [ ] EventFlow tests start with `expectNoEvents()`
- [ ] Using FakeDispatcherManager, not real dispatchers
- [ ] All coroutine tests use `runTest`
- [ ] Tests don't depend on execution order
- [ ] Complex mocks use `relaxed = true`
- [ ] Test data is created fresh for each test
- [ ] Mocking behavior, not value objects
- [ ] Testing observable behavior, not implementation

When tests fail mysteriously, check these gotchas first.