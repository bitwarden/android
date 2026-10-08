package com.x8bit.bitwarden.data.autofill.accessibility.parser.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class LogUtilsTest {
    @Test
    fun `accessibility autofill logs must be disabled`() {
        assertFalse(ENABLE_ACCESSIBILITY_LOGS)
    }
}
