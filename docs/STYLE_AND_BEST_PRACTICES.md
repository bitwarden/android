# Code Style Guidelines

## Contents

* [Style : Kotlin](#style--kotlin)
* [Style : ViewModels](#style--viewmodels)
* [Best Practices : Kotlin](#best-practices--kotlin)
* [Best Practices : Jetpack Compose](#best-practices--jetpack-compose)

The following outlines the general code style and best practices for Android development. It is expected that all developers are familiar with the code style documents referenced here and have them bookmarked for quick reference when necessary.

Rather than repeating the rules listed in those documents, this guide only mentions rules that **override** or are **in addition to** the ones present in them. Also note that while the [bitwarden-style.xml](/docs/bitwarden-style.xml) formatter and [detekt tool](https://github.com/detekt/detekt) will enforce many of the rules (such as the 100 character line limit) there are many it can not or will not enforce and it is therefore extremely important that all developers **read and follow this document.** For more details on what the formatter _does_ handle, refer directly to the settings in Android Studio under `Preferences > Editor > Code Style` and select the language of interest.

## Style : Kotlin

The library's Kotlin code style adheres closely to both the [code style documentation for the Kotlin language itself](https://kotlinlang.org/docs/reference/coding-conventions.html) and the [code style documentation for the Android Open Source Project](https://android.github.io/kotlin-guides/style.html). The former provides the "base" reference while the AOSP docs should be viewed as tweaks to that document. In the rare cases where the two documents disagree, the AOSP rules should apply.

### Disagreements between the Kotlin language and Android code style documents

Some notable disagreements between the two documents include:

- AOSP rules do not allow for two-letter acronyms to be capitalized separately when part of a member name. For example

    ```kotlin
    // Bad
    val fetcher: IDFetcher

    // Good
    val fetcher: IdFetcher
    ```

### Custom library style

In general, the code style documents referenced above cover most of the basic cases of interest. There will always be situations, however, where the documents are unclear, vague, or simply do not offer an opinion. Furthermore, there are some cases where we will directly break with the code style documents. Below are some examples of these cases (though it should be noted that no single document can ever fully classify our complete style and existing code should always be sought for a reference in questionable cases.)

#### Class and file layout

The documents specify that a class layout should use the following order:

```
- Property declarations and initializer blocks
- Secondary constructors
- Method declarations
- Companion object
```

We will adhere to this order, but there are a few additions / modifications :

- Nested class definitions should be placed _after_ the Companion object. In cases where the nested
  class is only used internally, it is also valid to simply place the class in the same file after
  the main source class.
- The code style documents say to use "logical ordering" for method declarations. We will adhere to this principle while also noting that does not mean new code in a file should simply be added to the end; be thoughtful about placement! In the absence of any compelling logistical ordering, alphabetization is a valid choice (though not required).

  Also please note the following:
    - Methods with certain "special modifiers" (`override`, `operator`, `abstact`, etc.) should come first and methods with the same modifiers should be grouped together. Certain types of annotations may also qualify as a "special modifier" (refer to existing library code).
    - If any kind of method group described above requires "sub-groups", these can be denoted by the use of the `//region <Name> ... //endregion <Name>` markers. These regions should be used for a very small set of methods, such as for the overrides to a single interface.

#### Type omission

We generally prefer to omit types whenever the compiler allows it **and** the return type is _unambiguous_. This is true for functions, properties, and variable declarations.

```kotlin
// Good: This is clearly an integer value.
val durationInSeconds = 100
```

```kotlin
// Bad: The return type can only be inferred by looking at the signature of another function, which
// might itself be omitting a type.
fun requestData() = apiService.getData()
```

When omitting a type for a function, it is very important that it is done _thoughtfully_; a function's type should not be the _accidental consequence_ of overly concise code.

#### Expression functions

We generally prefer using expression functions whenever possible to keep the code concise and free of unnecessary boilerplate. This should only be done when a call chain naturally suggests such a form, though; complicated functions should not be forced into a single expression simply to take advantage of this feature.

#### Functions as parameters

We strongly encourage the direct use of functions as parameters rather than defining one-off listener classes. For example

```kotlin
// Good
fun waitForEvent(handler: (Event) -> Unit) {
    val event = produceEvent()
    handler(event)
}
```

is preferable to

```kotlin
interface EventHandler {
    fun handleEvent(event: Event): Unit
}

...

// Bad
fun waitForEvent(handler: EventHandler) {
    val event = produceEvent()
    handler.handleEvent(event)
}
```

This not only reduces the amount of boilerplate code necessary, but it also ensures lambdas can be used properly from within Kotlin. When an interface is preferred for some reason, strongly consider the user of [functional interfaces](https://kotlinlang.org/docs/fun-interfaces.html) to continue to take advantage of SAM conversions at the callsite.


#### Long function calls

As discussed [in the code style document](https://kotlinlang.org/docs/coding-conventions.html#method-calls) long function calls should place each argument on their own line. The closing parenthesis should be on its own line as well:

```kotlin
drawSquare(
    x = 10,
    y = 10,
    width = 100,
    height = 100,
    fill = true,
)
```

Unlike those rules, however, we don't allow grouping "multiple closely related arguments on the same line".

When there is only one argument but the function call must wrap to multiple lines, that argument should still be placed on its own line(s).

```kotlin
// Good
.register(
    object : Callback<DeviceEvent>() {
        override fun onSuccess() = ...
        override fun onError() = ...
    }
 )
```

```kotlin
// Bad
.register(object : Callback() {
    override fun onSuccess() = ...
    override fun onError() = ...
})
```

#### Annotation-related formatting

When annotating properties, the annotations should be on their own lines and there must be a space between each annotated property. The following is correct:

```kotlin
// Good
@Inject
val propertyA: PropertyA

@Inject
val propertyB: PropertyB

@Inject
@CustomAnnotation
val propertyC: PropertyC

@Inject
val propertyD: PropertyD
```

while this is not:

```kotlin
// Bad
@Inject val propertyA: PropertyA
@Inject val propertyB: PropertyB
@Inject @CustomAnnotation
val propertyC: PropertyC
@Inject val propertyD: PropertyD
```

#### Chained call formatting

As mentioned in the main documents, for long call chains it is preferred to put all method calls on their own lines, even the first:

```kotlin
val anchor = owner
    ?.firstChild!!
    .siblings(forward = true)
    .dropWhile { it is PsiComment || it is PsiWhiteSpace }
```

The goal should always be to keep the start of function calls and the closing parenthesis vertically aligned. Consider the following example:

```kotlin
// Good
fragmentComponent
    .customFragmentComponent(
        CustomFragmentModule(
            this,
            argument,
        )
    )
    .inject(this)
```

Notice what happens if the first method call to `.customFragmentComponent` is placed on the first line:

```kotlin
// Bad
fragmentComponent.customFragmentComponent(
    CustomFragmentModule(
        this,
        argument,
    )
)
    .inject(this)
```

The closing parenthesis for `.customFragmentComponent()` does not align with either it nor with `.inject`.

Note that in cases where a single method is being called, it is valid for it to be on the same line as the instance:

```kotlin
// Good
return Bundle().apply {
    putParcelable(KEY_ARGUMENTS, arguments)
}
```

If a chain is created here, however, the method must be moved down:

```kotlin
// Good
return Bundle()
    .apply {
        putParcelable(KEY_ARGUMENTS, arguments)
    }
    .also { ... }
```

Failure to do so will result in the following incorrect formatting:

```kotlin
// Bad
return Bundle().apply {
    putParcelable(KEY_ARGUMENTS, arguments)
}
    .also { ... }
```

In some cases proper formatting will be impossible. Each of the following---while not ideal---is in the "correct" format because there is no good alternative:

```kotlin
// Acceptable (no better alternative)
topLevelFunction(
    ...long argument list...
)
    .apply { ... }
```

```kotlin
// Acceptable (no better alternative)
ObjectConstructor(
    ...long argument list...
)
    .apply { ... }
```

These types of scenarios should be avoided when possible (as described above) but are allowed when necessary.

#### Miscellaneous Code Formatting

Whenever questions about code formatting arise in which multiple options are valid according to all previously described rules, [the Rectangle Rule](https://github.com/google/google-java-format/wiki/The-Rectangle-Rule) is a good test to use to prefer one style over the other.

#### Documentation

All public classes, functions, and properties should include documentation in the [KDoc style](https://kotlinlang.org/docs/kotlin-doc.html). Private classes, companion objects, functions, and properties may optionally be documented as needed.

##### Class Documentation

Classes with more than 2 constructor properties should document each individually using the `@property` label; otherwise the property descriptions can be incorporated into the class description:

```kotlin
// Good. The class contains a single constructor property that is included in the class's own
// documentation

/**
 * A wrapper class for a unique idenfitier, [id].
 */
data class IdWrapper(
    val id: String,
)
```


```kotlin
// Good. The class contains more than two properties and each are documented separately.

/**
 * A class containing various data.
 *
 * @property id The unique identifier for the data.
 * @property name The name of the data.
 * @property value The value associated with the data.
 */
data class Data(
    val id: String,
    val name: String,
    val value: String,
)
```

```kotlin
// Bad. The constructor properties are not documented.

/**
 * A class containing various data.
 */
data class Data(
    val id: String,
    val name: String,
    val value: String,
)
```

```kotlin
// Bad. The constructor properties are not documented individually.

/**
 * A class containing various data ([id], [name], [value]).
 */
data class Data(
    val id: String,
    val name: String,
    val value: String,
)
```


```kotlin
// Bad. Not using KDoc style.

// A class containing various data ([id], [name], [value]).
data class Data(
    val id: String,
    val name: String,
    val value: String,
)
```

##### Functions

Functions are typically allowed to include documentation for parameters within the body of the function documentation itself independent of their number:

```kotlin
// Good

/**
 * Gets the data for the given [id].
 */
fun getData(id: String): Data
```

```kotlin
// Good

/**
 * Gets a list of data items from the [startDateInMillis] to the [endDateInMillis]. This number of
 * items in the list will not exceed [maxCount].
 */
fun getDataList(
    startDateInMillis: Long,
    endDateInMillis: Long,
    maxCount: Long
): List<Data>
```

```kotlin
// Bad. Not in KDoc style.

// Gets the data for the given [id].
fun getData(id: String): Data
```

When each parameter appears to require more focused documentation, `@param` may be used;

```kotlin
// Good

/**
 * Gets a list of data items.
 *
 * @param startDateInMillis The beginning data (in epoch time as milliseconds) to begin searching
 * for data items.
 * @param endDateInMillis The end data (in epoch time as milliseconds) to stop searching
 * for data items.
 * @param maxCount The maximum number of items to return.
 */
fun getDataList(
    startDateInMillis: Long,
    endDateInMillis: Long,
    maxCount: Long
): List<Data>
```

#### Inline comments

Inline comments are encouraged, particularly when the logic being described is not self-explanatory. The comments should:

- begin with `//`.
- include a space before the first word.
- capitalize the first word.
- optionally include punctuation for sentence fragments or single sentences.
- include punctuation for multiple sentences.
- prefer the "imperative" voice.

```kotlin
// Good

// Get the data from the database
val data = databaseDataSource.getData(id)
```

```kotlin
// Good

// Get the data from the database. This will happen synchronously.
val data = databaseDataSource.getData(id)
```

```kotlin
// OK. Not in the imperative voice.

// Gets the data from the database
val data = databaseDataSource.getData(id)
```

```kotlin
// Bad. Missing space before first word and missing capitalization on first word.

//get the data from the database
val data = databaseDataSource.getData(id)
```

```kotlin
// Bad. Missing punctuation for multiple sentences.

// Get the data from the database
// This will happen synchronously
val data = databaseDataSource.getData(id)
```

## Style : ViewModels

- Private functions that handle actions should be prefixed with "handle" and suffixed with the name of the action. (ex: `handleSubmitClick`)

## Best Practices : Kotlin

The following contains general tips and best practices that apply for Kotlin code (unless otherwise specified) that has not already been mentioned in their specific sections above.

- We will be adhering to the 100 character line limit. This will be enforced in most places by the auto-formatter.

- Avoid unnecessary `if` / `else` nesting and keep code as left-aligned as possible by using early return statements. For example

    ```kotlin
    // Bad
    fun someMethod() {
        if (someCondition) {
            // ...many lines of code...
        } else {
            // Do nothing
        }
    }

    // Bad
    fun someMethod() {
        if (someCondition) {
            // ...many lines of code...
        }
    }

    // Good
    fun someMethod() {
        if (!someCondition) {
            return
        }
        // ...many lines of code...
    }
    ```

  When an early return is not possible because additional code is required to run after the `if`, consider moving the `if` to its own method.

    ```kotlin
    // Bad
    fun someMethod() {
        if (someCondition) {
           // ...many lines of code...
        }
        // ...more code...
    }

    // Good
    fun someMethod() {
        internalHelperMethod()
        // ...more code...
    }

    // where...
    fun internalHelperMethod() {
        if (!someCondition) {
            return
        }
        // ...many lines of code...
    }
    ```

- Using an expression like `true == someBoolean` is acceptable only when `someBoolean` is nullable (i.e. `Boolean?`) and requires unwrapping:

    ```kotlin
    // Bad
    val nonNullBoolean: Boolean = true
    if (nonNullBoolean == true) ...

    // Good
    val nonNullBoolean: Boolean = true
    if (nonNullBoolean) ...

    // Bad (does not compile)
    val nullableBoolean: Boolean? = true
    if (nullableBoolean) ...

    // Good
    val nullableBoolean: Boolean? = true
    if (nullableBoolean == true) ...
    ```

- Any method or constructor left blank deliberately (such as an unneeded interface method) should be formatted as a one-line expression function with the explicit return type of `Unit` :

    ```kotlin
    override fun unusedMethod() = Unit
    ```

- Nullability should be very clearly communicated. In Kotlin, **only objects expected to be null should be given nullable types**:

    ```kotlin
    // Bad: Are these all *really* nullable? An network object with an "optional" ID does not make
    // much sense.
    data class NetworkData {
      val id: String?
      val name: String?
      val iconUrl: String?
    }

    // Good: The API docs for this class communicate that "id" and "name" will never be null, so
    // they should not be given nullable types. "iconUrl" is not guaranteed, so it may be made
    // nullable.
    data class NetworkData {
      val id: String
      val name: String
      val iconUrl: String?
    }
    ```

- Whenever possible, pass and return _interfaces_ rather than _implementations_. For example:

    ```kotlin
    // Bad: Why should the method care what type of List is passed to it and why should the caller
    // care what type of List is passed back?
    fun filter(input: ArrayList<String>): ArrayList<String> { ... }

    // Good
    fun filter(input: List<String>): List<String>  { ... }
    ```

- With very few exceptions, static variables should never be used. Their use is typically a symptom of a failure to properly declare and pass dependencies from one area of the library to another. It is better to explicitly pass the dependencies when possible. When "static" behavior is absolutely needed, an injected singleton should be used to provided the data instead:

    ```kotlin
    // Bad: This class should not depend on the static data held by another. Not only does it make
    // it hard to understand what the dependencies are here, this code is potentially unstable and
    // unpredictable and hard to control during tests.
    class NeedsExternalData {
        fun someMethod() {
            val data = SomeOtherClass.staticData
            // ...code requiring Data instance...
        }
    }
    ```

    ```kotlin
    // Good: This class requires a Data object to function properly so it is instantiated with it.
    class NeedsExternalData(
        private val data: Data,
    ) {
        fun someMethod() {
            // ...code requiring Data instance...
        }
    }
    ```

    ```kotlin
    // Good: This class requires a Data object for someMethod to function properly, so it is passed
    // in.
    class NeedsExternalData {
        fun someMethod(data: Data) {
            // ...code requiring Data instance...
        }
    }
    ```

    ```kotlin
    // Good: This class requires a Data object for someMethod to function properly, so we inject
    // an instance of DataProvider.
    class NeedsExternalData @Inject constructor(
        private val dataProvider: DataProvider
    ) {
        fun someMethod() {
             val data = dataProvider.getData()
            // ...code requiring Data instance...
        }
    }
    ```

- Functions should not intentionally throw exceptions! Any function that needs to represent the possibility of both a success and an error should either:
    - Return the [Result](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-result/) type.
    - Return a custom sealed class to model the possibilities.
    - Return a nullable value and clearly indicate a `null` represents an empty/failure state of some kind.

- When it is absolutely required (such as when dealing with external libraries that throw) exception handling should be done _properly_ and _only when necessary_. This means that (except for rare cases):

    - Never catch `Exception` generically. Always catch the specific errors that are _known to be possible_:

        ```kotlin
        // Bad: What exception is being thrown here and why? Is it something we can fix by trying
        // again or is it unrecoverable?
        try {
            service.makeServiceCall()
        } catch (e: Exception) {
            // ...
        }

        // Good
        try {
            service.makeServiceCall()
        } catch (e: RemoteException) {
            // ...
        }
        ```

    - Never catch a `RuntimeException` that _can't happen_:

        ```kotlin
        // Bad: A NumberFormatException is not possible here based on what we know about the value
        // we're using, so we're adding code that isn't necessary.
        val definitelyANumber = "1234"
        val value = try {
            Integer.parseInt(definitelyANumber)
        } catch (e: NumberFormatException) {
            // ...
        }
        ```

        ```kotlin
        // Good
        val definitelyANumber = "1234"
        val value = Integer.parseInt(definitlyANumber)
        ```

    - Never use `e.printStackTrace()`. Instead, use a configurable logging class:

        ```kotlin
        // Bad: This will print directly to the system, even in production.
        try {
            service.makeServiceCall()
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        ```

        ```kotlin
        // Good
        try {
            service.makeServiceCall()
        } catch (e: RemoteException) {
            Timber.e(e, "Error making a service call.")
        }
        ```

    - Never catch exceptions to "solve" bugs that should be managed elsewhere.

        ```kotlin
        // Bad: Why does the method throw the NPE? Is it our fault or a problem in the method itself?
        // It is impossible to tell here.
        val locationID = methodReturningNullableString()
        try {
            methodRequiringNonNullArguments(locationId)
        } catch (e: NullPointerException) {
            return
        }
        ```

        ```kotlin
        // Good
        val locationId = methodReturningNullableString()
        if (locationId == null) {
            return
        }
        methodRequiringNonNullArguments(locationId)
        ```

    - Never wrap huge chunks of code in a `try` / `catch`; only wrap the lines that throw an `Exception`:

        ```kotlin
        // Bad: It is difficult to follow the flow here. Which lines throw? At what point in the
        // code might we be forced to the catch block?
        try {
            val value = lineThatThrows()
            // ...many more lines of code...
        } catch (e: RemoteException) {
            // ...
        }
        ```

        ```kotlin
        // Good
        val value = try {
            lineThatThrows()
        } catch (e: RemoteException) {
            // ...handle the error case...
            return
        }
        // ...many more lines of code...
        ```

      When multiple lines throw the same exception, they may all be placed in the same `try` block:

        ```kotlin
        val value1: String
        val value2: String
        val value3: String
        try {
            value1 = lineThatThrows1()
            value2 = lineThatThrows2()
            value3 = lineThatThrows3()
        } catch (e: RemoteException) {
            // ...
        }
        ```

      When there is code between them that does _not_ throw, then each call should be wrapped separately:

        ```kotlin
        val value1 = try {
            lineThatThrows1()
        } catch (e: RemoteException) {
            // ...
        }
        // ...code that does not throw...
        val value2 = try {
            lineThatThrows2()
        } catch (e: RemoteException) {
            // ...
        }
        // ...code that does not throw...
        val value3 = try {
            lineThatThrows3()
        } catch (e: RemoteException) {
            // ...
        }
        ```

### Best Practices : Jetpack Compose

When writing UI layer code using Jetpack Compose, the [Compose API guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-api-guidelines.md) should be considered the go-to reference for style and best practices.

Special consideration should be taken to avoid unnecessary recompositions. There are now numerous sources that address the topic, including the following:

- https://developer.android.com/jetpack/compose/performance/bestpractices
- https://getstream.io/blog/jetpack-compose-guidelines/
- https://multithreaded.stitchfix.com/blog/2022/08/05/jetpack-compose-recomposition/

## Best Practices : Time and Clock Handling

To ensure testability and deterministic behavior, all code that needs the current time should use an injected `Clock` rather than calling `Instant.now()` or `DateTime.now()` directly.

### Why

- Direct calls to `Instant.now()` or `DateTime.now()` create non-deterministic behavior
- Testing requires brittle `mockkStatic` that can interfere across tests
- Injected `Clock` enables deterministic testing with `Clock.fixed(...)`
- Follows the dependency injection principle (no hidden dependencies)

### Pattern

**In ViewModels and classes with DI:**

```kotlin
// Good: Clock injected via Hilt
class MyViewModel @Inject constructor(
    private val clock: Clock,
    // other dependencies...
) : BaseViewModel<...>(...) {

    private fun handleSaveClick() {
        val item = Item(
            createdAt = clock.instant(),
            // ...
        )
    }
}

// Bad: Direct call creates hidden dependency
class MyViewModel @Inject constructor(
    // missing Clock...
) : BaseViewModel<...>(...) {

    private fun handleSaveClick() {
        val item = Item(
            createdAt = Instant.now(), // ❌ Non-testable
            // ...
        )
    }
}
```

**In extension functions and utilities:**

```kotlin
// Good: Accept Clock as parameter
fun SomeState.getRevisionDate(
    originalItem: Item?,
    clock: Clock,
): Instant = originalItem?.revisionDate ?: clock.instant()

// Bad: Hidden dependency on system clock
fun SomeState.getRevisionDate(
    originalItem: Item?,
): Instant = originalItem?.revisionDate ?: Instant.now() // ❌
```

### Testing

```kotlin
// Good: Fixed clock for deterministic tests
private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)

@Test
fun `test time-dependent logic`() = runTest {
    val viewModel = MyViewModel(
        clock = FIXED_CLOCK,
        // ...
    )
    // Test with predictable time
}

// Bad: Static mocking is fragile
mockkStatic(Instant::class)  // ❌ Avoid
every { Instant.now() } returns fixedInstant
```

### Clock Provider

The `Clock` is provided via Hilt in `CoreModule`:

```kotlin
@Provides
@Singleton
fun provideClock(): Clock = Clock.systemDefaultZone()
```

Reference: `core/src/main/kotlin/com/bitwarden/core/di/CoreModule.kt`
