package com.x8bit.bitwarden.authenticator.data.authenticator.datasource.disk

import com.bitwarden.core.CipherView
import kotlinx.coroutines.flow.Flow

/**
 * Primary access point for disk information related to authenticator data.
 */
interface AuthenticatorDiskSource {

    /**
     * Saves a cipher to the data source.
     */
    suspend fun saveCipher(cipher: CipherView)

    /**
     * Retrieves all ciphers from the data source.
     */
    fun getCiphers(): Flow<List<CipherView>>

    /**
     * Deletes a cipher from the data source for the given [cipherId].
     */
    suspend fun deleteCipher(cipherId: String)

}
