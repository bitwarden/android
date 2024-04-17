package com.bitwarden.authenticator.data.authenticator.datasource.disk

import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Primary access point for disk information related to authenticator data.
 */
interface AuthenticatorDiskSource {

    /**
     * Saves an authenticator item to the data source.
     */
    suspend fun saveItem(authenticatorItem: AuthenticatorItemEntity)

    /**
     * Retrieves all authenticator items from the data source.
     */
    fun getItems(): Flow<List<AuthenticatorItemEntity>>

    /**
     * Deletes an authenticator item from the data source with the given [itemId].
     */
    suspend fun deleteItem(itemId: String)

}
