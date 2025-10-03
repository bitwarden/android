package com.x8bit.bitwarden.ui.vault.feature.item.util

import androidx.annotation.DrawableRes
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.vault.AttachmentView
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.FieldType
import com.bitwarden.vault.FieldView
import com.bitwarden.vault.IdentityView
import com.bitwarden.vault.LoginUriView
import com.bitwarden.vault.LoginView
import com.bitwarden.vault.PasswordHistoryView
import com.bitwarden.vault.SshKeyView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkCipherPermissions
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkFido2CredentialList
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemState
import com.x8bit.bitwarden.ui.vault.feature.item.model.TotpCodeItemData
import kotlinx.collections.immutable.persistentListOf
import java.time.Instant

const val DEFAULT_IDENTITY_NAME: String = "Mr firstName middleName lastName"

val DEFAULT_ADDRESS: String =
    """
    address1
    address2
    address3
    city, state, postalCode
    country
    """
        .trimIndent()

fun createLoginView(isEmpty: Boolean): LoginView =
    LoginView(
        username = "username".takeUnless { isEmpty },
        password = "password".takeUnless { isEmpty },
        passwordRevisionDate = Instant.ofEpochSecond(1_000L).takeUnless { isEmpty },
        uris = listOf(
            LoginUriView(
                uri = "www.example.com",
                match = null,
                uriChecksum = null,
            ),
        )
            .takeUnless { isEmpty },
        totp = "otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example"
            .takeUnless { isEmpty },
        autofillOnPageLoad = false,
        fido2Credentials = createMockSdkFido2CredentialList(number = 1).takeUnless { isEmpty },
    )

@Suppress("CyclomaticComplexMethod")
fun createIdentityView(isEmpty: Boolean): IdentityView =
    IdentityView(
        title = "MR".takeUnless { isEmpty },
        firstName = "firstName".takeUnless { isEmpty },
        lastName = "lastName".takeUnless { isEmpty },
        middleName = "middleName".takeUnless { isEmpty },
        address1 = "address1".takeUnless { isEmpty },
        address2 = "address2".takeUnless { isEmpty },
        address3 = "address3".takeUnless { isEmpty },
        city = "city".takeUnless { isEmpty },
        state = "state".takeUnless { isEmpty },
        postalCode = "postalCode".takeUnless { isEmpty },
        country = "country".takeUnless { isEmpty },
        company = "company".takeUnless { isEmpty },
        email = "email".takeUnless { isEmpty },
        phone = "phone".takeUnless { isEmpty },
        ssn = "ssn".takeUnless { isEmpty },
        username = "username".takeUnless { isEmpty },
        passportNumber = "passportNumber".takeUnless { isEmpty },
        licenseNumber = "licenseNumber".takeUnless { isEmpty },
    )

fun createSshKeyView(isEmpty: Boolean): SshKeyView =
    SshKeyView(
        privateKey = "privateKey".takeUnless { isEmpty }.orEmpty(),
        publicKey = "publicKey".takeUnless { isEmpty }.orEmpty(),
        fingerprint = "fingerprint".takeUnless { isEmpty }.orEmpty(),
    )

fun createCipherView(type: CipherType, isEmpty: Boolean): CipherView =
    CipherView(
        id = null,
        organizationId = null,
        folderId = null,
        collectionIds = emptyList(),
        key = null,
        name = "mockName",
        notes = "Lots of notes".takeUnless { isEmpty },
        type = type,
        login = createLoginView(isEmpty = isEmpty),
        identity = createIdentityView(isEmpty = isEmpty),
        card = null,
        secureNote = null,
        favorite = false,
        reprompt = CipherRepromptType.PASSWORD,
        organizationUseTotp = false,
        edit = false,
        viewPassword = false,
        localData = null,
        attachments = listOf(
            AttachmentView(
                id = "attachment-id",
                sizeName = "11 MB",
                size = "11000000",
                url = "https://example.com",
                fileName = "test.mp4",
                key = "key",
            ),
        )
            .takeUnless { isEmpty },
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
        )
            .takeUnless { isEmpty },
        passwordHistory = listOf(
            PasswordHistoryView(
                password = "old_password",
                lastUsedDate = Instant.ofEpochSecond(1_000L),
            ),
        )
            .takeUnless { isEmpty },
        permissions = createMockSdkCipherPermissions(),
        creationDate = Instant.ofEpochSecond(1_000L),
        deletedDate = null,
        revisionDate = Instant.ofEpochSecond(1_000L),
        archivedDate = null,
        sshKey = createSshKeyView(isEmpty),
    )

fun createCommonContent(
    isEmpty: Boolean,
    isPremiumUser: Boolean,
    @DrawableRes iconResId: Int = BitwardenDrawable.ic_globe,
): VaultItemState.ViewState.Content.Common =
    if (isEmpty) {
        VaultItemState.ViewState.Content.Common(
            name = "mockName",
            created = BitwardenString.created.asText("Jan 1, 1970, 12:16\u202FAM"),
            lastUpdated = BitwardenString.last_edited.asText("Jan 1, 1970, 12:16\u202FAM"),
            notes = null,
            customFields = emptyList(),
            requiresCloneConfirmation = false,
            attachments = emptyList(),
            canDelete = true,
            canRestore = true,
            canAssignToCollections = true,
            canEdit = true,
            favorite = false,
            passwordHistoryCount = null,
            relatedLocations = persistentListOf(),
            iconData = IconData.Local(iconResId),
            hasOrganizations = true,
        )
    } else {
        VaultItemState.ViewState.Content.Common(
            name = "mockName",
            created = BitwardenString.created.asText("Jan 1, 1970, 12:16\u202FAM"),
            lastUpdated = BitwardenString.last_edited.asText("Jan 1, 1970, 12:16\u202FAM"),
            notes = "Lots of notes",
            customFields = listOf(
                FieldView(
                    name = "text",
                    value = "value",
                    type = FieldType.TEXT,
                    linkedId = null,
                )
                    .toCustomField(null),
                FieldView(
                    name = "hidden",
                    value = "value",
                    type = FieldType.HIDDEN,
                    linkedId = null,
                )
                    .toCustomField(null),
                FieldView(
                    name = "boolean",
                    value = "true",
                    type = FieldType.BOOLEAN,
                    linkedId = null,
                )
                    .toCustomField(null),
                FieldView(
                    name = "linked username",
                    value = null,
                    type = FieldType.LINKED,
                    linkedId = 100U,
                )
                    .toCustomField(null),
                FieldView(
                    name = "linked password",
                    value = null,
                    type = FieldType.LINKED,
                    linkedId = 101U,
                )
                    .toCustomField(null),
            ),
            requiresCloneConfirmation = true,
            attachments = listOf(
                VaultItemState.ViewState.Content.Common.AttachmentItem(
                    id = "attachment-id",
                    displaySize = "11 MB",
                    isLargeFile = true,
                    isDownloadAllowed = isPremiumUser,
                    url = "https://example.com",
                    title = "test.mp4",
                ),
            ),
            canDelete = true,
            canRestore = true,
            canAssignToCollections = true,
            canEdit = true,
            favorite = false,
            passwordHistoryCount = 1,
            relatedLocations = persistentListOf(),
            iconData = IconData.Local(iconResId),
            hasOrganizations = true,
        )
    }

fun createLoginContent(isEmpty: Boolean): VaultItemState.ViewState.Content.ItemType.Login =
    VaultItemState.ViewState.Content.ItemType.Login(
        username = "username".takeUnless { isEmpty },
        passwordData = VaultItemState.ViewState.Content.ItemType.Login.PasswordData(
            password = "password",
            isVisible = false,
            canViewPassword = false,
        )
            .takeUnless { isEmpty },
        uris = if (isEmpty) {
            emptyList()
        } else {
            listOf(
                VaultItemState.ViewState.Content.ItemType.Login.UriData(
                    uri = "www.example.com",
                    isCopyable = true,
                    isLaunchable = true,
                ),
            )
        },
        passwordRevisionDate = BitwardenString.password_last_updated
            .asText("Jan 1, 1970, 12:16\u202FAM")
            .takeUnless { isEmpty },
        isPremiumUser = true,
        totpCodeItemData = TotpCodeItemData(
            periodSeconds = 30,
            timeLeftSeconds = 15,
            verificationCode = "123456",
        )
            .takeUnless { isEmpty },
        fido2CredentialCreationDateText = BitwardenString.created_x
            .asText("Oct 27, 2023, 12:00\u202FPM")
            .takeUnless { isEmpty },
        canViewTotpCode = true,
    )

fun createIdentityContent(
    isEmpty: Boolean,
    address: String = DEFAULT_ADDRESS,
    identityName: String = DEFAULT_IDENTITY_NAME,
): VaultItemState.ViewState.Content.ItemType.Identity =
    VaultItemState.ViewState.Content.ItemType.Identity(
        username = "username".takeUnless { isEmpty },
        identityName = identityName.takeUnless { isEmpty },
        company = "company".takeUnless { isEmpty },
        ssn = "ssn".takeUnless { isEmpty },
        passportNumber = "passportNumber".takeUnless { isEmpty },
        licenseNumber = "licenseNumber".takeUnless { isEmpty },
        email = "email".takeUnless { isEmpty },
        phone = "phone".takeUnless { isEmpty },
        address = address.takeUnless { isEmpty },
    )

fun createSshKeyContent(isEmpty: Boolean): VaultItemState.ViewState.Content.ItemType.SshKey =
    VaultItemState.ViewState.Content.ItemType.SshKey(
        name = "mockName".takeUnless { isEmpty },
        privateKey = "privateKey".takeUnless { isEmpty }.orEmpty(),
        publicKey = "publicKey".takeUnless { isEmpty }.orEmpty(),
        fingerprint = "fingerprint".takeUnless { isEmpty }.orEmpty(),
        showPrivateKey = false,
    )
