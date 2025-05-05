package com.bitwarden.authenticator.ui.platform.base

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * JUnit 5 Extension for automatically setting a [testDispatcher] as the "main" dispatcher.
 *
 * Note that this may be used as a normal class property with [RegisterExtension] or may be applied
 * directly to a test class using [ExtendWith].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherExtension(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : AfterAllCallback,
    AfterEachCallback,
    BeforeAllCallback,
    BeforeEachCallback {
    override fun afterAll(context: ExtensionContext?) {
        Dispatchers.resetMain()
    }

    override fun afterEach(context: ExtensionContext?) {
        Dispatchers.resetMain()
    }

    override fun beforeAll(context: ExtensionContext?) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun beforeEach(context: ExtensionContext?) {
        Dispatchers.setMain(testDispatcher)
    }
}
