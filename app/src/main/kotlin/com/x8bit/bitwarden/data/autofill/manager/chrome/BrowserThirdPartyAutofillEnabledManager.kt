package com.x8bit.bitwarden.data.autofill.manager.chrome

import com.x8bit.bitwarden.data.autofill.model.chrome.BrowserThirdPartyAutofillStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manager which provides whether specific browser versions have third party autofill available and
 * enabled.
 */
interface BrowserThirdPartyAutofillEnabledManager {
    /**
     * Combined status for all concerned browser versions.
     */
    var browserThirdPartyAutofillStatus: BrowserThirdPartyAutofillStatus

    /**
     * An observable [StateFlow] of the combined third party autofill status of all concerned
     * browser versions.
     */
    val chromeThirdPartyAutofillStatusFlow: Flow<BrowserThirdPartyAutofillStatus>
}
