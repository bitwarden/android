package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.manager.model.CookieAcquisitionRequest
import kotlinx.coroutines.flow.StateFlow

/**
 * Manager for server communication configuration state.
 */
interface CookieAcquisitionRequestManager {

    /**
     * StateFlow of pending cookie acquisition.
     *
     * Emits non-null when cookie acquisition is needed, null otherwise.
     */
    val cookieAcquisitionRequestFlow: StateFlow<CookieAcquisitionRequest?>

    /**
     * Sets the pending cookie acquisition state.
     *
     * @param data The pending cookie acquisition data, or null to clear.
     */
    fun setPendingCookieAcquisition(data: CookieAcquisitionRequest?)
}
