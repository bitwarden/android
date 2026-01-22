package com.x8bit.bitwarden.ui.vault.feature.itemlisting.handlers

import com.bitwarden.ui.platform.components.account.model.AccountSummary
import com.bitwarden.ui.util.Text
import com.x8bit.bitwarden.ui.vault.components.model.CreateVaultItemType
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.MasterPasswordRepromptData
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingState
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingViewModel
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingsAction
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction

/**
 * A collection of handler functions for managing actions within the context of viewing a list of
 * items.
 */
data class VaultItemListingHandlers(
    val switchAccountClick: (AccountSummary) -> Unit,
    val lockAccountClick: (AccountSummary) -> Unit,
    val logoutAccountClick: (AccountSummary) -> Unit,
    val backClick: () -> Unit,
    val searchIconClick: () -> Unit,
    val addVaultItemClick: () -> Unit,
    val itemClick: (id: String, type: VaultItemListingState.DisplayItem.ItemType) -> Unit,
    val folderClick: (id: String) -> Unit,
    val collectionClick: (id: String) -> Unit,
    val masterPasswordRepromptSubmit: (password: String, MasterPasswordRepromptData) -> Unit,
    val refreshClick: () -> Unit,
    val syncClick: () -> Unit,
    val lockClick: () -> Unit,
    val overflowItemClick: (action: ListingItemOverflowAction) -> Unit,
    val dismissDialogRequest: () -> Unit,
    val dismissCredentialManagerErrorDialog: (Text) -> Unit,
    val confirmOverwriteExistingPasskey: (cipherViewId: String) -> Unit,
    val submitMasterPasswordCredentialVerification: (password: String, cipherId: String) -> Unit,
    val retryGetCredentialPasswordVerification: (cipherId: String) -> Unit,
    val submitPinCredentialVerification: (pin: String, cipherId: String) -> Unit,
    val retryPinCredentialVerification: (cipherViewId: String) -> Unit,
    val submitPinSetUpCredentialVerification: (pin: String, cipherId: String) -> Unit,
    val retryPinSetUpCredentialVerification: (cipherId: String) -> Unit,
    val dismissUserVerification: () -> Unit,
    val vaultItemTypeSelected: (CreateVaultItemType) -> Unit,
    val trustPrivilegedAppClick: (selectedCipherId: String?) -> Unit,
    val shareCipherDecryptionErrorClick: (selectedCipherId: String) -> Unit,
    val upgradeToPremiumClick: () -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [VaultItemListingHandlers] by binding actions to the provided
         * [VaultItemListingViewModel].
         */
        @Suppress("LongMethod")
        fun create(
            viewModel: VaultItemListingViewModel,
        ): VaultItemListingHandlers =
            VaultItemListingHandlers(
                switchAccountClick = {
                    viewModel.trySendAction(VaultItemListingsAction.SwitchAccountClick(it))
                },
                lockAccountClick = {
                    viewModel.trySendAction(VaultItemListingsAction.LockAccountClick(it))
                },
                logoutAccountClick = {
                    viewModel.trySendAction(VaultItemListingsAction.LogoutAccountClick(it))
                },
                backClick = { viewModel.trySendAction(VaultItemListingsAction.BackClick) },
                searchIconClick = {
                    viewModel.trySendAction(VaultItemListingsAction.SearchIconClick)
                },
                addVaultItemClick = {
                    viewModel.trySendAction(VaultItemListingsAction.AddVaultItemClick)
                },
                collectionClick = {
                    viewModel.trySendAction(VaultItemListingsAction.CollectionClick(it))
                },
                itemClick = { id, type ->
                    viewModel.trySendAction(
                        VaultItemListingsAction.ItemClick(id = id, type = type),
                    )
                },
                folderClick = { viewModel.trySendAction(VaultItemListingsAction.FolderClick(it)) },
                masterPasswordRepromptSubmit = { password, data ->
                    viewModel.trySendAction(
                        VaultItemListingsAction.MasterPasswordRepromptSubmit(
                            password = password,
                            masterPasswordRepromptData = data,
                        ),
                    )
                },
                refreshClick = { viewModel.trySendAction(VaultItemListingsAction.RefreshClick) },
                syncClick = { viewModel.trySendAction(VaultItemListingsAction.SyncClick) },
                lockClick = { viewModel.trySendAction(VaultItemListingsAction.LockClick) },
                overflowItemClick = {
                    viewModel.trySendAction(VaultItemListingsAction.OverflowOptionClick(it))
                },

                dismissDialogRequest = {
                    viewModel.trySendAction(VaultItemListingsAction.DismissDialogClick)
                },
                dismissCredentialManagerErrorDialog = { errorMessage ->
                    viewModel.trySendAction(
                        VaultItemListingsAction.DismissCredentialManagerErrorDialogClick(
                            message = errorMessage,
                        ),
                    )
                },
                confirmOverwriteExistingPasskey = { cipherId ->
                    viewModel.trySendAction(
                        VaultItemListingsAction.ConfirmOverwriteExistingPasskeyClick(cipherId),
                    )
                },
                submitMasterPasswordCredentialVerification = { password, cipherId ->
                    viewModel.trySendAction(
                        VaultItemListingsAction.MasterPasswordUserVerificationSubmit(
                            password = password,
                            selectedCipherId = cipherId,
                        ),
                    )
                },
                retryGetCredentialPasswordVerification = {
                    viewModel.trySendAction(
                        VaultItemListingsAction.RetryUserVerificationPasswordVerificationClick(it),
                    )
                },
                submitPinCredentialVerification = { pin, cipherId ->
                    viewModel.trySendAction(
                        VaultItemListingsAction.PinUserVerificationSubmit(
                            pin = pin,
                            selectedCipherId = cipherId,
                        ),
                    )
                },
                retryPinCredentialVerification = {
                    viewModel.trySendAction(
                        VaultItemListingsAction.RetryUserVerificationPinVerificationClick(it),
                    )
                },
                submitPinSetUpCredentialVerification = { pin, cipherId ->
                    viewModel.trySendAction(
                        VaultItemListingsAction.UserVerificationPinSetUpSubmit(
                            pin = pin,
                            selectedCipherId = cipherId,
                        ),
                    )
                },
                retryPinSetUpCredentialVerification = {
                    viewModel.trySendAction(
                        VaultItemListingsAction.UserVerificationPinSetUpRetryClick(it),
                    )
                },
                dismissUserVerification = {
                    viewModel.trySendAction(
                        VaultItemListingsAction.DismissUserVerificationDialogClick,
                    )
                },
                vaultItemTypeSelected = {
                    viewModel.trySendAction(VaultItemListingsAction.ItemTypeToAddSelected(it))
                },
                trustPrivilegedAppClick = {
                    viewModel.trySendAction(VaultItemListingsAction.TrustPrivilegedAppClick(it))
                },
                shareCipherDecryptionErrorClick = {
                    viewModel.trySendAction(
                        VaultItemListingsAction.ShareCipherDecryptionErrorClick(it),
                    )
                },
                upgradeToPremiumClick = {
                    viewModel.trySendAction(VaultItemListingsAction.UpgradeToPremiumClick)
                },
            )
    }
}
