package com.bitwarden.authenticator.data.authenticator.repository

import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.repository.model.AuthenticatorData
import com.bitwarden.authenticator.data.authenticator.repository.model.CreateItemResult
import com.bitwarden.authenticator.data.authenticator.repository.model.DeleteItemResult
import com.bitwarden.authenticator.data.authenticator.repository.model.TotpCodeResult
import com.bitwarden.authenticator.data.authenticator.repository.model.UpdateItemRequest
import com.bitwarden.authenticator.data.authenticator.repository.model.UpdateItemResult
import com.bitwarden.authenticator.data.platform.repository.model.DataState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides and API for managing authenticator data.
 */
interface AuthenticatorRepository {

    /**
     * Flow that represents the current authenticator data.
     *
     * Note that the [StateFlow.value] will return the last known value but the [StateFlow] itself
     * must be collected in order to trigger state changes.
     */
    val authenticatorDataFlow: StateFlow<DataState<AuthenticatorData>>

    /**
     * Flow that represents the TOTP code result.
     */
    val totpCodeFlow: Flow<TotpCodeResult>

    /**
     * Flow that represents all ciphers for the active user.
     *
     * Note that the [StateFlow.value] will return the last known value but the [StateFlow] itself
     * must be collected in order to trigger state changes.
     */
    val ciphersStateFlow: StateFlow<DataState<List<AuthenticatorItemEntity>>>

    /**
     * Flow that represents the data for a specific vault item as found by ID. This may emit `null`
     * if the item cannot be found.
     */
    fun getItemStateFlow(itemId: String): StateFlow<DataState<AuthenticatorItemEntity?>>

    /**
     * Flow that represents the data for a single verification code item.
     * This may emit null if any issues arise during code generation.
     */
    fun getAuthCodeFlow(cipherId: String): StateFlow<DataState<VerificationCodeItem?>>

    /**
     * Flow that represents the data for the TOTP verification codes for ciphers items.
     * This may emit an empty list if any issues arise during code generation.
     */
    fun getAuthCodesFlow(): StateFlow<DataState<List<VerificationCodeItem>>>

    /**
     * Emits the totp code result flow to listeners.
     */
    fun emitTotpCodeResult(totpCodeResult: TotpCodeResult)

    /**
     * Attempt to create a cipher.
     */
    suspend fun createItem(item: AuthenticatorItemEntity): CreateItemResult

    /**
     * Attempt to delete a cipher.
     */
    suspend fun hardDeleteItem(itemId: String): DeleteItemResult

    /**
     * Attempt to update a cipher.
     */
    suspend fun updateItem(
        itemId: String,
        updateItemRequest: UpdateItemRequest,
    ): UpdateItemResult
}
