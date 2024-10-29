package com.x8bit.bitwarden.ui.vault.feature.vault.util

import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultState
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterData
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultStateExtensionsTest {
    @Test
    fun `vaultFilterDataIfRequired with my vault should return correct data`() {
        val vaultFilterData = createVaultFilterData(hasMyVault = true)
        mapOf(
            VaultState.ViewState.NoItems to vaultFilterData,
            VaultState.ViewState.Loading to null,
            VaultState.ViewState.Error(message = "error".asText()) to null,
            createContentState() to vaultFilterData,
        )
            .forEach { (viewState, expected) ->
                assertEquals(
                    expected,
                    viewState.vaultFilterDataIfRequired(vaultFilterData = vaultFilterData),
                )
            }
    }

    @Test
    fun `vaultFilterDataIfRequired without my vault should return correct data`() {
        val vaultFilterData = createVaultFilterData(hasMyVault = false)
        mapOf<VaultState.ViewState, VaultFilterData?>(
            VaultState.ViewState.NoItems to null,
            VaultState.ViewState.Loading to null,
            VaultState.ViewState.Error(message = "error".asText()) to null,
            createContentState() to null,
        )
            .forEach { (viewState, expected) ->
                assertEquals(
                    expected,
                    viewState.vaultFilterDataIfRequired(vaultFilterData = vaultFilterData),
                )
            }
    }

    private fun createVaultFilterData(hasMyVault: Boolean = false): VaultFilterData =
        VaultFilterData(
            selectedVaultFilterType = VaultFilterType.AllVaults,
            vaultFilterTypes = listOfNotNull(
                VaultFilterType.AllVaults,
                if (hasMyVault) {
                    VaultFilterType.MyVault
                } else {
                    null
                },
                VaultFilterType.OrganizationVault(
                    organizationId = "organizationId-A",
                    organizationName = "Organization A",
                ),
            ),
        )

    private fun createContentState(): VaultState.ViewState.Content =
        VaultState.ViewState.Content(
            loginItemsCount = 1,
            cardItemsCount = 0,
            identityItemsCount = 0,
            secureNoteItemsCount = 0,
            favoriteItems = listOf(),
            folderItems = listOf(
                VaultState.ViewState.FolderItem(
                    id = "mockId-1",
                    name = "mockName-1".asText(),
                    itemCount = 1,
                ),
            ),
            collectionItems = listOf(
                VaultState.ViewState.CollectionItem(
                    id = "mockId-1",
                    name = "mockName-1",
                    itemCount = 1,
                ),
            ),
            noFolderItems = listOf(),
            trashItemsCount = 0,
            totpItemsCount = 1,
            itemTypesCount = 4,
            sshKeyItemsCount = 0,
        )
}
