package com.x8bit.bitwarden.data.platform.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResultTest {

    @Test
    fun `flatMap where receiver is success and argument is success should be argument success`() {
        val intResult = Result.success(1)
        val stringResult = intResult.flatMap {
            Result.success(it.toString())
        }
        assertTrue(stringResult.isSuccess)
        assertEquals("1", stringResult.getOrNull())
    }

    @Test
    fun `flatMap where receiver is success and argument is failure should be argument failure`() {
        val expectedException = IllegalStateException("Exception in transform")
        val intResult = Result.success(1)
        val stringResult = intResult.flatMap {
            Result.failure<String>(expectedException)
        }
        assertTrue(stringResult.isFailure)
        assertEquals(expectedException, stringResult.exceptionOrNull())
    }

    @Test
    @Suppress("TooGenericExceptionThrown")
    fun `flatMap where receiver is failure and argument is failure should be receiver failure`() {
        val expectedException = RuntimeException("Exception on int result.")
        val intResult = Result.failure<Int>(expectedException)
        val stringResult = intResult.flatMap<Int, String> {
            throw RuntimeException("transform function should not be called for failed Results")
        }
        assertTrue(stringResult.isFailure)
        assertEquals(expectedException, stringResult.exceptionOrNull())
    }

    @Test
    fun `flatMap where receiver is failure and argument is success should be receiver failure`() {
        val expectedException = RuntimeException("Exception on int result.")
        val intResult = Result.failure<Int>(expectedException)
        val stringResult = intResult.flatMap {
            Result.success("1")
        }
        assertTrue(stringResult.isFailure)
        assertEquals(expectedException, stringResult.exceptionOrNull())
    }

    @Test
    fun `asSuccess returns a success Result with the correct content`() {
        assertEquals(
            Result.success("Test"),
            "Test".asSuccess(),
        )
    }

    @Test
    fun `asFailure returns a failure Result with the correct content`() {
        val throwable = IllegalStateException("Test")
        assertEquals(
            Result.failure<Nothing>(throwable),
            throwable.asFailure(),
        )
    }
}
