@file:Suppress("LongParameterList")

package com.x8bit.bitwarden.data.auth.repository.model

import com.bitwarden.organizations.CollectionManagementSettings
import com.bitwarden.organizations.FamilySponsorship
import com.bitwarden.organizations.MemberDecryptionType
import com.bitwarden.organizations.OrganizationDetails
import com.bitwarden.organizations.OrganizationMembership
import com.bitwarden.organizations.OrganizationPlan
import com.bitwarden.organizations.OrganizationProvider
import com.bitwarden.organizations.OrganizationUserStatusType
import com.bitwarden.organizations.OrganizationUserType
import com.bitwarden.organizations.Permissions
import com.bitwarden.organizations.ProductTierType
import com.bitwarden.organizations.ProfileOrganization
import com.bitwarden.organizations.ProviderType
import com.bitwarden.organizations.SsoSettings
import java.time.Instant

/**
 * Creates a mock [ProfileOrganization] with the given [number].
 */
@Suppress("LongMethod")
fun createMockSdkProfileOrganization(
    number: Int,
    usePolicies: Boolean = false,
    keyConnectorEnabled: Boolean = false,
    keyConnectorUrl: String? = "mockKeyConnectorUrl-$number",
    type: OrganizationUserType = OrganizationUserType.ADMIN,
    seats: UInt? = 1u,
    enabled: Boolean = false,
    providerType: ProviderType? = ProviderType.RESELLER,
    maxCollections: UInt? = 1u,
    selfHost: Boolean = false,
    permissions: Permissions = createMockSdkPermissions(),
    providerId: String? = "mockProviderId-$number",
    id: String = "mockId-$number",
    useGroups: Boolean = false,
    useDirectory: Boolean = false,
    providerName: String? = "mockProviderName-$number",
    usersGetPremium: Boolean = false,
    maxStorageGb: UInt? = 1u,
    identifier: String? = "mockIdentifier-$number",
    use2fa: Boolean = false,
    familySponsorshipToDelete: Boolean? = false,
    userId: String = "mockUserId-$number",
    shouldUseEvents: Boolean = false,
    familySponsorshipFriendlyName: String? = "mockFamilySponsorshipFriendlyName-$number",
    useTotp: Boolean = false,
    familySponsorshipLastSyncDate: Instant? = Instant.parse("2023-10-27T12:00:00Z"),
    name: String = "mockName-$number",
    useApi: Boolean = false,
    familySponsorshipValidUntil: Instant? = Instant.parse("2023-10-27T12:00:00Z"),
    status: OrganizationUserStatusType = OrganizationUserStatusType.CONFIRMED,
    userIsClaimedByOrganization: Boolean = false,
    limitItemDeletion: Boolean = false,
    useSso: Boolean = false,
    useOrganizationDomains: Boolean = false,
    shouldUseKeyConnector: Boolean = false,
    useScim: Boolean = false,
    useCustomPermissions: Boolean = false,
    useResetPassword: Boolean = false,
    useSecretsManager: Boolean = false,
    usePasswordManager: Boolean = false,
    useActivateAutofillPolicy: Boolean = false,
    useAutomaticUserConfirmation: Boolean = false,
    ssoBound: Boolean = false,
    resetPasswordEnrolled: Boolean = false,
    organizationUserId: String? = "mockOrganizationUserId-$number",
    hasPublicAndPrivateKeys: Boolean = false,
    isProviderUser: Boolean = false,
    isMember: Boolean = false,
    familySponsorshipAvailable: Boolean = false,
    productTierType: ProductTierType = ProductTierType.FREE,
    accessSecretsManager: Boolean = false,
    accessPam: Boolean = false,
    limitCollectionCreation: Boolean = false,
    limitCollectionDeletion: Boolean = false,
    allowAdminAccessToAllCollectionItems: Boolean = false,
    useAccessIntelligence: Boolean = false,
    useAdminSponsoredFamilies: Boolean = false,
    useDisableSmAdsForUsers: Boolean = false,
    isAdminInitiated: Boolean = false,
    ssoEnabled: Boolean = false,
    ssoMemberDecryptionType: MemberDecryptionType? = null,
    usePhishingBlocker: Boolean = false,
    useMyItems: Boolean = false,
    usePam: Boolean = false,
    useInviteLinks: Boolean = false,
): ProfileOrganization =
    ProfileOrganization(
        details = OrganizationDetails(
            id = id,
            name = name,
            enabled = enabled,
            hasPublicAndPrivateKeys = hasPublicAndPrivateKeys,
        ),
        plan = OrganizationPlan(
            productTierType = productTierType,
            seats = seats,
            maxCollections = maxCollections,
            maxStorageGb = maxStorageGb,
            selfHost = selfHost,
            usersGetPremium = usersGetPremium,
            usePolicies = usePolicies,
            useGroups = useGroups,
            useDirectory = useDirectory,
            useEvents = shouldUseEvents,
            useTotp = useTotp,
            use2fa = use2fa,
            useApi = useApi,
            useSso = useSso,
            useOrganizationDomains = useOrganizationDomains,
            useKeyConnector = shouldUseKeyConnector,
            useScim = useScim,
            useCustomPermissions = useCustomPermissions,
            useResetPassword = useResetPassword,
            useSecretsManager = useSecretsManager,
            usePasswordManager = usePasswordManager,
            usePam = usePam,
            useActivateAutofillPolicy = useActivateAutofillPolicy,
            useAutomaticUserConfirmation = useAutomaticUserConfirmation,
            useAccessIntelligence = useAccessIntelligence,
            useAdminSponsoredFamilies = useAdminSponsoredFamilies,
            useDisableSmAdsForUsers = useDisableSmAdsForUsers,
            usePhishingBlocker = usePhishingBlocker,
            useMyItems = useMyItems,
            useInviteLinks = useInviteLinks,
        ),
        membership = OrganizationMembership(
            status = status,
            type = type,
            permissions = permissions,
            userId = userId,
            organizationUserId = organizationUserId,
            isMember = isMember,
            isProviderUser = isProviderUser,
            resetPasswordEnrolled = resetPasswordEnrolled,
            ssoBound = ssoBound,
            userIsClaimedByOrganization = userIsClaimedByOrganization,
            accessSecretsManager = accessSecretsManager,
            accessPam = accessPam,
        ),
        collectionManagement = CollectionManagementSettings(
            limitCollectionCreation = limitCollectionCreation,
            limitCollectionDeletion = limitCollectionDeletion,
            limitItemDeletion = limitItemDeletion,
            allowAdminAccessToAllCollectionItems = allowAdminAccessToAllCollectionItems,
        ),
        provider = providerId
            ?.let { id ->
                providerType?.let { type ->
                    OrganizationProvider(id = id, type = type, name = providerName)
                }
            },
        sso = SsoSettings(
            enabled = ssoEnabled,
            identifier = identifier,
            memberDecryptionType = ssoMemberDecryptionType,
            keyConnectorEnabled = keyConnectorEnabled,
            keyConnectorUrl = keyConnectorUrl,
        ),
        familySponsorship = FamilySponsorship(
            available = familySponsorshipAvailable,
            friendlyName = familySponsorshipFriendlyName,
            validUntil = familySponsorshipValidUntil,
            lastSyncDate = familySponsorshipLastSyncDate,
            toDelete = familySponsorshipToDelete,
            isAdminInitiated = isAdminInitiated,
        ),
    )

/**
 * Create a mock [Permissions].
 */
fun createMockSdkPermissions(
    shouldManageResetPassword: Boolean = false,
    shouldManagePolicies: Boolean = false,
    accessEventLogs: Boolean = false,
    accessImportExport: Boolean = false,
    accessReports: Boolean = false,
    createNewCollections: Boolean = false,
    editAnyCollection: Boolean = false,
    deleteAnyCollection: Boolean = false,
    manageGroups: Boolean = false,
    manageSso: Boolean = false,
    manageUsers: Boolean = false,
    manageScim: Boolean = false,
    manageAccessRules: Boolean = false,
    manageRotation: Boolean = false,
): Permissions =
    Permissions(
        manageResetPassword = shouldManageResetPassword,
        managePolicies = shouldManagePolicies,
        accessEventLogs = accessEventLogs,
        accessImportExport = accessImportExport,
        accessReports = accessReports,
        createNewCollections = createNewCollections,
        editAnyCollection = editAnyCollection,
        deleteAnyCollection = deleteAnyCollection,
        manageGroups = manageGroups,
        manageSso = manageSso,
        manageUsers = manageUsers,
        manageScim = manageScim,
        manageAccessRules = manageAccessRules,
        manageRotation = manageRotation,
    )
