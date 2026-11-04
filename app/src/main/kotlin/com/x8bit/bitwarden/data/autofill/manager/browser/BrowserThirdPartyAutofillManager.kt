package com.x8bit.bitwarden.data.autofill.manager.browser

import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutoFillData

/**
 * Manager class used to determine if a device has installed versions of a browser (either the
 * stable release or beta channel) which support and require opt in to third party autofill.
 */
interface BrowserThirdPartyAutofillManager {

    /**
     * Retrieves the data representing the status of the stable Brave version.
     */
    suspend fun getStableBraveAutofillStatus(): BrowserThirdPartyAutoFillData

    /**
     * Retrieves the data representing the status of the stable Chrome version.
     */
    suspend fun getStableChromeAutofillStatus(): BrowserThirdPartyAutoFillData

    /**
     * Retrieves the data representing the status of the beta Chrome version.
     */
    suspend fun getBetaChromeAutofillStatus(): BrowserThirdPartyAutoFillData

    /**
     * Retrieves the data representing the status of the Vivaldi version.
     */
    suspend fun getStableVivaldiAutofillStatus(): BrowserThirdPartyAutoFillData

    /**
     * Retrieves the package name of the device's default browser, or null if it cannot be
     * determined.
     */
    suspend fun getDefaultBrowserPackageName(): String?
}
