package com.bitwarden.cxf.registry

import android.app.Application
import androidx.credentials.providerevents.ProviderEventsManager
import androidx.credentials.providerevents.transfer.ExportEntry
import androidx.credentials.providerevents.transfer.RegisterExportRequest
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.cxf.registry.model.RegistrationRequest
import java.util.UUID

/**
 * Default implementation of [CredentialExchangeRegistry].
 */
@OmitFromCoverage
internal class CredentialExchangeRegistryImpl(
    application: Application,
) : CredentialExchangeRegistry {
    private val providerEventsManager: ProviderEventsManager =
        ProviderEventsManager.create(application)

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

    override suspend fun unregister(): Boolean =
        providerEventsManager.registerExport(
            // This is a workaround for unregistering an account since an explicit "unregister" API
            // is not currently available.
            request = RegisterExportRequest(
                entries = emptyList(),
            ),
        )
}
