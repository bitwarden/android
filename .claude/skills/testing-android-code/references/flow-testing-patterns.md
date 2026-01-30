# Flow Testing with Turbine

Bitwarden Android uses Turbine for testing Kotlin Flows, including the critical distinction between StateFlow and EventFlow patterns.

## StateFlow vs EventFlow

### StateFlow (Replayed)

**Characteristics:**
- `replay = 1` - Always emits current value to new collectors
- First `awaitItem()` returns the current/initial state
- Survives configuration changes
- Used for UI state that needs to be immediately available

**Test Pattern:**
```kotlin
@Test
fun `action should update state`() = runTest {
    val viewModel = MyViewModel(savedStateHandle, mockRepository)

    viewModel.stateFlow.test {
        // First awaitItem() gets CURRENT state
        assertEquals(INITIAL_STATE, awaitItem())

        // Trigger action
        viewModel.trySendAction(MyAction.LoadData)

        // Next awaitItem() gets UPDATED state
        assertEquals(LOADING_STATE, awaitItem())
        assertEquals(SUCCESS_STATE, awaitItem())
    }
}
```

### EventFlow (No Replay)

**Characteristics:**
- `replay = 0` - Only emits new events after subscription
- No initial value emission
- One-time events (navigation, toasts, dialogs)
- Does not survive configuration changes

**Test Pattern:**
```kotlin
@Test
fun `action should emit event`() = runTest {
    val viewModel = MyViewModel(savedStateHandle, mockRepository)

    viewModel.eventFlow.test {
        // MUST call expectNoEvents() first - nothing emitted yet
        expectNoEvents()

        // Trigger action
        viewModel.trySendAction(MyAction.Submit)

        // Now expect the event
        assertEquals(MyEvent.NavigateToNext, awaitItem())
    }
}
```

**Critical:** Always call `expectNoEvents()` before triggering actions on EventFlow. Forgetting this causes flaky tests.

## Testing State and Events Simultaneously

Use the `stateEventFlow()` helper from `BaseViewModelTest`:

```kotlin
@Test
fun `complex action should update state and emit event`() = runTest {
    val viewModel = MyViewModel(savedStateHandle, mockRepository)

    viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
        // Initial state
        assertEquals(INITIAL_STATE, stateFlow.awaitItem())

        // No events yet
        eventFlow.expectNoEvents()

        // Trigger action
        viewModel.trySendAction(MyAction.ComplexAction)

        // Verify state progression
        assertEquals(LOADING_STATE, stateFlow.awaitItem())
        assertEquals(SUCCESS_STATE, stateFlow.awaitItem())

        // Verify event emission
        assertEquals(MyEvent.ShowToast, eventFlow.awaitItem())
    }
}
```

## Repository Flow Testing

### Testing Database Flows

```kotlin
@Test
fun `dataFlow should emit when database updates`() = runTest {
    val dataFlow = MutableStateFlow(initialData)
    every { mockDiskSource.dataFlow } returns dataFlow

    repository.dataFlow.test {
        // Initial value
        assertEquals(initialData, awaitItem())

        // Update disk source
        dataFlow.value = updatedData

        // Verify emission
        assertEquals(updatedData, awaitItem())
    }
}
```

### Testing Transformed Flows

```kotlin
@Test
fun `flow transformation should map correctly`() = runTest {
    val sourceFlow = MutableStateFlow(UserEntity(id = "1", name = "John"))
    every { mockDao.observeUser() } returns sourceFlow

    // Repository transforms entity to domain model
    repository.userFlow.test {
        val expectedUser = User(id = "1", name = "John")
        assertEquals(expectedUser, awaitItem())
    }
}
```

## Common Patterns

### Pattern 1: Testing Initial State + Action

```kotlin
@Test
fun `load data should update from idle to loading to success`() = runTest {
    coEvery { repository.getData() } returns "data".asSuccess()

    viewModel.stateFlow.test {
        assertEquals(DEFAULT_STATE, awaitItem())

        viewModel.loadData()

        assertEquals(DEFAULT_STATE.copy(loadingState = LoadingState.Loading), awaitItem())
        assertEquals(DEFAULT_STATE.copy(loadingState = LoadingState.Success), awaitItem())
    }
}
```

### Pattern 2: Testing Error States

```kotlin
@Test
fun `load data with error should emit failure state`() = runTest {
    val error = Exception("Network error")
    coEvery { repository.getData() } returns error.asFailure()

    viewModel.stateFlow.test {
        assertEquals(DEFAULT_STATE, awaitItem())

        viewModel.loadData()

        assertEquals(DEFAULT_STATE.copy(loadingState = LoadingState.Loading), awaitItem())
        assertEquals(
            DEFAULT_STATE.copy(loadingState = LoadingState.Error("Network error")),
            awaitItem(),
        )
    }
}
```

### Pattern 3: Testing Event Sequences

```kotlin
@Test
fun `submit should emit validation then navigation events`() = runTest {
    viewModel.eventFlow.test {
        expectNoEvents()

        viewModel.trySendAction(MyAction.Submit)

        assertEquals(MyEvent.ShowValidation, awaitItem())
        assertEquals(MyEvent.NavigateToNext, awaitItem())
    }
}
```

### Pattern 4: Testing Cancellation

```kotlin
@Test
fun `cancelling collection should stop emissions`() = runTest {
    val flow = flow {
        repeat(100) {
            emit(it)
            delay(100)
        }
    }

    flow.test {
        assertEquals(0, awaitItem())
        assertEquals(1, awaitItem())

        // Cancel after 2 items
        cancel()

        // No more items received
    }
}
```

## Anti-Patterns

### ❌ Forgetting expectNoEvents() on EventFlow

```kotlin
// WRONG
viewModel.eventFlow.test {
    viewModel.trySendAction(action)  // May fail - no initial expectNoEvents
    assertEquals(event, awaitItem())
}

// CORRECT
viewModel.eventFlow.test {
    expectNoEvents()  // ALWAYS do this first
    viewModel.trySendAction(action)
    assertEquals(event, awaitItem())
}
```

### ❌ Not Using runTest

```kotlin
// WRONG - Missing runTest
@Test
fun `test flow`() {
    flow.test { /* ... */ }
}

// CORRECT
@Test
fun `test flow`() = runTest {
    flow.test { /* ... */ }
}
```

### ❌ Mixing StateFlow and EventFlow Patterns

```kotlin
// WRONG - Treating StateFlow like EventFlow
stateFlow.test {
    expectNoEvents()  // Unnecessary - StateFlow always has value
    /* ... */
}

// WRONG - Treating EventFlow like StateFlow
eventFlow.test {
    val item = awaitItem()  // Will hang - no initial value!
    /* ... */
}
```

## Reference Implementations

**ViewModel with StateFlow and EventFlow:**
`app/src/test/kotlin/com/x8bit/bitwarden/ui/tools/feature/generator/GeneratorViewModelTest.kt`

**Repository Flow Testing:**
`data/src/test/kotlin/com/bitwarden/data/tools/generator/repository/GeneratorRepositoryTest.kt`

**Complex Flow Transformations:**
`data/src/test/kotlin/com/bitwarden/data/vault/repository/VaultRepositoryTest.kt`
