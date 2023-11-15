package com.x8bit.bitwarden.testingtools

import io.mockk.MockKAdditionalAnswerScope
import io.mockk.MockKStubScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

/**
 * A helper alias for [Hangs] to be consistent with the mockk DSL.
 *
 * Example: coEvery { service.suspend() } just hangs()
 */
typealias hangs<T> = Hangs<T>

/**
 * A class that will hang indefinitely when invoked.
 */
class Hangs<T> {
    suspend operator fun invoke(): T = MutableSharedFlow<T>().first()
}

/**
 * A coAnswers placeholder for suspending functions that should never return.
 */
suspend infix fun <T, B> MockKStubScope<T, B>.just(
    hangs: Hangs<T>,
): MockKAdditionalAnswerScope<T, B> = coAnswers { hangs() }
