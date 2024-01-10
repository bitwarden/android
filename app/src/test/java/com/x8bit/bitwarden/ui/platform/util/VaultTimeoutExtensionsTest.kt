package com.x8bit.bitwarden.ui.platform.util

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.ui.platform.base.util.asText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultTimeoutExtensionsTest {
    @Test
    fun `displayLabel should return the correct value for each type`() {
        mapOf(
            VaultTimeout.Type.IMMEDIATELY to R.string.immediately.asText(),
            VaultTimeout.Type.ONE_MINUTE to R.string.one_minute.asText(),
            VaultTimeout.Type.FIVE_MINUTES to R.string.five_minutes.asText(),
            VaultTimeout.Type.FIFTEEN_MINUTES to R.string.fifteen_minutes.asText(),
            VaultTimeout.Type.THIRTY_MINUTES to R.string.thirty_minutes.asText(),
            VaultTimeout.Type.ONE_HOUR to R.string.one_hour.asText(),
            VaultTimeout.Type.FOUR_HOURS to R.string.four_hours.asText(),
            VaultTimeout.Type.ON_APP_RESTART to R.string.on_restart.asText(),
            VaultTimeout.Type.NEVER to R.string.never.asText(),
            VaultTimeout.Type.CUSTOM to R.string.custom.asText(),
        )
            .forEach { (type, label) ->
                assertEquals(
                    label,
                    type.displayLabel,
                )
            }
    }
}
