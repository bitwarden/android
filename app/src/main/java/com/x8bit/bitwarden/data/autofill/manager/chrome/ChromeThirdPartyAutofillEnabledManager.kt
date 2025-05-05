package com.x8bit.bitwarden.data.autofill.manager.chrome

import com.x8bit.bitwarden.data.autofill.model.chrome.ChromeThirdPartyAutofillStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manager which provides whether specific Chrome versions have third party autofill available and
 * enabled.
 */
interface ChromeThirdPartyAutofillEnabledManager {
    /**
     * Combined status for all concerned Chrome versions.
     */
    var chromeThirdPartyAutofillStatus: ChromeThirdPartyAutofillStatus

    /**
     * An observable [StateFlow] of the combined third party autofill status of all concerned
     * chrome versions.
     */
    val chromeThirdPartyAutofillStatusFlow: Flow<ChromeThirdPartyAutofillStatus>
}
