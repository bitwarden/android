package com.x8bit.bitwarden.ui.platform.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BrowserPackageExtensionsTest {
    @Test
    fun `displayLabelRes should return the correct value for each type`() {
        mapOf(
            BrowserPackage.BRAVE_RELEASE to BitwardenString.brave,
            BrowserPackage.CHROME_STABLE to BitwardenString.chrome,
            BrowserPackage.CHROME_BETA to BitwardenString.chrome_beta,
            BrowserPackage.VIVALDI_STABLE to BitwardenString.vivaldi,
        )
            .forEach { (type, labelRes) ->
                assertEquals(
                    labelRes,
                    type.displayLabelRes,
                )
            }
    }
}
