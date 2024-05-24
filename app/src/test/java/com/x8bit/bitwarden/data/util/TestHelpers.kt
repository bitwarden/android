package com.x8bit.bitwarden.data.util

import com.x8bit.bitwarden.data.platform.datasource.network.di.PlatformNetworkModule
import io.mockk.MockKMatcherScope
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * Helper method for comparing JSON string and ignoring the formatting.
 */
fun assertJsonEquals(
    expected: String,
    actual: String,
    json: Json = PlatformNetworkModule.providesJson(),
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
