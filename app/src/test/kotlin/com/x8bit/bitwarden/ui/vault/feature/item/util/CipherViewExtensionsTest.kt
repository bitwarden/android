package com.x8bit.bitwarden.ui.vault.feature.item.util

import android.net.Uri
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.vault.CipherType
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCardView
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemState
import com.x8bit.bitwarden.ui.vault.feature.item.model.TotpCodeItemData
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CipherViewExtensionsTest {

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )

    @BeforeEach
    fun setUp() {
        setupMockUri()
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `toViewState should transform full CipherView into ViewState Login Content with premium`() {
        val cipherView = createCipherView(type = CipherType.LOGIN, isEmpty = false)
        val viewState = cipherView.toViewState(
            previousState = null,
            isPremiumUser = true,
            totpCodeItemData = TotpCodeItemData(
                periodSeconds = 30,
                timeLeftSeconds = 15,
                verificationCode = "123456",
            ),
            clock = fixedClock,
            canDelete = true,
            canRestore = true,
            canAssignToCollections = true,
            canEdit = true,
            baseIconUrl = "https://example.com/",
            isIconLoadingDisabled = true,
            relatedLocations = persistentListOf(),
            hasOrganizations = true,
        )

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(isEmpty = false, isPremiumUser = true)
                    .copy(currentCipher = cipherView),
                type = createLoginContent(isEmpty = false),
            ),
            viewState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should transform full CipherView into ViewState Login Content without premium`() {
        val isPremiumUser = false
        val cipherView = createCipherView(type = CipherType.LOGIN, isEmpty = false)
        val viewState = cipherView.toViewState(
            previousState = null,
            isPremiumUser = isPremiumUser,
            totpCodeItemData = TotpCodeItemData(
                periodSeconds = 30,
                timeLeftSeconds = 15,
                verificationCode = "123456",
            ),
            clock = fixedClock,
            canDelete = true,
            canRestore = true,
            canAssignToCollections = true,
            canEdit = true,
            baseIconUrl = "https://example.com/",
            isIconLoadingDisabled = true,
            relatedLocations = persistentListOf(),
            hasOrganizations = true,
        )

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(isEmpty = false, isPremiumUser = isPremiumUser)
                    .copy(currentCipher = cipherView),
                type = createLoginContent(isEmpty = false).copy(
                    isPremiumUser = isPremiumUser,
                    canViewTotpCode = false,
                ),
            ),
            viewState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should transform full CipherView into ViewState Login Content without premium but with org totp access`() {
        val isPremiumUser = false
        val cipherView = createCipherView(
            type = CipherType.LOGIN,
            isEmpty = false,
        ).copy(organizationUseTotp = true)
        val viewState = cipherView.toViewState(
            previousState = null,
            isPremiumUser = isPremiumUser,
            totpCodeItemData = TotpCodeItemData(
                periodSeconds = 30,
                timeLeftSeconds = 15,
                verificationCode = "123456",
            ),
            clock = fixedClock,
            canDelete = true,
            canRestore = true,
            canAssignToCollections = true,
            canEdit = true,
            baseIconUrl = "https://example.com/",
            isIconLoadingDisabled = true,
            relatedLocations = persistentListOf(),
            hasOrganizations = true,
        )

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(isEmpty = false, isPremiumUser = isPremiumUser)
                    .copy(currentCipher = cipherView),
                type = createLoginContent(isEmpty = false).copy(
                    isPremiumUser = isPremiumUser,
                    canViewTotpCode = true,
                ),
            ),
            viewState,
        )
    }

    @Test
    fun `toViewState should transform empty CipherView into ViewState Login Content`() {
        val cipherView = createCipherView(type = CipherType.LOGIN, isEmpty = true)
        val viewState = cipherView.toViewState(
            previousState = null,
            isPremiumUser = true,
            totpCodeItemData = null,
            clock = fixedClock,
            canDelete = true,
            canRestore = true,
            canAssignToCollections = true,
            canEdit = true,
            baseIconUrl = "https://example.com/",
            isIconLoadingDisabled = true,
            relatedLocations = persistentListOf(),
            hasOrganizations = true,
        )

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(isEmpty = true, isPremiumUser = true).copy(
                    currentCipher = cipherView,
                ),
                type = createLoginContent(isEmpty = true),
            ),
            viewState,
        )
    }

    @Test
    fun `toViewState should transform full CipherView into ViewState Identity Content`() {
        val cipherView = createCipherView(type = CipherType.IDENTITY, isEmpty = false)
        val viewState = cipherView.toViewState(
            previousState = null,
            isPremiumUser = true,
            totpCodeItemData = null,
            clock = fixedClock,
            canDelete = true,
            canRestore = true,
            canAssignToCollections = true,
            canEdit = true,
            baseIconUrl = "https://example.com/",
            isIconLoadingDisabled = true,
            relatedLocations = persistentListOf(),
            hasOrganizations = true,
        )

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(
                    isEmpty = false,
                    isPremiumUser = true,
                    iconResId = BitwardenDrawable.ic_id_card,
                )
                    .copy(currentCipher = cipherView),
                type = createIdentityContent(isEmpty = false),
            ),
            viewState,
        )
    }

    @Test
    fun `toViewState should transform empty CipherView into ViewState Identity Content`() {
        val cipherView = createCipherView(type = CipherType.IDENTITY, isEmpty = true)
        val viewState = cipherView.toViewState(
            previousState = null,
            isPremiumUser = true,
            totpCodeItemData = null,
            clock = fixedClock,
            canDelete = true,
            canRestore = true,
            canAssignToCollections = true,
            canEdit = true,
            baseIconUrl = "https://example.com/",
            isIconLoadingDisabled = true,
            relatedLocations = persistentListOf(),
            hasOrganizations = true,
        )

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(
                    isEmpty = true,
                    isPremiumUser = true,
                    iconResId = BitwardenDrawable.ic_id_card,
                )
                    .copy(currentCipher = cipherView),
                type = createIdentityContent(isEmpty = true),
            ),
            viewState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should transform CipherView with odd naming into ViewState Identity Content`() {
        val initialCipherView = createCipherView(type = CipherType.IDENTITY, isEmpty = false)
        val cipherView = initialCipherView
            .copy(
                identity = initialCipherView.identity?.copy(
                    title = "MX",
                    firstName = null,
                    middleName = "middleName",
                    lastName = null,
                ),
            )
        val viewState = cipherView.toViewState(
            previousState = null,
            isPremiumUser = true,
            totpCodeItemData = null,
            clock = fixedClock,
            canDelete = true,
            canRestore = true,
            canAssignToCollections = true,
            canEdit = true,
            baseIconUrl = "https://example.com/",
            isIconLoadingDisabled = true,
            relatedLocations = persistentListOf(),
            hasOrganizations = true,
        )

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(
                    isEmpty = false,
                    isPremiumUser = true,
                    iconResId = BitwardenDrawable.ic_id_card,
                )
                    .copy(currentCipher = cipherView),
                type = createIdentityContent(
                    isEmpty = false,
                    identityName = "Mx middleName",
                ),
            ),
            viewState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should transform CipherView with odd address into ViewState Identity Content`() {
        val initialCipherView = createCipherView(type = CipherType.IDENTITY, isEmpty = false)
        val cipherView = initialCipherView.copy(
            identity = initialCipherView.identity?.copy(
                address1 = null,
                address2 = null,
                address3 = "address3",
                city = null,
                state = "state",
                postalCode = null,
                country = null,
            ),
        )
        val result = cipherView.toViewState(
            previousState = null,
            isPremiumUser = true,
            totpCodeItemData = null,
            clock = fixedClock,
            canDelete = true,
            canRestore = true,
            canAssignToCollections = true,
            canEdit = true,
            baseIconUrl = "https://example.com/",
            isIconLoadingDisabled = true,
            relatedLocations = persistentListOf(),
            hasOrganizations = true,
        )

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(
                    isEmpty = false,
                    isPremiumUser = true,
                    iconResId = BitwardenDrawable.ic_id_card,
                ).copy(
                    currentCipher = cipherView.copy(
                        identity = cipherView.identity?.copy(
                            address1 = null,
                            address2 = null,
                            address3 = "address3",
                            city = null,
                            state = "state",
                            postalCode = null,
                            country = null,
                        ),
                    ),
                ),
                type = createIdentityContent(
                    isEmpty = false,
                    address = """
                        address3
                        -, state, -
                    """.trimIndent(),
                ),
            ),
            result,
        )
    }

    @Test
    fun `toViewState should transform full CipherView into ViewState Secure Note Content`() {
        val cipherView = createCipherView(type = CipherType.SECURE_NOTE, isEmpty = false)
        val viewState = cipherView.toViewState(
            previousState = null,
            isPremiumUser = true,
            totpCodeItemData = null,
            clock = fixedClock,
            canDelete = true,
            canRestore = true,
            canAssignToCollections = true,
            canEdit = true,
            baseIconUrl = "https://example.com/",
            isIconLoadingDisabled = true,
            relatedLocations = persistentListOf(),
            hasOrganizations = true,
        )

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(
                    isEmpty = false,
                    isPremiumUser = true,
                    iconResId = BitwardenDrawable.ic_note,
                )
                    .copy(currentCipher = cipherView),
                type = VaultItemState.ViewState.Content.ItemType.SecureNote,
            ),
            viewState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should transform empty Secure Note CipherView into ViewState Secure Note Content`() {
        val cipherView = createCipherView(type = CipherType.SECURE_NOTE, isEmpty = true)
        val viewState = cipherView.toViewState(
            previousState = null,
            isPremiumUser = true,
            totpCodeItemData = null,
            clock = fixedClock,
            canDelete = true,
            canRestore = true,
            canAssignToCollections = true,
            canEdit = true,
            baseIconUrl = "https://example.com/",
            isIconLoadingDisabled = true,
            relatedLocations = persistentListOf(),
            hasOrganizations = true,
        )

        val expectedState = VaultItemState.ViewState.Content(
            common = createCommonContent(
                isEmpty = true,
                isPremiumUser = true,
                iconResId = BitwardenDrawable.ic_note,
            )
                .copy(currentCipher = cipherView),
            type = VaultItemState.ViewState.Content.ItemType.SecureNote,
        )

        assertEquals(expectedState, viewState)
    }

    @Test
    fun `toViewState should transform full CipherView into ViewState SSH Key Content`() {
        val cipherView = createCipherView(type = CipherType.SSH_KEY, isEmpty = false)
        val viewState = cipherView.toViewState(
            previousState = null,
            isPremiumUser = true,
            totpCodeItemData = null,
            clock = fixedClock,
            canDelete = true,
            canRestore = true,
            canAssignToCollections = true,
            canEdit = true,
            baseIconUrl = "https://example.com/",
            isIconLoadingDisabled = true,
            relatedLocations = persistentListOf(),
            hasOrganizations = true,
        )
        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(
                    isEmpty = false,
                    isPremiumUser = true,
                    iconResId = BitwardenDrawable.ic_ssh_key,
                ).copy(
                    currentCipher = cipherView.copy(
                        name = "mockName",
                        sshKey = cipherView.sshKey?.copy(
                            publicKey = "publicKey",
                            privateKey = "privateKey",
                            fingerprint = "fingerprint",
                        ),
                    ),
                ),
                type = createSshKeyContent(isEmpty = false),
            ),
            viewState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should transform full CipherView into ViewState with iconData based on cipher type`() {
        mapOf(
            CipherType.LOGIN to BitwardenDrawable.ic_globe,
            CipherType.IDENTITY to BitwardenDrawable.ic_id_card,
            CipherType.CARD to BitwardenDrawable.ic_payment_card,
            CipherType.SECURE_NOTE to BitwardenDrawable.ic_note,
            CipherType.SSH_KEY to BitwardenDrawable.ic_ssh_key,
        )
            .forEach {
                val cipherView = createCipherView(type = it.key, isEmpty = false)
                val viewState = cipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = null,
                    clock = fixedClock,
                    canDelete = true,
                    canRestore = true,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = "https://example.com/",
                    isIconLoadingDisabled = true,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
                assertEquals(
                    it.value,
                    (viewState.asContentOrNull()?.common?.iconData as? IconData.Local)?.iconRes,
                )
            }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should transform full CipherView into ViewState Card content with iconData based on payment card brand`() {
        mapOf<VaultCardBrand, Int>()
            .forEach {
                val cipherView = createCipherView(type = CipherType.CARD, isEmpty = false)
                    .copy(card = createMockCardView(number = 1, brand = it.key.toString()))
                val viewState = cipherView.toViewState(
                    previousState = null,
                    isPremiumUser = true,
                    totpCodeItemData = null,
                    clock = fixedClock,
                    canDelete = true,
                    canRestore = true,
                    canAssignToCollections = true,
                    canEdit = true,
                    baseIconUrl = "https://example.com/",
                    isIconLoadingDisabled = true,
                    relatedLocations = persistentListOf(),
                    hasOrganizations = true,
                )
                assertEquals(
                    IconData.Local(it.value),
                    (viewState
                        .asContentOrNull()
                        ?.type as? VaultItemState.ViewState.Content.ItemType.Card)
                        ?.paymentCardBrandIconData,
                )
            }
    }

    private fun setupMockUri() {
        mockkStatic(Uri::class)
        val uriMock = mockk<Uri>()
        every { Uri.parse(any()) } returns uriMock
        every { uriMock.host } returns "www.mockuri.com"
    }
}
