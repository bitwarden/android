package com.x8bit.bitwarden.ui.vault.feature.vault.model

import android.os.Parcelable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import kotlinx.parcelize.Parcelize

/**
 * Represents a way to filter on vaults when more than one may be present in a list of vault items.
 */
sealed class VaultFilterType : Parcelable {
    /**
     *  A short name for the filter.
     */
    abstract val name: Text

    /**
     * A potentially longer description of the filter. This may be the same as the [name] when there
     * is no distinction necessary.
     */
    abstract val description: Text

    /**
     * Data from all vaults should be present (i.e. there is no filtering).
     */
    @Parcelize
    data object AllVaults : VaultFilterType() {
        override val name: Text get() = BitwardenString.all.asText()
        override val description: Text get() = BitwardenString.all_vaults.asText()
    }

    /**
     * Only data from the user's personal vault should be present.
     */
    @Parcelize
    data object MyVault : VaultFilterType() {
        override val name: Text get() = BitwardenString.my_vault.asText()
        override val description: Text get() = BitwardenString.my_vault.asText()
    }

    /**
     * Only data from the organization with the given [organizationId] should be present.
     */
    @Parcelize
    data class OrganizationVault(
        val organizationId: String,
        val organizationName: String,
    ) : VaultFilterType() {
        override val name: Text get() = organizationName.asText()
        override val description: Text get() = organizationName.asText()
    }
}
