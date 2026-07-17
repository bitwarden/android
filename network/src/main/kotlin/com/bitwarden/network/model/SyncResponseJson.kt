package com.bitwarden.network.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonObject
import java.time.Instant

private const val DEFAULT_FIDO_2_KEY_TYPE = "public-key"
private const val DEFAULT_FIDO_2_KEY_ALGORITHM = "ECDSA"
private const val DEFAULT_FIDO_2_KEY_CURVE = "P-256"

/**
 * Represents the response model for vault data fetched from the server.
 *
 * @property folders A list of folders associated with the vault data (nullable).
 * @property collections A list of collections associated with the vault data (nullable).
 * @property profile The profile associated with the vault data.
 * @property ciphers A list of ciphers associated with the vault data (nullable).
 * @property policies A list of policies associated with the vault data (nullable).
 * @property domains A domains object associated with the vault data.
 * @property sends A list of send objects associated with the vault data (nullable).
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SyncResponseJson(
    @SerialName("folders")
    val folders: List<Folder>?,

    @SerialName("collections")
    val collections: List<Collection>?,

    @SerialName("profile")
    @JsonNames("Profile")
    val profile: Profile,

    @Contextual
    @SerialName("ciphers")
    val ciphers: List<Cipher>?,

    @Contextual
    @SerialName("policies")
    private val legacyPolicies: List<Policy>?,

    @Contextual
    @SerialName("policiesNew")
    private val newPolicies: List<Policy>?,

    @SerialName("domains")
    @JsonNames("Domains")
    val domains: Domains?,

    @SerialName("sends")
    val sends: List<Send>?,

    @SerialName("userDecryption")
    val userDecryption: UserDecryptionJson?,
) {
    /**
     * A list of policies associated with the vault data (nullable).
     */
    val policies: List<Policy>? get() = newPolicies ?: legacyPolicies

    /**
     * Represents domains in the vault response.
     *
     * @property globalEquivalentDomains A list of global equivalent domains (nullable).
     * @property equivalentDomains List of equivalent domains (nullable).
     */
    @Serializable
    data class Domains(
        @SerialName("globalEquivalentDomains")
        val globalEquivalentDomains: List<GlobalEquivalentDomain>?,

        @SerialName("equivalentDomains")
        val equivalentDomains: List<List<String>>?,
    ) {
        /**
         * Represents the global equivalent domain in the vault response.
         *
         * @property isExcluded If the global equivalent domain is excluded.
         * @property domains A List of domains associated with
         * the global equivalent domain (nullable).
         * @property type The type of global equivalent domain.
         */
        @Serializable
        data class GlobalEquivalentDomain(
            @SerialName("excluded")
            val isExcluded: Boolean,

            @SerialName("domains")
            val domains: List<String>?,

            @SerialName("type")
            val type: Int,
        )
    }

    /**
     * Represents a folder in the vault response.
     *
     * @property revisionDate The revision date of the folder.
     * @property name The name of the folder (nullable).
     * @property id The ID of the folder.
     */
    @Serializable
    data class Folder(
        @SerialName("revisionDate")
        @Contextual
        val revisionDate: Instant,

        @SerialName("name")
        val name: String?,

        @SerialName("id")
        val id: String,
    )

    /**
     * Represents a policy in the vault response.
     *
     * @property organizationId The organization ID of the policy.
     * @property id The ID of the policy.
     * @property type The type of policy.
     * @property isEnabled If the policy is enabled or not.
     * @property data Any extra data about the policy, in the form of a JSON string.
     * @property revisionDate The revision date of the policy (nullable).
     */
    @Serializable
    data class Policy(
        @SerialName("organizationId")
        val organizationId: String,

        @SerialName("id")
        val id: String,

        @SerialName("type")
        val type: PolicyTypeJson,

        @SerialName("enabled")
        val isEnabled: Boolean,

        @SerialName("data")
        val data: JsonObject?,

        @SerialName("revisionDate")
        @Contextual
        val revisionDate: Instant?,
    )

    /**
     * Represents a profile in the vault response.
     *
     * @property providerOrganizations A list of provider organizations
     * associated with the profile (nullable).
     * @property isPremiumFromOrganization If the profile is Premium from organization.
     * @property shouldForcePasswordReset If the profile should force password reset.
     * @property avatarColor The avatar color of the profile (nullable).
     * @property isEmailVerified If the profile has a verified email.
     * @property isTwoFactorEnabled If the profile has two factor authentication enabled.
     * @property privateKey The private key of the profile (nullable).
     * @property accountKeys The account keys associated with the profile. This is temporarily
     * nullable to maintain backwards compatibility.
     * @property isPremium If the profile is Premium.
     * @property culture The culture of the profile (nullable).
     * @property name The name of the profile (nullable).
     * @property organizations A list of organizations associated with the profile (nullable).
     * @property shouldUseKeyConnector If the profile should use a key connector.
     * @property id The ID of the profile.
     * @property masterPasswordHint The master password hint of the profile (nullable).
     * @property email The email of the profile (nullable).
     * @property key The key of the profile (nullable).
     * @property securityStamp The secure stamp of the profile (nullable).
     * @property providers A list of providers associated with the profile (nullable).
     * @property creationDate The creation date of the account.
     */
    @Serializable
    data class Profile(
        @SerialName("providerOrganizations")
        val providerOrganizations: List<Organization>?,

        @SerialName("premiumFromOrganization")
        val isPremiumFromOrganization: Boolean,

        @SerialName("forcePasswordReset")
        val shouldForcePasswordReset: Boolean,

        @SerialName("avatarColor")
        val avatarColor: String?,

        @SerialName("emailVerified")
        val isEmailVerified: Boolean,

        @SerialName("twoFactorEnabled")
        val isTwoFactorEnabled: Boolean,

        @Deprecated(
            message = "Use `accountKeys` instead",
            ReplaceWith("profile.accountKeys?.publicKeyEncryptionKeyPair?.wrappedPrivateKey"),
        )
        @SerialName("privateKey")
        val privateKey: String?,

        @SerialName("accountKeys")
        val accountKeys: AccountKeysJson?,

        @SerialName("premium")
        val isPremium: Boolean,

        @SerialName("culture")
        val culture: String?,

        @SerialName("name")
        val name: String?,

        @SerialName("organizations")
        private val legacyOrganizations: List<Organization>?,

        @SerialName("organizationsNew")
        private val newOrganizations: List<Organization>?,

        @SerialName("usesKeyConnector")
        val shouldUseKeyConnector: Boolean,

        @SerialName("id")
        val id: String,

        @SerialName("masterPasswordHint")
        val masterPasswordHint: String?,

        @SerialName("email")
        val email: String?,

        @SerialName("key")
        val key: String?,

        @SerialName("securityStamp")
        val securityStamp: String?,

        @SerialName("providers")
        val providers: List<Provider>?,

        @SerialName("creationDate")
        @Contextual
        val creationDate: Instant,
    ) {
        /**
         * A list of organizations associated with the profile (nullable).
         */
        val organizations: List<Organization>? get() = newOrganizations ?: legacyOrganizations

        /**
         * Represents an organization profile for the current user, containing the organization's
         * feature flags, membership details, and configuration settings.
         *
         * @property shouldUsePolicies Whether the organization has access to policies features.
         * @property isKeyConnectorEnabled Whether Key Connector is enabled for this organization.
         * @property keyConnectorUrl The URL of the Key Connector service, if enabled.
         * @property type The user's role in the organization.
         * @property seats The number of licensed seats for the organization.
         * @property isEnabled Whether the organization is currently enabled.
         * @property providerType The type of provider managing this organization, if any.
         * @property isProviderUser Whether the current user accesses this organization through a
         * provider.
         * @property maxCollections The maximum number of collections the organization can create.
         * @property isSelfHost Whether the organization can create a license file for a self-hosted
         * instance.
         * @property permissions The current user's custom permissions, relevant when
         * [OrganizationType.CUSTOM] is the user's type.
         * @property providerId The ID of the provider managing this organization, if any.
         * @property id Unique identifier for the organization.
         * @property shouldUseGroups Whether the organization has access to groups features.
         * @property shouldUseDirectory Whether the organization has access to directory sync
         * features.
         * @property key The key of the organization (nullable).
         * @property providerName The name of the provider managing this organization, if any.
         * @property shouldUsersGetPremium Whether organization members receive premium features.
         * @property maxStorageGb The maximum encrypted storage in gigabytes, if limited.
         * @property identifier The organization's SSO identifier.
         * @property use2fa Whether the organization has access to two-factor authentication
         * features.
         * @property familySponsorshipToDelete Whether the families sponsorship is scheduled for
         * deletion.
         * @property userId The current user's personal user ID.
         * @property shouldUseEvents Whether the organization has access to event logging features.
         * @property familySponsorshipFriendlyName The friendly name of a pending families
         * sponsorship, if any.
         * @property shouldUseTotp Whether the organization can enforce TOTP for members.
         * @property familySponsorshipLastSyncDate The date the families sponsorship was last
         * synced, if applicable.
         * @property name Display name of the organization.
         * @property shouldUseApi Whether the organization has access to the Bitwarden Public API.
         * @property familySponsorshipValidUntil The date the families sponsorship expires, if
         * applicable.
         * @property status The user's membership status in the organization.
         * @property userIsClaimedByOrganization Whether the current user has been claimed by this
         * organization.
         * @property limitItemDeletion Whether item deletion is restricted to members with the
         * Manage collection permission. When false, members with Edit permission can also delete
         * items within their collections.
         * @property useSso Whether the organization has access to SSO features.
         * @property useOrganizationDomains Whether the organization can manage verified domains.
         * @property useScim Whether the organization has access to SCIM provisioning.
         * @property useCustomPermissions Whether the organization can use the
         * [OrganizationType.CUSTOM] role.
         * @property useResetPassword Whether the organization has access to the account recovery
         * (admin password reset) feature.
         * @property useSecretsManager Whether the organization has access to Secrets Manager.
         * @property usePasswordManager Whether the organization has access to Password Manager.
         * @property useActivateAutofillPolicy Whether the organization can use the activate
         * autofill policy.
         * @property useAutomaticUserConfirmation Whether the organization can automatically
         * confirm new members without manual admin approval.
         * @property ssoBound Whether the current user's account is bound to this organization
         * via SSO.
         * @property resetPasswordEnrolled Whether the current user is enrolled in account recovery
         * for this organization.
         * @property organizationUserId The current user's organization membership ID.
         * @property hasPublicAndPrivateKeys Whether the organization has both a public and private
         * key configured.
         * @property isMember Whether the current user is a direct member of this organization (as
         * opposed to provider-only access).
         * @property familySponsorshipAvailable Whether the organization can sponsor a families
         * plan for the current user.
         * @property productTierType The subscription tier of the organization.
         * @property shouldUseKeyConnector Whether the organization uses Key Connector for
         * decryption.
         * @property accessSecretsManager Whether the current user has access to Secrets Manager
         * for this organization.
         * @property limitCollectionCreation Whether collection creation is restricted to owners
         * and admins only. When false, any member can create collections and automatically
         * receives manage permissions over collections they create.
         * @property limitCollectionDeletion Whether collection deletion is restricted to owners and
         * admins only. When true, regular users cannot delete collections that they manage.
         * @property allowAdminAccessToAllCollectionItems Whether owners and admins have implicit
         * manage permissions over all collections. When true, owners and admins can alter items,
         * groups, and permissions across all collections without requiring explicit collection
         * assignments. When false, admins can only access collections where they have been
         * explicitly assigned.
         * @property useAccessIntelligence Whether the organization has access to Access
         * Intelligence features.
         * @property useAdminSponsoredFamilies Whether the organization can sponsor families plans
         * for members (Families For Enterprises).
         * @property useDisableSmAdsForUsers Whether Secrets Manager ads are disabled for users.
         * @property isAdminInitiated Whether the organization's Families For Enterprises
         * sponsorship was initiated by an admin.
         * @property ssoEnabled Whether SSO login is currently enabled for this organization.
         * @property ssoMemberDecryptionType The decryption type used for SSO members, if SSO is
         * enabled.
         * @property usePhishingBlocker Whether the organization has access to phishing blocker
         * features.
         * @property useMyItems Whether the organization has access to the My Items collection
         * feature. This allows users to store personal items in the organization vault if the
         * Centralize Organization Ownership policy is enabled.
         */
        @Serializable
        data class Organization(
            @SerialName("usePolicies")
            val shouldUsePolicies: Boolean,

            @SerialName("keyConnectorEnabled")
            val isKeyConnectorEnabled: Boolean,

            @SerialName("keyConnectorUrl")
            val keyConnectorUrl: String?,

            @SerialName("type")
            val type: OrganizationType,

            @SerialName("seats")
            val seats: UInt?,

            @SerialName("enabled")
            val isEnabled: Boolean,

            @SerialName("providerType")
            val providerType: ProviderType?,

            @SerialName("isProviderUser")
            val isProviderUser: Boolean = false,

            @SerialName("maxCollections")
            val maxCollections: UInt?,

            @SerialName("selfHost")
            val isSelfHost: Boolean,

            @SerialName("permissions")
            val permissions: Permissions,

            @SerialName("providerId")
            val providerId: String?,

            @SerialName("id")
            val id: String,

            @SerialName("useGroups")
            val shouldUseGroups: Boolean,

            @SerialName("useDirectory")
            val shouldUseDirectory: Boolean,

            @SerialName("key")
            val key: String?,

            @SerialName("providerName")
            val providerName: String?,

            @SerialName("usersGetPremium")
            val shouldUsersGetPremium: Boolean,

            @SerialName("maxStorageGb")
            val maxStorageGb: UInt?,

            @SerialName("identifier")
            val identifier: String?,

            @SerialName("use2fa")
            val use2fa: Boolean,

            @SerialName("familySponsorshipToDelete")
            val familySponsorshipToDelete: Boolean?,

            @SerialName("userId")
            val userId: String?,

            @SerialName("useEvents")
            val shouldUseEvents: Boolean,

            @SerialName("familySponsorshipFriendlyName")
            val familySponsorshipFriendlyName: String?,

            @SerialName("useTotp")
            val shouldUseTotp: Boolean,

            @SerialName("familySponsorshipLastSyncDate")
            @Contextual
            val familySponsorshipLastSyncDate: Instant?,

            @SerialName("name")
            val name: String?,

            @SerialName("useApi")
            val shouldUseApi: Boolean,

            @SerialName("familySponsorshipValidUntil")
            @Contextual
            val familySponsorshipValidUntil: Instant?,

            @SerialName("status")
            val status: OrganizationStatusType,

            @SerialName("userIsClaimedByOrganization")
            val userIsClaimedByOrganization: Boolean = false,

            @SerialName("limitItemDeletion")
            val limitItemDeletion: Boolean = false,

            @SerialName("useSso")
            val useSso: Boolean = false,

            @SerialName("useOrganizationDomains")
            val useOrganizationDomains: Boolean = false,

            @SerialName("useScim")
            val useScim: Boolean = false,

            @SerialName("useCustomPermissions")
            val useCustomPermissions: Boolean = false,

            @SerialName("useResetPassword")
            val useResetPassword: Boolean = false,

            @SerialName("useSecretsManager")
            val useSecretsManager: Boolean = false,

            @SerialName("usePasswordManager")
            val usePasswordManager: Boolean = false,

            @SerialName("useActivateAutofillPolicy")
            val useActivateAutofillPolicy: Boolean = false,

            @SerialName("useAutomaticUserConfirmation")
            val useAutomaticUserConfirmation: Boolean = false,

            @SerialName("ssoBound")
            val ssoBound: Boolean = false,

            @SerialName("resetPasswordEnrolled")
            val resetPasswordEnrolled: Boolean = false,

            @SerialName("organizationUserId")
            val organizationUserId: String?,

            @SerialName("hasPublicAndPrivateKeys")
            val hasPublicAndPrivateKeys: Boolean = false,

            @SerialName("isMember")
            val isMember: Boolean = false,

            @SerialName("familySponsorshipAvailable")
            val familySponsorshipAvailable: Boolean = false,

            @SerialName("productTierType")
            val productTierType: ProductTierType = ProductTierType.FREE,

            @SerialName("useKeyConnector")
            val shouldUseKeyConnector: Boolean = false,

            @SerialName("accessSecretsManager")
            val accessSecretsManager: Boolean = false,

            @SerialName("limitCollectionCreation")
            val limitCollectionCreation: Boolean = false,

            @SerialName("limitCollectionDeletion")
            val limitCollectionDeletion: Boolean = false,

            @SerialName("allowAdminAccessToAllCollectionItems")
            val allowAdminAccessToAllCollectionItems: Boolean = false,

            @SerialName("useAccessIntelligence")
            val useAccessIntelligence: Boolean = false,

            @SerialName("useAdminSponsoredFamilies")
            val useAdminSponsoredFamilies: Boolean = false,

            @SerialName("useDisableSmAdsForUsers")
            val useDisableSmAdsForUsers: Boolean = false,

            @SerialName("isAdminInitiated")
            val isAdminInitiated: Boolean = false,

            @SerialName("ssoEnabled")
            val ssoEnabled: Boolean = false,

            @SerialName("ssoMemberDecryptionType")
            val ssoMemberDecryptionType: MemberDecryptionType?,

            @SerialName("usePhishingBlocker")
            val usePhishingBlocker: Boolean = false,

            @SerialName("useMyItems")
            val useMyItems: Boolean = false,
        )

        /**
         * Represents a provider in the vault response.
         *
         * @property shouldUseEvents If the provider should use events.
         * @property permissions The permissions of the provider.
         * @property name The name of the provider (nullable).
         * @property id The ID of the provider.
         * @property type The type of provider.
         * @property userId The user ID of the provider (nullable).
         * @property key The key of the provider (nullable).
         * @property isEnabled If the provider is enabled.
         * @property status The status of the provider.
         */
        @Serializable
        data class Provider(
            @SerialName("useEvents")
            val shouldUseEvents: Boolean,

            @SerialName("permissions")
            val permissions: Permissions,

            @SerialName("name")
            val name: String?,

            @SerialName("id")
            val id: String,

            @SerialName("type")
            val type: Int,

            @SerialName("userId")
            val userId: String?,

            @SerialName("key")
            val key: String?,

            @SerialName("enabled")
            val isEnabled: Boolean,

            @SerialName("status")
            val status: Int,
        )

        /**
         * Custom permission set for a user with the [OrganizationType.CUSTOM] role.
         *
         * @property shouldManageResetPassword Can manage the account recovery (password reset)
         * feature.
         * @property shouldManagePolicies Can manage organization policies.
         * @property accessEventLogs Can view the organization's event logs.
         * @property accessImportExport Can import and export organization vault data.
         * @property accessReports Can access organization reports.
         * @property createNewCollections Can create new collections.
         * @property editAnyCollection Can edit any collection, including those they are not
         * assigned to.
         * @property deleteAnyCollection Can delete any collection, including those they are not
         * assigned to.
         * @property manageGroups Can manage groups within the organization.
         * @property manageSso Can manage SSO configuration.
         * @property manageUsers Can manage organization members.
         * @property manageScim Can manage SCIM (System for Cross-domain Identity Management)
         * configuration.
         */
        @Serializable
        data class Permissions(
            @SerialName("manageResetPassword")
            val shouldManageResetPassword: Boolean,

            @SerialName("managePolicies")
            val shouldManagePolicies: Boolean,

            @SerialName("accessEventLogs")
            val accessEventLogs: Boolean = false,

            @SerialName("accessImportExport")
            val accessImportExport: Boolean = false,

            @SerialName("accessReports")
            val accessReports: Boolean = false,

            @SerialName("createNewCollections")
            val createNewCollections: Boolean = false,

            @SerialName("editAnyCollection")
            val editAnyCollection: Boolean = false,

            @SerialName("deleteAnyCollection")
            val deleteAnyCollection: Boolean = false,

            @SerialName("manageGroups")
            val manageGroups: Boolean = false,

            @SerialName("manageSso")
            val manageSso: Boolean = false,

            @SerialName("manageUsers")
            val manageUsers: Boolean = false,

            @SerialName("manageScim")
            val manageScim: Boolean = false,
        )
    }

    /**
     * Represents a cipher in the vault response.
     *
     * @property notes The notes of the cipher (nullable).
     * @property attachments A list of attachments associated with the cipher (nullable).
     * @property shouldOrganizationUseTotp If organizations use TOTP for the cipher.
     * @property reprompt The reprompt of the cipher.
     * @property shouldEdit If the cipher can edit.
     * @property passwordHistory A list of password history objects
     * associated with the cipher (nullable).
     * @property revisionDate The revision date of the cipher.
     * @property type The type of cipher.
     * @property login The login of the cipher.
     * @property creationDate The creation date of the cipher.
     * @property secureNote The secure note of the cipher.
     * @property folderId The folder ID of the cipher (nullable).
     * @property organizationId The organization ID of the cipher (nullable).
     * @property deletedDate The deleted date of the cipher (nullable).
     * @property identity The identity of the cipher.
     * @property collectionIds A list of collection IDs associated with the cipher (nullable).
     * @property name The name of the cipher (nullable).
     * @property id The ID of the cipher.
     * @property fields A list of fields associated with the cipher (nullable).
     * @property shouldViewPassword If the password can be viewed for the cipher.
     * @property isFavorite If the cipher is a favorite.
     * @property card The card of the cipher.
     * @property key The key of the cipher (nullable).
     * @property encryptedFor ID of the user who the cipher is encrypted by.
     * @property archivedDate The archived date of the cipher (nullable).
     */
    @Serializable
    data class Cipher(
        @SerialName("notes")
        val notes: String?,

        @SerialName("attachments")
        val attachments: List<Attachment>?,

        @SerialName("organizationUseTotp")
        val shouldOrganizationUseTotp: Boolean,

        @SerialName("reprompt")
        val reprompt: CipherRepromptTypeJson,

        @SerialName("edit")
        val shouldEdit: Boolean,

        @SerialName("passwordHistory")
        val passwordHistory: List<PasswordHistory>?,

        @SerialName("permissions")
        val permissions: CipherPermissions?,

        @SerialName("revisionDate")
        @Contextual
        val revisionDate: Instant,

        @SerialName("type")
        val type: CipherTypeJson,

        @SerialName("login")
        val login: Login?,

        @SerialName("creationDate")
        @Contextual
        val creationDate: Instant,

        @SerialName("secureNote")
        val secureNote: SecureNote?,

        @SerialName("folderId")
        val folderId: String?,

        @SerialName("organizationId")
        val organizationId: String?,

        @SerialName("deletedDate")
        @Contextual
        val deletedDate: Instant?,

        @SerialName("identity")
        val identity: Identity?,

        @SerialName("sshKey")
        val sshKey: SshKey?,

        @SerialName("bankAccount")
        val bankAccount: BankAccount?,

        @SerialName("driversLicense")
        val driversLicense: DriversLicense?,

        @SerialName("passport")
        val passport: Passport?,

        @SerialName("collectionIds")
        val collectionIds: List<String>?,

        @SerialName("name")
        val name: String?,

        @SerialName("id")
        val id: String,

        @SerialName("fields")
        val fields: List<Field>?,

        @SerialName("viewPassword")
        val shouldViewPassword: Boolean,

        @SerialName("favorite")
        val isFavorite: Boolean,

        @SerialName("card")
        val card: Card?,

        @SerialName("key")
        val key: String?,

        @SerialName("encryptedFor")
        val encryptedFor: String?,

        @SerialName("archivedDate")
        @Contextual
        val archivedDate: Instant?,
    ) {
        /**
         * Represents an attachment in the vault response.
         *
         * @property fileName The file name of the attachment (nullable).
         * @property size The size of the attachment (nullable).
         * @property sizeName The size name of the attachment (nullable).
         * @property id The ID of the attachment (nullable).
         * @property url The URL of the attachment (nullable).
         * @property key The key of the attachment (nullable).
         */
        @Serializable
        data class Attachment(
            @SerialName("fileName")
            val fileName: String?,

            @SerialName("size")
            val size: Int,

            @SerialName("sizeName")
            val sizeName: String?,

            @SerialName("id")
            val id: String?,

            @SerialName("url")
            val url: String?,

            @SerialName("key")
            val key: String?,
        )

        /**
         * Represents a card in the vault response.
         *
         * @property number The number of the card (nullable).
         * @property expMonth The expiration month of the card (nullable).
         * @property code The code of the card (nullable).
         * @property expirationYear The expiration year of the card (nullable).
         * @property cardholderName The name of the card holder (nullable).
         * @property brand The brand of the card (nullable).
         */
        @Serializable
        data class Card(
            @SerialName("number")
            val number: String?,

            @SerialName("expMonth")
            val expMonth: String?,

            @SerialName("code")
            val code: String?,

            @SerialName("expYear")
            val expirationYear: String?,

            @SerialName("cardholderName")
            val cardholderName: String?,

            @SerialName("brand")
            val brand: String?,
        )

        /**
         * Represents a field in the vault response.
         *
         * @property linkedIdType The linked ID of the field (nullable).
         * @property name The name of the field (nullable).
         * @property type The type of field.
         * @property value The value of the field (nullable).
         */
        @Serializable
        data class Field(
            @SerialName("linkedId")
            val linkedIdType: LinkedIdTypeJson?,

            @SerialName("name")
            val name: String?,

            @SerialName("type")
            val type: FieldTypeJson,

            @SerialName("value")
            val value: String?,
        )

        /**
         * Represents an identity in the vault response.
         *
         * @property passportNumber The passport number of the identity (nullable).
         * @property lastName The last name of the identity (nullable).
         * @property country The country of the identity (nullable).
         * @property address3 The third address of the identity (nullable).
         * @property address2 The second address of the identity (nullable).
         * @property city The city of the identity (nullable).
         * @property address1 The first address of the identity (nullable).
         * @property postalCode The postal code of the identity (nullable).
         * @property title The title of the identity (nullable).
         * @property ssn The social security number of the identity (nullable).
         * @property firstName The first name of the identity (nullable).
         * @property phone The phone of the identity (nullable).
         * @property middleName The middle name of the identity (nullable).
         * @property company The company of the identity (nullable).
         * @property licenseNumber The license number of the identity (nullable).
         * @property state The state of the identity (nullable).
         * @property email The email of the identity (nullable).
         * @property username The username of the identity (nullable).
         */
        @Serializable
        data class Identity(
            @SerialName("passportNumber")
            val passportNumber: String?,

            @SerialName("lastName")
            val lastName: String?,

            @SerialName("country")
            val country: String?,

            @SerialName("address3")
            val address3: String?,

            @SerialName("address2")
            val address2: String?,

            @SerialName("city")
            val city: String?,

            @SerialName("address1")
            val address1: String?,

            @SerialName("postalCode")
            val postalCode: String?,

            @SerialName("title")
            val title: String?,

            @SerialName("ssn")
            val ssn: String?,

            @SerialName("firstName")
            val firstName: String?,

            @SerialName("phone")
            val phone: String?,

            @SerialName("middleName")
            val middleName: String?,

            @SerialName("company")
            val company: String?,

            @SerialName("licenseNumber")
            val licenseNumber: String?,

            @SerialName("state")
            val state: String?,

            @SerialName("email")
            val email: String?,

            @SerialName("username")
            val username: String?,
        )

        /**
         * Represents a login object in the vault response.
         *
         * @property uris A list of URIs (nullable).
         * @property totp The TOTP (nullable).
         * @property password The password (nullable).
         * @property passwordRevisionDate The password revision date (nullable).
         * @property shouldAutofillOnPageLoad If autofill is used on page load (nullable).
         * @property uri The URI (nullable).
         * @property username The username (nullable).
         * @property fido2Credentials A list of FIDO 2 credentials (nullable).
         */
        @Serializable
        data class Login(
            @SerialName("uris")
            val uris: List<Uri>?,

            @SerialName("totp")
            val totp: String?,

            @SerialName("password")
            val password: String?,

            @SerialName("passwordRevisionDate")
            @Contextual
            val passwordRevisionDate: Instant?,

            @SerialName("autofillOnPageLoad")
            val shouldAutofillOnPageLoad: Boolean?,

            @SerialName("uri")
            val uri: String?,

            @SerialName("username")
            val username: String?,

            @SerialName("fido2Credentials")
            val fido2Credentials: List<Fido2Credential>?,
        ) {
            /**
             * Represents a URI in the vault response.
             *
             * @property uriMatchType The match type of the URI.
             * @property uri The actual string representing the URI (nullable).
             */
            @Serializable
            data class Uri(
                @SerialName("match")
                val uriMatchType: UriMatchTypeJson?,

                @SerialName("uri")
                val uri: String?,

                @SerialName("uriChecksum")
                val uriChecksum: String?,
            )
        }

        /**
         * Represents a SSH key in the vault response.
         *
         * @property publicKey The public key of the SSH key.
         * @property privateKey The private key of the SSH key.
         * @property keyFingerprint The key fingerprint of the SSH key.
         */
        @Serializable
        data class SshKey(
            @SerialName("publicKey")
            val publicKey: String?,

            @SerialName("privateKey")
            val privateKey: String,

            @SerialName("keyFingerprint")
            val keyFingerprint: String?,
        )

        /**
         * Represents a bank account in the vault response.
         *
         * @property bankName The name of the bank (nullable).
         * @property nameOnAccount The name on the account (nullable).
         * @property accountType The type of bank account (nullable).
         * @property accountNumber The account number (nullable).
         * @property routingNumber The routing/transit number (nullable).
         * @property branchNumber The branch/institution number (nullable).
         * @property pin The PIN (nullable).
         * @property swiftCode The SWIFT code (nullable).
         * @property iban The IBAN (nullable).
         * @property bankContactPhone The bank contact phone number (nullable).
         */
        @Serializable
        data class BankAccount(
            @SerialName("bankName")
            val bankName: String?,

            @SerialName("nameOnAccount")
            val nameOnAccount: String?,

            @SerialName("accountType")
            val accountType: String?,

            @SerialName("accountNumber")
            val accountNumber: String?,

            @SerialName("routingNumber")
            val routingNumber: String?,

            @SerialName("branchNumber")
            val branchNumber: String?,

            @SerialName("pin")
            val pin: String?,

            @SerialName("swiftCode")
            val swiftCode: String?,

            @SerialName("iban")
            val iban: String?,

            @SerialName("bankContactPhone")
            val bankContactPhone: String?,
        )

        /**
         * Represents a driver's license in the vault response.
         *
         * @property firstName The first name (nullable).
         * @property middleName The middle name (nullable).
         * @property lastName The last name (nullable).
         * @property licenseNumber The license number (nullable).
         * @property dateOfBirth The date of birth (nullable).
         * @property issuingCountry The issuing country (nullable).
         * @property issuingState The issuing state/province (nullable).
         * @property issuingAuthority The issuing authority (nullable).
         * @property issueDate The issue date (nullable).
         * @property expirationDate The expiration date (nullable).
         * @property licenseClass The license class (nullable).
         */
        @Serializable
        data class DriversLicense(
            @SerialName("firstName")
            val firstName: String?,

            @SerialName("middleName")
            val middleName: String?,

            @SerialName("lastName")
            val lastName: String?,

            @SerialName("licenseNumber")
            val licenseNumber: String?,

            @SerialName("dateOfBirth")
            val dateOfBirth: String?,

            @SerialName("issuingCountry")
            val issuingCountry: String?,

            @SerialName("issuingAuthority")
            val issuingAuthority: String?,

            @SerialName("issuingState")
            val issuingState: String?,

            @SerialName("issueDate")
            val issueDate: String?,

            @SerialName("expirationDate")
            val expirationDate: String?,

            @SerialName("licenseClass")
            val licenseClass: String?,
        )

        /**
         * Represents a passport in the vault response.
         *
         * @property surname The surname (nullable).
         * @property givenName The given name (nullable).
         * @property dateOfBirth The date of birth (nullable).
         * @property birthPlace The place of birth (nullable).
         * @property sex The sex of the individual (nullable).
         * @property nationality The nationality (nullable).
         * @property passportNumber The passport number (nullable).
         * @property passportType The passport type (nullable).
         * @property issuingCountry The issuing country (nullable).
         * @property issuingAuthority The issuing authority/office (nullable).
         * @property issueDate The issue date (nullable).
         * @property expirationDate The expiration date (nullable).
         * @property nationalIdentificationNumber The nation ID (nullable).
         */
        @Serializable
        data class Passport(
            @SerialName("surname")
            val surname: String?,

            @SerialName("givenName")
            val givenName: String?,

            @SerialName("dateOfBirth")
            val dateOfBirth: String?,

            @SerialName("birthPlace")
            val birthPlace: String?,

            @SerialName("sex")
            val sex: String?,

            @SerialName("nationality")
            val nationality: String?,

            @SerialName("passportNumber")
            val passportNumber: String?,

            @SerialName("passportType")
            val passportType: String?,

            @SerialName("issuingCountry")
            val issuingCountry: String?,

            @SerialName("issuingAuthority")
            val issuingAuthority: String?,

            @SerialName("issueDate")
            val issueDate: String?,

            @SerialName("expirationDate")
            val expirationDate: String?,

            @SerialName("nationalIdentificationNumber")
            val nationalIdentificationNumber: String?,
        )

        /**
         * Represents password history in the vault response.
         *
         * @property password The password of the password history object.
         * @property lastUsedDate The last used date of the password history object.
         */
        @Serializable
        data class PasswordHistory(
            @SerialName("password")
            val password: String,

            @SerialName("lastUsedDate")
            @Contextual
            val lastUsedDate: Instant,
        )

        /**
         * Represents a cipher permissions in the vault response.
         *
         * @property delete whether the delete permissions is active.
         * @property restore whether the restore permissions is active.
         */
        @Serializable
        data class CipherPermissions(
            @SerialName("delete")
            val delete: Boolean,

            @SerialName("restore")
            val restore: Boolean,
        )

        /**
         * Represents a secure note in the vault response.
         *
         * @property type The type of secure note.
         */
        @Serializable
        data class SecureNote(
            @SerialName("type")
            val type: SecureNoteTypeJson,
        )

        /**
         * Represents a FIDO2 credential object in the vault response.
         *
         * @property credentialId The unique identifier of the FIDO2 credential.
         * @property keyType The type of public key of the FIDO2 credential.
         * @property keyAlgorithm The public Key algorithm of the credential.
         * @property keyValue The public key of the credential.
         * @property rpId The relying party (RP) identity.
         * @property rpName The optional name of the relying party (RP).
         * @property userHandle The optional unique identifier used to identify an account.
         * @property userName The conditional, formal name of the user associated to the credential.
         * @property userDisplayName The optional display name of the user associated to the
         * credential.
         * @property counter The signature counter for the credential.
         * @property discoverable Whether the FIDO2 credential is discoverable or non-discoverable.
         * @property creationDate The creation date and time of the credential.
         */
        @Serializable
        data class Fido2Credential(
            @SerialName("credentialId")
            val credentialId: String,

            @SerialName("keyType")
            val keyType: String = DEFAULT_FIDO_2_KEY_TYPE,

            @SerialName("keyAlgorithm")
            val keyAlgorithm: String = DEFAULT_FIDO_2_KEY_ALGORITHM,

            @SerialName("keyCurve")
            val keyCurve: String = DEFAULT_FIDO_2_KEY_CURVE,

            @SerialName("keyValue")
            val keyValue: String,

            @SerialName("rpId")
            val rpId: String,

            @SerialName("rpName")
            val rpName: String?,

            @SerialName("userHandle")
            val userHandle: String?,

            @SerialName("userName")
            val userName: String?,

            @SerialName("userDisplayName")
            val userDisplayName: String?,

            @SerialName("counter")
            val counter: String,

            @SerialName("discoverable")
            val discoverable: String,

            @SerialName("creationDate")
            @Contextual
            val creationDate: Instant,
        )
    }

    /**
     * Represents a send object in the vault response.
     *
     * @property accessCount The access count of the send object.
     * @property notes The notes of the send object (nullable).
     * @property revisionDate The revision date of the send object.
     * @property maxAccessCount The max access count of the send object (nullable).
     * @property shouldHideEmail If the send object should hide the email.
     * @property type The type of send object.
     * @property authType Specifies the authentication method required to access this Send.
     * @property accessId The access ID of the send object (nullable).
     * @property password The password of the send object (nullable).
     * Mutually exclusive with [emails]
     * @property emails Comma-separated list of emails that may access the send using OTP
     * authentication. Mutually exclusive with [password]
     * @property file The file of the send object.
     * @property deletionDate The max access count of the send object.
     * @property name The name of the send object (nullable).
     * @property isDisabled If the send object is disabled.
     * @property id The ID the send object.
     * @property text The text of the send object.
     * @property key The key of the send object (nullable).
     * @property expirationDate The expiration date of the send object (nullable).
     */
    @Serializable
    data class Send(
        @SerialName("accessCount")
        val accessCount: Int,

        @SerialName("notes")
        val notes: String?,

        @SerialName("revisionDate")
        @Contextual
        val revisionDate: Instant,

        @SerialName("maxAccessCount")
        val maxAccessCount: Int?,

        @SerialName("hideEmail")
        val shouldHideEmail: Boolean,

        @SerialName("type")
        val type: SendTypeJson,

        @SerialName("authType")
        val authType: SendAuthTypeJson?,

        @SerialName("accessId")
        val accessId: String?,

        @SerialName("password")
        val password: String?,

        @SerialName("emails")
        val emails: String?,

        @SerialName("file")
        val file: File?,

        @SerialName("deletionDate")
        @Contextual
        val deletionDate: Instant,

        @SerialName("name")
        val name: String?,

        @SerialName("disabled")
        val isDisabled: Boolean,

        @SerialName("id")
        val id: String,

        @SerialName("text")
        val text: Text?,

        @SerialName("key")
        val key: String?,

        @SerialName("expirationDate")
        @Contextual
        val expirationDate: Instant?,
    ) {
        /**
         * Represents a file in the vault response.
         *
         * @property fileName The name of the file (nullable).
         * @property size The size of the file (nullable).
         * @property sizeName The size name of the file (nullable).
         * @property id The ID of the file (nullable).
         */
        @Serializable
        data class File(
            @SerialName("fileName")
            val fileName: String?,

            @SerialName("size")
            val size: Int?,

            @SerialName("sizeName")
            val sizeName: String?,

            @SerialName("id")
            val id: String?,
        )

        /**
         * Represents text in the vault response.
         *
         * @property isHidden If the text is hidden or not.
         * @property text The actual string representing the text (nullable).
         */
        @Serializable
        data class Text(
            @SerialName("hidden")
            val isHidden: Boolean,

            @SerialName("text")
            val text: String?,
        )
    }

    /**
     * Represents a collection in the vault response.
     *
     * @property organizationId The organization ID of the collection.
     * @property shouldHidePasswords If the collection should hide passwords.
     * @property name The name of the collection.
     * @property externalId The external ID of the collection (nullable).
     * @property isReadOnly If the collection is marked as read only.
     * @property id The ID of the collection.
     * @property defaultUserCollectionEmail The offboarded user's email address to be used as name
     * for the collection.
     * @property type The collection's type.
     */
    @Serializable
    data class Collection(
        @SerialName("organizationId")
        val organizationId: String,

        @SerialName("hidePasswords")
        val shouldHidePasswords: Boolean,

        @SerialName("name")
        val name: String,

        @SerialName("externalId")
        val externalId: String?,

        @SerialName("readOnly")
        val isReadOnly: Boolean,

        @SerialName("id")
        val id: String,

        @SerialName("manage")
        val canManage: Boolean?,

        @SerialName("defaultUserCollectionEmail")
        val defaultUserCollectionEmail: String?,

        @SerialName("type")
        val type: CollectionTypeJson = CollectionTypeJson.SHARED_COLLECTION,
    )
}
