package com.x8bit.bitwarden.data.autofill.parser.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class LogUtilTests {
    @Test
    fun `autofill logs must be disabled`() {
        assertFalse(ENABLE_ASSIST_STRUCTURE_LOGS)
    }
}
