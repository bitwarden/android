package com.x8bit.bitwarden.ui.platform.base.util

import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull

class ListExtensionsTest {

    @Test
    fun `nullIfAllEqual should return null for lists with identical values`() {
        val initialList = listOf("-", "-", "-", "-", "-")

        val result = initialList.nullIfAllEqual("-")

        assertNull(result)
    }

    @Test
    fun `nullIfAllEqual should return the initial list for lists with non-identical values`() {
        val initialList = listOf("-", "-", "-", "-", "1")

        val result = initialList.nullIfAllEqual("-")

        assertEquals(
            initialList,
            result,
        )
    }
}
