package com.bitwarden.core.data.util

import com.bitwarden.core.di.CoreModule
import io.mockk.MockKMatcherScope
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Helper method for comparing JSON string and ignoring the formatting.
 */
fun assertJsonEquals(
    expected: String,
    actual: String,
    json: Json = CoreModule.providesJson(),
) {
    assertEquals(
        json.parseToJsonElement(expected),
        json.parseToJsonElement(actual),
    )
}

/**
 * Helper method for mocking pipeline operations within the builder pattern. This saves a lot of
 * boiler plate. In order to use this, the builder's constructor must be mockked.
 *
 * Example:
 * ```
 *     // Setup
 *     mockkConstructor(FillResponse.Builder::class)
 *     mockBuilder<FillResponse.Builder> { it.setIgnoredIds() }
 *     every { anyConstructed<FillResponse.Builder>().build() } returns mockk()
 *
 *     // Test
 *     ...
 *
 *     // Verify
 *     verify(exactly = 1) {
 *         anyConstructed<FillResponse.Builder>().setIgnoredIds()
 *         anyConstructed<FillResponse.Builder>().build()
 *     }
 *     unmockkConstructor(FillResponse.Builder::class)
 * ```
 */
inline fun <reified T : Any> mockBuilder(crossinline block: MockKMatcherScope.(T) -> T) {
    every { block(anyConstructed<T>()) } answers {
        this.self as T
    }
}

/**
 * A helper method that calls both [TestCoroutineScheduler.advanceTimeBy] and
 * [TestCoroutineScheduler.runCurrent].
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestDispatcher.advanceTimeByAndRunCurrent(delayTimeMillis: Long) {
    scheduler.advanceTimeBy(delayTimeMillis = delayTimeMillis)
    scheduler.runCurrent()
}

/**
 * This is for testing exceptions that are thrown in a different coroutine.
 *
 * You cannot wrap this in a `runTest`. The `runTest` invocation catches all uncaught exceptions
 * and rethrows them. The [assertCoroutineThrows]'s tests will pass, but the outer `runTest`
 * will throw an exception and cause the test to fail.
 *
 * Never do this:
 * ```
 * @Test
 * fun test() = runTest {
 *     assertCoroutineThrows(Exception::class.java) {
 *         throw Exception("Something is wrong.")
 *     }
 * }
 * ```
 *
 * Always do this:
 * @Test
 * fun test() {
 *     assertCoroutineThrows(Exception::class.java) {
 *         throw Exception("Something is wrong.")
 *     }
 * }
 * ```
 *
 * Check this issue for more info: https://github.com/Kotlin/kotlinx.coroutines/issues/3889
 */
inline fun <reified T : Throwable> assertCoroutineThrows(
    context: CoroutineContext = EmptyCoroutineContext,
    noinline block: suspend TestScope.() -> Unit,
): T = assertThrows<T> {
    runTest(context = context, testBody = block)
}
