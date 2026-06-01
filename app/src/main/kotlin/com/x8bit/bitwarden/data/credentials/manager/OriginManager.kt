package com.x8bit.bitwarden.data.credentials.manager

import androidx.credentials.provider.CallingAppInfo
import com.x8bit.bitwarden.data.credentials.model.ValidateOriginResult

/**
 * Responsible for managing FIDO2 origin validation.
 */
interface OriginManager {

    /**
     * Validates the origin of a calling app.
     *
     * @param relyingPartyId The ID of the relying party that sent the request.
     * @param callingAppInfo The calling app info.
     *
     * @return The result of the validation.
     */
    suspend fun validateOrigin(
        relyingPartyId: String,
        callingAppInfo: CallingAppInfo,
    ): ValidateOriginResult
}
