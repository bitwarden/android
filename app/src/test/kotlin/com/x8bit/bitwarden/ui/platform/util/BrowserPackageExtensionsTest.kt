package com.x8bit.bitwarden.ui.platform.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BrowserPackageExtensionsTest {
    @Test
    fun `displayLabel should return the correct value for each type`() {
        mapOf(
            BrowserPackage.BRAVE_RELEASE to BitwardenString.brave.asText(),
            BrowserPackage.CHROME_STABLE to BitwardenString.chrome.asText(),
            BrowserPackage.CHROME_BETA to BitwardenString.chrome_beta.asText(),
            BrowserPackage.VIVALDI_STABLE to BitwardenString.vivaldi.asText(),
        )
            .forEach { (type, label) ->
                assertEquals(
                    label,
                    type.displayLabel,
                )
            }
    }
}
