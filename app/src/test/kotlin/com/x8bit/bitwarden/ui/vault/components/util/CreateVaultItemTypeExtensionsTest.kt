package com.x8bit.bitwarden.ui.vault.components.util

import com.x8bit.bitwarden.ui.vault.components.model.CreateVaultItemType
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CreateVaultItemTypeExtensionsTest {

    @Test
    fun `Add CreateVaultItemType values should map to correct VaultItemCipherType or null`() {
        CreateVaultItemType.entries.forEach { createVaultItemType ->
            val actualResult = createVaultItemType.toVaultItemCipherTypeOrNull()
            when (createVaultItemType) {
                CreateVaultItemType.LOGIN -> assertEquals(
                    VaultItemCipherType.LOGIN,
                    actualResult,
                )

                CreateVaultItemType.CARD -> assertEquals(
                    VaultItemCipherType.CARD,
                    actualResult,
                )

                CreateVaultItemType.IDENTITY -> assertEquals(
                    VaultItemCipherType.IDENTITY,
                    actualResult,
                )

                CreateVaultItemType.SECURE_NOTE -> assertEquals(
                    VaultItemCipherType.SECURE_NOTE,
                    actualResult,
                )

                CreateVaultItemType.SSH_KEY -> assertEquals(
                    VaultItemCipherType.SSH_KEY,
                    actualResult,
                )

                CreateVaultItemType.FOLDER -> assertNull(actualResult)
            }
        }
    }
}
