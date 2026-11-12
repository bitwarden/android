package com.x8bit.bitwarden.data.autofill.model.browser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BrowserThirdPartyAutofillStatusTest {

    @Test
    fun `defaultBrowserPackage should return the matching BrowserPackage for each package`() {
        BrowserPackage.entries.forEach { browserPackage ->
            val status = createStatus(defaultBrowserPackageName = browserPackage.packageName)
            assertEquals(browserPackage, status.defaultBrowserPackage)
        }
    }

    @Test
    fun `defaultBrowserPackage should return null for an unsupported browser`() {
        val status = createStatus(defaultBrowserPackageName = "org.mozilla.firefox")
        assertNull(status.defaultBrowserPackage)
    }

    @Test
    fun `defaultBrowserPackage should return null when the default browser is unknown`() {
        val status = createStatus(defaultBrowserPackageName = null)
        assertNull(status.defaultBrowserPackage)
    }

    @Test
    fun `isDefaultBrowserAvailableAndDisabled should be true when default browser is off`() {
        val status = createStatus(
            defaultBrowserPackageName = BrowserPackage.BRAVE_RELEASE.packageName,
            brave = AVAILABLE_DISABLED,
        )
        assertTrue(status.isDefaultBrowserAvailableAndDisabled)
    }

    @Test
    fun `isDefaultBrowserAvailableAndDisabled should be false when default browser is on`() {
        val status = createStatus(
            defaultBrowserPackageName = BrowserPackage.CHROME_STABLE.packageName,
            chromeStable = AVAILABLE_ENABLED,
            brave = AVAILABLE_DISABLED,
        )
        assertFalse(status.isDefaultBrowserAvailableAndDisabled)
    }

    @Test
    fun `isDefaultBrowserAvailableAndDisabled should be false when default does not report`() {
        val status = createStatus(
            defaultBrowserPackageName = BrowserPackage.VIVALDI_STABLE.packageName,
            vivaldi = UNAVAILABLE,
            chromeStable = AVAILABLE_DISABLED,
        )
        assertFalse(status.isDefaultBrowserAvailableAndDisabled)
    }

    @Test
    fun `isDefaultBrowserAvailableAndDisabled should be false for an unsupported default`() {
        val status = createStatus(
            defaultBrowserPackageName = "org.mozilla.firefox",
            chromeStable = AVAILABLE_DISABLED,
        )
        assertFalse(status.isDefaultBrowserAvailableAndDisabled)
    }

    private fun createStatus(
        defaultBrowserPackageName: String?,
        brave: BrowserThirdPartyAutoFillData = UNAVAILABLE,
        chromeStable: BrowserThirdPartyAutoFillData = UNAVAILABLE,
        chromeBeta: BrowserThirdPartyAutoFillData = UNAVAILABLE,
        vivaldi: BrowserThirdPartyAutoFillData = UNAVAILABLE,
    ): BrowserThirdPartyAutofillStatus = BrowserThirdPartyAutofillStatus(
        braveStableStatusData = brave,
        chromeStableStatusData = chromeStable,
        chromeBetaChannelStatusData = chromeBeta,
        vivaldiStableChannelStatusData = vivaldi,
        defaultBrowserPackageName = defaultBrowserPackageName,
    )
}

private val UNAVAILABLE = BrowserThirdPartyAutoFillData(
    isAvailable = false,
    isThirdPartyEnabled = false,
)
private val AVAILABLE_DISABLED = BrowserThirdPartyAutoFillData(
    isAvailable = true,
    isThirdPartyEnabled = false,
)
private val AVAILABLE_ENABLED = BrowserThirdPartyAutoFillData(
    isAvailable = true,
    isThirdPartyEnabled = true,
)
