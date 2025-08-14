package com.x8bit.bitwarden.data.autofill.manager.browser

import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutoFillData

/**
 * Manager class used to determine if a device has installed versions of a browser (either the
 * stable release or beta channel) which support and require opt in to third party autofill.
 */
interface BrowserThirdPartyAutofillManager {

    /**
     * The data representing the status of the stable Brave version
     */
    val stableBraveAutofillStatus: BrowserThirdPartyAutoFillData

    /**
     * The data representing the status of the stable Chrome version
     */
    val stableChromeAutofillStatus: BrowserThirdPartyAutoFillData

    /**
     * The data representing the status of the beta Chrome version
     */
    val betaChromeAutofillStatus: BrowserThirdPartyAutoFillData
}
