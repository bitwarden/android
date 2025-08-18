@file:Suppress("LongParameterList")

package com.bitwarden.network.model

import java.time.ZonedDateTime

/**
 * Create a mock [SyncResponseJson.Profile] with a given [number].
 */
fun createMockProfile(
    number: Int,
    providerOrganizations: List<SyncResponseJson.Profile.Organization>? = listOf(
        createMockOrganization(number = number),
    ),
    isPremiumFromOrganization: Boolean = false,
    shouldForcePasswordReset: Boolean = false,
    avatarColor: String? = "mockAvatarColor-$number",
    isEmailVerified: Boolean = false,
    isTwoFactorEnabled: Boolean = false,
    privateKey: String? = "mockPrivateKey-$number",
    accountKeys: AccountKeysJson? = createMockAccountKeysJson(number = number),
    isPremium: Boolean = false,
    culture: String? = "mockCulture-$number",
    name: String? = "mockName-$number",
    organizations: List<SyncResponseJson.Profile.Organization>? = listOf(
        createMockOrganization(number = number),
    ),
    shouldUseKeyConnector: Boolean = false,
    id: String = "mockId-$number",
    masterPasswordHint: String? = "mockMasterPasswordHint-$number",
    email: String? = "mockEmail-$number",
    key: String? = "mockKey-$number",
    securityStamp: String? = "mockSecurityStamp-$number",
    providers: List<SyncResponseJson.Profile.Provider>? = listOf(
        createMockProvider(number = number),
    ),
    creationDate: ZonedDateTime = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
): SyncResponseJson.Profile =
    SyncResponseJson.Profile(
        providerOrganizations = providerOrganizations,
        isPremiumFromOrganization = isPremiumFromOrganization,
        shouldForcePasswordReset = shouldForcePasswordReset,
        avatarColor = avatarColor,
        isEmailVerified = isEmailVerified,
        isTwoFactorEnabled = isTwoFactorEnabled,
        privateKey = privateKey,
        accountKeys = accountKeys,
        isPremium = isPremium,
        culture = culture,
        name = name,
        organizations = organizations,
        shouldUseKeyConnector = shouldUseKeyConnector,
        id = id,
        masterPasswordHint = masterPasswordHint,
        email = email,
        key = key,
        securityStamp = securityStamp,
        providers = providers,
        creationDate = creationDate,
    )

/**
 * Create a mock [SyncResponseJson.Profile.Organization] with a given [number].
 */
fun createMockOrganization(
    number: Int,
    shouldUsePolicies: Boolean = false,
    shouldUseKeyConnector: Boolean = false,
    keyConnectorUrl: String? = "mockKeyConnectorUrl-$number",
    type: OrganizationType = OrganizationType.ADMIN,
    seats: Int? = 1,
    isEnabled: Boolean = false,
    providerType: Int? = 1,
    maxCollections: Int? = 1,
    isSelfHost: Boolean = false,
    permissions: SyncResponseJson.Profile.Permissions = createMockPermissions(),
    providerId: String? = "mockProviderId-$number",
    id: String = "mockId-$number",
    shouldUseGroups: Boolean = false,
    shouldUseDirectory: Boolean = false,
    key: String? = "mockKey-$number",
    providerName: String? = "mockProviderName-$number",
    shouldUsersGetPremium: Boolean = false,
    maxStorageGb: Int? = 1,
    identifier: String? = "mockIdentifier-$number",
    use2fa: Boolean = false,
    familySponsorshipToDelete: Boolean? = false,
    userId: String? = "mockUserId-$number",
    shouldUseEvents: Boolean = false,
    familySponsorshipFriendlyName: String? = "mockFamilySponsorshipFriendlyName-$number",
    shouldUseTotp: Boolean = false,
    familySponsorshipLastSyncDate: ZonedDateTime? = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
    name: String? = "mockName-$number",
    shouldUseApi: Boolean = false,
    familySponsorshipValidUntil: ZonedDateTime? = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
    status: OrganizationStatusType = OrganizationStatusType.ACCEPTED,
    userIsClaimedByOrganization: Boolean = false,
    limitItemDeletion: Boolean = false,
): SyncResponseJson.Profile.Organization =
    SyncResponseJson.Profile.Organization(
        shouldUsePolicies = shouldUsePolicies,
        shouldUseKeyConnector = shouldUseKeyConnector,
        keyConnectorUrl = keyConnectorUrl,
        type = type,
        seats = seats,
        isEnabled = isEnabled,
        providerType = providerType,
        maxCollections = maxCollections,
        isSelfHost = isSelfHost,
        permissions = permissions,
        providerId = providerId,
        id = id,
        shouldUseGroups = shouldUseGroups,
        shouldUseDirectory = shouldUseDirectory,
        key = key,
        providerName = providerName,
        shouldUsersGetPremium = shouldUsersGetPremium,
        maxStorageGb = maxStorageGb,
        identifier = identifier,
        use2fa = use2fa,
        familySponsorshipToDelete = familySponsorshipToDelete,
        userId = userId,
        shouldUseEvents = shouldUseEvents,
        familySponsorshipFriendlyName = familySponsorshipFriendlyName,
        shouldUseTotp = shouldUseTotp,
        familySponsorshipLastSyncDate = familySponsorshipLastSyncDate,
        name = name,
        shouldUseApi = shouldUseApi,
        familySponsorshipValidUntil = familySponsorshipValidUntil,
        status = status,
        userIsClaimedByOrganization = userIsClaimedByOrganization,
        limitItemDeletion = limitItemDeletion,
    )

/**
 * Create a mock set of organization keys with the given [number].
 */
fun createMockOrganizationKeys(
    number: Int,
    organization: SyncResponseJson.Profile.Organization = createMockOrganization(number = number),
): Map<String, String> =
    mapOf(organization.id to requireNotNull(organization.key))

/**
 * Create a mock [SyncResponseJson.Profile.Permissions].
 */
fun createMockPermissions(
    shouldManageResetPassword: Boolean = false,
    shouldManagePolicies: Boolean = false,
): SyncResponseJson.Profile.Permissions =
    SyncResponseJson.Profile.Permissions(
        shouldManageResetPassword = shouldManageResetPassword,
        shouldManagePolicies = shouldManagePolicies,
    )

/**
 * Create a mock [SyncResponseJson.Profile.Provider] with a given [number].
 */
fun createMockProvider(
    number: Int,
    shouldUseEvents: Boolean = false,
    permissions: SyncResponseJson.Profile.Permissions = createMockPermissions(),
    name: String? = "mockName-$number",
    id: String = "mockId-$number",
    type: Int = 1,
    userId: String? = "mockUserId-$number",
    key: String? = "mockKey-$number",
    isEnabled: Boolean = false,
    status: Int = 1,
): SyncResponseJson.Profile.Provider =
    SyncResponseJson.Profile.Provider(
        shouldUseEvents = shouldUseEvents,
        permissions = permissions,
        name = name,
        id = id,
        type = type,
        userId = userId,
        key = key,
        isEnabled = isEnabled,
        status = status,
    )
