# Testing Patterns Quick Reference

Quick reference for Bitwarden Android testing patterns during code reviews. For comprehensive details, read `docs/ARCHITECTURE.md` and `docs/STYLE_AND_BEST_PRACTICES.md`.

## ViewModel Tests

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

## Repository Tests

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

## Quick Checklist

### Testing
- [ ] ViewModels have unit tests?
- [ ] Tests verify behavior, not implementation?
- [ ] Edge cases covered?
- [ ] Error scenarios tested?

### Code Quality
- [ ] Null safety handled properly (no `!!` without guarantee)?
- [ ] Public APIs have KDoc?
- [ ] Following naming conventions?

---

For comprehensive details, always refer to:
- `docs/ARCHITECTURE.md` - Full architecture patterns
- `docs/STYLE_AND_BEST_PRACTICES.md` - Complete style guide
