package com.bitwarden.authenticator.ui.platform.util

import com.bitwarden.authenticator.ui.platform.feature.settings.data.model.DefaultSaveOption
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DefaultSaveOptionExtensionsTest {

    @Test
    fun `displayLabel should map to correct labels`() {
        DefaultSaveOption.entries.forEach {
            val expected = when (it) {
                DefaultSaveOption.BITWARDEN_APP -> BitwardenString.save_to_bitwarden.asText()
                DefaultSaveOption.LOCAL -> BitwardenString.save_here.asText()
                DefaultSaveOption.NONE -> BitwardenString.none.asText()
            }
            assertEquals(
                expected,
                it.displayLabel,
            )
        }
    }
}
