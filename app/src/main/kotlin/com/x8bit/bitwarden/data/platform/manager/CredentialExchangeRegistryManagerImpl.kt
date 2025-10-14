package com.x8bit.bitwarden.data.platform.manager

import androidx.credentials.providerevents.transfer.CredentialTypes
import com.bitwarden.cxf.registry.CredentialExchangeRegistry
import com.bitwarden.cxf.registry.model.RegistrationRequest
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.model.RegisterExportResult
import com.x8bit.bitwarden.data.platform.manager.model.UnregisterExportResult
import timber.log.Timber

/**
 * Default implementation of [CredentialExchangeRegistryManager].
 */
class CredentialExchangeRegistryManagerImpl(
    private val credentialExchangeRegistry: CredentialExchangeRegistry,
    private val settingsDiskSource: SettingsDiskSource,
) : CredentialExchangeRegistryManager {
    override suspend fun register(): RegisterExportResult = credentialExchangeRegistry
        .register(
            registrationRequest = RegistrationRequest(
                appNameRes = R.string.app_name,
                credentialTypes = setOf(
                    CredentialTypes.CREDENTIAL_TYPE_BASIC_AUTH,
                    CredentialTypes.CREDENTIAL_TYPE_PUBLIC_KEY,
                    CredentialTypes.CREDENTIAL_TYPE_TOTP,
                    CredentialTypes.CREDENTIAL_TYPE_CREDIT_CARD,
                    CredentialTypes.CREDENTIAL_TYPE_SSH_KEY,
                    CredentialTypes.CREDENTIAL_TYPE_ADDRESS,
                ),
                iconResId = BitwardenDrawable.icon,
            ),
        )
        .fold(
            onSuccess = {
                Timber.d("Successfully registered for CXP export")
                settingsDiskSource.storeAppRegisteredForExport(isRegistered = true)
                RegisterExportResult.Success
            },
            onFailure = {
                Timber.e(it, "Failed to register for CXP export")
                RegisterExportResult.Failure(it)
            },
        )

    override suspend fun unregister(): UnregisterExportResult = credentialExchangeRegistry
        .unregister()
        .fold(
            onSuccess = {
                Timber.d("Successfully unregistered for CXP export")
                settingsDiskSource.storeAppRegisteredForExport(isRegistered = false)
                UnregisterExportResult.Success
            },
            onFailure = {
                Timber.e(it, "Failed to unregister for CXP export")
                UnregisterExportResult.Failure(it)
            },
        )
}
