package com.bitwarden.cxf.registry

import android.content.Context
import androidx.credentials.providerevents.ProviderEventsManager
import androidx.credentials.providerevents.transfer.ExportEntry
import androidx.credentials.providerevents.transfer.RegisterExportRequest
import com.bitwarden.cxf.registry.model.RegistrationRequest
import java.util.UUID

/**
 * Default implementation of [CredentialExchangeRegistry].
 */
internal class CredentialExchangeRegistryImpl(
    context: Context,
    private val providerEventsManager: ProviderEventsManager =
        ProviderEventsManager.create(context),
) : CredentialExchangeRegistry {

    /**
     * Register as a credential provider that allows exporting credentials.
     *
     * By registering as a credential provider, the application will be presented as an option to
     * users when they initiate the Import process from another credential manager.
     *
     * @param registrationRequest The request to register as a credential provider.
     *
     * @return True if the registration was successful, false otherwise.
     */
    override suspend fun register(
        registrationRequest: RegistrationRequest,
    ): Boolean {
        val request = RegisterExportRequest(
            entries = listOf(
                ExportEntry(
                    id = UUID.randomUUID().toString(),
                    accountDisplayName = null,
                    userDisplayName = registrationRequest.appName,
                    icon = registrationRequest.bitmap,
                    supportedCredentialTypes = registrationRequest.credentialTypes,
                ),
            ),
        )
        return providerEventsManager.registerExport(request = request)
    }

    /**
     * Unregister as a credential export source.
     *
     * By unregistering as a credential provider, the application will no longer be presented as an
     * option to users when they initiate the Import process from another credential manager.
     *
     * @return True if the unregistration was successful, false otherwise.
     */
    override suspend fun unregister(): Boolean =
        providerEventsManager.registerExport(
            // This is a workaround for unregistering an account since an explicit "unregister" API
            // is not currently available.
            request = RegisterExportRequest(
                entries = emptyList(),
            ),
        )
}
