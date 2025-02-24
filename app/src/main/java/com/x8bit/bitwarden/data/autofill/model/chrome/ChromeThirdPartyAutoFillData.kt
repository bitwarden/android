package com.x8bit.bitwarden.data.autofill.model.chrome

/**
 * Relevant data relating to the third party autofill status of a version of the Chrome browser app.
 */
data class ChromeThirdPartyAutoFillData(
    val isAvailable: Boolean,
    val isThirdPartyEnabled: Boolean,
)

/**
 * The overall status for all relevant release channels of Chrome.
 */
data class ChromeThirdPartyAutofillStatus(
    val stableStatusData: ChromeThirdPartyAutoFillData,
    val betaChannelStatusData: ChromeThirdPartyAutoFillData,
)
