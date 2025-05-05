package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import androidx.core.os.bundleOf
import androidx.credentials.provider.ProviderCreateCredentialRequest
import com.x8bit.bitwarden.data.credentials.model.CreateCredentialRequest
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class CreateCredentialRequestExtensionsTest {

    @BeforeEach
    fun setUp() {
        mockkStatic(UUID::class)
        mockkObject(ProviderCreateCredentialRequest.Companion)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(UUID::class)
        unmockkObject(ProviderCreateCredentialRequest.Companion)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toDefaultAddTypeContent should return the correct content when calling app is not privileged`() {
        every { UUID.randomUUID().toString() } returns "uuid"
        every { ProviderCreateCredentialRequest.fromBundle(any()) } returns mockk(relaxed = true) {
            every { callingRequest.origin } returns null
            every { callingAppInfo.packageName } returns "mockPackageName-1"
        }
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
            CreateCredentialRequest(
                userId = "mockUserId-1",
                isUserPreVerified = false,
                requestData = bundleOf(),
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
        every { ProviderCreateCredentialRequest.fromBundle(any()) } returns mockk(relaxed = true) {
            every { callingRequest.origin } returns "www.test.com"
            every { callingAppInfo.packageName } returns "mockPackageName-1"
        }
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
            CreateCredentialRequest(
                userId = "mockUserId-1",
                isUserPreVerified = false,
                requestData = bundleOf(),
            )
                .toDefaultAddTypeContent(
                    attestationOptions = createMockPasskeyAttestationOptions(number = 1),
                    isIndividualVaultDisabled = false,
                ),
        )
    }
}
