package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import android.content.pm.SigningInfo
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CreateCredentialRequest
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriItem
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class Fido2CredentialRequestExtensionsTest {

    @BeforeEach
    fun setUp() {
        mockkStatic(UUID::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(UUID::class)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toDefaultAddTypeContent should return the correct content when calling app is not privileged`() {
        every { UUID.randomUUID().toString() } returns "uuid"
        assertEquals(
            VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(
                    name = "mockPublicKeyCredentialRpEntityName-1",
                ),
                isIndividualVaultDisabled = false,
                type = VaultAddEditState.ViewState.Content.ItemType.Login(
                    username = "mockPublicKeyCredentialUserEntityName-1",
                    uriList = listOf(
                        UriItem(
                            id = "uuid",
                            uri = "androidapp://mockPackageName-1",
                            match = null,
                            checksum = null,
                        ),
                    ),
                ),
            ),
            Fido2CreateCredentialRequest(
                userId = "mockUserId-1",
                requestJson = "mockRequestJson-1",
                packageName = "mockPackageName-1",
                signingInfo = SigningInfo(),
                origin = null,
            )
                .toDefaultAddTypeContent(
                    attestationOptions = createMockPasskeyAttestationOptions(1),
                    isIndividualVaultDisabled = false,
                ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toDefaultAddTypeContent should return the correct content when calling app is privileged`() {
        every { UUID.randomUUID().toString() } returns "uuid"
        assertEquals(
            VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(
                    name = "mockPublicKeyCredentialRpEntityName-1",
                ),
                isIndividualVaultDisabled = false,
                type = VaultAddEditState.ViewState.Content.ItemType.Login(
                    username = "mockPublicKeyCredentialUserEntityName-1",
                    uriList = listOf(
                        UriItem(
                            id = "uuid",
                            uri = "www.test.com",
                            match = null,
                            checksum = null,
                        ),
                    ),
                ),
            ),
            Fido2CreateCredentialRequest(
                userId = "mockUserId-1",
                requestJson = "mockRequestJson-1",
                packageName = "mockPackageName-1",
                signingInfo = SigningInfo(),
                origin = "www.test.com",
            )
                .toDefaultAddTypeContent(
                    attestationOptions = createMockPasskeyAttestationOptions(number = 1),
                    isIndividualVaultDisabled = false,
                ),
        )
    }
}
