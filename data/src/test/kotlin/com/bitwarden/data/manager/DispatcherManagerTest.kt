package com.bitwarden.data.manager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DispatcherManagerTest {

    private val dispatcherManager: DispatcherManager = DispatcherManagerImpl()

    @Test
    fun `io should return Dispatchers IO`() = runTest {
        val expected = Dispatchers.IO

        val actual = dispatcherManager.io

        Assertions.assertEquals(expected, actual)
    }

    @Test
    fun `main should return Dispatchers Main`() = runTest {
        val expected = Dispatchers.Main

        val actual = dispatcherManager.main

        Assertions.assertEquals(expected, actual)
    }

    @Test
    fun `default should return Dispatchers Default`() = runTest {
        val expected = Dispatchers.Default

        val actual = dispatcherManager.default

        Assertions.assertEquals(expected, actual)
    }
}
