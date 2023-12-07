package com.x8bit.bitwarden.ui.platform.util

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.platform.base.util.asText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EnvironmentExtensionsTest {
    @Test
    fun `displayLabel for US type should return the correct value`() {
        assertEquals(
            "bitwarden.com".asText(),
            Environment.Type.US.displayLabel,
        )
    }

    @Test
    fun `displayLabel for EU type should return the correct value`() {
        assertEquals(
            "bitwarden.eu".asText(),
            Environment.Type.EU.displayLabel,
        )
    }

    @Test
    fun `displayLabel for SELF_HOSTED type should return the correct value`() {
        assertEquals(
            R.string.self_hosted.asText(),
            Environment.Type.SELF_HOSTED.displayLabel,
        )
    }
}
