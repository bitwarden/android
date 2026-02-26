package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import android.os.Parcelable
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemAlgorithm
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemEntity
import com.bitwarden.authenticator.data.authenticator.datasource.disk.entity.AuthenticatorItemType
import com.bitwarden.authenticator.data.authenticator.manager.TotpCodeManager
import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.authenticator.repository.model.AuthenticatorItem
import com.bitwarden.authenticator.data.authenticator.repository.model.CreateItemResult
import com.bitwarden.authenticator.data.authenticator.repository.model.DeleteItemResult
import com.bitwarden.authenticator.data.authenticator.repository.model.SharedVerificationCodesState
import com.bitwarden.authenticator.data.authenticator.repository.model.TotpCodeResult
import com.bitwarden.authenticator.data.platform.manager.BitwardenEncodingManager
import com.bitwarden.authenticator.data.platform.manager.clipboard.BitwardenClipboardManager
import com.bitwarden.authenticator.data.platform.manager.imports.model.GoogleAuthenticatorProtos
import com.bitwarden.authenticator.data.platform.repository.SettingsRepository
import com.bitwarden.authenticator.ui.authenticator.feature.util.toDisplayItem
import com.bitwarden.authenticator.ui.authenticator.feature.util.toSharedCodesDisplayState
import com.bitwarden.authenticator.ui.platform.components.listitem.model.SharedCodesDisplayState
import com.bitwarden.authenticator.ui.platform.components.listitem.model.VaultDropdownMenuAction
import com.bitwarden.authenticator.ui.platform.components.listitem.model.VerificationCodeDisplayItem
import com.bitwarden.authenticator.ui.platform.model.SnackbarRelay
import com.bitwarden.authenticatorbridge.manager.AuthenticatorBridgeManager
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.manager.snackbar.SnackbarRelayManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.util.UUID
import javax.inject.Inject

/**
 * View model responsible for handling user interactions with the item listing screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class ItemListingViewModel @Inject constructor(
    private val authenticatorRepository: AuthenticatorRepository,
    private val authenticatorBridgeManager: AuthenticatorBridgeManager,
    private val clipboardManager: BitwardenClipboardManager,
    private val encodingManager: BitwardenEncodingManager,
    private val settingsRepository: SettingsRepository,
    snackbarRelayManager: SnackbarRelayManager<SnackbarRelay>,
) : BaseViewModel<ItemListingState, ItemListingEvent, ItemListingAction>(
    initialState = ItemListingState(
        alertThresholdSeconds = settingsRepository.authenticatorAlertThresholdSeconds,
        viewState = ItemListingState.ViewState.Loading,
        dialog = null,
    ),
) {

    init {
        settingsRepository
            .authenticatorAlertThresholdSecondsFlow
            .map { ItemListingAction.Internal.AlertThresholdSecondsReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        combine(
            flow = authenticatorRepository.getLocalVerificationCodesFlow(),
            flow2 = authenticatorRepository.sharedCodesStateFlow,
            ItemListingAction.Internal::AuthCodesUpdated,
        )
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        authenticatorRepository
            .totpCodeFlow
            .map { ItemListingAction.Internal.TotpCodeReceive(totpResult = it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        authenticatorRepository
            .firstTimeAccountSyncFlow
            .map { ItemListingAction.Internal.FirstTimeUserSyncReceive }
            .onEach(::sendAction)
            .launchIn(viewModelScope)

        snackbarRelayManager
            .getSnackbarDataFlow(SnackbarRelay.ITEM_SAVED, SnackbarRelay.ITEM_ADDED)
            .map(ItemListingEvent::ShowSnackbar)
            .onEach(::sendEvent)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: ItemListingAction) {
        when (action) {
            ItemListingAction.ScanQrCodeClick -> {
                sendEvent(ItemListingEvent.NavigateToQrCodeScanner)
            }

            ItemListingAction.EnterSetupKeyClick -> {
                sendEvent(ItemListingEvent.NavigateToManualAddItem)
            }

            ItemListingAction.BackClick -> {
                sendEvent(ItemListingEvent.NavigateBack)
            }

            is ItemListingAction.ConfirmDeleteClick -> {
                handleConfirmDeleteClick(action)
            }

            is ItemListingAction.SearchClick -> {
                sendEvent(ItemListingEvent.NavigateToSearch)
            }

            is ItemListingAction.ItemClick -> {
                handleCopyCodeClick(action.authCode)
            }

            is ItemListingAction.DialogDismiss -> {
                handleDialogDismiss()
            }

            is ItemListingAction.SettingsClick -> {
                handleSettingsClick()
            }

            is ItemListingAction.Internal -> {
                handleInternalAction(action)
            }

            is ItemListingAction.DropdownMenuClick -> {
                handleDropdownMenuClick(action)
            }

            ItemListingAction.DownloadBitwardenClick -> {
                handleDownloadBitwardenClick()
            }

            ItemListingAction.DownloadBitwardenDismiss -> {
                handleDownloadBitwardenDismiss()
            }

            ItemListingAction.SyncWithBitwardenClick -> {
                handleSyncWithBitwardenClick()
            }

            ItemListingAction.SyncWithBitwardenDismiss -> {
                handleSyncWithBitwardenDismiss()
            }

            ItemListingAction.SyncLearnMoreClick -> {
                handleSyncLearnMoreClick()
            }

            is ItemListingAction.SectionExpandedClick -> {
                handleSectionExpandedClick(action)
            }
        }
    }

    private fun handleSettingsClick() {
        sendEvent(ItemListingEvent.NavigateToAppSettings)
    }

    private fun handleCopyCodeClick(authCode: String) {
        clipboardManager.setText(authCode)
    }

    private fun handleEditItemClick(itemId: String) {
        sendEvent(ItemListingEvent.NavigateToEditItem(itemId))
    }

    private fun handleCopyToBitwardenClick(itemId: String) {
        viewModelScope.launch {
            val item = authenticatorRepository
                .getItemStateFlow(itemId)
                .first { it.data != null }

            val didLaunchAddTotpFlow = authenticatorBridgeManager.startAddTotpLoginItemFlow(
                totpUri = item.data?.toOtpAuthUriString().orEmpty(),
            )
            if (!didLaunchAddTotpFlow) {
                mutableStateFlow.update {
                    it.copy(
                        dialog = ItemListingState.DialogState.Error(
                            title = BitwardenString.something_went_wrong.asText(),
                            message = BitwardenString.please_try_again.asText(),
                        ),
                    )
                }
            }
        }
    }

    private fun handleDeleteItemClick(itemId: String) {
        mutableStateFlow.update {
            it.copy(
                dialog = ItemListingState.DialogState.DeleteConfirmationPrompt(
                    message = BitwardenString
                        .do_you_really_want_to_permanently_delete_this_cannot_be_undone
                        .asText(),
                    itemId = itemId,
                ),
            )
        }
    }

    private fun handleConfirmDeleteClick(action: ItemListingAction.ConfirmDeleteClick) {
        mutableStateFlow.update {
            it.copy(
                dialog = ItemListingState.DialogState.Loading,
            )
        }

        viewModelScope.launch {
            trySendAction(
                ItemListingAction.Internal.DeleteItemReceive(
                    authenticatorRepository.hardDeleteItem(action.itemId),
                ),
            )
        }
    }

    private fun handleInternalAction(internalAction: ItemListingAction.Internal) {
        when (internalAction) {
            is ItemListingAction.Internal.AuthCodesUpdated -> {
                handleAuthenticatorDataReceive(internalAction)
            }

            is ItemListingAction.Internal.AlertThresholdSecondsReceive -> {
                handleAlertThresholdSecondsReceive(internalAction)
            }

            is ItemListingAction.Internal.TotpCodeReceive -> {
                handleTotpCodeReceive(internalAction)
            }

            is ItemListingAction.Internal.CreateItemResultReceive -> {
                handleCreateItemResultReceive(internalAction)
            }

            is ItemListingAction.Internal.DeleteItemReceive -> {
                handleDeleteItemReceive(internalAction.result)
            }

            ItemListingAction.Internal.FirstTimeUserSyncReceive -> {
                handleFirstTimeUserSync()
            }
        }
    }

    private fun handleFirstTimeUserSync() {
        sendEvent(
            event = ItemListingEvent.ShowSnackbar(
                message = BitwardenString.account_synced_from_bitwarden_app.asText(),
                withDismissAction = true,
            ),
        )
    }

    private fun handleDeleteItemReceive(result: DeleteItemResult) {
        when (result) {
            DeleteItemResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = ItemListingState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.generic_error_message.asText(),
                        ),
                    )
                }
            }

            DeleteItemResult.Success -> {
                mutableStateFlow.update {
                    it.copy(dialog = null)
                }
                sendEvent(ItemListingEvent.ShowSnackbar(BitwardenString.item_deleted.asText()))
            }
        }
    }

    private fun handleCreateItemResultReceive(
        action: ItemListingAction.Internal.CreateItemResultReceive,
    ) {
        mutableStateFlow.update { it.copy(dialog = null) }

        when (action.result) {
            CreateItemResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = ItemListingState.DialogState.Error(
                            title = BitwardenString.an_error_has_occurred.asText(),
                            message = BitwardenString.authenticator_key_read_error.asText(),
                        ),
                    )
                }
            }

            CreateItemResult.Success -> {
                sendEvent(
                    event = ItemListingEvent.ShowSnackbar(
                        message = BitwardenString.verification_code_added.asText(),
                    ),
                )
            }
        }
    }

    private fun handleTotpCodeReceive(action: ItemListingAction.Internal.TotpCodeReceive) {
        mutableStateFlow.update { it.copy(viewState = ItemListingState.ViewState.Loading) }

        viewModelScope.launch {
            when (val totpResult = action.totpResult) {
                TotpCodeResult.CodeScanningError -> {
                    handleCodeScanningErrorReceive()
                }

                is TotpCodeResult.TotpCodeScan -> {
                    handleTotpCodeScanReceive(totpResult)
                }

                is TotpCodeResult.GoogleExportScan -> {
                    handleGoogleExportScan(totpResult)
                }
            }
        }
    }

    private suspend fun handleTotpCodeScanReceive(
        totpResult: TotpCodeResult.TotpCodeScan,
    ) {
        val item = totpResult.code.toAuthenticatorEntityOrNull()
            ?: run {
                handleCodeScanningErrorReceive()
                return
            }

        val result = authenticatorRepository.createItem(item)
        sendAction(ItemListingAction.Internal.CreateItemResultReceive(result))
    }

    private suspend fun handleGoogleExportScan(
        totpResult: TotpCodeResult.GoogleExportScan,
    ) {
        val base64EncodedMigrationData = encodingManager.uriDecode(
            value = totpResult.data,
        )

        val decodedMigrationData = encodingManager.base64Decode(
            value = base64EncodedMigrationData,
        )

        val payload = GoogleAuthenticatorProtos.MigrationPayload
            .parseFrom(decodedMigrationData)

        val entries = payload
            .otpParametersList
            .mapNotNull { otpParam ->
                val secret = encodingManager.base32Encode(
                    byteArray = otpParam.secret.toByteArray(),
                )

                // Google Authenticator only supports TOTP and HOTP codes. We do not support HOTP
                // codes so we skip over codes that are not TOTP.
                val type = when (otpParam.type) {
                    GoogleAuthenticatorProtos.MigrationPayload.OtpType.OTP_TOTP -> {
                        AuthenticatorItemType.TOTP
                    }

                    else -> return@mapNotNull null
                }

                // Google Authenticator does not always provide a valid digits value so we double
                // check it and fallback to the default value if it is not within our valid range.
                val digits = if (otpParam.digits in TotpCodeManager.TOTP_DIGITS_RANGE) {
                    otpParam.digits
                } else {
                    TotpCodeManager.TOTP_DIGITS_DEFAULT
                }

                // Google Authenticator only supports SHA1 algorithms.
                val algorithm = AuthenticatorItemAlgorithm.SHA1

                // Google Authenticator ignores period so we always set it to our default.
                val period = TotpCodeManager.PERIOD_SECONDS_DEFAULT

                val accountName: String = when {
                    otpParam.issuer.isNullOrEmpty().not() &&
                        otpParam.name.startsWith("${otpParam.issuer}:") -> {
                        otpParam.name.replace("${otpParam.issuer}:", "")
                    }

                    else -> otpParam.name
                }

                // If the issuer is not provided fallback to the token name since issuer is required
                // in our database
                val issuer = when {
                    otpParam.issuer.isNullOrEmpty() -> otpParam.name
                    else -> otpParam.issuer
                }

                AuthenticatorItemEntity(
                    id = UUID.randomUUID().toString(),
                    key = secret,
                    type = type,
                    algorithm = algorithm,
                    period = period,
                    digits = digits,
                    issuer = issuer,
                    accountName = accountName,
                    userId = null,
                    favorite = false,
                )
            }

        val result = authenticatorRepository.addItems(*entries.toTypedArray())
        sendAction(ItemListingAction.Internal.CreateItemResultReceive(result))
    }

    private suspend fun handleCodeScanningErrorReceive() {
        sendAction(
            action = ItemListingAction.Internal.CreateItemResultReceive(
                result = CreateItemResult.Error,
            ),
        )
    }

    private fun handleAlertThresholdSecondsReceive(
        action: ItemListingAction.Internal.AlertThresholdSecondsReceive,
    ) {
        mutableStateFlow.update {
            it.copy(
                alertThresholdSeconds = action.thresholdSeconds,
            )
        }
    }

    private fun handleDialogDismiss() {
        mutableStateFlow.update {
            it.copy(dialog = null)
        }
    }

    @Suppress("LongMethod")
    private fun handleAuthenticatorDataReceive(
        action: ItemListingAction.Internal.AuthCodesUpdated,
    ) {
        val localItems = action.localCodes.data ?: run {
            // If local items haven't loaded from DB, show Loading:
            mutableStateFlow.update {
                it.copy(
                    viewState = ItemListingState.ViewState.Loading,
                )
            }
            return
        }
        val sharedItemsState: SharedCodesDisplayState = when (action.sharedCodesState) {
            SharedVerificationCodesState.Error -> SharedCodesDisplayState.Error
            SharedVerificationCodesState.AppNotInstalled,
            SharedVerificationCodesState.FeatureNotEnabled,
            SharedVerificationCodesState.Loading,
            SharedVerificationCodesState.OsVersionNotSupported,
            SharedVerificationCodesState.SyncNotEnabled,
                -> SharedCodesDisplayState.Codes(persistentListOf())

            is SharedVerificationCodesState.Success -> {
                val viewState = state.viewState as? ItemListingState.ViewState.Content
                val currentCodes = viewState?.sharedItems as? SharedCodesDisplayState.Codes
                action.sharedCodesState.toSharedCodesDisplayState(
                    alertThresholdSeconds = state.alertThresholdSeconds,
                    currentSections = currentCodes?.sections.orEmpty(),
                )
            }
        }

        if (localItems.isEmpty() && sharedItemsState.isEmpty()) {
            // If there are no items, show empty state:
            mutableStateFlow.update {
                it.copy(
                    viewState = ItemListingState.ViewState.NoItems(
                        actionCard = action.sharedCodesState.toActionCard(),
                    ),
                )
            }
        } else {
            val viewState = ItemListingState.ViewState.Content(
                favoriteItems = localItems
                    .filter { it.source is AuthenticatorItem.Source.Local && it.source.isFavorite }
                    .map {
                        it.toDisplayItem(
                            alertThresholdSeconds = state.alertThresholdSeconds,
                            sharedVerificationCodesState = authenticatorRepository
                                .sharedCodesStateFlow
                                .value,
                            showOverflow = true,
                        )
                    }
                    .toImmutableList(),
                itemList = localItems
                    .filter { it.source is AuthenticatorItem.Source.Local && !it.source.isFavorite }
                    .map {
                        it.toDisplayItem(
                            alertThresholdSeconds = state.alertThresholdSeconds,
                            sharedVerificationCodesState = authenticatorRepository
                                .sharedCodesStateFlow
                                .value,
                            showOverflow = true,
                        )
                    }
                    .toImmutableList(),
                sharedItems = sharedItemsState,
                actionCard = action.sharedCodesState.toActionCard(),
            )
            mutableStateFlow.update { it.copy(viewState = viewState) }
        }
    }

    private fun handleDownloadBitwardenClick() {
        sendEvent(ItemListingEvent.NavigateToBitwardenListing)
    }

    private fun handleDropdownMenuClick(action: ItemListingAction.DropdownMenuClick) {
        when (action.menuAction) {
            VaultDropdownMenuAction.COPY_CODE -> handleCopyCodeClick(action.item.authCode)
            VaultDropdownMenuAction.EDIT -> handleEditItemClick(action.item.id)
            VaultDropdownMenuAction.COPY_TO_BITWARDEN -> handleCopyToBitwardenClick(action.item.id)
            VaultDropdownMenuAction.DELETE -> handleDeleteItemClick(action.item.id)
        }
    }

    private fun handleDownloadBitwardenDismiss() {
        settingsRepository.hasUserDismissedDownloadBitwardenCard = true
        mutableStateFlow.update {
            it.copy(
                viewState = when (it.viewState) {
                    ItemListingState.ViewState.Loading -> it.viewState
                    is ItemListingState.ViewState.Content -> it.viewState.copy(actionCard = null)
                    is ItemListingState.ViewState.NoItems -> it.viewState.copy(actionCard = null)
                },
            )
        }
    }

    private fun handleSyncWithBitwardenClick() {
        sendEvent(ItemListingEvent.NavigateToBitwardenSettings)
    }

    private fun handleSyncWithBitwardenDismiss() {
        settingsRepository.hasUserDismissedSyncWithBitwardenCard = true
        mutableStateFlow.update {
            it.copy(
                viewState = when (it.viewState) {
                    ItemListingState.ViewState.Loading -> it.viewState
                    is ItemListingState.ViewState.Content -> it.viewState.copy(actionCard = null)
                    is ItemListingState.ViewState.NoItems -> it.viewState.copy(actionCard = null)
                },
            )
        }
    }

    private fun handleSyncLearnMoreClick() {
        sendEvent(ItemListingEvent.NavigateToSyncInformation)
    }

    private fun handleSectionExpandedClick(action: ItemListingAction.SectionExpandedClick) {
        updateSharedItems { codes ->
            codes.copy(
                sections = codes
                    .sections
                    .map {
                        it.copy(
                            isExpanded = if (it == action.section) {
                                !it.isExpanded
                            } else {
                                it.isExpanded
                            },
                        )
                    }
                    .toImmutableList(),
            )
        }
    }

    /**
     * Converts a [SharedVerificationCodesState] into an action card for display.
     */
    private fun SharedVerificationCodesState.toActionCard(): ItemListingState.ActionCardState? =
        when (this) {
            SharedVerificationCodesState.AppNotInstalled ->
                if (!settingsRepository.hasUserDismissedDownloadBitwardenCard) {
                    ItemListingState.ActionCardState.DownloadBitwardenApp
                } else {
                    null
                }

            SharedVerificationCodesState.SyncNotEnabled ->
                if (!settingsRepository.hasUserDismissedSyncWithBitwardenCard) {
                    ItemListingState.ActionCardState.SyncWithBitwarden
                } else {
                    null
                }

            SharedVerificationCodesState.Error,
            SharedVerificationCodesState.FeatureNotEnabled,
            SharedVerificationCodesState.Loading,
            SharedVerificationCodesState.OsVersionNotSupported,
            is SharedVerificationCodesState.Success,
                -> null
        }

    private fun String.toAuthenticatorEntityOrNull(): AuthenticatorItemEntity? {
        val uri = this.toUri()

        val type = AuthenticatorItemType
            .entries
            .find { it.name.lowercase() == uri.host }
            ?: return null

        val label = uri.pathSegments.firstOrNull() ?: return null
        val accountName = if (label.contains(":")) {
            label
                .split(":")
                .last()
        } else {
            label
        }

        val key = uri.getQueryParameter(SECRET) ?: return null

        val algorithm = AuthenticatorItemAlgorithm
            .entries
            .find { it.name == uri.getQueryParameter(ALGORITHM) }
            ?: AuthenticatorItemAlgorithm.SHA1

        val digits = uri.getQueryParameter(DIGITS)?.toIntOrNull()
            ?: TotpCodeManager.TOTP_DIGITS_DEFAULT

        val issuer = uri.getQueryParameter(ISSUER)
            ?: label

        val period = uri.getQueryParameter(PERIOD)?.toIntOrNull()
            ?: TotpCodeManager.PERIOD_SECONDS_DEFAULT

        return AuthenticatorItemEntity(
            id = UUID.randomUUID().toString(),
            key = key,
            accountName = accountName,
            type = type,
            algorithm = algorithm,
            period = period,
            digits = digits,
            issuer = issuer,
            userId = null,
            favorite = false,
        )
    }

    private inline fun updateContent(
        crossinline block: (
            ItemListingState.ViewState.Content,
        ) -> ItemListingState.ViewState.Content,
    ) {
        val updatedContent = (state.viewState as? ItemListingState.ViewState.Content)
            ?.let(block)
            ?: return
        mutableStateFlow.update { it.copy(viewState = updatedContent) }
    }

    private inline fun updateSharedItems(
        crossinline block: (SharedCodesDisplayState.Codes) -> SharedCodesDisplayState.Codes,
    ) {
        updateContent {
            it.copy(
                sharedItems = (it.sharedItems as? SharedCodesDisplayState.Codes)
                    ?.let(block)
                    ?: it.sharedItems,
            )
        }
    }
}

const val ALGORITHM = "algorithm"
const val DIGITS = "digits"
const val PERIOD = "period"
const val SECRET = "secret"
const val ISSUER = "issuer"

/**
 * Represents the state for displaying the item listing.
 *
 * @property viewState Current state of the [ItemListingScreen].
 * @property dialog State of the dialog show on the [ItemListingScreen]. `null` if no dialog is
 * shown.
 */
@Parcelize
data class ItemListingState(
    val alertThresholdSeconds: Int,
    val viewState: ViewState,
    val dialog: DialogState?,
) : Parcelable {
    /**
     * Represents the different view states of the [ItemListingScreen].
     */
    @Parcelize
    sealed class ViewState : Parcelable {

        /**
         * Represents the [ItemListingScreen] data is processing.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * Represents a state where the [ItemListingScreen] has no items to display.
         */
        @Parcelize
        data class NoItems(
            val actionCard: ActionCardState?,
        ) : ViewState()

        /**
         * Represents a loaded content state for the [ItemListingScreen].
         */
        @Parcelize
        data class Content(
            val actionCard: ActionCardState?,
            val favoriteItems: ImmutableList<VerificationCodeDisplayItem>,
            val itemList: ImmutableList<VerificationCodeDisplayItem>,
            val sharedItems: SharedCodesDisplayState,
        ) : ViewState() {

            /**
             * Whether or not there should be a "Local codes" header shown above local codes.
             */
            val shouldShowLocalHeader
                get() =
                    // Only show if local codes are present
                    itemList.isNotEmpty() &&
                        // and if there are shared items or favorites
                        (!sharedItems.isEmpty() || favoriteItems.isNotEmpty())
        }
    }

    /**
     * Display an action card on the item [ItemListingScreen].
     */
    sealed class ActionCardState : Parcelable {
        /**
         * Display the "Download the Bitwarden app" card.
         */
        @Parcelize
        data object DownloadBitwardenApp : ActionCardState()

        /**
         * Display the "Sync with the Bitwarden app" card.
         */
        @Parcelize
        data object SyncWithBitwarden : ActionCardState()
    }

    /**
     * Display a dialog on the [ItemListingScreen].
     */
    sealed class DialogState : Parcelable {
        /**
         * Displays the loading dialog to the user.
         */
        @Parcelize
        data object Loading : DialogState()

        /**
         * Displays a generic error dialog to the user.
         */
        @Parcelize
        data class Error(
            val title: Text,
            val message: Text,
        ) : DialogState()

        /**
         * Displays a prompt to confirm item deletion.
         */
        @Parcelize
        data class DeleteConfirmationPrompt(
            val message: Text,
            val itemId: String,
        ) : DialogState()
    }
}

/**
 * Represents a set of events related to viewing the item listing.
 */
sealed class ItemListingEvent {
    /**
     * Navigates to the Create Account screen.
     */
    data object NavigateBack : ItemListingEvent()

    /**
     * Navigates to the Search screen.
     */
    data object NavigateToSearch : ItemListingEvent()

    /**
     * Navigate to the QR Code Scanner screen.
     */
    data object NavigateToQrCodeScanner : ItemListingEvent()

    /**
     * Navigate to the Manual Add Item screen.
     */
    data object NavigateToManualAddItem : ItemListingEvent()

    /**
     * Navigate to the Edit Item screen.
     */
    data class NavigateToEditItem(
        val id: String,
    ) : ItemListingEvent()

    /**
     * Navigate to the app settings.
     */
    data object NavigateToAppSettings : ItemListingEvent()

    /**
     * Navigate to the sync information web page.
     */
    data object NavigateToSyncInformation : ItemListingEvent()

    /**
     * Navigate to Bitwarden play store listing.
     */
    data object NavigateToBitwardenListing : ItemListingEvent()

    /**
     * Navigate to Bitwarden account security settings.
     */
    data object NavigateToBitwardenSettings : ItemListingEvent()

    /**
     * Show a Snackbar with the given [data].
     */
    data class ShowSnackbar(
        val data: BitwardenSnackbarData,
    ) : ItemListingEvent(), BackgroundEvent {
        constructor(
            message: Text,
            messageHeader: Text? = null,
            actionLabel: Text? = null,
            withDismissAction: Boolean = false,
        ) : this(
            data = BitwardenSnackbarData(
                message = message,
                messageHeader = messageHeader,
                actionLabel = actionLabel,
                withDismissAction = withDismissAction,
            ),
        )
    }
}

/**
 * Represents a set of actions related to viewing the authenticator item listing.
 * Each subclass of this sealed class denotes a distinct action that can be taken.
 */
sealed class ItemListingAction {
    /**
     * The user clicked the back button.
     */
    data object BackClick : ItemListingAction()

    /**
     * The user has clicked the search button.
     */
    data object SearchClick : ItemListingAction()

    /**
     * The user clicked the Scan QR Code button.
     */
    data object ScanQrCodeClick : ItemListingAction()

    /**
     * The user clicked the Enter Setup Key button.
     */
    data object EnterSetupKeyClick : ItemListingAction()

    /**
     * The user clicked a list item to copy its auth code.
     */
    data class ItemClick(val authCode: String) : ItemListingAction()

    /**
     * The user dismissed the dialog.
     */
    data object DialogDismiss : ItemListingAction()

    /**
     * The user has clicked the settings button
     */
    data object SettingsClick : ItemListingAction()

    /**
     * The user tapped download Bitwarden action card.
     */
    data object DownloadBitwardenClick : ItemListingAction()

    /**
     * The user dismissed download Bitwarden action card.
     */
    data object DownloadBitwardenDismiss : ItemListingAction()

    /**
     * The user tapped sync Bitwarden action card.
     */
    data object SyncWithBitwardenClick : ItemListingAction()

    /**
     * The user tapped the learn more button on the sync action card.
     */
    data object SyncLearnMoreClick : ItemListingAction()

    /**
     * The user tapped the section header to expand or collapse the section.
     */
    data class SectionExpandedClick(
        val section: SharedCodesDisplayState.SharedCodesAccountSection,
    ) : ItemListingAction()

    /**
     * The user dismissed sync Bitwarden action card.
     */
    data object SyncWithBitwardenDismiss : ItemListingAction()

    /**
     * The user clicked confirm when prompted to delete an item.
     */
    data class ConfirmDeleteClick(val itemId: String) : ItemListingAction()

    /**
     * Represents an action triggered when the user clicks an item in the dropdown menu.
     *
     * @param menuAction The action selected from the dropdown menu.
     * @param item The item on which the action is being performed.
     */
    data class DropdownMenuClick(
        val menuAction: VaultDropdownMenuAction,
        val item: VerificationCodeDisplayItem,
    ) : ItemListingAction()

    /**
     * Models actions that [ItemListingScreen] itself may send.
     */
    sealed class Internal : ItemListingAction() {
        /**
         * Indicates verification items have been received.
         */
        data class AuthCodesUpdated(
            val localCodes: DataState<List<VerificationCodeItem>>,
            val sharedCodesState: SharedVerificationCodesState,
        ) : Internal()

        /**
         * Indicates authenticator item alert threshold seconds changes has been received.
         */
        data class AlertThresholdSecondsReceive(
            val thresholdSeconds: Int,
        ) : Internal()

        /**
         * Indicates a new TOTP code scan result has been received.
         */
        data class TotpCodeReceive(val totpResult: TotpCodeResult) : Internal()

        /**
         * Indicates a result for creating and item has been received.
         */
        data class CreateItemResultReceive(val result: CreateItemResult) : Internal()

        /**
         * Indicates a result for deleting an item has been received.
         */
        data class DeleteItemReceive(val result: DeleteItemResult) : Internal()

        /**
         * Indicates that a user synced with Bitwarden for the first time.
         */
        data object FirstTimeUserSyncReceive : Internal()
    }
}
