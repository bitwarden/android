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
     * @param relyingPartyId The relying party ID.
     *
     * @return The result of the validation.
     */
    suspend fun validateOrigin(
        callingAppInfo: CallingAppInfo,
        relyingPartyId: String,
    ): Fido2ValidateOriginResult

    /**
     * Returns the privileged app origin, or null if the calling app is not allowed.
     *
     * @param callingAppInfo The calling app info.
     *
     * @return The privileged app origin, or null.
     */
    suspend fun getPrivilegedAppOriginOrNull(callingAppInfo: CallingAppInfo): String?
}
