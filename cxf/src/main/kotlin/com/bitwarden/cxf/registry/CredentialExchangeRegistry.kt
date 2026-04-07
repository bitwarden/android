package com.bitwarden.cxf.registry

import androidx.credentials.providerevents.transfer.RegisterExportResponse
import com.bitwarden.cxf.registry.model.RegistrationRequest

/**
 * Registry for credential providers that allow exporting credentials.
 */
interface CredentialExchangeRegistry {
    /**
     * Register as a credential provider that allows exporting credentials.
     *
     * By registering as a credential provider, the application will be presented as an option to
     * users when they initiate the Import process from another credential manager.
     *
     * @param registrationRequest The request to register as a credential provider.
     *
     * @return A [Result] indicating if the application was add to the registry. [Result.isSuccess]
     * does not indicate if the application was added to the registry. Use the result value to check
     * if the application was added or not. [Result.isFailure] only indicates if an error occurred.
     */
    suspend fun register(registrationRequest: RegistrationRequest): Result<RegisterExportResponse>

    /**
     * Unregister as a credential export source.
     *
     * By unregistering as a credential provider, the application will no longer be presented as an
     * option to users when they initiate the Import process from another credential manager.
     *
     * @return True if the unregistration was successful, false otherwise.
     */
    suspend fun unregister(): Result<RegisterExportResponse>
}
