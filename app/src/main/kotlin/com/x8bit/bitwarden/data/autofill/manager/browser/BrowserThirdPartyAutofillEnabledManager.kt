package com.x8bit.bitwarden.data.autofill.manager.browser

import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutofillStatus
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
    val browserThirdPartyAutofillStatusFlow: Flow<BrowserThirdPartyAutofillStatus>

    /**
     * Emits `true` once [browserThirdPartyAutofillStatus] has been set from a device check at
     * least once. Until then, [browserThirdPartyAutofillStatus] holds a placeholder value that
     * does not reflect the device.
     *
     * Callers that prompt the user based on the status (such as the autofill banner) should wait
     * for this to be `true`. Otherwise the placeholder reads as "disabled" and the prompt can
     * briefly appear on launch for users whose browser autofill is already on.
     */
    val isBrowserThirdPartyAutofillStatusCheckedFlow: StateFlow<Boolean>
}
