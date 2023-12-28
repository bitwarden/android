package com.x8bit.bitwarden.ui.vault.feature.item.util

import com.bitwarden.core.CipherRepromptType
import com.bitwarden.core.CipherType
import com.bitwarden.core.CipherView
import com.bitwarden.core.FieldType
import com.bitwarden.core.FieldView
import com.bitwarden.core.LoginUriView
import com.bitwarden.core.LoginView
import com.bitwarden.core.PasswordHistoryView
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemState
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.TimeZone

class CipherViewExtensionsTest {

    @BeforeEach
    fun setup() {
        // Setting the timezone so the tests pass consistently no matter the environment.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @AfterEach
    fun tearDown() {
        // Clearing the timezone after the test.
        TimeZone.setDefault(null)
    }

    @Test
    fun `toViewState should transform full CipherView into ViewState Login Content with premium`() {
        val viewState = DEFAULT_FULL_LOGIN_CIPHER_VIEW.toViewState(isPremiumUser = true)

        assertEquals(DEFAULT_FULL_LOGIN_VIEW_STATE, viewState)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should transform full CipherView into ViewState Login Content without premium`() {
        val isPremiumUser = false
        val viewState = DEFAULT_FULL_LOGIN_CIPHER_VIEW.toViewState(isPremiumUser = isPremiumUser)

        assertEquals(
            DEFAULT_FULL_LOGIN_VIEW_STATE.copy(
                common = DEFAULT_FULL_LOGIN_VIEW_STATE.common.copy(isPremiumUser = isPremiumUser),
            ),
            viewState,
        )
    }

    @Test
    fun `toViewState should transform empty CipherView into ViewState Login Content`() {
        val viewState = DEFAULT_EMPTY_LOGIN_CIPHER_VIEW.toViewState(isPremiumUser = true)

        assertEquals(DEFAULT_EMPTY_LOGIN_VIEW_STATE, viewState)
    }
}

val DEFAULT_FULL_LOGIN_VIEW: LoginView = LoginView(
    username = "username",
    password = "password",
    passwordRevisionDate = Instant.ofEpochSecond(1_000L),
    uris = listOf(
        LoginUriView(
            uri = "www.example.com",
            match = null,
        ),
    ),
    totp = "otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example",
    autofillOnPageLoad = false,
)

val DEFAULT_EMPTY_LOGIN_VIEW: LoginView = LoginView(
    username = null,
    password = null,
    passwordRevisionDate = null,
    uris = emptyList(),
    totp = null,
    autofillOnPageLoad = false,
)

val DEFAULT_FULL_LOGIN_CIPHER_VIEW: CipherView = CipherView(
    id = null,
    organizationId = null,
    folderId = null,
    collectionIds = emptyList(),
    key = null,
    name = "login cipher",
    notes = "Lots of notes",
    type = CipherType.LOGIN,
    login = DEFAULT_FULL_LOGIN_VIEW,
    identity = null,
    card = null,
    secureNote = null,
    favorite = false,
    reprompt = CipherRepromptType.PASSWORD,
    organizationUseTotp = false,
    edit = false,
    viewPassword = false,
    localData = null,
    attachments = null,
    fields = listOf(
        FieldView(
            name = "text",
            value = "value",
            type = FieldType.TEXT,
            linkedId = null,
        ),
        FieldView(
            name = "hidden",
            value = "value",
            type = FieldType.HIDDEN,
            linkedId = null,
        ),
        FieldView(
            name = "boolean",
            value = "true",
            type = FieldType.BOOLEAN,
            linkedId = null,
        ),
        FieldView(
            name = "linked username",
            value = null,
            type = FieldType.LINKED,
            linkedId = 100U,
        ),
        FieldView(
            name = "linked password",
            value = null,
            type = FieldType.LINKED,
            linkedId = 101U,
        ),
    ),
    passwordHistory = listOf(
        PasswordHistoryView(
            password = "old_password",
            lastUsedDate = Instant.ofEpochSecond(1_000L),
        ),
    ),
    creationDate = Instant.ofEpochSecond(1_000L),
    deletedDate = null,
    revisionDate = Instant.ofEpochSecond(1_000L),
)

val DEFAULT_EMPTY_LOGIN_CIPHER_VIEW: CipherView = CipherView(
    id = null,
    organizationId = null,
    folderId = null,
    collectionIds = emptyList(),
    key = null,
    name = "login cipher",
    notes = null,
    type = CipherType.LOGIN,
    login = DEFAULT_EMPTY_LOGIN_VIEW,
    identity = null,
    card = null,
    secureNote = null,
    favorite = false,
    reprompt = CipherRepromptType.PASSWORD,
    organizationUseTotp = false,
    edit = false,
    viewPassword = false,
    localData = null,
    attachments = null,
    fields = null,
    passwordHistory = null,
    creationDate = Instant.ofEpochSecond(1_000L),
    deletedDate = null,
    revisionDate = Instant.ofEpochSecond(1_000L),
)

val DEFAULT_FULL_LOGIN_VIEW_STATE: VaultItemState.ViewState.Content =
    VaultItemState.ViewState.Content(
        common = VaultItemState.ViewState.Content.Common(
            name = "login cipher",
            lastUpdated = "1/1/70 12:16 AM",
            notes = "Lots of notes",
            isPremiumUser = true,
            customFields = listOf(
                VaultItemState.ViewState.Content.Common.Custom.TextField(
                    name = "text",
                    value = "value",
                    isCopyable = true,
                ),
                VaultItemState.ViewState.Content.Common.Custom.HiddenField(
                    name = "hidden",
                    value = "value",
                    isCopyable = true,
                    isVisible = false,
                ),
                VaultItemState.ViewState.Content.Common.Custom.BooleanField(
                    name = "boolean",
                    value = true,
                ),
                VaultItemState.ViewState.Content.Common.Custom.LinkedField(
                    name = "linked username",
                    vaultLinkedFieldType = VaultLinkedFieldType.USERNAME,
                ),
                VaultItemState.ViewState.Content.Common.Custom.LinkedField(
                    name = "linked password",
                    vaultLinkedFieldType = VaultLinkedFieldType.PASSWORD,
                ),
            ),
            requiresReprompt = true,
        ),
        type = VaultItemState.ViewState.Content.ItemType.Login(
            passwordHistoryCount = 1,
            username = "username",
            passwordData = VaultItemState.ViewState.Content.ItemType.Login.PasswordData(
                password = "password",
                isVisible = false,
            ),
            uris = listOf(
                VaultItemState.ViewState.Content.ItemType.Login.UriData(
                    uri = "www.example.com",
                    isCopyable = true,
                    isLaunchable = true,
                ),
            ),
            passwordRevisionDate = "1/1/70 12:16 AM",
            totp = "otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example",
        ),
    )

val DEFAULT_EMPTY_LOGIN_VIEW_STATE: VaultItemState.ViewState.Content =
    VaultItemState.ViewState.Content(
        common = VaultItemState.ViewState.Content.Common(
            name = "login cipher",
            lastUpdated = "1/1/70 12:16 AM",

            notes = null,
            isPremiumUser = true,
            customFields = emptyList(),
            requiresReprompt = true,

            ),
        type = VaultItemState.ViewState.Content.ItemType.Login(
            passwordHistoryCount = null,
            username = null,
            passwordData = null,
            uris = emptyList(),
            passwordRevisionDate = null,
            totp = null,
        ),
    )
