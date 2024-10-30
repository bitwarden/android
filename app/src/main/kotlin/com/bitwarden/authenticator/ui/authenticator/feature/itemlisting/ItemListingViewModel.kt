package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting

import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.bitwarden.authenticator.R
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
import com.bitwarden.authenticator.data.platform.repository.model.DataState
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model.SharedCodesDisplayState
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model.VerificationCodeDisplayItem
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.util.toDisplayItem
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.util.toSharedCodesDisplayState
import com.bitwarden.authenticator.ui.platform.base.BaseViewModel
import com.bitwarden.authenticator.ui.platform.base.util.Text
import com.bitwarden.authenticator.ui.platform.base.util.asText
import com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.authenticatorbridge.manager.AuthenticatorBridgeManager
import dagger.hilt.android.lifecycle.HiltViewModel
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
) : BaseViewModel<ItemListingState, ItemListingEvent, ItemListingAction>(
    initialState = ItemListingState(
        settingsRepository.appTheme,
        settingsRepository.authenticatorAlertThresholdSeconds,
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

        settingsRepository
            .appThemeStateFlow
            .map { ItemListingAction.Internal.AppThemeChangeReceive(it) }
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

            is ItemListingAction.DeleteItemClick -> {
                handleDeleteItemClick(action)
            }

            is ItemListingAction.ConfirmDeleteClick -> {
                handleConfirmDeleteClick(action)
            }

            is ItemListingAction.SearchClick -> {
                sendEvent(ItemListingEvent.NavigateToSearch)
            }

            is ItemListingAction.ItemClick -> {
                handleItemClick(action)
            }

            is ItemListingAction.EditItemClick -> {
                handleEditItemClick(action)
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

            is ItemListingAction.MoveToBitwardenClick -> {
                handleMoveToBitwardenClick(action)
            }
        }
    }

    private fun handleSettingsClick() {
        sendEvent(ItemListingEvent.NavigateToAppSettings)
    }

    private fun handleItemClick(action: ItemListingAction.ItemClick) {
        clipboardManager.setText(action.authCode)
        sendEvent(
            ItemListingEvent.ShowToast(
                message = R.string.value_has_been_copied.asText(action.authCode),
            ),
        )
    }

    private fun handleEditItemClick(action: ItemListingAction.EditItemClick) {
        sendEvent(ItemListingEvent.NavigateToEditItem(action.itemId))
    }

    private fun handleMoveToBitwardenClick(action: ItemListingAction.MoveToBitwardenClick) {
        viewModelScope.launch {
            val item = authenticatorRepository
                .getItemStateFlow(action.entityId)
                .first { it.data != null }

            val didLaunchAddTotpFlow = authenticatorBridgeManager.startAddTotpLoginItemFlow(
                totpUri = item.data!!.toOtpAuthUriString(),
            )
            if (!didLaunchAddTotpFlow) {
                mutableStateFlow.update {
                    it.copy(
                        dialog = ItemListingState.DialogState.Error(
                            title = R.string.something_went_wrong.asText(),
                            message = R.string.please_try_again.asText(),
                        ),
                    )
                }
            }
        }
    }

    private fun handleDeleteItemClick(action: ItemListingAction.DeleteItemClick) {
        mutableStateFlow.update {
            it.copy(
                dialog = ItemListingState.DialogState.DeleteConfirmationPrompt(
                    message = R.string.do_you_really_want_to_permanently_delete_cipher.asText(),
                    itemId = action.itemId,
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

            is ItemListingAction.Internal.AppThemeChangeReceive -> {
                handleAppThemeChangeReceive(internalAction.appTheme)
            }

            ItemListingAction.Internal.FirstTimeUserSyncReceive -> {
                handleFirstTimeUserSync()
            }
        }
    }

    private fun handleFirstTimeUserSync() {
        sendEvent(ItemListingEvent.ShowFirstTimeSyncSnackbar)
    }

    private fun handleAppThemeChangeReceive(appTheme: AppTheme) {
        mutableStateFlow.update {
            it.copy(appTheme = appTheme)
        }
    }

    private fun handleDeleteItemReceive(result: DeleteItemResult) {
        when (result) {
            DeleteItemResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialog = ItemListingState.DialogState.Error(
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.generic_error_message.asText(),
                        ),
                    )
                }
            }

            DeleteItemResult.Success -> {
                mutableStateFlow.update {
                    it.copy(dialog = null)
                }
                sendEvent(
                    ItemListingEvent.ShowToast(
                        message = R.string.item_deleted.asText(),
                    ),
                )
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
                            title = R.string.an_error_has_occurred.asText(),
                            message = R.string.authenticator_key_read_error.asText(),
                        ),
                    )
                }
            }

            CreateItemResult.Success -> {
                sendEvent(
                    event = ItemListingEvent.ShowToast(
                        message = R.string.verification_code_added.asText(),
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
                -> SharedCodesDisplayState.Codes(emptyList())

            is SharedVerificationCodesState.Success ->
                action.sharedCodesState.toSharedCodesDisplayState(
                    alertThresholdSeconds = state.alertThresholdSeconds,
                )
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
                            sharedVerificationCodesState =
                            authenticatorRepository.sharedCodesStateFlow.value,
                        )
                    },
                itemList = localItems
                    .filter { it.source is AuthenticatorItem.Source.Local && !it.source.isFavorite }
                    .map {
                        it.toDisplayItem(
                            alertThresholdSeconds = state.alertThresholdSeconds,
                            sharedVerificationCodesState =
                            authenticatorRepository.sharedCodesStateFlow.value,
                        )
                    },
                sharedItems = sharedItemsState,
                actionCard = action.sharedCodesState.toActionCard(),
            )
            mutableStateFlow.update { it.copy(viewState = viewState) }
        }
    }

    private fun handleDownloadBitwardenClick() {
        sendEvent(ItemListingEvent.NavigateToBitwardenListing)
    }

    private fun handleDownloadBitwardenDismiss() {
        settingsRepository.hasUserDismissedDownloadBitwardenCard = true
        mutableStateFlow.update {
            it.copy(
                viewState = when (it.viewState) {
                    ItemListingState.ViewState.Loading -> it.viewState
                    is ItemListingState.ViewState.Content -> it.viewState.copy(
                        actionCard = ItemListingState.ActionCardState.None,
                    )

                    is ItemListingState.ViewState.NoItems -> it.viewState.copy(
                        actionCard = ItemListingState.ActionCardState.None,
                    )
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
                    is ItemListingState.ViewState.Content -> it.viewState.copy(
                        actionCard = ItemListingState.ActionCardState.None,
                    )

                    is ItemListingState.ViewState.NoItems -> it.viewState.copy(
                        actionCard = ItemListingState.ActionCardState.None,
                    )
                },
            )
        }
    }

    /**
     * Converts a [SharedVerificationCodesState] into an action card for display.
     */
    private fun SharedVerificationCodesState.toActionCard(): ItemListingState.ActionCardState =
        when (this) {
            SharedVerificationCodesState.AppNotInstalled ->
                if (!settingsRepository.hasUserDismissedDownloadBitwardenCard) {
                    ItemListingState.ActionCardState.DownloadBitwardenApp
                } else {
                    ItemListingState.ActionCardState.None
                }

            SharedVerificationCodesState.SyncNotEnabled ->
                if (!settingsRepository.hasUserDismissedSyncWithBitwardenCard) {
                    ItemListingState.ActionCardState.SyncWithBitwarden
                } else {
                    ItemListingState.ActionCardState.None
                }

            SharedVerificationCodesState.Error,
            SharedVerificationCodesState.FeatureNotEnabled,
            SharedVerificationCodesState.Loading,
            SharedVerificationCodesState.OsVersionNotSupported,
            is SharedVerificationCodesState.Success,
                -> ItemListingState.ActionCardState.None
        }

    private fun String.toAuthenticatorEntityOrNull(): AuthenticatorItemEntity? {
        val uri = Uri.parse(this)

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
    val appTheme: AppTheme,
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
            val actionCard: ActionCardState,
        ) : ViewState()

        /**
         * Represents a loaded content state for the [ItemListingScreen].
         */
        @Parcelize
        data class Content(
            val actionCard: ActionCardState,
            val favoriteItems: List<VerificationCodeDisplayItem>,
            val itemList: List<VerificationCodeDisplayItem>,
            val sharedItems: SharedCodesDisplayState,
        ) : ViewState()
    }

    /**
     * Display an action card on the item [ItemListingScreen].
     */
    sealed class ActionCardState : Parcelable {

        /**
         * Display no action card.
         */
        @Parcelize
        data object None : ActionCardState()

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
     * Navigate to Bitwarden play store listing.
     */
    data object NavigateToBitwardenListing : ItemListingEvent()

    /**
     * Navigate to Bitwarden account security settings.
     */
    data object NavigateToBitwardenSettings : ItemListingEvent()

    /**
     * Show a Toast with [message].
     */
    data class ShowToast(
        val message: Text,
    ) : ItemListingEvent()

    /**
     * Show a Snackbar letting the user know accounts have synced.
     */
    data object ShowFirstTimeSyncSnackbar : ItemListingEvent()
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
     * The user clicked edit item.
     */
    data class EditItemClick(val itemId: String) : ItemListingAction()

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
     * The user dismissed sync Bitwarden action card.
     */
    data object SyncWithBitwardenDismiss : ItemListingAction()

    /**
     * The user clicked the "Move to Bitwarden" action on a local verification item.
     */
    data class MoveToBitwardenClick(val entityId: String) : ItemListingAction()

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
         * Indicates app theme change has been received.
         */
        data class AppThemeChangeReceive(val appTheme: AppTheme) : Internal()

        /**
         * Indicates that a user synced with Bitwarden for the first time.
         */
        data object FirstTimeUserSyncReceive : Internal()
    }

    /**
     * The user clicked Delete.
     */
    data class DeleteItemClick(val itemId: String) : ItemListingAction()

    /**
     * The user clicked confirm when prompted to delete an item.
     */
    data class ConfirmDeleteClick(val itemId: String) : ItemListingAction()
}
