package com.x8bit.bitwarden.ui.vault.feature.addedit

import com.bitwarden.ui.platform.base.createMockNavHostController
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import io.mockk.verify
import org.junit.jupiter.api.Test

class VaultAddEditNavigationTest {
    private val navController = createMockNavHostController()

    @Test
    fun `navigateToVaultAddEdit should pass along the selected collection ID`() {
        navController.navigateToVaultAddEdit(
            args = VaultAddEditArgs(
                vaultAddEditType = VaultAddEditType.AddItem,
                vaultItemCipherType = VaultItemCipherType.LOGIN,
                selectedCollectionId = "mockCollectionId",
            ),
        )

        verify(exactly = 1) {
            navController.navigate(
                route = VaultAddEditRoute(
                    vaultAddEditMode = VaultAddEditMode.ADD,
                    vaultItemId = null,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
                    selectedFolderId = null,
                    selectedCollectionId = "mockCollectionId",
                ),
                navOptions = null,
            )
        }
    }

    @Test
    fun `navigateToVaultAddEdit should pass along the selected folder ID`() {
        navController.navigateToVaultAddEdit(
            args = VaultAddEditArgs(
                vaultAddEditType = VaultAddEditType.AddItem,
                vaultItemCipherType = VaultItemCipherType.LOGIN,
                selectedFolderId = "mockFolderId",
            ),
        )

        verify(exactly = 1) {
            navController.navigate(
                route = VaultAddEditRoute(
                    vaultAddEditMode = VaultAddEditMode.ADD,
                    vaultItemId = null,
                    vaultItemCipherType = VaultItemCipherType.LOGIN,
                    selectedFolderId = "mockFolderId",
                    selectedCollectionId = null,
                ),
                navOptions = null,
            )
        }
    }
}
