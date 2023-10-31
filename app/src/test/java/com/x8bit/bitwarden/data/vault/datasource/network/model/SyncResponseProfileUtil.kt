package com.x8bit.bitwarden.data.vault.datasource.network.model

import java.time.LocalDateTime

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
    )

/**
 * Create a mock [SyncResponseJson.Profile.Organization] with a given [number].
 */
fun createMockOrganization(number: Int): SyncResponseJson.Profile.Organization =
    SyncResponseJson.Profile.Organization(
        shouldUsePolicies = false,
        keyConnectorUrl = "mockKeyConnectorUrl-$number",
        type = 1,
        seats = 1,
        isEnabled = false,
        providerType = 1,
        isResetPasswordEnrolled = false,
        shouldUseSecretsManager = false,
        maxCollections = 1,
        isSelfHost = false,
        shouldUseKeyConnector = false,
        permissions = createMockPermissions(),
        hasPublicAndPrivateKeys = false,
        providerId = "mockProviderId-$number",
        id = "mockId-$number",
        shouldUseGroups = false,
        shouldUseDirectory = false,
        key = "mockKey-$number",
        providerName = "mockProviderName-$number",
        shouldUsersGetPremium = false,
        maxStorageGb = 1,
        identifier = "mockIdentifier-$number",
        shouldUseSso = false,
        shouldUseCustomPermissions = false,
        isFamilySponsorshipAvailable = false,
        shouldUseResetPassword = false,
        planProductType = 1,
        accessSecretsManager = false,
        use2fa = false,
        familySponsorshipToDelete = false,
        userId = "mockUserId-$number",
        shouldUseActivateAutofillPolicy = false,
        shouldUseEvents = false,
        familySponsorshipFriendlyName = "mockFamilySponsorshipFriendlyName-$number",
        isKeyConnectorEnabled = false,
        shouldUseTotp = false,
        familySponsorshipLastSyncDate = LocalDateTime.parse("2023-10-27T12:00:00"),
        shouldUseScim = false,
        name = "mockName-$number",
        shouldUseApi = false,
        isSsoBound = false,
        familySponsorshipValidUntil = LocalDateTime.parse("2023-10-27T12:00:00"),
        status = 1,
    )

/**
 * Create a mock [SyncResponseJson.Profile.Permissions].
 */
fun createMockPermissions(): SyncResponseJson.Profile.Permissions =
    SyncResponseJson.Profile.Permissions(
        shouldManageGroups = false,
        shouldManageResetPassword = false,
        shouldAccessReports = false,
        shouldManagePolicies = false,
        shouldDeleteAnyCollection = false,
        shouldManageSso = false,
        shouldDeleteAssignedCollections = false,
        shouldManageUsers = false,
        shouldManageScim = false,
        shouldAccessImportExport = false,
        shouldEditAnyCollection = false,
        shouldAccessEventLogs = false,
        shouldCreateNewCollections = false,
        shouldEditAssignedCollections = false,
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
