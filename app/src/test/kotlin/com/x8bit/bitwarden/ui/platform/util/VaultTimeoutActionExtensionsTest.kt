package com.x8bit.bitwarden.ui.platform.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultTimeoutActionExtensionsTest {
    @Test
    fun `displayLabel should return the correct value for each type`() {
        mapOf(
            VaultTimeoutAction.LOCK to BitwardenString.lock.asText(),
            VaultTimeoutAction.LOGOUT to BitwardenString.log_out.asText(),
        )
            .forEach { (type, label) ->
                assertEquals(
                    label,
                    type.displayLabel,
                )
            }
    }
}
