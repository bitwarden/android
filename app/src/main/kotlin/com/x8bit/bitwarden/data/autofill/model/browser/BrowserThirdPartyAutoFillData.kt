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
     * The total number of available browsers.
     */
    val availableCount: Int
        get() = (if (braveStableStatusData.isAvailable) 1 else 0) +
            (if (chromeStableStatusData.isAvailable) 1 else 0) +
            (if (chromeBetaChannelStatusData.isAvailable) 1 else 0)

    /**
     * Whether any of the available browsers have third party autofill disabled.
     */
    val isAnyIsAvailableAndDisabled: Boolean
        get() = braveStableStatusData.isAvailableButDisabled ||
            chromeStableStatusData.isAvailableButDisabled ||
            chromeBetaChannelStatusData.isAvailableButDisabled
}
