package com.bitwarden.cxf.registry

import android.app.Application
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.credentials.providerevents.ProviderEventsManager
import androidx.credentials.providerevents.transfer.ExportEntry
import androidx.credentials.providerevents.transfer.RegisterExportRequest
import androidx.credentials.providerevents.transfer.RegisterExportResponse
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.cxf.registry.model.RegistrationRequest
import java.util.UUID

/**
 * Default implementation of [CredentialExchangeRegistry].
 */
@OmitFromCoverage
internal class CredentialExchangeRegistryImpl(
    private val application: Application,
) : CredentialExchangeRegistry {
    private val providerEventsManager: ProviderEventsManager =
        ProviderEventsManager.create(application)

    override suspend fun register(
        registrationRequest: RegistrationRequest,
    ): Result<RegisterExportResponse> {
        val icon = ContextCompat
            .getDrawable(
                application,
                registrationRequest.iconResId,
            )
            ?.toBitmapOrNull()
            ?: return IllegalArgumentException("Icon drawable must not be null.")
                .asFailure()

        val request = RegisterExportRequest(
            entries = listOf(
                ExportEntry(
                    id = UUID.randomUUID().toString(),
                    accountDisplayName = null,
                    userDisplayName = registrationRequest.appName,
                    icon = icon,
                    supportedCredentialTypes = registrationRequest.credentialTypes,
                ),
            ),
        )
        return providerEventsManager
            .registerExport(request = request)
            .asSuccess()
    }

    override suspend fun unregister(): RegisterExportResponse =
        providerEventsManager.registerExport(
            // This is a workaround for unregistering an account since an explicit "unregister" API
            // is not currently available.
            request = RegisterExportRequest(
                entries = emptyList(),
            ),
        )
}
