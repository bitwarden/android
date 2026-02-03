package com.x8bit.bitwarden.data.credentials.manager

import com.x8bit.bitwarden.data.credentials.manager.model.CredentialProviderRequest
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Singleton

/**
 * Primary implementation of [CredentialProviderRequestManager].
 *
 * Uses an [AtomicReference] for thread-safe get-and-clear semantics, ensuring
 * the pending request is only processed once.
 */
@Singleton
class CredentialProviderRequestManagerImpl : CredentialProviderRequestManager {

    private val pendingRequest = AtomicReference<CredentialProviderRequest?>(null)

    override fun setPendingCredentialRequest(request: CredentialProviderRequest) {
        pendingRequest.set(request)
    }

    override fun getPendingCredentialRequest(): CredentialProviderRequest? {
        return pendingRequest.getAndSet(null)
    }
}
