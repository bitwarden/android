package com.x8bit.bitwarden.data.auth.datasource.network.util

import com.x8bit.bitwarden.data.auth.datasource.network.model.TwoFactorAuthMethod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TwoFactorAuthMethodExtensionTest {
    @Test
    fun `priority returns the expected value`() {
        mapOf(
            TwoFactorAuthMethod.AUTHENTICATOR_APP to 1,
            TwoFactorAuthMethod.EMAIL to 0,
            TwoFactorAuthMethod.YUBI_KEY to 3,
            TwoFactorAuthMethod.U2F to -1,
            TwoFactorAuthMethod.REMEMBER to -1,
            TwoFactorAuthMethod.DUO_ORGANIZATION to 20,
            TwoFactorAuthMethod.WEB_AUTH to 4,
            TwoFactorAuthMethod.RECOVERY_CODE to -1,
        )
            .forEach { (type, priority) ->
                assertEquals(
                    priority,
                    type.priority,
                )
            }
    }
}
