@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.migratetomyitems

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.entryProvider
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.navigation.components.BitwardenNavDisplay
import com.bitwarden.ui.platform.navigation.util.rememberNavigationController
import com.bitwarden.ui.platform.navigation.util.rootTransitionEntry
import com.bitwarden.ui.platform.theme.RootTransition
import com.x8bit.bitwarden.ui.platform.feature.rootnav.RootNavGraph
import com.x8bit.bitwarden.ui.vault.feature.leaveorganization.leaveOrganizationDestination
import com.x8bit.bitwarden.ui.vault.feature.leaveorganization.navigateToLeaveOrganization

/**
 * Add the migrate to my items section to the root navigation entries. The
 * [RootNavGraph.MigrateToMyItemsRoute] hosts the [MigrateToMyItemsGraph].
 */
fun EntryProviderScope<RootNavGraph>.migrateToMyItemsGraph() {
    rootTransitionEntry<RootNavGraph.MigrateToMyItemsRoute>(
        rootTransition = RootTransition.SLIDE_UP,
    ) {
        val navigationController = rememberNavigationController<MigrateToMyItemsGraph>(
            routes = arrayOf(MigrateToMyItemsGraph.MigrateToMyItemsRoute),
        )
        BitwardenNavDisplay(
            navigationController = navigationController,
            entryProvider = entryProvider {
                migrateToMyItemsDestination(
                    onNavigateToLeaveOrganization = { organizationId, organizationName ->
                        navigationController.navigateToLeaveOrganization(
                            organizationId = organizationId,
                            organizationName = organizationName,
                        )
                    },
                )
                leaveOrganizationDestination(
                    onNavigateBack = { navigationController.navigateBack() },
                )
            },
        )
    }
}
