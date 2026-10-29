@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.leaveorganization

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.navigation.NavigationController
import com.bitwarden.ui.platform.navigation.util.pushEntry
import com.x8bit.bitwarden.ui.vault.feature.migratetomyitems.MigrateToMyItemsGraph

/**
 * Add the leave organization screen to the navigation entries.
 */
fun EntryProviderScope<MigrateToMyItemsGraph>.leaveOrganizationDestination(
    onNavigateBack: () -> Unit,
) {
    pushEntry<MigrateToMyItemsGraph.LeaveOrganizationRoute> { route ->
        LeaveOrganizationScreen(
            viewModel = hiltViewModel(
                creationCallback = { factory: LeaveOrganizationViewModel.Factory ->
                    factory.create(route = route)
                },
            ),
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the leave organization screen.
 *
 * @param organizationId The ID of the organization to leave.
 * @param organizationName The name of the organization to leave.
 */
fun NavigationController<MigrateToMyItemsGraph>.navigateToLeaveOrganization(
    organizationId: String,
    organizationName: String,
) {
    this.navigate(
        route = MigrateToMyItemsGraph.LeaveOrganizationRoute(
            organizationId = organizationId,
            organizationName = organizationName,
        ),
    )
}
