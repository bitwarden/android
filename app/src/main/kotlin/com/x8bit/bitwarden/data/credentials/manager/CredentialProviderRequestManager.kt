package com.x8bit.bitwarden.data.credentials.manager

import com.x8bit.bitwarden.CredentialProviderActivity
import com.x8bit.bitwarden.MainActivity
import com.x8bit.bitwarden.MainViewModel
import com.x8bit.bitwarden.data.credentials.manager.model.CredentialProviderRequest

/**
 * Manages pending credential provider requests, relaying them from [CredentialProviderActivity]
 * to [MainActivity] via a pull-based pattern.
 *
 * This approach ensures credential data is never passed through intent extras to
 * exported activities. [CredentialProviderActivity] sets the request, then [MainViewModel]
 * retrieves it once when handling the incoming intent.
 */
interface CredentialProviderRequestManager {
    /**
     * Set a pending credential request.
     */
    fun setPendingCredentialRequest(request: CredentialProviderRequest)

    /**
     * Get and clear the pending credential request. Returns null if no request is pending.
     */
    fun getPendingCredentialRequest(): CredentialProviderRequest?
}
