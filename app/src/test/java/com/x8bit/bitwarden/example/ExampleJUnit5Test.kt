package com.x8bit.bitwarden.example

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Example showing that JUnit5 tests using "jupiter" imports work.
 */
class ExampleJUnit5Test {
    @Nested
    inner class NestedSample {
        @Test
        fun `an empty listOf should be the same as emptyList`() {
            assertEquals(listOf<Any>(), emptyList<Any>())
        }
    }
}
