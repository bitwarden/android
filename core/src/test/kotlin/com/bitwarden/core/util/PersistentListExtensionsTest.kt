package com.bitwarden.core.util

import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PersistentListExtensionsTest {

    @Test
    fun `PersistentListOfNotNull should filter out null values`() {
        val expected = persistentListOf("Hello", "World")

        val result = persistentListOfNotNull(
            "Hello",
            null,
            "World",
            null,
            null,
        )

        assertEquals(expected, result)
    }
}
