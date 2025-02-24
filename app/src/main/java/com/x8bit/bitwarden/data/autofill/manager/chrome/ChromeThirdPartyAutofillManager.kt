package com.x8bit.bitwarden.data.autofill.manager.chrome

import com.x8bit.bitwarden.data.autofill.model.chrome.ChromeThirdPartyAutoFillData

/**
 * Manager class used to determine if a device has installed versions of Chrome (either the
 * stable release or beta channel) which support and require opt in to third party autofill.
 */
interface ChromeThirdPartyAutofillManager {

    /**
     * The data representing the status of the stable chrome version
     */
    val stableChromeAutofillStatus: ChromeThirdPartyAutoFillData

    /**
     * The data representing the status of the beta chrome version
     */
    val betaChromeAutofillStatus: ChromeThirdPartyAutoFillData
}
