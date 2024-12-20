package com.x8bit.bitwarden.ui.vault.feature.item

import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.BreachCountResult
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.ValidatePasswordResult
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.event.OrganizationEventManager
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.util.combineDataStates
import com.x8bit.bitwarden.data.platform.repository.util.mapNullable
import com.x8bit.bitwarden.data.vault.manager.FileManager
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.DeleteCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.DownloadAttachmentResult
import com.x8bit.bitwarden.data.vault.repository.model.RestoreCipherResult
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.vault.feature.item.model.TotpCodeItemData
import com.x8bit.bitwarden.ui.vault.feature.item.model.VaultItemStateData
import com.x8bit.bitwarden.ui.vault.feature.item.util.toViewState
import com.x8bit.bitwarden.ui.vault.feature.util.canAssignToCollections
import com.x8bit.bitwarden.ui.vault.feature.util.hasDeletePermissionInAtLeastOneCollection
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.io.File
import javax.inject.Inject

private const val KEY_STATE = "state"
private const val KEY_TEMP_ATTACHMENT = "tempAttachmentFile"

/**
 * ViewModel responsible for handling user interactions in the vault item screen
 */
@Suppress("LargeClass", "TooManyFunctions")
@HiltViewModel
class VaultItemViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val clipboardManager: BitwardenClipboardManager,
    private val authRepository: AuthRepository,
    private val vaultRepository: VaultRepository,
    private val fileManager: FileManager,
    private val organizationEventManager: OrganizationEventManager,
) : BaseViewModel<VaultItemState, VaultItemEvent, VaultItemAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE] ?: VaultItemState(
        vaultItemId = VaultItemArgs(savedStateHandle).vaultItemId,
        viewState = VaultItemState.ViewState.Loading,
        dialog = null,
    ),
) {
    /**
     * Reference to a temporary attachment saved in cache.
     */
    private var temporaryAttachmentData: File?
        get() = savedStateHandle[KEY_TEMP_ATTACHMENT]
        set(value) {
            savedStateHandle[KEY_TEMP_ATTACHMENT] = value
        }

    //region Initialization and Overrides
    init {
        organizationEventManager.trackEvent(
            event = OrganizationEvent.CipherClientViewed(cipherId = state.vaultItemId),
        )
        combine(
            vaultRepository.getVaultItemStateFlow(state.vaultItemId),
            authRepository.userStateFlow,
            vaultRepository.getAuthCodeFlow(state.vaultItemId),
            vaultRepository.collectionsStateFlow,
        ) { cipherViewState, userState, authCodeState, collectionsState ->
            val totpCodeData = authCodeState.data?.let {
                TotpCodeItemData(
                    periodSeconds = it.periodSeconds,
                    timeLeftSeconds = it.timeLeftSeconds,
                    totpCode = it.totpCode,
                    verificationCode = it.code,
                )
            }
            VaultItemAction.Internal.VaultDataReceive(
                userState = userState,
                vaultDataState = combineDataStates(
                    cipherViewState,
                    authCodeState,
                    collectionsState,
                ) { _, _, _ ->
                    // We are only combining the DataStates to know the overall state,
                    // we map it to the appropriate value below.
                }
                    .mapNullable {
                        val canDelete = collectionsState
                            .data
                            .hasDeletePermissionInAtLeastOneCollection(
                                collectionIds = cipherViewState.data?.collectionIds,
                            )

                        val canAssignToCollections = collectionsState
                            .data
                            .canAssignToCollections(cipherViewState.data?.collectionIds)

                        VaultItemStateData(
                            cipher = cipherViewState.data,
                            totpCodeItemData = totpCodeData,
                            canDelete = canDelete,
                            canAssociateToCollections = canAssignToCollections,
                        )
                    },
            )
        }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: VaultItemAction) {
        when (action) {
            is VaultItemAction.ItemType.Login -> handleLoginTypeActions(action)
            is VaultItemAction.ItemType.Card -> handleCardTypeActions(action)
            is VaultItemAction.ItemType.SshKey -> handleSshKeyTypeActions(action)
            is VaultItemAction.ItemType.Identity -> handleIdentityTypeActions(action)
            is VaultItemAction.Common -> handleCommonActions(action)
            is VaultItemAction.Internal -> handleInternalAction(action)
        }
    }
    //endregion Initialization and Overrides

    //region Common Handlers

    private fun handleCommonActions(action: VaultItemAction.Common) {
        when (action) {
            is VaultItemAction.Common.CloseClick -> handleCloseClick()
            is VaultItemAction.Common.DismissDialogClick -> handleDismissDialogClick()
            is VaultItemAction.Common.EditClick -> handleEditClick()
            is VaultItemAction.Common.MasterPasswordSubmit -> handleMasterPasswordSubmit(action)
            is VaultItemAction.Common.RefreshClick -> handleRefreshClick()
            is VaultItemAction.Common.CopyCustomHiddenFieldClick -> {
                handleCopyCustomHiddenFieldClick(action)
            }

            is VaultItemAction.Common.CopyCustomTextFieldClick -> {
                handleCopyCustomTextFieldClick(action)
            }

            is VaultItemAction.Common.HiddenFieldVisibilityClicked -> {
                handleHiddenFieldVisibilityClicked(action)
            }

            is VaultItemAction.Common.AttachmentDownloadClick -> {
                handleAttachmentDownloadClick(action)
            }

            is VaultItemAction.Common.AttachmentFileLocationReceive -> {
                handleAttachmentFileLocationReceive(action)
            }

            is VaultItemAction.Common.NoAttachmentFileLocationReceive -> {
                handleNoAttachmentFileLocationReceive()
            }

            is VaultItemAction.Common.AttachmentsClick -> handleAttachmentsClick()
            is VaultItemAction.Common.CloneClick -> handleCloneClick()
            is VaultItemAction.Common.MoveToOrganizationClick -> handleMoveToOrganizationClick()
            is VaultItemAction.Common.CollectionsClick -> handleCollectionsClick()
            is VaultItemAction.Common.ConfirmDeleteClick -> handleConfirmDeleteClick()
            is VaultItemAction.Common.ConfirmRestoreClick -> handleConfirmRestoreClick()
            is VaultItemAction.Common.DeleteClick -> handleDeleteClick()
            is VaultItemAction.Common.ConfirmCloneWithoutFido2CredentialClick -> {
                handleConfirmCloneClick()
            }

            is VaultItemAction.Common.RestoreVaultItemClick -> handleRestoreItemClicked()
            is VaultItemAction.Common.CopyNotesClick -> handleCopyNotesClick()
        }
    }

    private fun handleCloseClick() {
        sendEvent(VaultItemEvent.NavigateBack)
    }

    private fun handleDismissDialogClick() {
        dismissDialog()
    }

    private fun handleDeleteClick() {
        onContent { content ->
            if (content.common.requiresReprompt) {
                updateDialogState(
                    VaultItemState.DialogState.MasterPasswordDialog(
                        action = PasswordRepromptAction.DeleteClick,
                    ),
                )
                return@onContent
            } else {
                updateDialogState(
                    VaultItemState
                        .DialogState
                        .DeleteConfirmationPrompt(state.deletionConfirmationText),
                )
            }
        }
    }

    private fun handleEditClick() {
        onContent { content ->
            if (content.common.requiresReprompt) {
                updateDialogState(
                    VaultItemState.DialogState.MasterPasswordDialog(
                        action = PasswordRepromptAction.EditClick,
                    ),
                )
                return@onContent
            }
            sendEvent(
                VaultItemEvent.NavigateToAddEdit(
                    itemId = state.vaultItemId,
                    isClone = false,
                ),
            )
        }
    }

    private fun handleMasterPasswordSubmit(action: VaultItemAction.Common.MasterPasswordSubmit) {
        updateDialogState(VaultItemState.DialogState.Loading(R.string.loading.asText()))
        viewModelScope.launch {
            val result = authRepository.validatePassword(action.masterPassword)
            sendAction(VaultItemAction.Internal.ValidatePasswordReceive(result, action.action))
        }
    }

    private fun handleRefreshClick() {
        // No need to update the view state, the vault repo will emit a new state during this time
        vaultRepository.sync(forced = true)
    }

    private fun handleCopyCustomHiddenFieldClick(
        action: VaultItemAction.Common.CopyCustomHiddenFieldClick,
    ) {
        onContent { content ->
            if (content.common.requiresReprompt) {
                updateDialogState(
                    VaultItemState.DialogState.MasterPasswordDialog(
                        action = PasswordRepromptAction.CopyClick(action.field),
                    ),
                )
                return@onContent
            }
            clipboardManager.setText(text = action.field)
            organizationEventManager.trackEvent(
                event = OrganizationEvent.CipherClientCopiedHiddenField(
                    cipherId = state.vaultItemId,
                ),
            )
        }
    }

    private fun handleCopyCustomTextFieldClick(
        action: VaultItemAction.Common.CopyCustomTextFieldClick,
    ) {
        clipboardManager.setText(text = action.field)
    }

    private fun handleHiddenFieldVisibilityClicked(
        action: VaultItemAction.Common.HiddenFieldVisibilityClicked,
    ) {
        onContent { content ->
            if (content.common.requiresReprompt) {
                updateDialogState(
                    VaultItemState.DialogState.MasterPasswordDialog(
                        action = PasswordRepromptAction.ViewHiddenFieldClicked(
                            field = action.field,
                            isVisible = action.isVisible,
                        ),
                    ),
                )
                return@onContent
            }
            mutableStateFlow.update { currentState ->
                currentState.copy(
                    viewState = content.copy(
                        common = content.common.copy(
                            customFields = content.common.customFields.map { customField ->
                                if (customField == action.field) {
                                    action.field.copy(isVisible = action.isVisible)
                                } else {
                                    customField
                                }
                            },
                        ),
                    ),
                )
            }
            if (action.isVisible) {
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.CipherClientToggledHiddenFieldVisible(
                        cipherId = state.vaultItemId,
                    ),
                )
            }
        }
    }

    private fun handleAttachmentDownloadClick(
        action: VaultItemAction.Common.AttachmentDownloadClick,
    ) {
        onContent { content ->
            if (content.common.requiresReprompt) {
                updateDialogState(
                    VaultItemState.DialogState.MasterPasswordDialog(
                        action = PasswordRepromptAction.AttachmentDownloadClick(
                            attachment = action.attachment,
                        ),
                    ),
                )
                return@onContent
            }

            updateDialogState(VaultItemState.DialogState.Loading(R.string.downloading.asText()))

            viewModelScope.launch {
                val result = vaultRepository
                    .downloadAttachment(
                        cipherView = requireNotNull(content.common.currentCipher),
                        attachmentId = action.attachment.id,
                    )

                trySendAction(
                    VaultItemAction.Internal.AttachmentDecryptReceive(
                        result = result,
                        fileName = action.attachment.title,
                    ),
                )
            }
        }
    }

    private fun handleAttachmentFileLocationReceive(
        action: VaultItemAction.Common.AttachmentFileLocationReceive,
    ) {
        dismissDialog()

        val file = temporaryAttachmentData ?: return
        viewModelScope.launch {
            val result = fileManager
                .fileToUri(
                    fileUri = action.fileUri,
                    file = file,
                )
            sendAction(
                VaultItemAction.Internal.AttachmentFinishedSavingToDisk(
                    isSaved = result,
                    file = file,
                ),
            )
        }
    }

    private fun handleNoAttachmentFileLocationReceive() {
        viewModelScope.launch {
            temporaryAttachmentData?.let { fileManager.delete(it) }
        }

        updateDialogState(
            VaultItemState.DialogState.Generic(
                R.string.unable_to_save_attachment.asText(),
            ),
        )
    }

    private fun handleAttachmentsClick() {
        onContent { content ->
            if (content.common.requiresReprompt) {
                updateDialogState(
                    VaultItemState.DialogState.MasterPasswordDialog(
                        action = PasswordRepromptAction.AttachmentsClick,
                    ),
                )
                return@onContent
            }
            sendEvent(VaultItemEvent.NavigateToAttachments(itemId = state.vaultItemId))
        }
    }

    @Suppress("MaxLineLength")
    private fun handleCloneClick() {
        onContent { content ->
            if (content.common.requiresCloneConfirmation) {
                updateDialogState(
                    VaultItemState.DialogState.Fido2CredentialCannotBeCopiedConfirmationPrompt(
                        message = R.string.the_passkey_will_not_be_copied_to_the_cloned_item_do_you_want_to_continue_cloning_this_item.asText(),
                    ),
                )
                return@onContent
            } else if (content.common.requiresReprompt) {
                updateDialogState(
                    VaultItemState.DialogState.MasterPasswordDialog(
                        action = PasswordRepromptAction.CloneClick,
                    ),
                )
                return@onContent
            }
            sendEvent(VaultItemEvent.NavigateToAddEdit(itemId = state.vaultItemId, isClone = true))
        }
    }

    private fun handleConfirmCloneClick() {
        onContent { content ->
            mutableStateFlow.update {
                it.copy(
                    dialog = null,
                    viewState = content.copy(
                        common = content.common.copy(
                            requiresCloneConfirmation = false,
                        ),
                    ),
                )
            }

            trySendAction(VaultItemAction.Common.CloneClick)
        }
    }

    private fun handleMoveToOrganizationClick() {
        onContent { content ->
            if (content.common.requiresReprompt) {
                updateDialogState(
                    VaultItemState.DialogState.MasterPasswordDialog(
                        action = PasswordRepromptAction.MoveToOrganizationClick,
                    ),
                )
                return@onContent
            }
            sendEvent(VaultItemEvent.NavigateToMoveToOrganization(itemId = state.vaultItemId))
        }
    }

    private fun handleCollectionsClick() {
        sendEvent(VaultItemEvent.NavigateToCollections(itemId = state.vaultItemId))
    }

    private fun handleConfirmDeleteClick() {
        onContent { content ->
            updateDialogState(
                VaultItemState.DialogState.Loading(
                    if (state.isCipherDeleted) {
                        R.string.deleting.asText()
                    } else {
                        R.string.soft_deleting.asText()
                    },
                ),
            )
            content.common.currentCipher?.let { cipher ->
                viewModelScope.launch {
                    trySendAction(
                        VaultItemAction.Internal.DeleteCipherReceive(
                            if (state.isCipherDeleted) {
                                vaultRepository.hardDeleteCipher(
                                    cipherId = state.vaultItemId,
                                )
                            } else {
                                vaultRepository.softDeleteCipher(
                                    cipherId = state.vaultItemId,
                                    cipherView = cipher,
                                )
                            },
                        ),
                    )
                }
            }
        }
    }

    private fun handleConfirmRestoreClick() {
        updateDialogState(
            VaultItemState.DialogState.Loading(
                R.string.restoring.asText(),
            ),
        )
        onContent { content ->
            content
                .common
                .currentCipher
                ?.let { cipher ->
                    viewModelScope.launch {
                        trySendAction(
                            VaultItemAction.Internal.RestoreCipherReceive(
                                result = vaultRepository.restoreCipher(
                                    cipherId = state.vaultItemId,
                                    cipherView = cipher,
                                ),
                            ),
                        )
                    }
                }
        }
    }

    private fun handleCopyNotesClick() {
        onContent { content ->
            val notes = content.common.notes.orEmpty()
            clipboardManager.setText(text = notes)
        }
    }

    //endregion Common Handlers

    //region Login Type Handlers

    private fun handleLoginTypeActions(action: VaultItemAction.ItemType.Login) {
        when (action) {
            is VaultItemAction.ItemType.Login.CheckForBreachClick -> {
                handleCheckForBreachClick()
            }

            is VaultItemAction.ItemType.Login.CopyPasswordClick -> {
                handleCopyPasswordClick()
            }

            is VaultItemAction.ItemType.Login.CopyTotpClick -> {
                handleCopyTotpClick()
            }

            is VaultItemAction.ItemType.Login.CopyUriClick -> {
                handleCopyUriClick(action)
            }

            is VaultItemAction.ItemType.Login.CopyUsernameClick -> {
                handleCopyUsernameClick()
            }

            is VaultItemAction.ItemType.Login.LaunchClick -> {
                handleLaunchClick(action)
            }

            is VaultItemAction.ItemType.Login.PasswordHistoryClick -> {
                handlePasswordHistoryClick()
            }

            is VaultItemAction.ItemType.Login.PasswordVisibilityClicked -> {
                handlePasswordVisibilityClicked(action)
            }
        }
    }

    private fun handleCheckForBreachClick() {
        onLoginContent { _, login ->
            val password = requireNotNull(login.passwordData?.password)
            updateDialogState(VaultItemState.DialogState.Loading(R.string.loading.asText()))
            viewModelScope.launch {
                val result = authRepository.getPasswordBreachCount(password = password)
                sendAction(VaultItemAction.Internal.PasswordBreachReceive(result))
            }
        }
    }

    private fun handleCopyPasswordClick() {
        onLoginContent { content, login ->
            val password = requireNotNull(login.passwordData?.password)
            if (content.common.requiresReprompt) {
                updateDialogState(
                    VaultItemState.DialogState.MasterPasswordDialog(
                        action = PasswordRepromptAction.CopyClick(value = password),
                    ),
                )
                return@onLoginContent
            }
            clipboardManager.setText(text = password)
            organizationEventManager.trackEvent(
                event = OrganizationEvent.CipherClientCopiedPassword(cipherId = state.vaultItemId),
            )
        }
    }

    private fun handleCopyTotpClick() {
        onLoginContent { _, login ->
            val code = login.totpCodeItemData?.verificationCode ?: return@onLoginContent
            clipboardManager.setText(text = code)
        }
    }

    private fun handleCopyUriClick(action: VaultItemAction.ItemType.Login.CopyUriClick) {
        clipboardManager.setText(text = action.uri)
    }

    private fun handleCopyUsernameClick() {
        onLoginContent { _, login ->
            val username = requireNotNull(login.username)
            clipboardManager.setText(text = username)
        }
    }

    private fun handleLaunchClick(
        action: VaultItemAction.ItemType.Login.LaunchClick,
    ) {
        sendEvent(VaultItemEvent.NavigateToUri(action.uri))
    }

    private fun handlePasswordHistoryClick() {
        onContent { content ->
            if (content.common.requiresReprompt) {
                updateDialogState(
                    VaultItemState.DialogState.MasterPasswordDialog(
                        action = PasswordRepromptAction.PasswordHistoryClick,
                    ),
                )
                return@onContent
            }
            sendEvent(VaultItemEvent.NavigateToPasswordHistory(state.vaultItemId))
        }
    }

    private fun handlePasswordVisibilityClicked(
        action: VaultItemAction.ItemType.Login.PasswordVisibilityClicked,
    ) {
        onLoginContent { content, login ->
            if (content.common.requiresReprompt) {
                updateDialogState(
                    VaultItemState.DialogState.MasterPasswordDialog(
                        action = PasswordRepromptAction.ViewPasswordClick(
                            isVisible = action.isVisible,
                        ),
                    ),
                )
                return@onLoginContent
            }
            mutableStateFlow.update { currentState ->
                currentState.copy(
                    viewState = content.copy(
                        type = login.copy(
                            passwordData = login.passwordData?.copy(
                                isVisible = action.isVisible,
                            ),
                        ),
                    ),
                )
            }
            if (action.isVisible) {
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.CipherClientToggledPasswordVisible(
                        cipherId = state.vaultItemId,
                    ),
                )
            }
        }
    }

    //endregion Login Type Handlers

    //region Card Type Handlers

    private fun handleCardTypeActions(action: VaultItemAction.ItemType.Card) {
        when (action) {
            is VaultItemAction.ItemType.Card.CodeVisibilityClick -> {
                handleCodeVisibilityClick(action)
            }

            VaultItemAction.ItemType.Card.CopyNumberClick -> handleCopyNumberClick()
            VaultItemAction.ItemType.Card.CopySecurityCodeClick -> handleCopySecurityCodeClick()
            is VaultItemAction.ItemType.Card.NumberVisibilityClick -> {
                handleNumberVisibilityClick(action)
            }
        }
    }

    private fun handleCodeVisibilityClick(
        action: VaultItemAction.ItemType.Card.CodeVisibilityClick,
    ) {
        onCardContent { content, card ->
            if (content.common.requiresReprompt) {
                updateDialogState(
                    VaultItemState.DialogState.MasterPasswordDialog(
                        action = PasswordRepromptAction.ViewCodeClick(
                            isVisible = action.isVisible,
                        ),
                    ),
                )
                return@onCardContent
            }
            mutableStateFlow.update { currentState ->
                currentState.copy(
                    viewState = content.copy(
                        type = card.copy(
                            securityCode = card.securityCode?.copy(
                                isVisible = action.isVisible,
                            ),
                        ),
                    ),
                )
            }
            if (action.isVisible) {
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.CipherClientToggledCardCodeVisible(
                        cipherId = state.vaultItemId,
                    ),
                )
            }
        }
    }

    private fun handleCopyNumberClick() {
        onCardContent { content, card ->
            val number = requireNotNull(card.number).number
            if (content.common.requiresReprompt) {
                updateDialogState(
                    VaultItemState.DialogState.MasterPasswordDialog(
                        action = PasswordRepromptAction.CopyClick(value = number),
                    ),
                )
                return@onCardContent
            }
            clipboardManager.setText(text = number)
        }
    }

    private fun handleCopySecurityCodeClick() {
        onCardContent { content, card ->
            val securityCode = requireNotNull(card.securityCode).code
            if (content.common.requiresReprompt) {
                updateDialogState(
                    VaultItemState.DialogState.MasterPasswordDialog(
                        action = PasswordRepromptAction.CopyClick(value = securityCode),
                    ),
                )
                return@onCardContent
            }
            clipboardManager.setText(text = securityCode)
        }
    }

    private fun handleNumberVisibilityClick(
        action: VaultItemAction.ItemType.Card.NumberVisibilityClick,
    ) {
        onCardContent { content, card ->
            if (content.common.requiresReprompt) {
                updateDialogState(
                    VaultItemState.DialogState.MasterPasswordDialog(
                        action = PasswordRepromptAction.ViewNumberClick(
                            isVisible = action.isVisible,
                        ),
                    ),
                )
                return@onCardContent
            }
            mutableStateFlow.update { currentState ->
                currentState.copy(
                    viewState = content.copy(
                        type = card.copy(
                            number = card.number?.copy(
                                isVisible = action.isVisible,
                            ),
                        ),
                    ),
                )
            }
            if (action.isVisible) {
                organizationEventManager.trackEvent(
                    event = OrganizationEvent.CipherClientToggledCardNumberVisible(
                        cipherId = state.vaultItemId,
                    ),
                )
            }
        }
    }

    //endregion Card Type Handlers

    //region SSH Key Type Handlers

    private fun handleSshKeyTypeActions(action: VaultItemAction.ItemType.SshKey) {
        when (action) {
            VaultItemAction.ItemType.SshKey.CopyPublicKeyClick -> handleCopyPublicKeyClick()

            is VaultItemAction.ItemType.SshKey.PrivateKeyVisibilityClicked -> {
                handlePrivateKeyVisibilityClicked(action)
            }

            is VaultItemAction.ItemType.SshKey.CopyPrivateKeyClick -> handleCopyPrivateKeyClick()

            VaultItemAction.ItemType.SshKey.CopyFingerprintClick -> handleCopyFingerprintClick()
        }
    }

    private fun handleCopyPublicKeyClick() {
        onSshKeyContent { _, sshKey ->
            clipboardManager.setText(text = sshKey.publicKey)
        }
    }

    private fun handlePrivateKeyVisibilityClicked(
        action: VaultItemAction.ItemType.SshKey.PrivateKeyVisibilityClicked,
    ) {
        onSshKeyContent { content, sshKey ->
            if (content.common.requiresReprompt) {
                updateDialogState(
                    VaultItemState.DialogState.MasterPasswordDialog(
                        action = PasswordRepromptAction.ViewPrivateKeyClicked(
                            isVisible = action.isVisible,
                        ),
                    ),
                )
                return@onSshKeyContent
            }
            mutableStateFlow.update { currentState ->
                currentState.copy(
                    viewState = content.copy(
                        type = sshKey.copy(showPrivateKey = action.isVisible),
                    ),
                )
            }
        }
    }

    private fun handleCopyPrivateKeyClick() {
        onSshKeyContent { content, sshKey ->
            if (content.common.requiresReprompt) {
                updateDialogState(
                    VaultItemState.DialogState.MasterPasswordDialog(
                        action = PasswordRepromptAction.CopyClick(value = sshKey.privateKey),
                    ),
                )
                return@onSshKeyContent
            }
            clipboardManager.setText(text = sshKey.privateKey)
        }
    }

    private fun handleCopyFingerprintClick() {
        onSshKeyContent { _, sshKey ->
            clipboardManager.setText(text = sshKey.fingerprint)
        }
    }

    //endregion SSH Key Type Handlers

    //region Identity Type Handlers

    private fun handleIdentityTypeActions(action: VaultItemAction.ItemType.Identity) {
        when (action) {
            VaultItemAction.ItemType.Identity.CopyIdentityNameClick -> {
                handleCopyIdentityNameClick()
            }

            VaultItemAction.ItemType.Identity.CopyUsernameClick -> {
                handleCopyIdentityUsernameClick()
            }

            VaultItemAction.ItemType.Identity.CopyCompanyClick -> handleCopyCompanyClick()
            VaultItemAction.ItemType.Identity.CopySsnClick -> handleCopySsnClick()
            VaultItemAction.ItemType.Identity.CopyPassportNumberClick -> {
                handleCopyPassportNumberClick()
            }

            VaultItemAction.ItemType.Identity.CopyLicenseNumberClick -> {
                handleCopyLicenseNumberClick()
            }

            VaultItemAction.ItemType.Identity.CopyEmailClick -> handleCopyEmailClick()
            VaultItemAction.ItemType.Identity.CopyPhoneClick -> handleCopyPhoneClick()
            VaultItemAction.ItemType.Identity.CopyAddressClick -> handleCopyAddressClick()
        }
    }

    private fun handleCopyIdentityNameClick() {
        onIdentityContent { _, identity ->
            val identityName = identity.identityName.orEmpty()
            clipboardManager.setText(text = identityName)
        }
    }

    private fun handleCopyIdentityUsernameClick() {
        onIdentityContent { _, identity ->
            val username = identity.username.orEmpty()
            clipboardManager.setText(text = username)
        }
    }

    private fun handleCopyCompanyClick() {
        onIdentityContent { _, identity ->
            val company = identity.company.orEmpty()
            clipboardManager.setText(text = company)
        }
    }

    private fun handleCopySsnClick() {
        onIdentityContent { _, identity ->
            val ssn = identity.ssn.orEmpty()
            clipboardManager.setText(text = ssn)
        }
    }

    private fun handleCopyPassportNumberClick() {
        onIdentityContent { _, identity ->
            val passportNumber = identity.passportNumber.orEmpty()
            clipboardManager.setText(text = passportNumber)
        }
    }

    private fun handleCopyLicenseNumberClick() {
        onIdentityContent { _, identity ->
            val licenseNumber = identity.licenseNumber.orEmpty()
            clipboardManager.setText(text = licenseNumber)
        }
    }

    private fun handleCopyEmailClick() {
        onIdentityContent { _, identity ->
            val email = identity.email.orEmpty()
            clipboardManager.setText(text = email)
        }
    }

    private fun handleCopyPhoneClick() {
        onIdentityContent { _, identity ->
            val phone = identity.phone.orEmpty()
            clipboardManager.setText(text = phone)
        }
    }

    private fun handleCopyAddressClick() {
        onIdentityContent { _, identity ->
            val address = identity.address.orEmpty()
            clipboardManager.setText(text = address)
        }
    }

    //endregion Identity Type Handlers

    //region Internal Type Handlers

    private fun handleInternalAction(action: VaultItemAction.Internal) {
        when (action) {
            is VaultItemAction.Internal.CopyValue -> handleCopyValue(action)
            is VaultItemAction.Internal.PasswordBreachReceive -> handlePasswordBreachReceive(action)
            is VaultItemAction.Internal.VaultDataReceive -> handleVaultDataReceive(action)
            is VaultItemAction.Internal.ValidatePasswordReceive -> handleValidatePasswordReceive(
                action,
            )

            is VaultItemAction.Internal.DeleteCipherReceive -> handleDeleteCipherReceive(action)
            is VaultItemAction.Internal.RestoreCipherReceive -> handleRestoreCipherReceive(action)

            is VaultItemAction.Internal.AttachmentDecryptReceive -> {
                handleAttachmentDecryptReceive(action)
            }

            is VaultItemAction.Internal.AttachmentFinishedSavingToDisk -> {
                handleAttachmentFinishedSavingToDisk(action)
            }
        }
    }

    private fun handleCopyValue(action: VaultItemAction.Internal.CopyValue) {
        clipboardManager.setText(action.value)
    }

    private fun handlePasswordBreachReceive(
        action: VaultItemAction.Internal.PasswordBreachReceive,
    ) {
        val message = when (val result = action.result) {
            BreachCountResult.Error -> R.string.generic_error_message.asText()
            is BreachCountResult.Success -> {
                if (result.breachCount > 0) {
                    R.string.password_exposed.asText(result.breachCount)
                } else {
                    R.string.password_safe.asText()
                }
            }
        }
        updateDialogState(VaultItemState.DialogState.Generic(message = message))
    }

    @Suppress("LongMethod")
    private fun handleVaultDataReceive(action: VaultItemAction.Internal.VaultDataReceive) {
        // Leave the current data alone if there is no UserState; we are in the process of logging
        // out.
        val userState = action.userState ?: return

        when (val vaultDataState = action.vaultDataState) {
            is DataState.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = vaultDataState.toViewStateOrError(
                            account = userState.activeAccount,
                            errorText = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            is DataState.Loaded -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = vaultDataState.toViewStateOrError(
                            account = userState.activeAccount,
                            errorText = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            DataState.Loading -> {
                mutableStateFlow.update {
                    it.copy(viewState = VaultItemState.ViewState.Loading)
                }
            }

            is DataState.NoNetwork -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = vaultDataState.toViewStateOrError(
                            account = userState.activeAccount,
                            errorText = R.string.internet_connection_required_title
                                .asText()
                                .concat(
                                    " ".asText(),
                                    R.string.internet_connection_required_message.asText(),
                                ),
                        ),
                    )
                }
            }

            is DataState.Pending -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = vaultDataState.toViewStateOrError(
                            account = userState.activeAccount,
                            errorText = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }
        }
    }

    private fun DataState<VaultItemStateData>.toViewStateOrError(
        account: UserState.Account,
        errorText: Text,
    ): VaultItemState.ViewState = this
        .data
        ?.cipher
        ?.let { cipher ->
            val ownerOrg: Organization? = account.organizations.find {
                cipher.organizationId == it.id
            }
            cipher.toViewState(
                previousState = state.viewState.asContentOrNull(),
                isPremiumUser = ownerOrg?.shouldUsersGetPremium ?: account.isPremium,
                hasMasterPassword = account.hasMasterPassword,
                totpCodeItemData = this.data?.totpCodeItemData,
                canDelete = this.data?.canDelete == true,
                canAssignToCollections = this.data?.canAssociateToCollections == true,
            )
        }
        ?: VaultItemState.ViewState.Error(message = errorText)

    private fun handleValidatePasswordReceive(
        action: VaultItemAction.Internal.ValidatePasswordReceive,
    ) {
        when (val result = action.result) {
            ValidatePasswordResult.Error -> {
                updateDialogState(
                    VaultItemState.DialogState.Generic(
                        message = R.string.generic_error_message.asText(),
                    ),
                )
            }

            is ValidatePasswordResult.Success -> {
                if (result.isValid) {
                    onContent { content ->
                        mutableStateFlow.update {
                            it.copy(
                                dialog = null,
                                viewState = content.copy(
                                    common = content.common.copy(requiresReprompt = false),
                                ),
                            )
                        }
                        trySendAction(action.repromptAction.vaultItemAction)
                    }
                } else {
                    updateDialogState(
                        VaultItemState.DialogState.Generic(
                            message = R.string.invalid_master_password.asText(),
                        ),
                    )
                }
            }
        }
    }

    private fun handleDeleteCipherReceive(action: VaultItemAction.Internal.DeleteCipherReceive) {
        when (action.result) {
            DeleteCipherResult.Error -> {
                updateDialogState(
                    VaultItemState.DialogState.Generic(
                        message = R.string.generic_error_message.asText(),
                    ),
                )
            }

            DeleteCipherResult.Success -> {
                dismissDialog()
                sendEvent(
                    VaultItemEvent.ShowToast(
                        message = if (state.isCipherDeleted) {
                            R.string.item_deleted.asText()
                        } else {
                            R.string.item_soft_deleted.asText()
                        },
                    ),
                )
                sendEvent(VaultItemEvent.NavigateBack)
            }
        }
    }

    private fun handleRestoreCipherReceive(action: VaultItemAction.Internal.RestoreCipherReceive) {
        when (action.result) {
            RestoreCipherResult.Error -> {
                updateDialogState(
                    VaultItemState.DialogState.Generic(
                        message = R.string.generic_error_message.asText(),
                    ),
                )
            }

            RestoreCipherResult.Success -> {
                dismissDialog()
                sendEvent(VaultItemEvent.ShowToast(message = R.string.item_restored.asText()))
                sendEvent(VaultItemEvent.NavigateBack)
            }
        }
    }

    private fun handleAttachmentDecryptReceive(
        action: VaultItemAction.Internal.AttachmentDecryptReceive,
    ) {
        when (val result = action.result) {
            DownloadAttachmentResult.Failure -> {
                updateDialogState(
                    VaultItemState.DialogState.Generic(
                        message = R.string.unable_to_download_file.asText(),
                    ),
                )
            }

            is DownloadAttachmentResult.Success -> {
                temporaryAttachmentData = result.file
                sendEvent(
                    VaultItemEvent.NavigateToSelectAttachmentSaveLocation(
                        fileName = action.fileName,
                    ),
                )
            }
        }
    }

    private fun handleAttachmentFinishedSavingToDisk(
        action: VaultItemAction.Internal.AttachmentFinishedSavingToDisk,
    ) {
        viewModelScope.launch {
            fileManager.delete(action.file)
        }

        if (action.isSaved) {
            sendEvent(VaultItemEvent.ShowToast(R.string.save_attachment_success.asText()))
        } else {
            updateDialogState(
                VaultItemState.DialogState.Generic(
                    R.string.unable_to_save_attachment.asText(),
                ),
            )
        }
    }

    private fun handleRestoreItemClicked() {
        onContent { content ->
            if (content.common.requiresReprompt) {
                updateDialogState(
                    VaultItemState.DialogState.MasterPasswordDialog(
                        action = PasswordRepromptAction.RestoreItemClick,
                    ),
                )
            } else {
                updateDialogState(VaultItemState.DialogState.RestoreItemDialog)
            }
        }
    }

    //endregion Internal Type Handlers

    private fun updateDialogState(dialog: VaultItemState.DialogState?) {
        mutableStateFlow.update {
            it.copy(dialog = dialog)
        }
    }

    private fun dismissDialog() {
        updateDialogState(null)
    }

    private inline fun onContent(
        crossinline block: (VaultItemState.ViewState.Content) -> Unit,
    ) {
        state.viewState.asContentOrNull()?.let(block)
    }

    private inline fun onLoginContent(
        crossinline block: (
            VaultItemState.ViewState.Content,
            VaultItemState.ViewState.Content.ItemType.Login,
        ) -> Unit,
    ) {
        state.viewState.asContentOrNull()
            ?.let { content ->
                (content.type as? VaultItemState.ViewState.Content.ItemType.Login)
                    ?.let { loginContent ->
                        block(content, loginContent)
                    }
            }
    }

    private inline fun onCardContent(
        crossinline block: (
            VaultItemState.ViewState.Content,
            VaultItemState.ViewState.Content.ItemType.Card,
        ) -> Unit,
    ) {
        state.viewState.asContentOrNull()
            ?.let { content ->
                (content.type as? VaultItemState.ViewState.Content.ItemType.Card)
                    ?.let { loginContent ->
                        block(content, loginContent)
                    }
            }
    }

    private inline fun onSshKeyContent(
        crossinline block: (
            VaultItemState.ViewState.Content,
            VaultItemState.ViewState.Content.ItemType.SshKey,
        ) -> Unit,
    ) {
        state.viewState.asContentOrNull()
            ?.let { content ->
                (content.type as? VaultItemState.ViewState.Content.ItemType.SshKey)
                    ?.let { sshKeyContent ->
                        block(content, sshKeyContent)
                    }
            }
    }

    private inline fun onIdentityContent(
        crossinline block: (
            VaultItemState.ViewState.Content,
            VaultItemState.ViewState.Content.ItemType.Identity,
        ) -> Unit,
    ) {
        state.viewState.asContentOrNull()
            ?.let { content ->
                (content.type as? VaultItemState.ViewState.Content.ItemType.Identity)
                    ?.let { identityContent ->
                        block(content, identityContent)
                    }
            }
    }
}

/**
 * Represents the state for viewing an item in the vault.
 */
@Parcelize
data class VaultItemState(
    val vaultItemId: String,
    val viewState: ViewState,
    val dialog: DialogState?,
) : Parcelable {

    /**
     * Whether or not the cipher has been deleted.
     */
    val isCipherDeleted: Boolean
        get() = viewState.asContentOrNull()
            ?.common
            ?.currentCipher
            ?.deletedDate != null

    /**
     * Whether or not the fab is visible.
     */
    val isFabVisible: Boolean
        get() = viewState is ViewState.Content && !isCipherDeleted

    /**
     * Whether or not the cipher is in a collection.
     */
    val isCipherInCollection: Boolean
        get() = viewState.asContentOrNull()
            ?.common
            ?.currentCipher
            ?.collectionIds
            ?.isNotEmpty()
            ?: false

    /**
     * Whether or not the cipher can be deleted.
     */
    val canDelete: Boolean
        get() = viewState.asContentOrNull()
            ?.common
            ?.canDelete == true

    val canAssignToCollections: Boolean
        get() = viewState.asContentOrNull()
            ?.common
            ?.canAssignToCollections
            ?: false

    /**
     * The text to display on the deletion confirmation dialog.
     */
    val deletionConfirmationText: Text
        get() = if (isCipherDeleted) {
            R.string.do_you_really_want_to_permanently_delete_cipher
        } else {
            R.string.do_you_really_want_to_soft_delete_cipher
        }
            .asText()

    /**
     * Represents the specific view states for the [VaultItemScreen].
     */
    sealed class ViewState : Parcelable {
        /**
         * Represents an error state for the [VaultItemScreen].
         */
        @Parcelize
        data class Error(
            val message: Text,
        ) : ViewState()

        /**
         * Loading state for the [VaultItemScreen], signifying that the content is being processed.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Represents a loaded content state for the [VaultItemScreen].
         */
        @Parcelize
        data class Content(
            val common: Common,
            val type: ItemType,
        ) : ViewState() {

            /**
             * Content data that is common for all item types.
             *
             * @property name The name of the item.
             * @property lastUpdated A formatted date string indicating when the item was last
             * updated.
             * @property notes Contains general notes taken by the user.
             * @property customFields A list of custom fields that user has added.
             * @property requiresReprompt Indicates if a master password prompt is required to view
             * secure fields.
             * @property requiresCloneConfirmation Indicates user confirmation is required when
             * cloning a cipher.
             * @property currentCipher The cipher that is currently being viewed (nullable).
             * @property attachments A list of attachments associated with the cipher.
             * @property canDelete Indicates if the cipher can be deleted.
             * @property canAssignToCollections Indicates if the cipher can be assigned to
             * collections.
             */
            @Parcelize
            data class Common(
                val name: String,
                val lastUpdated: String,
                val notes: String?,
                val customFields: List<Custom>,
                val requiresReprompt: Boolean,
                val requiresCloneConfirmation: Boolean,
                @IgnoredOnParcel
                val currentCipher: CipherView? = null,
                val attachments: List<AttachmentItem>?,
                val canDelete: Boolean,
                val canAssignToCollections: Boolean,
            ) : Parcelable {

                /**
                 * Represents an attachment.
                 */
                @Parcelize
                data class AttachmentItem(
                    val id: String,
                    val title: String,
                    val displaySize: String,
                    val url: String,
                    val isLargeFile: Boolean,
                    val isDownloadAllowed: Boolean,
                ) : Parcelable

                /**
                 * Represents a custom field, TextField, HiddenField, BooleanField, or LinkedField.
                 */
                sealed class Custom : Parcelable {
                    /**
                     * Represents the data for displaying a custom text field.
                     */
                    @Parcelize
                    data class TextField(
                        val name: String,
                        val value: String,
                        val isCopyable: Boolean,
                    ) : Custom()

                    /**
                     * Represents the data for displaying a custom hidden text field.
                     */
                    @Parcelize
                    data class HiddenField(
                        val name: String,
                        val value: String,
                        val isCopyable: Boolean,
                        val isVisible: Boolean,
                    ) : Custom()

                    /**
                     * Represents the data for displaying a custom boolean property field.
                     */
                    @Parcelize
                    data class BooleanField(
                        val name: String,
                        val value: Boolean,
                    ) : Custom()

                    /**
                     * Represents the data for displaying a custom linked field.
                     */
                    @Parcelize
                    data class LinkedField(
                        val vaultLinkedFieldType: VaultLinkedFieldType,
                        val name: String,
                    ) : Custom()
                }
            }

            /**
             * Content data specific to an item type.
             */
            @Parcelize
            sealed class ItemType : Parcelable {

                /**
                 * Represents the `Login` item type.
                 *
                 * @property username The username required for the login item.
                 * @property passwordData The password required for the login item.
                 * @property passwordHistoryCount An integer indicating how many times the password
                 * has been changed.
                 * @property uris The URI associated with the login item.
                 * @property passwordRevisionDate An optional string indicating the last time the
                 * password was changed.
                 * @property totpCodeItemData The optional data related the TOTP code.
                 * @property isPremiumUser Indicates if the user has subscribed to a premium
                 * account.
                 * @property canViewTotpCode Indicates if the user can view an associated TOTP code.
                 * @property fido2CredentialCreationDateText Optional creation date and time of the
                 * FIDO2 credential associated with the login item.
                 *
                 * **NOTE** [canViewTotpCode] currently supports a deprecated edge case where an
                 * organization supports TOTP but not through the current premium model.
                 * This additional field is added to allow for [isPremiumUser] to be an independent
                 * value.
                 * @see [CipherView.organizationUseTotp]
                 *
                 */
                @Parcelize
                data class Login(
                    val username: String?,
                    val passwordData: PasswordData?,
                    val passwordHistoryCount: Int?,
                    val uris: List<UriData>,
                    val passwordRevisionDate: String?,
                    val totpCodeItemData: TotpCodeItemData?,
                    val isPremiumUser: Boolean,
                    val canViewTotpCode: Boolean,
                    val fido2CredentialCreationDateText: Text?,
                ) : ItemType() {

                    /**
                     * A wrapper for the password data.
                     *
                     * @property password The password itself.
                     * @property isVisible Whether or not it is currently visible.
                     * @property canViewPassword Indicates whether the current user can view and
                     * copy passwords associated with the login item.
                     */
                    @Parcelize
                    data class PasswordData(
                        val password: String,
                        val isVisible: Boolean,
                        val canViewPassword: Boolean,
                    ) : Parcelable

                    /**
                     * A wrapper for URI data, including the [uri] itself and whether it is
                     * copyable and launch-able.
                     */
                    @Parcelize
                    data class UriData(
                        val uri: String,
                        val isCopyable: Boolean,
                        val isLaunchable: Boolean,
                    ) : Parcelable
                }

                /**
                 * Represents the `SecureNote` item type.
                 */
                data object SecureNote : ItemType()

                /**
                 * Represents the `Identity` item type.
                 *
                 * @property identityName The name for the identity.
                 * @property username The username for the identity.
                 * @property company The company associated with the identity.
                 * @property ssn The SSN for the identity.
                 * @property passportNumber The passport number for the identity.
                 * @property licenseNumber The license number for the identity.
                 * @property email The email for the identity.
                 * @property phone The phone number for the identity.
                 * @property address The address for the identity.
                 */
                data class Identity(
                    val identityName: String?,
                    val username: String?,
                    val company: String?,
                    val ssn: String?,
                    val passportNumber: String?,
                    val licenseNumber: String?,
                    val email: String?,
                    val phone: String?,
                    val address: String?,
                ) : ItemType()

                /**
                 * Represents the `Card` item type.
                 *
                 * @property cardholderName The cardholder name for the card.
                 * @property number The number for the card.
                 * @property brand The brand for the card.
                 * @property expiration The expiration for the card.
                 * @property securityCode The securityCode for the card.
                 */
                data class Card(
                    val cardholderName: String?,
                    val number: NumberData?,
                    val brand: VaultCardBrand?,
                    val expiration: String?,
                    val securityCode: CodeData?,
                ) : ItemType() {

                    /**
                     * A wrapper for the number data.
                     *
                     * @property number The card number itself.
                     * @property isVisible Whether or not it is currently visible.
                     */
                    @Parcelize
                    data class NumberData(
                        val number: String,
                        val isVisible: Boolean,
                    ) : Parcelable

                    /**
                     * A wrapper for the code data.
                     *
                     * @property code The security code itself.
                     * @property isVisible Whether or not it is currently visible.
                     */
                    @Parcelize
                    data class CodeData(
                        val code: String,
                        val isVisible: Boolean,
                    ) : Parcelable
                }

                /**
                 * Represents the data for displaying an `SSHKey` item type.
                 *
                 * @property name The name of the key.
                 * @property privateKey The SSH private key.
                 */
                data class SshKey(
                    val name: String?,
                    val publicKey: String,
                    val privateKey: String,
                    val fingerprint: String,
                    val showPrivateKey: Boolean,
                ) : ItemType()
            }
        }

        /**
         * Convenience function to keep the syntax a little cleaner when safe casting specifically
         * for [Content]
         */
        fun asContentOrNull(): Content? = this as? Content
    }

    /**
     * Displays a dialog.
     */
    sealed class DialogState : Parcelable {

        /**
         * Displays a generic dialog to the user.
         */
        @Parcelize
        data class Generic(
            val message: Text,
        ) : DialogState()

        /**
         * Displays the loading dialog to the user with a message.
         */
        @Parcelize
        data class Loading(
            val message: Text,
        ) : DialogState()

        /**
         * Displays the master password dialog to the user.
         */
        @Parcelize
        data class MasterPasswordDialog(
            val action: PasswordRepromptAction,
        ) : DialogState()

        /**
         * Displays the dialog for deleting the item to the user.
         */
        @Parcelize
        data class DeleteConfirmationPrompt(
            val message: Text,
        ) : DialogState()

        /**
         * Displays the dialog for cloning without copying FIDO2 credentials to the user.
         */
        @Parcelize
        data class Fido2CredentialCannotBeCopiedConfirmationPrompt(
            val message: Text,
        ) : DialogState()

        /**
         * Displays the dialog to prompt the user to confirm restoring a deleted item.
         */
        @Parcelize
        data object RestoreItemDialog : DialogState()
    }
}

/**
 * Represents a set of events related view a vault item.
 */
sealed class VaultItemEvent {
    /**
     * Navigates back.
     */
    data object NavigateBack : VaultItemEvent()

    /**
     * Navigates to the edit screen.
     */
    data class NavigateToAddEdit(
        val itemId: String,
        val isClone: Boolean,
    ) : VaultItemEvent()

    /**
     * Navigates to the password history screen.
     */
    data class NavigateToPasswordHistory(
        val itemId: String,
    ) : VaultItemEvent()

    /**
     * Launches the external URI.
     */
    data class NavigateToUri(
        val uri: String,
    ) : VaultItemEvent()

    /**
     * Navigates to the attachments screen.
     */
    data class NavigateToAttachments(
        val itemId: String,
    ) : VaultItemEvent()

    /**
     * Navigates to the move to organization screen.
     */
    data class NavigateToMoveToOrganization(
        val itemId: String,
    ) : VaultItemEvent()

    /**
     * Navigates to the collections screen.
     */
    data class NavigateToCollections(
        val itemId: String,
    ) : VaultItemEvent()

    /**
     * Navigates to select a location where to save an attachment with the name [fileName].
     */
    data class NavigateToSelectAttachmentSaveLocation(
        val fileName: String,
    ) : VaultItemEvent()

    /**
     * Places the given [message] in your clipboard.
     */
    data class ShowToast(
        val message: Text,
    ) : VaultItemEvent()
}

/**
 * Represents a set of actions related to viewing a vault item.
 * Each subclass of this sealed class denotes a distinct action that can be taken.
 */
sealed class VaultItemAction {

    /**
     * Represents actions common across all item types.
     */
    sealed class Common : VaultItemAction() {

        /**
         * The user has clicked the close button.
         */
        data object CloseClick : Common()

        /**
         * The user has clicked the delete button.
         */
        data object DeleteClick : Common()

        /**
         * The user has confirmed to deleted the cipher.
         */
        data object ConfirmDeleteClick : Common()

        /**
         * The user has clicked to restore a deleted item.

         */
        data object RestoreVaultItemClick : Common()

        /**
         * The user has confirmed to restore the cipher.
         */
        data object ConfirmRestoreClick : Common()

        /**
         * The user has clicked to dismiss the dialog.
         */
        data object DismissDialogClick : Common()

        /**
         * The user has clicked the edit button.
         */
        data object EditClick : Common()

        /**
         * The user has submitted their master password.
         */
        data class MasterPasswordSubmit(
            val masterPassword: String,
            val action: PasswordRepromptAction,
        ) : Common()

        /**
         * The user has clicked the refresh button.
         */
        data object RefreshClick : Common()

        /**
         * The user has clicked the copy button for a custom hidden field.
         */
        data class CopyCustomHiddenFieldClick(
            val field: String,
        ) : Common()

        /**
         * The user has clicked the copy button for a custom text field.
         */
        data class CopyCustomTextFieldClick(
            val field: String,
        ) : Common()

        /**
         * The user has clicked to display the hidden field.
         */
        data class HiddenFieldVisibilityClicked(
            val field: VaultItemState.ViewState.Content.Common.Custom.HiddenField,
            val isVisible: Boolean,
        ) : Common()

        /**
         * The user has clicked the attachments button.
         */
        data object AttachmentsClick : Common()

        /**
         * The user has clicked the clone button.
         */
        data object CloneClick : Common()

        /**
         * The user has clicked the move to organization button.
         */
        data object MoveToOrganizationClick : Common()

        /**
         * The user has clicked the collections button.
         */
        data object CollectionsClick : Common()

        /**
         * The user has clicked the download button.
         */
        data class AttachmentDownloadClick(
            val attachment: VaultItemState.ViewState.Content.Common.AttachmentItem,
        ) : Common()

        /**
         * The user has selected a location to save the file.
         */
        data class AttachmentFileLocationReceive(
            val fileUri: Uri,
        ) : Common()

        /**
         * The user skipped selecting a location for the attachment file.
         */
        data object NoAttachmentFileLocationReceive : Common()

        /**
         * The user confirmed cloning a cipher without its FIDO 2 credentials.
         */
        data object ConfirmCloneWithoutFido2CredentialClick : Common()

        /**
         * The user has clicked the copy button for notes text field.
         */
        data object CopyNotesClick : Common()
    }

    /**
     * Represents actions specific to an item type.
     */
    sealed class ItemType : VaultItemAction() {

        /**
         * Represents actions specific to the Login type.
         */
        sealed class Login : ItemType() {
            /**
             * The user has clicked the check for breach button.
             */
            data object CheckForBreachClick : Login()

            /**
             * The user has clicked the copy button for the password.
             */
            data object CopyPasswordClick : Login()

            /**
             * The user has clicked the copy button for the TOTP code.
             */
            data object CopyTotpClick : Login()

            /**
             * The user has clicked the copy button for a URI.
             */
            data class CopyUriClick(
                val uri: String,
            ) : Login()

            /**
             * The user has clicked the copy button for the username.
             */
            data object CopyUsernameClick : Login()

            /**
             * The user has clicked the launch button for a URI.
             */
            data class LaunchClick(
                val uri: String,
            ) : Login()

            /**
             * The user has clicked the password history text.
             */
            data object PasswordHistoryClick : Login()

            /**
             * The user has clicked to display the password.
             */
            data class PasswordVisibilityClicked(
                val isVisible: Boolean,
            ) : Login()
        }

        /**
         * Represents actions specific to the Card type.
         */
        sealed class Card : ItemType() {

            /**
             * The user has clicked to display the code.
             */
            data class CodeVisibilityClick(val isVisible: Boolean) : Card()

            /**
             * The user has clicked the copy button for the number.
             */
            data object CopyNumberClick : Card()

            /**
             * The user has clicked the copy button for the security code.
             */
            data object CopySecurityCodeClick : Card()

            /**
             * The user has clicked to display the Number.
             */
            data class NumberVisibilityClick(val isVisible: Boolean) : Card()
        }

        /**
         * Represents actions specific to the SshKey type.
         */
        sealed class SshKey : ItemType() {
            /**
             * The user has clicked the copy button for the public key.
             */
            data object CopyPublicKeyClick : SshKey()

            /**
             * The user has clicked to display the private key.
             */
            data class PrivateKeyVisibilityClicked(val isVisible: Boolean) : SshKey()

            /**
             * The user has clicked the copy button for the private key.
             */
            data object CopyPrivateKeyClick : SshKey()

            /**
             * The user has clicked the copy button for the fingerprint.
             */
            data object CopyFingerprintClick : SshKey()
        }

        /**
         * Represents actions specific to the Identity type.
         */
        sealed class Identity : VaultItemAction() {
            /**
             * The user has clicked the copy button for the identity name.
             */
            data object CopyIdentityNameClick : Identity()

            /**
             * The user has clicked the copy button for the username.
             */
            data object CopyUsernameClick : Identity()

            /**
             * The user has clicked the copy button for the company.
             */
            data object CopyCompanyClick : Identity()

            /**
             * The user has clicked the copy button for the SSN.
             */
            data object CopySsnClick : Identity()

            /**
             * The user has clicked the copy button for the passport number.
             */
            data object CopyPassportNumberClick : Identity()

            /**
             * The user has clicked the copy button for the license number.
             */
            data object CopyLicenseNumberClick : Identity()

            /**
             * The user has clicked the copy button for the email.
             */
            data object CopyEmailClick : Identity()

            /**
             * The user has clicked the copy button for the phone number.
             */
            data object CopyPhoneClick : Identity()

            /**
             * The user has clicked the copy button for the address.
             */
            data object CopyAddressClick : Identity()
        }
    }

    /**
     * Models actions that the [VaultItemViewModel] itself might send.
     */
    sealed class Internal : VaultItemAction() {

        /**
         * Copies the given [value] to the clipboard.
         */
        data class CopyValue(
            val value: String,
        ) : Internal()

        /**
         * Indicates that the password breach results have been received.
         */
        data class PasswordBreachReceive(
            val result: BreachCountResult,
        ) : Internal()

        /**
         * Indicates that the vault item data has been received.
         */
        data class VaultDataReceive(
            val userState: UserState?,
            val vaultDataState: DataState<VaultItemStateData>,
        ) : Internal()

        /**
         * Indicates that the verify password result has been received.
         */
        data class ValidatePasswordReceive(
            val result: ValidatePasswordResult,
            val repromptAction: PasswordRepromptAction,
        ) : Internal()

        /**
         * Indicates that the delete cipher result has been received.
         */
        data class DeleteCipherReceive(
            val result: DeleteCipherResult,
        ) : Internal()

        /**
         * Indicates that the restore cipher result has been received.
         */
        data class RestoreCipherReceive(
            val result: RestoreCipherResult,
        ) : Internal()

        /**
         * Indicates the attachment download and decryption is complete.
         */
        data class AttachmentDecryptReceive(
            val result: DownloadAttachmentResult,
            val fileName: String,
        ) : Internal()

        /**
         * The attempt to save the temporary [file] attachment to disk has finished. [isSaved]
         * indicates if it was successful.
         */
        data class AttachmentFinishedSavingToDisk(
            val isSaved: Boolean,
            val file: File,
        ) : Internal()
    }
}

/**
 * Represents all the actions that can be taken after being prompted to a master password check.
 */
sealed class PasswordRepromptAction : Parcelable {
    /**
     * The Vault action that should be sent when password validation has completed.
     */
    abstract val vaultItemAction: VaultItemAction

    /**
     * Indicates that we should launch the [VaultItemAction.Common.EditClick] upon password
     * validation.
     */
    @Parcelize
    data object EditClick : PasswordRepromptAction() {
        override val vaultItemAction: VaultItemAction
            get() = VaultItemAction.Common.EditClick
    }

    /**
     * Indicates that we should launch the [VaultItemAction.ItemType.Login.PasswordHistoryClick]
     * upon password validation.
     */
    @Parcelize
    data object PasswordHistoryClick : PasswordRepromptAction() {
        override val vaultItemAction: VaultItemAction
            get() = VaultItemAction.ItemType.Login.PasswordHistoryClick
    }

    /**
     * Indicates that we should launch the [VaultItemAction.Common.AttachmentsClick] upon password
     * validation.
     */
    @Parcelize
    data object AttachmentsClick : PasswordRepromptAction() {
        override val vaultItemAction: VaultItemAction
            get() = VaultItemAction.Common.AttachmentsClick
    }

    /**
     * Indicates an attachment download was clicked.
     */
    @Parcelize
    data class AttachmentDownloadClick(
        val attachment: VaultItemState.ViewState.Content.Common.AttachmentItem,
    ) : PasswordRepromptAction() {
        override val vaultItemAction: VaultItemAction
            get() = VaultItemAction.Common.AttachmentDownloadClick(attachment)
    }

    /**
     * Indicates that we should launch the [VaultItemAction.Common.CloneClick] upon password
     * validation.
     */
    @Parcelize
    data object CloneClick : PasswordRepromptAction() {
        override val vaultItemAction: VaultItemAction
            get() = VaultItemAction.Common.CloneClick
    }

    /**
     * Indicates that we should launch the [VaultItemAction.Common.MoveToOrganizationClick] upon
     * password validation.
     */
    @Parcelize
    data object MoveToOrganizationClick : PasswordRepromptAction() {
        override val vaultItemAction: VaultItemAction
            get() = VaultItemAction.Common.MoveToOrganizationClick
    }

    /**
     * Indicates that we should launch the [VaultItemAction.Common.ConfirmDeleteClick] upon
     * password validation.
     */
    @Parcelize
    data object DeleteClick : PasswordRepromptAction() {
        override val vaultItemAction: VaultItemAction
            get() = VaultItemAction.Common.DeleteClick
    }

    /**
     * Indicates that we should launch the [VaultItemAction.Internal.CopyValue] upon password
     * validation.
     */
    @Parcelize
    data class CopyClick(
        val value: String,
    ) : PasswordRepromptAction() {
        override val vaultItemAction: VaultItemAction
            get() = VaultItemAction.Internal.CopyValue(
                value = value,
            )
    }

    /**
     * Indicates that we should launch the [VaultItemAction.ItemType.Card.CodeVisibilityClick]
     * upon password validation.
     */
    @Parcelize
    data class ViewCodeClick(
        val isVisible: Boolean,
    ) : PasswordRepromptAction() {
        override val vaultItemAction: VaultItemAction
            get() = VaultItemAction.ItemType.Card.CodeVisibilityClick(isVisible = isVisible)
    }

    /**
     * Indicates that we should launch the [VaultItemAction.ItemType.Card.NumberVisibilityClick]
     * upon password validation.
     */
    @Parcelize
    data class ViewNumberClick(
        val isVisible: Boolean,
    ) : PasswordRepromptAction() {
        override val vaultItemAction: VaultItemAction
            get() = VaultItemAction.ItemType.Card.NumberVisibilityClick(isVisible = isVisible)
    }

    /**
     * Indicates that we should launch the
     * [VaultItemAction.ItemType.Login.PasswordVisibilityClicked] upon password validation.
     */
    @Parcelize
    data class ViewPasswordClick(
        val isVisible: Boolean,
    ) : PasswordRepromptAction() {
        override val vaultItemAction: VaultItemAction
            get() = VaultItemAction.ItemType.Login.PasswordVisibilityClicked(
                isVisible = isVisible,
            )
    }

    /**
     * Indicates that we should launch the [VaultItemAction.Common.HiddenFieldVisibilityClicked]
     * upon password validation.
     */
    @Parcelize
    data class ViewHiddenFieldClicked(
        val field: VaultItemState.ViewState.Content.Common.Custom.HiddenField,
        val isVisible: Boolean,
    ) : PasswordRepromptAction() {
        override val vaultItemAction: VaultItemAction
            get() = VaultItemAction.Common.HiddenFieldVisibilityClicked(
                field = this.field,
                isVisible = this.isVisible,
            )
    }

    /**
     * Indicates that we should show the confirm restore
     */
    @Parcelize
    data object RestoreItemClick : PasswordRepromptAction() {
        override val vaultItemAction: VaultItemAction
            get() = VaultItemAction.Common.RestoreVaultItemClick
    }

    /**
     * Indicates that we should launch the
     * [VaultItemAction.ItemType.SshKey.PrivateKeyVisibilityClicked] upon password validation.
     */
    @Parcelize
    data class ViewPrivateKeyClicked(
        val isVisible: Boolean,
    ) : PasswordRepromptAction() {
        override val vaultItemAction: VaultItemAction
            get() = VaultItemAction.ItemType.SshKey.PrivateKeyVisibilityClicked(
                isVisible = isVisible,
            )
    }
}
