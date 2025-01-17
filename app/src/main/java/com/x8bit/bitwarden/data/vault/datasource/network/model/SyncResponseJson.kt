package com.x8bit.bitwarden.data.vault.datasource.network.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonObject
import java.time.ZonedDateTime

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

    @SerialName("ciphers")
    val ciphers: List<Cipher>?,

    @SerialName("policies")
    val policies: List<Policy>?,

    @SerialName("domains")
    @JsonNames("Domains")
    val domains: Domains?,

    @SerialName("sends")
    val sends: List<Send>?,
) {
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
        val revisionDate: ZonedDateTime,

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
    )

    /**
     * Represents a profile in the vault response.
     *
     * @property providerOrganizations A list of provider organizations
     * associated with the profile (nullable).
     * @property isPremiumFromOrganization If the profile is premium from organization.
     * @property shouldForcePasswordReset If the profile should force password reset.
     * @property avatarColor The avatar color of the profile (nullable).
     * @property isEmailVerified If the profile has a verified email.
     * @property isTwoFactorEnabled If the profile has two factor authentication enabled.
     * @property privateKey The private key of the profile (nullable).
     * @property isPremium If the profile is premium.
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

        @SerialName("privateKey")
        val privateKey: String?,

        @SerialName("premium")
        val isPremium: Boolean,

        @SerialName("culture")
        val culture: String?,

        @SerialName("name")
        val name: String?,

        @SerialName("organizations")
        val organizations: List<Organization>?,

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
        val creationDate: ZonedDateTime,
    ) {
        /**
         * Represents an organization in the vault response.
         *
         * @property shouldUsePolicies If the organization should use policies.
         * @property keyConnectorUrl The key connector URL of the organization (nullable).
         * @property type The type of organization.
         * @property seats The number of seats in the organization (nullable).
         * @property isEnabled If the organization is enabled.
         * @property providerType They type of provider for the organization (nullable).
         * @property maxCollections The max collections of the organization (nullable).
         * @property isSelfHost If the organization is self hosted.
         * @property permissions The permissions of the organization.
         * @property providerId The provider ID of the organization (nullable).
         * @property id The ID of the organization.
         * @property shouldUseGroups If the organization should use groups.
         * @property shouldUseDirectory If the organization should use a directory.
         * @property key The key of the organization (nullable).
         * @property providerName The provider name of the organization (nullable).
         * @property shouldUsersGetPremium If users of the organization get premium.
         * @property maxStorageGb The max storage in Gb of the organization (nullable).
         * @property identifier The identifier of the organization (nullable).
         * @property use2fa If the organization uses 2FA.
         * @property familySponsorshipToDelete If the organization has a
         * family sponsorship to delete (nullable).
         * @property userId The user id (nullable).
         * @property shouldUseEvents If the organization should use events.
         * @property familySponsorshipFriendlyName If the family sponsorship is a friendly name.
         * @property shouldUseTotp If he organization should use TOTP.
         * @property familySponsorshipLastSyncDate The last date the family sponsorship
         * was synced (nullable).
         * @property name The name of the organization (nullable).
         * @property shouldUseApi If the organization should use API.
         * @property familySponsorshipValidUntil The family sponsorship valid until
         * of the organization (nullable).
         * @property status The status of the organization.
         */
        @Serializable
        data class Organization(
            @SerialName("usePolicies")
            val shouldUsePolicies: Boolean,

            @SerialName("keyConnectorEnabled")
            val shouldUseKeyConnector: Boolean,

            @SerialName("keyConnectorUrl")
            val keyConnectorUrl: String?,

            @SerialName("type")
            val type: OrganizationType,

            @SerialName("seats")
            val seats: Int?,

            @SerialName("enabled")
            val isEnabled: Boolean,

            @SerialName("providerType")
            val providerType: Int?,

            @SerialName("maxCollections")
            val maxCollections: Int?,

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
            val maxStorageGb: Int?,

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
            val familySponsorshipLastSyncDate: ZonedDateTime?,

            @SerialName("name")
            val name: String?,

            @SerialName("useApi")
            val shouldUseApi: Boolean,

            @SerialName("familySponsorshipValidUntil")
            @Contextual
            val familySponsorshipValidUntil: ZonedDateTime?,

            @SerialName("status")
            val status: OrganizationStatusType,
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
         * Represents permissions in the vault response.
         *
         * @property shouldManageResetPassword If reset password should be managed.
         * @property shouldManagePolicies If policies should be managed.
         */
        @Serializable
        data class Permissions(
            @SerialName("manageResetPassword")
            val shouldManageResetPassword: Boolean,

            @SerialName("managePolicies")
            val shouldManagePolicies: Boolean,
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

        @SerialName("revisionDate")
        @Contextual
        val revisionDate: ZonedDateTime,

        @SerialName("type")
        val type: CipherTypeJson,

        @SerialName("login")
        val login: Login?,

        @SerialName("creationDate")
        @Contextual
        val creationDate: ZonedDateTime,

        @SerialName("secureNote")
        val secureNote: SecureNote?,

        @SerialName("folderId")
        val folderId: String?,

        @SerialName("organizationId")
        val organizationId: String?,

        @SerialName("deletedDate")
        @Contextual
        val deletedDate: ZonedDateTime?,

        @SerialName("identity")
        val identity: Identity?,

        @SerialName("sshKey")
        val sshKey: SshKey?,

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
            val passwordRevisionDate: ZonedDateTime?,

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
            val publicKey: String,

            @SerialName("privateKey")
            val privateKey: String,

            @SerialName("keyFingerprint")
            val keyFingerprint: String,
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
            val lastUsedDate: ZonedDateTime,
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
            val creationDate: ZonedDateTime,
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
     * @property accessId The access ID of the send object (nullable).
     * @property password The password of the send object (nullable).
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
        val revisionDate: ZonedDateTime,

        @SerialName("maxAccessCount")
        val maxAccessCount: Int?,

        @SerialName("hideEmail")
        val shouldHideEmail: Boolean,

        @SerialName("type")
        val type: SendTypeJson,

        @SerialName("accessId")
        val accessId: String?,

        @SerialName("password")
        val password: String?,

        @SerialName("file")
        val file: File?,

        @SerialName("deletionDate")
        @Contextual
        val deletionDate: ZonedDateTime,

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
        val expirationDate: ZonedDateTime?,
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
    )
}
