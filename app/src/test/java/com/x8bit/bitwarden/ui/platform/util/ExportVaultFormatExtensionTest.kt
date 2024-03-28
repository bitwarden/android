package com.x8bit.bitwarden.ui.platform.util

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.feature.settings.exportvault.model.ExportVaultFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExportVaultFormatExtensionTest {
    @Test
    fun `displayLabel should return the correct value for each type`() {
        mapOf(
            ExportVaultFormat.JSON to R.string.json_extension.asText(),
            ExportVaultFormat.CSV to R.string.csv_extension.asText(),
            ExportVaultFormat.JSON_ENCRYPTED to R.string.json_extension_formatted.asText(
                R.string.password_protected.asText(),
            ),
        )
            .forEach { (type, label) ->
                assertEquals(
                    label,
                    type.displayLabel,
                )
            }
    }

    @Test
    fun `fileExtension should return the correct value for each type`() {
        mapOf(
            ExportVaultFormat.JSON to "json",
            ExportVaultFormat.CSV to "csv",
            ExportVaultFormat.JSON_ENCRYPTED to "json",
        )
            .forEach { (type, label) ->
                assertEquals(
                    label,
                    type.fileExtension,
                )
            }
    }
}
