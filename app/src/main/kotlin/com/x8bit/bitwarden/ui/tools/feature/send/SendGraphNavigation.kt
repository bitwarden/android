package com.x8bit.bitwarden.ui.tools.feature.send

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.NavigationController
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.navigateToSendItemListing
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.sendItemListingDestination
import com.x8bit.bitwarden.ui.vault.model.VaultItemListingType

/**
 * Add the send destinations to the send navigation entries. The [SendGraph.SendRoute] is the root
 * of the send tab's [navigationController].
 */
fun EntryProviderScope<SendGraph>.sendDestinations(
    navigationController: NavigationController<SendGraph>,
    onNavigateToAddEditSend: (route: VaultUnlockedGraph.AddEditSendRoute) -> Unit,
    onNavigateToViewSend: (VaultUnlockedGraph.ViewSendRoute) -> Unit,
    onNavigateToSearchSend: (searchType: SearchType.Sends) -> Unit,
    onNavigateToPlan: () -> Unit,
) {
    sendDestination(
        onNavigateToAddEditSend = onNavigateToAddEditSend,
        onNavigateToViewSend = onNavigateToViewSend,
        onNavigateToSendFilesList = {
            navigationController.navigateToSendItemListing(
                vaultItemListingType = VaultItemListingType.SendFile,
            )
        },
        onNavigateToSendTextList = {
            navigationController.navigateToSendItemListing(
                vaultItemListingType = VaultItemListingType.SendText,
            )
        },
        onNavigateToSearchSend = onNavigateToSearchSend,
        onNavigateToPlan = onNavigateToPlan,
    )
    sendItemListingDestination(
        onNavigateBack = { navigationController.navigateBack() },
        onNavigateToAddEditSendItem = onNavigateToAddEditSend,
        onNavigateToViewSendItem = onNavigateToViewSend,
        onNavigateToSearchSend = onNavigateToSearchSend,
        onNavigateToPlan = onNavigateToPlan,
    )
}
