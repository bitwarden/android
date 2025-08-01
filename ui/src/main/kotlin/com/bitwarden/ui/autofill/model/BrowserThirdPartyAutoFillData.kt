package com.bitwarden.ui.autofill.model

/**
 * Relevant data relating to the third party autofill status of a specific browser app.
 */
data class BrowserThirdPartyAutoFillData(
    val isAvailable: Boolean,
    val isThirdPartyEnabled: Boolean,
)

/**
 * The overall status for all relevant browsers.
 */
data class BrowserThirdPartyAutofillStatus(
    val braveStableStatusData: BrowserThirdPartyAutoFillData,
    val chromeStableStatusData: BrowserThirdPartyAutoFillData,
    val chromeBetaChannelStatusData: BrowserThirdPartyAutoFillData,
)
