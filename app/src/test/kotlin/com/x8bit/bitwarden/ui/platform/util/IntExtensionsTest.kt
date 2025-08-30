package com.x8bit.bitwarden.ui.platform.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IntExtensionsTest {
    @Test
    fun `orZero returns zero when null`() {
        val nullInt: Int? = null
        assertEquals(0, nullInt.orZero())
    }

    @Test
    fun `orZero returns value when not null`() {
        val nonNullInt = 42
        assertEquals(42, nonNullInt.orZero())
        val negativeNonNullInt = -42
        assertEquals(-42, negativeNonNullInt.orZero())
        assertEquals(0, 0.orZero())
    }
}
