package com.x8bit.bitwarden.data.autofill.fido2.manager

import androidx.credentials.provider.CallingAppInfo
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2ValidateOriginResult

/**
 * Responsible for managing FIDO2 origin validation.
 */
interface Fido2OriginManager {

    /**
     * Validates the origin of a calling app.
     *
     * @param callingAppInfo The calling app info.
     *
     * @return The result of the validation.
     */
    suspend fun validateOrigin(
        callingAppInfo: CallingAppInfo,
    ): Fido2ValidateOriginResult
}
