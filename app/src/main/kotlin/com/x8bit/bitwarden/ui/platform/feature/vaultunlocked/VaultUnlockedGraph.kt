package com.x8bit.bitwarden.ui.platform.feature.vaultunlocked

import androidx.navigation3.runtime.NavKey
import com.x8bit.bitwarden.ui.platform.feature.premium.plan.PlanMode
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.feature.settings.folders.model.FolderAddEditType
import com.x8bit.bitwarden.ui.platform.feature.vaultunlockednavbar.VaultUnlockedNavBarGraph
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorMode
import com.x8bit.bitwarden.ui.tools.feature.generator.model.GeneratorPasswordHistoryMode
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.ModeType
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy of the Vault Unlocked UI navigation graph and provides type-safe routes
 * to each destination available in the graph.
 */
@Serializable
sealed class VaultUnlockedGraph : NavKey {
    /**
     * The type-safe route for the vault unlocked navigation bar screen, which hosts the
     * [VaultUnlockedNavBarGraph].
     */
    @Serializable
    data object NavBarRoute : VaultUnlockedGraph()

    /**
     * The type-safe route for the vault item listing screen displayed at the root of the vault
     * unlocked back stack.
     */
    @Serializable
    data class VaultItemListingRoute(
        val vaultItemListingType: VaultItemListingType,
    ) : VaultUnlockedGraph()

    /**
     * The type-safe route for the vault item screen.
     */
    @Serializable
    data class VaultItemRoute(
        val vaultItemId: String,
        val cipherType: VaultItemCipherType,
        val showCreatedSnackbar: Boolean = false,
    ) : VaultUnlockedGraph()

    /**
     * The type-safe route for the vault add/edit screen.
     */
    @Serializable
    data class VaultAddEditRoute(
        val vaultAddEditType: VaultAddEditType,
        val vaultItemCipherType: VaultItemCipherType,
        val selectedFolderId: String? = null,
        val selectedCollectionId: String? = null,
    ) : VaultUnlockedGraph()

    /**
     * The type-safe route for the vault move to organization screen.
     */
    @Serializable
    data class VaultMoveToOrganizationRoute(
        val vaultItemId: String,
        val showOnlyCollections: Boolean,
    ) : VaultUnlockedGraph()

    /**
     * The type-safe route for the attachments screen.
     */
    @Serializable
    data class AttachmentsRoute(
        val cipherId: String,
    ) : VaultUnlockedGraph()

    /**
     * The type-safe route for the preview attachment screen.
     */
    @Serializable
    data class PreviewAttachmentRoute(
        val cipherId: String,
        val attachmentId: String,
        val fileName: String,
        val displaySize: String,
        val isLargeFile: Boolean,
    ) : VaultUnlockedGraph()

    /**
     * The type-safe route for the card scan screen.
     */
    @Serializable
    data object CardScanRoute : VaultUnlockedGraph()

    /**
     * The type-safe route for the QR code scan screen.
     */
    @Serializable
    data object QrCodeScanRoute : VaultUnlockedGraph()

    /**
     * The type-safe route for the manual code entry screen.
     */
    @Serializable
    data object ManualCodeEntryRoute : VaultUnlockedGraph()

    /**
     * The type-safe route for the search screen.
     */
    @Serializable
    data class SearchRoute(
        val searchType: SearchType,
    ) : VaultUnlockedGraph()

    /**
     * The type-safe route for the add/edit send screen.
     */
    @Serializable
    data class AddEditSendRoute(
        val sendType: SendItemType,
        val modeType: ModeType,
        val sendId: String? = null,
    ) : VaultUnlockedGraph()

    /**
     * The type-safe route for the view send screen.
     */
    @Serializable
    data class ViewSendRoute(
        val sendId: String,
        val sendType: SendItemType,
    ) : VaultUnlockedGraph()

    /**
     * The type-safe route for the generator screen displayed as a modal.
     */
    @Serializable
    data class GeneratorModalRoute(
        val generatorMode: GeneratorMode.Modal,
    ) : VaultUnlockedGraph()

    /**
     * The type-safe route for the password history screen.
     */
    @Serializable
    data class PasswordHistoryRoute(
        val passwordHistoryMode: GeneratorPasswordHistoryMode,
    ) : VaultUnlockedGraph()

    /**
     * The type-safe route for the login approval screen.
     */
    @Serializable
    data class LoginApprovalRoute(
        val fingerprint: String?,
    ) : VaultUnlockedGraph()

    /**
     * The type-safe route for the pending requests screen.
     */
    @Serializable
    data object PendingRequestsRoute : VaultUnlockedGraph()

    /**
     * The type-safe route for the manage devices screen.
     */
    @Serializable
    data object ManageDevicesRoute : VaultUnlockedGraph()

    /**
     * The type-safe route for the delete account screen.
     */
    @Serializable
    data object DeleteAccountRoute : VaultUnlockedGraph()

    /**
     * The type-safe route for the delete account confirmation screen.
     */
    @Serializable
    data object DeleteAccountConfirmationRoute : VaultUnlockedGraph()

    /**
     * The type-safe route for the export vault screen.
     */
    @Serializable
    data object ExportVaultRoute : VaultUnlockedGraph()

    /**
     * The type-safe route for the folders screen.
     */
    @Serializable
    data object FoldersRoute : VaultUnlockedGraph()

    /**
     * The type-safe route for the folder add/edit screen.
     */
    @Serializable
    data class FolderAddEditRoute(
        val folderAddEditType: FolderAddEditType,
        val parentFolderName: String? = null,
    ) : VaultUnlockedGraph()

    /**
     * The type-safe route for the setup unlock screen.
     */
    @Serializable
    data object SetupUnlockRoute : VaultUnlockedGraph()

    /**
     * The type-safe route for the setup autofill screen.
     */
    @Serializable
    data object SetupAutofillRoute : VaultUnlockedGraph()

    /**
     * The type-safe route for the setup browser autofill screen.
     */
    @Serializable
    data object SetupBrowserAutofillRoute : VaultUnlockedGraph()

    /**
     * The type-safe route for the import logins screen.
     */
    @Serializable
    data object ImportLoginsRoute : VaultUnlockedGraph()

    /**
     * The type-safe route for the flight recorder screen.
     */
    @Serializable
    data object FlightRecorderRoute : VaultUnlockedGraph()

    /**
     * The type-safe route for the recorded logs screen.
     */
    @Serializable
    data object RecordedLogsRoute : VaultUnlockedGraph()

    /**
     * The type-safe route for the about privileged apps screen.
     */
    @Serializable
    data object AboutPrivilegedAppsRoute : VaultUnlockedGraph()

    /**
     * The type-safe route for the plan screen displayed as a modal, with the navigation bar
     * hidden.
     */
    @Serializable
    data object PlanModalRoute : VaultUnlockedGraph()

    /**
     * The type-safe route for the "Upgraded to Premium" screen. The [planMode] encodes how the
     * user reached the originating plan screen and drives the dismiss semantics.
     */
    @Serializable
    data class UpgradedToPremiumRoute(
        val planMode: PlanMode,
    ) : VaultUnlockedGraph()
}
