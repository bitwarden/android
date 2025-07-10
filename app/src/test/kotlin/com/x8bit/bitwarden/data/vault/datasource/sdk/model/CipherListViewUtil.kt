package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.vault.CardListView
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.CipherPermissions
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CopyableCipherFields
import com.bitwarden.vault.Fido2CredentialListView
import com.bitwarden.vault.LocalDataView
import com.bitwarden.vault.LoginListView
import com.bitwarden.vault.LoginUriView
import java.time.Instant
import java.time.ZonedDateTime

/**
 * Default date time used for [ZonedDateTime] properties of mock objects.
 */
private const val DEFAULT_TIMESTAMP = "2023-10-27T12:00:00Z"

/**
 * Creates a mock [CipherListView] for testing. Defaults to a Login cipher. Set [type] to override
 * the default behavior.
 */
@Suppress("LongParameterList")
fun createMockCipherListView(
    number: Int,
    id: String = "mockId-$number",
    organizationId: String? = "mockOrganizationId-$number",
    folderId: String? = "mockId-$number",
    type: CipherListViewType = CipherListViewType.Login(
        createMockLoginListView(number = 1),
    ),
    reprompt: CipherRepromptType = CipherRepromptType.NONE,
    name: String = "mockName-$number",
    favorite: Boolean = false,
    collectionIds: List<String> = listOf("mockId-$number"),
    revisionDate: Instant = Instant.parse(DEFAULT_TIMESTAMP),
    creationDate: Instant = Instant.parse(DEFAULT_TIMESTAMP),
    attachments: UInt = 0U,
    organizationUseTotp: Boolean = false,
    edit: Boolean = false,
    viewPassword: Boolean = false,
    permissions: CipherPermissions? = createMockSdkCipherPermissions(),
    localData: LocalDataView? = null,
    key: String = "mockKey-$number",
    subtitle: String = "mockSubtitle-$number",
    hasOldAttachments: Boolean = false,
    copyableFields: List<CopyableCipherFields> = emptyList(),
    isDeleted: Boolean = false,
): CipherListView = CipherListView(
    id = id,
    organizationId = organizationId,
    folderId = folderId,
    type = type,
    reprompt = reprompt,
    name = name,
    favorite = favorite,
    collectionIds = collectionIds,
    revisionDate = revisionDate,
    creationDate = creationDate,
    deletedDate = if (isDeleted) Instant.parse(DEFAULT_TIMESTAMP) else null,
    attachments = attachments,
    organizationUseTotp = organizationUseTotp,
    edit = edit,
    viewPassword = viewPassword,
    permissions = permissions,
    localData = localData,
    key = key,
    subtitle = subtitle,
    hasOldAttachments = hasOldAttachments,
    copyableFields = copyableFields,
)

/**
 * Creates a mock [LoginListView] for testing.
 */
@Suppress("LongParameterList")
fun createMockLoginListView(
    number: Int,
    fido2Credentials: List<Fido2CredentialListView> = listOf(
        createMockFido2CredentialListView(number = 1),
    ),
    hasFido2: Boolean = true,
    username: String = "mockUsername-$number",
    totp: String? = "mockTotp-$number",
    uris: List<LoginUriView> = listOf(createMockUriView(number = 1)),
): LoginListView = LoginListView(
    fido2Credentials = fido2Credentials,
    hasFido2 = hasFido2,
    username = username,
    totp = totp,
    uris = uris,
)

/**
 * Creates a mock [Fido2CredentialListView] for testing.
 */
@Suppress("LongParameterList")
fun createMockFido2CredentialListView(
    number: Int,
    credentialId: String = "mockCredentialId-$number",
    rpId: String = "mockRpId-$number",
    userHandle: String = "mockUserHandle-$number",
    userName: String = "mockUserName-$number",
    userDisplayName: String = "mockUserDisplayName-$number",
): Fido2CredentialListView = Fido2CredentialListView(
    credentialId = credentialId,
    rpId = rpId,
    userHandle = userHandle,
    userName = userName,
    userDisplayName = userDisplayName,
)

/**
 * Creates a mock [CardListView] for testing.
 */
fun createMockCardListView(
    number: Int,
    brand: String = "mockBrand-$number",
): CardListView = CardListView(
    brand = brand,
)
