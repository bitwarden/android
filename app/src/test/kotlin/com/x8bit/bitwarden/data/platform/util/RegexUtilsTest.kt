package com.x8bit.bitwarden.data.platform.util

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RegexUtilsTest {

    @Test
    fun `regexOrNull should return nonnull when pattern is valid`() {
        assertNotNull(regexOrNull(pattern = ".*/", option = RegexOption.IGNORE_CASE))
    }

    @Test
    fun `regexOrNull should return null when pattern is invalid`() {
        assertNull(regexOrNull(pattern = ".*\\", option = RegexOption.IGNORE_CASE))
    }
}
