package com.x8bit.bitwarden.data.auth.repository.util

import com.bitwarden.core.data.util.decodeFromStringOrNull
import com.bitwarden.network.model.MemberDecryptionType
import com.bitwarden.network.model.OrganizationStatusType
import com.bitwarden.network.model.OrganizationType
import com.bitwarden.network.model.ProductTierType
import com.bitwarden.network.model.ProviderType
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.organizations.OrganizationUserStatusType
import com.bitwarden.organizations.OrganizationUserType
import com.bitwarden.organizations.Permissions
import com.bitwarden.organizations.ProfileOrganization
import com.bitwarden.policies.PolicyType
import com.bitwarden.policies.PolicyView
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.PolicyInformation
import kotlinx.serialization.json.Json
import com.bitwarden.organizations.MemberDecryptionType as SdkMemberDecryptionType
import com.bitwarden.organizations.ProductTierType as SdkProductTierType
import com.bitwarden.organizations.ProviderType as SdkProviderType

private val JSON = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
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
 * the [SyncResponseJson.Profile.Organization.name] is not present.
 */
@Suppress("LongMethod")
private fun SyncResponseJson.Profile.Organization.toSdkProfileOrganization(): ProfileOrganization? =
    this.name?.let {
        ProfileOrganization(
            id = this.id,
            name = it,
            status = this.status.toSdkOrganizationUserStatusType(),
            type = this.type.toSdkOrganizationUserType(),
            enabled = this.isEnabled,
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
            useActivateAutofillPolicy = this.useActivateAutofillPolicy,
            useAutomaticUserConfirmation = this.useAutomaticUserConfirmation,
            selfHost = this.isSelfHost,
            usersGetPremium = this.shouldUsersGetPremium,
            seats = this.seats,
            maxCollections = this.maxCollections,
            maxStorageGb = this.maxStorageGb,
            ssoBound = this.ssoBound,
            identifier = this.identifier,
            permissions = this.permissions.toSdkPermissions(),
            resetPasswordEnrolled = this.resetPasswordEnrolled,
            userId = this.userId,
            organizationUserId = this.organizationUserId,
            hasPublicAndPrivateKeys = this.hasPublicAndPrivateKeys,
            providerId = this.providerId,
            providerName = this.providerName,
            providerType = this.providerType?.toSdkProviderType(),
            isProviderUser = this.isProviderUser,
            isMember = this.isMember,
            familySponsorshipFriendlyName = this.familySponsorshipFriendlyName,
            familySponsorshipAvailable = this.familySponsorshipAvailable,
            productTierType = this.productTierType.toSdkProductTierType(),
            keyConnectorEnabled = this.isKeyConnectorEnabled,
            keyConnectorUrl = this.keyConnectorUrl,
            familySponsorshipLastSyncDate = this.familySponsorshipLastSyncDate,
            familySponsorshipValidUntil = this.familySponsorshipValidUntil,
            familySponsorshipToDelete = this.familySponsorshipToDelete,
            accessSecretsManager = this.accessSecretsManager,
            limitCollectionCreation = this.limitCollectionCreation,
            limitCollectionDeletion = this.limitCollectionDeletion,
            limitItemDeletion = this.limitItemDeletion,
            allowAdminAccessToAllCollectionItems = this.allowAdminAccessToAllCollectionItems,
            userIsManagedByOrganization = this.userIsClaimedByOrganization,
            useAccessIntelligence = this.useAccessIntelligence,
            useAdminSponsoredFamilies = this.useAdminSponsoredFamilies,
            useDisableSmAdsForUsers = this.useDisableSmAdsForUsers,
            isAdminInitiated = this.isAdminInitiated,
            ssoEnabled = this.ssoEnabled,
            ssoMemberDecryptionType = this.ssoMemberDecryptionType?.toSdkMemberDecryptionType(),
            usePhishingBlocker = this.usePhishingBlocker,
            useMyItems = this.useMyItems,
        )
    }

/**
 * Convert the JSON data of the [PolicyView] object into [PolicyInformation] data.
 */
val PolicyView.policyInformation: PolicyInformation?
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

            else -> null
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
