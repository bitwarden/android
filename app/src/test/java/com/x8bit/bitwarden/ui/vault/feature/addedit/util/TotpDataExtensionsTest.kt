package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.model.TotpData
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class TotpDataExtensionsTest {

    @BeforeEach
    fun setup() {
        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns "uuid"
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(UUID::class)
    }

    @Test
    fun `toDefaultAddTypeContent should return the correct content with totp data`() {
        val uri = "otpauth://totp/issuer:accountName?secret=secret"
        val totpData = TotpData(
            uri = uri,
            issuer = "issuer",
            accountName = "accountName",
            secret = "secret",
            digits = 6,
            period = 30,
            algorithm = TotpData.CryptoHashAlgorithm.SHA_1,
        )
        assertEquals(
            VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(name = "issuer"),
                isIndividualVaultDisabled = false,
                type = VaultAddEditState.ViewState.Content.ItemType.Login(totp = uri),
            ),
            totpData.toDefaultAddTypeContent(isIndividualVaultDisabled = false),
        )
    }
}
