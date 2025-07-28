package com.x8bit.bitwarden.ui.platform.util

import com.bitwarden.data.repository.model.Environment
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
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
            BitwardenString.self_hosted.asText(),
            Environment.Type.SELF_HOSTED.displayLabel,
        )
    }
}
