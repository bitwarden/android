package com.x8bit.bitwarden.ui.platform.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultTimeoutExtensionsTest {
    @Test
    fun `displayLabel should return the correct value for each type`() {
        mapOf(
            VaultTimeout.Type.IMMEDIATELY to BitwardenString.immediately.asText(),
            VaultTimeout.Type.ONE_MINUTE to BitwardenString.one_minute.asText(),
            VaultTimeout.Type.FIVE_MINUTES to BitwardenString.five_minutes.asText(),
            VaultTimeout.Type.FIFTEEN_MINUTES to BitwardenString.fifteen_minutes.asText(),
            VaultTimeout.Type.THIRTY_MINUTES to BitwardenString.thirty_minutes.asText(),
            VaultTimeout.Type.ONE_HOUR to BitwardenString.one_hour.asText(),
            VaultTimeout.Type.FOUR_HOURS to BitwardenString.four_hours.asText(),
            VaultTimeout.Type.ON_APP_RESTART to BitwardenString.on_restart.asText(),
            VaultTimeout.Type.NEVER to BitwardenString.never.asText(),
            VaultTimeout.Type.CUSTOM to BitwardenString.custom.asText(),
        )
            .forEach { (type, label) ->
                assertEquals(
                    label,
                    type.displayLabel,
                )
            }
    }

    @Test
    fun `minutes should return the correct value for each type`() {
        mapOf(
            VaultTimeout.Type.IMMEDIATELY to 0,
            VaultTimeout.Type.ONE_MINUTE to 1,
            VaultTimeout.Type.FIVE_MINUTES to 5,
            VaultTimeout.Type.FIFTEEN_MINUTES to 15,
            VaultTimeout.Type.THIRTY_MINUTES to 30,
            VaultTimeout.Type.ONE_HOUR to 60,
            VaultTimeout.Type.FOUR_HOURS to 240,
            VaultTimeout.Type.ON_APP_RESTART to Int.MAX_VALUE,
            VaultTimeout.Type.NEVER to Int.MAX_VALUE,
            VaultTimeout.Type.CUSTOM to 0,
        )
            .forEach { (type, value) ->
                assertEquals(
                    value,
                    type.minutes,
                )
            }
    }
}
