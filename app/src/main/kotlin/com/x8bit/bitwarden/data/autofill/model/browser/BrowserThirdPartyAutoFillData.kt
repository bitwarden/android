package com.x8bit.bitwarden.data.autofill.model.browser

/**
 * Relevant data relating to the third party autofill status of a specific browser app.
 */
data class BrowserThirdPartyAutoFillData(
    val isAvailable: Boolean,
    val isThirdPartyEnabled: Boolean,
) {
    val isAvailableButDisabled: Boolean = isAvailable && !isThirdPartyEnabled
}

/**
 * The overall status for all relevant browsers.
 */
data class BrowserThirdPartyAutofillStatus(
    val braveStableStatusData: BrowserThirdPartyAutoFillData,
    val chromeStableStatusData: BrowserThirdPartyAutoFillData,
    val chromeBetaChannelStatusData: BrowserThirdPartyAutoFillData,
) {
    /**
     * Whether any of the available browsers have third party autofill disabled.
     */
    val isAnyIsAvailableAndDisabled: Boolean
        get() = braveStableStatusData.isAvailableButDisabled ||
            chromeStableStatusData.isAvailableButDisabled ||
            chromeBetaChannelStatusData.isAvailableButDisabled
}
