package com.x8bit.bitwarden.ui.tools.feature.send

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.ui.platform.navigation.util.rootPushEntry
import com.x8bit.bitwarden.ui.platform.feature.search.model.SearchType
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph

/**
 * Add send destination to the navigation entries.
 */
@Suppress("LongParameterList")
fun EntryProviderScope<SendGraph>.sendDestination(
    onNavigateToAddEditSend: (route: VaultUnlockedGraph.AddEditSendRoute) -> Unit,
    onNavigateToViewSend: (VaultUnlockedGraph.ViewSendRoute) -> Unit,
    onNavigateToSendFilesList: () -> Unit,
    onNavigateToSendTextList: () -> Unit,
    onNavigateToSearchSend: (searchType: SearchType.Sends) -> Unit,
    onNavigateToPlan: () -> Unit,
) {
    rootPushEntry<SendGraph.SendRoute> {
        SendScreen(
            onNavigateToAddEditSend = onNavigateToAddEditSend,
            onNavigateToViewSend = onNavigateToViewSend,
            onNavigateToSendFilesList = onNavigateToSendFilesList,
            onNavigateToSendTextList = onNavigateToSendTextList,
            onNavigateToSearchSend = onNavigateToSearchSend,
            onNavigateToPlan = onNavigateToPlan,
        )
    }
}
