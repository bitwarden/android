package com.x8bit.bitwarden.data.auth.repository.util

import com.bitwarden.core.data.serializer.SafeListSerializer
import com.bitwarden.core.data.util.decodeFromStringOrNull
import com.bitwarden.network.model.MemberDecryptionType
import com.bitwarden.network.model.OrganizationStatusType
import com.bitwarden.network.model.OrganizationType
import com.bitwarden.network.model.ProductTierType
import com.bitwarden.network.model.ProviderType
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.organizations.CollectionManagementSettings
import com.bitwarden.organizations.FamilySponsorship
import com.bitwarden.organizations.OrganizationDetails
import com.bitwarden.organizations.OrganizationMembership
import com.bitwarden.organizations.OrganizationPlan
import com.bitwarden.organizations.OrganizationProvider
import com.bitwarden.organizations.OrganizationUserStatusType
import com.bitwarden.organizations.OrganizationUserType
import com.bitwarden.organizations.Permissions
import com.bitwarden.organizations.ProfileOrganization
import com.bitwarden.organizations.SsoSettings
import com.bitwarden.policies.Policy
import com.bitwarden.policies.PolicyType
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import com.bitwarden.organizations.MemberDecryptionType as SdkMemberDecryptionType
import com.bitwarden.organizations.ProductTierType as SdkProductTierType
import com.bitwarden.organizations.ProviderType as SdkProviderType

private val JSON = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    serializersModule = SerializersModule {
        contextual(List::class) { args -> SafeListSerializer(args.first()) }
    }
}

/**
 * Maps the given [SyncResponseJson.Profile.Organization] to an [Organization] or `null` if the
 * [SyncResponseJson.Profile.Organization.name] is not present.
 */
fun SyncResponseJson.Profile.Organization.toOrganization(): Organization? =
    this.name?.let {
        Organization(
            id = this.id,
            name = it,
            isKeyConnectorEnabled = this.isKeyConnectorEnabled,
            role = this.type,
            shouldManageResetPassword = this.permissions.shouldManageResetPassword,
            keyConnectorUrl = this.keyConnectorUrl,
            userIsClaimedByOrganization = this.userIsClaimedByOrganization,
            limitItemDeletion = this.limitItemDeletion,
            shouldUseEvents = this.shouldUseEvents,
        )
    }

/**
 * Maps the given list of [SyncResponseJson.Profile.Organization] to a list of
 * [Organization]s.
 */
fun List<SyncResponseJson.Profile.Organization>.toOrganizations(): List<Organization> =
    this.mapNotNull { it.toOrganization() }

/**
 * Maps the given list of [SyncResponseJson.Profile.Organization] to a list of
 * [ProfileOrganization]s.
 */
@Suppress("MaxLineLength")
fun List<SyncResponseJson.Profile.Organization>.toSdkProfileOrganizations(): List<ProfileOrganization> =
    this.mapNotNull { it.toSdkProfileOrganization() }

/**
 * Maps the given [SyncResponseJson.Profile.Organization] to a [ProfileOrganization] or `null` if
 * the [SyncResponseJson.Profile.Organization.name] or
 * [SyncResponseJson.Profile.Organization.userId] is not present.
 */
@Suppress("LongMethod")
private fun SyncResponseJson.Profile.Organization.toSdkProfileOrganization(): ProfileOrganization? {
    val organizationName = this.name ?: return null
    // The SDK requires a non-null user ID, so an organization without one cannot be mapped.
    val memberUserId = this.userId ?: return null
    return ProfileOrganization(
        details = OrganizationDetails(
            id = this.id,
            name = organizationName,
            enabled = this.isEnabled,
            hasPublicAndPrivateKeys = this.hasPublicAndPrivateKeys,
        ),
        plan = OrganizationPlan(
            productTierType = this.productTierType.toSdkProductTierType(),
            seats = this.seats,
            maxCollections = this.maxCollections,
            maxStorageGb = this.maxStorageGb,
            selfHost = this.isSelfHost,
            usersGetPremium = this.shouldUsersGetPremium,
            usePolicies = this.shouldUsePolicies,
            useGroups = this.shouldUseGroups,
            useDirectory = this.shouldUseDirectory,
            useEvents = this.shouldUseEvents,
            useTotp = this.shouldUseTotp,
            use2fa = this.use2fa,
            useApi = this.shouldUseApi,
            useSso = this.useSso,
            useOrganizationDomains = this.useOrganizationDomains,
            useKeyConnector = this.shouldUseKeyConnector,
            useScim = this.useScim,
            useCustomPermissions = this.useCustomPermissions,
            useResetPassword = this.useResetPassword,
            useSecretsManager = this.useSecretsManager,
            usePasswordManager = this.usePasswordManager,
            // Not provided by the sync response.
            usePam = false,
            useActivateAutofillPolicy = this.useActivateAutofillPolicy,
            useAutomaticUserConfirmation = this.useAutomaticUserConfirmation,
            useAccessIntelligence = this.useAccessIntelligence,
            useAdminSponsoredFamilies = this.useAdminSponsoredFamilies,
            useDisableSmAdsForUsers = this.useDisableSmAdsForUsers,
            usePhishingBlocker = this.usePhishingBlocker,
            useMyItems = this.useMyItems,
            // Not provided by the sync response.
            useInviteLinks = false,
        ),
        membership = OrganizationMembership(
            status = this.status.toSdkOrganizationUserStatusType(),
            type = this.type.toSdkOrganizationUserType(),
            permissions = this.permissions.toSdkPermissions(),
            userId = memberUserId,
            organizationUserId = this.organizationUserId,
            isMember = this.isMember,
            isProviderUser = this.isProviderUser,
            resetPasswordEnrolled = this.resetPasswordEnrolled,
            ssoBound = this.ssoBound,
            userIsClaimedByOrganization = this.userIsClaimedByOrganization,
            accessSecretsManager = this.accessSecretsManager,
            accessPam = this.accessPam,
        ),
        collectionManagement = CollectionManagementSettings(
            limitCollectionCreation = this.limitCollectionCreation,
            limitCollectionDeletion = this.limitCollectionDeletion,
            limitItemDeletion = this.limitItemDeletion,
            allowAdminAccessToAllCollectionItems = this.allowAdminAccessToAllCollectionItems,
        ),
        provider = this.providerId?.let { providerId ->
            this.providerType?.let { providerType ->
                OrganizationProvider(
                    id = providerId,
                    type = providerType.toSdkProviderType(),
                    name = this.providerName,
                )
            }
        },
        sso = SsoSettings(
            enabled = this.ssoEnabled,
            identifier = this.identifier,
            memberDecryptionType = this.ssoMemberDecryptionType?.toSdkMemberDecryptionType(),
            keyConnectorEnabled = this.isKeyConnectorEnabled,
            keyConnectorUrl = this.keyConnectorUrl,
        ),
        familySponsorship = FamilySponsorship(
            available = this.familySponsorshipAvailable,
            friendlyName = this.familySponsorshipFriendlyName,
            validUntil = this.familySponsorshipValidUntil,
            lastSyncDate = this.familySponsorshipLastSyncDate,
            toDelete = this.familySponsorshipToDelete,
            isAdminInitiated = this.isAdminInitiated,
        ),
    )
}

/**
 * Convert the JSON data of the [Policy] object into [PolicyInformation] data.
 */
val Policy.policyInformation: PolicyInformation?
    get() = data?.let {
        when (type) {
            PolicyType.MASTER_PASSWORD -> {
                JSON.decodeFromStringOrNull<PolicyInformation.MasterPassword>(it)
            }

            PolicyType.PASSWORD_GENERATOR -> {
                JSON.decodeFromStringOrNull<PolicyInformation.PasswordGenerator>(it)
            }

            PolicyType.MAXIMUM_VAULT_TIMEOUT -> {
                JSON.decodeFromStringOrNull<PolicyInformation.VaultTimeout>(it)
            }

            PolicyType.SEND_OPTIONS -> {
                JSON.decodeFromStringOrNull<PolicyInformation.SendOptions>(it)
            }

            PolicyType.SEND_CONTROLS -> {
                JSON.decodeFromStringOrNull<PolicyInformation.SendControls>(it)
            }

            PolicyType.ORGANIZATION_USER_NOTIFICATION -> {
                JSON.decodeFromStringOrNull<PolicyInformation.OrganizationUserNotification>(it)
            }

            PolicyType.TWO_FACTOR_AUTHENTICATION,
            PolicyType.SINGLE_ORG,
            PolicyType.REQUIRE_SSO,
            PolicyType.ORGANIZATION_DATA_OWNERSHIP,
            PolicyType.DISABLE_SEND,
            PolicyType.RESET_PASSWORD,
            PolicyType.DISABLE_PERSONAL_VAULT_EXPORT,
            PolicyType.ACTIVATE_AUTOFILL,
            PolicyType.AUTOMATIC_APP_LOG_IN,
            PolicyType.FREE_FAMILIES_SPONSORSHIP,
            PolicyType.REMOVE_UNLOCK_WITH_PIN,
            PolicyType.RESTRICTED_ITEM_TYPES,
            PolicyType.URI_MATCH_DEFAULTS,
            PolicyType.AUTOTYPE_DEFAULT_SETTING,
            PolicyType.AUTOMATIC_USER_CONFIRMATION,
            PolicyType.BLOCK_CLAIMED_DOMAIN_ACCOUNT_CREATION,
            PolicyType.FILL_ASSIST,
            PolicyType.AUTOTYPE_GA_POLICY,
                -> null
        }
    }

private fun SyncResponseJson.Profile.Permissions.toSdkPermissions(): Permissions =
    Permissions(
        accessEventLogs = this.accessEventLogs,
        accessImportExport = this.accessImportExport,
        accessReports = this.accessReports,
        createNewCollections = this.createNewCollections,
        editAnyCollection = this.editAnyCollection,
        deleteAnyCollection = this.deleteAnyCollection,
        manageGroups = this.manageGroups,
        manageSso = this.manageSso,
        managePolicies = this.shouldManagePolicies,
        manageUsers = this.manageUsers,
        manageResetPassword = this.shouldManageResetPassword,
        manageScim = this.manageScim,
        manageAccessRules = this.manageAccessRules,
        // Not provided by the sync response.
        manageRotation = false,
    )

private fun OrganizationStatusType.toSdkOrganizationUserStatusType(): OrganizationUserStatusType =
    when (this) {
        OrganizationStatusType.REVOKED -> OrganizationUserStatusType.REVOKED
        OrganizationStatusType.INVITED -> OrganizationUserStatusType.INVITED
        OrganizationStatusType.ACCEPTED -> OrganizationUserStatusType.ACCEPTED
        OrganizationStatusType.CONFIRMED -> OrganizationUserStatusType.CONFIRMED
        OrganizationStatusType.STAGED -> OrganizationUserStatusType.STAGED
    }

private fun OrganizationType.toSdkOrganizationUserType(): OrganizationUserType =
    when (this) {
        OrganizationType.OWNER -> OrganizationUserType.OWNER
        OrganizationType.ADMIN -> OrganizationUserType.ADMIN
        OrganizationType.USER -> OrganizationUserType.USER
        OrganizationType.CUSTOM -> OrganizationUserType.CUSTOM
    }

private fun ProviderType.toSdkProviderType(): SdkProviderType =
    when (this) {
        ProviderType.MSP -> SdkProviderType.MSP
        ProviderType.RESELLER -> SdkProviderType.RESELLER
        ProviderType.BUSINESS_UNIT -> SdkProviderType.BUSINESS_UNIT
    }

private fun ProductTierType.toSdkProductTierType(): SdkProductTierType =
    when (this) {
        ProductTierType.FREE -> SdkProductTierType.FREE
        ProductTierType.FAMILIES -> SdkProductTierType.FAMILIES
        ProductTierType.TEAMS -> SdkProductTierType.TEAMS
        ProductTierType.ENTERPRISE -> SdkProductTierType.ENTERPRISE
        ProductTierType.TEAMS_STARTER -> SdkProductTierType.TEAMS_STARTER
    }

private fun MemberDecryptionType.toSdkMemberDecryptionType(): SdkMemberDecryptionType =
    when (this) {
        MemberDecryptionType.MASTER_PASSWORD -> SdkMemberDecryptionType.MASTER_PASSWORD
        MemberDecryptionType.KEY_CONNECTOR -> SdkMemberDecryptionType.KEY_CONNECTOR
        MemberDecryptionType.TRUSTED_DEVICE_ENCRYPTION -> {
            SdkMemberDecryptionType.TRUSTED_DEVICE_ENCRYPTION
        }
    }
