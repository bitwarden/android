package com.bitwarden.authenticator.data.authenticator.repository

import android.net.Uri
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.repository.model.CreateItemResult
import com.bitwarden.authenticator.data.authenticator.repository.model.DeleteItemResult
import com.bitwarden.authenticator.data.authenticator.repository.model.ExportDataResult
import com.bitwarden.authenticator.data.authenticator.repository.model.SharedVerificationCodesState
import com.bitwarden.authenticator.data.authenticator.repository.model.TotpCodeResult
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportDataResult
import com.bitwarden.authenticator.data.platform.manager.imports.model.ImportFileFormat
import com.bitwarden.authenticator.data.platform.repository.model.DataState
import com.bitwarden.authenticator.ui.platform.feature.settings.export.model.ExportVaultFormat
import com.bitwarden.authenticator.ui.platform.manager.intent.IntentManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides and API for managing authenticator data.
 */
interface AuthenticatorRepository {

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
     * State flow that represents the state of verification codes and accounts shared from the
     * main Bitwarden app.
     */
    val sharedCodesStateFlow: StateFlow<SharedVerificationCodesState>

    /**
     * Flow that represents the data for the TOTP verification codes for ciphers items.
     * This may emit an empty list if any issues arise during code generation.
     */
    fun getLocalVerificationCodesFlow(): StateFlow<DataState<List<VerificationCodeItem>>>

    /**
     * Emits the totp code result flow to listeners.
     */
    fun emitTotpCodeResult(totpCodeResult: TotpCodeResult)

    /**
     * Attempt to create a cipher.
     */
    suspend fun createItem(item: AuthenticatorItemEntity): CreateItemResult

    /**
     * Attempt to add provided [items].
     */
    suspend fun addItems(vararg items: AuthenticatorItemEntity): CreateItemResult

    /**
     * Attempt to delete a cipher.
     */
    suspend fun hardDeleteItem(itemId: String): DeleteItemResult

    /**
     *  Attempt to get the user's data for export.
     */
    suspend fun exportVaultData(format: ExportVaultFormat, fileUri: Uri): ExportDataResult

    /**
     * Attempt to read the user's data from a file
     */
    suspend fun importVaultData(
        format: ImportFileFormat,
        fileData: IntentManager.FileData,
    ): ImportDataResult
}
