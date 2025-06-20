package com.x8bit.bitwarden.data.autofill.model.chrome

/**
 * Relevant data relating to the third party autofill status of a specific browser app.
 */
data class BrowserThirdPartyAutoFillData(
    val isAvailable: Boolean,
    val isThirdPartyEnabled: Boolean,
)

/**
 * The overall status for all relevant channels of a browser.
 */
data class BrowserThirdPartyAutofillStatus(
    val chromeStableStatusData: BrowserThirdPartyAutoFillData,
    val chromeBetaChannelStatusData: BrowserThirdPartyAutoFillData,
)
