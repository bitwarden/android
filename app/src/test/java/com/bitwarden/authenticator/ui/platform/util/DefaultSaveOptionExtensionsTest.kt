package com.bitwarden.authenticator.ui.platform.util

import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.base.util.asText
import com.bitwarden.authenticator.ui.platform.feature.settings.data.model.DefaultSaveOption
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DefaultSaveOptionExtensionsTest {

    @Test
    fun `displayLabel should map to correct labels`() {
        DefaultSaveOption.entries.forEach {
            val expected = when (it) {
                DefaultSaveOption.BITWARDEN_APP -> R.string.save_to_bitwarden.asText()
                DefaultSaveOption.LOCAL -> R.string.save_here.asText()
                DefaultSaveOption.NONE -> R.string.none.asText()
            }
            assertEquals(
                expected,
                it.displayLabel,
            )
        }
    }
}
