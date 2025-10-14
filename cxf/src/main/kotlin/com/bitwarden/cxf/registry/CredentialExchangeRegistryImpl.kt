package com.bitwarden.cxf.registry

import android.app.Application
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.credentials.providerevents.ProviderEventsManager
import androidx.credentials.providerevents.exception.RegisterExportException
import androidx.credentials.providerevents.transfer.ExportEntry
import androidx.credentials.providerevents.transfer.RegisterExportRequest
import androidx.credentials.providerevents.transfer.RegisterExportResponse
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.cxf.registry.model.RegistrationRequest
import timber.log.Timber
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
                    userDisplayName = application.getString(registrationRequest.appNameRes),
                    icon = icon,
                    supportedCredentialTypes = registrationRequest.credentialTypes,
                ),
            ),
        )
        return try {
            providerEventsManager
                .registerExport(request = request)
                .asSuccess()
        } catch (e: RegisterExportException) {
            Timber.e(e, "Failed to register application for export.")
            e.asFailure()
        }
    }

    override suspend fun unregister(): Result<RegisterExportResponse> =
        try {
            providerEventsManager.registerExport(
                // This is a workaround for unregistering an account since an explicit "unregister"
                //  API is not currently available.
                request = RegisterExportRequest(
                    entries = emptyList(),
                ),
            )
                .asSuccess()
        } catch (e: RegisterExportException) {
            Timber.e(e, "Failed to unregister application for export.")
            e.asFailure()
        }
}
