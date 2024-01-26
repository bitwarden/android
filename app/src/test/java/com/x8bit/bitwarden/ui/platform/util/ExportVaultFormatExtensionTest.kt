package com.x8bit.bitwarden.ui.platform.util

import com.x8bit.bitwarden.ui.platform.feature.settings.exportvault.model.ExportVaultFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExportVaultFormatExtensionTest {
    @Test
    fun `displayLabel should return the correct value for each type`() {
        mapOf(
            ExportVaultFormat.JSON to ".json",
            ExportVaultFormat.CSV to ".csv",
            ExportVaultFormat.JSON_ENCRYPTED to ".json (Encrypted)",
        )
            .forEach { (type, label) ->
                assertEquals(
                    label,
                    type.displayLabel,
                )
            }
    }
}
