package com.x8bit.bitwarden.data.vault.datasource.network.model

import java.time.ZonedDateTime

/**
 * Create a mock [SyncResponseJson.Profile] with a given [number].
 */
fun createMockProfile(number: Int): SyncResponseJson.Profile =
    SyncResponseJson.Profile(
        providerOrganizations = listOf(createMockOrganization(number = number)),
        isPremiumFromOrganization = false,
        shouldForcePasswordReset = false,
        avatarColor = "mockAvatarColor-$number",
        isEmailVerified = false,
        isTwoFactorEnabled = false,
        privateKey = "mockPrivateKey-$number",
        isPremium = false,
        culture = "mockCulture-$number",
        name = "mockName-$number",
        organizations = listOf(createMockOrganization(number = number)),
        shouldUseKeyConnector = false,
        id = "mockId-$number",
        masterPasswordHint = "mockMasterPasswordHint-$number",
        email = "mockEmail-$number",
        key = "mockKey-$number",
        securityStamp = "mockSecurityStamp-$number",
        providers = listOf(createMockProvider(number = number)),
        creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
    )

/**
 * Create a mock [SyncResponseJson.Profile.Organization] with a given [number].
 */
fun createMockOrganization(
    number: Int,
    isEnabled: Boolean = false,
    shouldUsePolicies: Boolean = false,
    shouldManageResetPassword: Boolean = false,
): SyncResponseJson.Profile.Organization =
    SyncResponseJson.Profile.Organization(
        shouldUsePolicies = shouldUsePolicies,
        shouldUseKeyConnector = false,
        keyConnectorUrl = "mockKeyConnectorUrl-$number",
        type = OrganizationType.ADMIN,
        seats = 1,
        isEnabled = isEnabled,
        providerType = 1,
        maxCollections = 1,
        isSelfHost = false,
        permissions = createMockPermissions(shouldManageResetPassword = shouldManageResetPassword),
        providerId = "mockProviderId-$number",
        id = "mockId-$number",
        shouldUseGroups = false,
        shouldUseDirectory = false,
        key = "mockKey-$number",
        providerName = "mockProviderName-$number",
        shouldUsersGetPremium = false,
        maxStorageGb = 1,
        identifier = "mockIdentifier-$number",
        use2fa = false,
        familySponsorshipToDelete = false,
        userId = "mockUserId-$number",
        shouldUseEvents = false,
        familySponsorshipFriendlyName = "mockFamilySponsorshipFriendlyName-$number",
        shouldUseTotp = false,
        familySponsorshipLastSyncDate = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
        name = "mockName-$number",
        shouldUseApi = false,
        familySponsorshipValidUntil = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
        status = OrganizationStatusType.ACCEPTED,
    )

/**
 * Create a mock set of organization keys with the given [number].
 */
fun createMockOrganizationKeys(number: Int): Map<String, String> =
    createMockOrganization(number = number)
        .let { mapOf(it.id to requireNotNull(it.key)) }

/**
 * Create a mock [SyncResponseJson.Profile.Permissions].
 */
fun createMockPermissions(
    shouldManageResetPassword: Boolean = false,
): SyncResponseJson.Profile.Permissions =
    SyncResponseJson.Profile.Permissions(
        shouldManageResetPassword = shouldManageResetPassword,
        shouldManagePolicies = false,
    )

/**
 * Create a mock [SyncResponseJson.Profile.Provider] with a given [number].
 */
fun createMockProvider(number: Int): SyncResponseJson.Profile.Provider =
    SyncResponseJson.Profile.Provider(
        shouldUseEvents = false,
        permissions = createMockPermissions(),
        name = "mockName-$number",
        id = "mockId-$number",
        type = 1,
        userId = "mockUserId-$number",
        key = "mockKey-$number",
        isEnabled = false,
        status = 1,
    )
