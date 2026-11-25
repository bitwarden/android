@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.migratetomyitems

import androidx.navigation3.runtime.EntryProviderScope
import com.bitwarden.annotation.OmitFromCoverage

/**
 * Add the migrate to my items screen to the migrate to my items navigation entries.
 */
fun EntryProviderScope<MigrateToMyItemsGraph>.migrateToMyItemsDestination(
    onNavigateToLeaveOrganization: (organizationId: String, organizationName: String) -> Unit,
) {
    entry<MigrateToMyItemsGraph.MigrateToMyItemsRoute> {
        MigrateToMyItemsScreen(
            onNavigateToLeaveOrganization = onNavigateToLeaveOrganization,
        )
    }
}
