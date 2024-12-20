package com.x8bit.bitwarden.ui.vault.feature.item.util

import com.bitwarden.vault.CardView
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.Fido2Credential
import com.bitwarden.vault.FieldType
import com.bitwarden.vault.FieldView
import com.bitwarden.vault.IdentityView
import com.bitwarden.vault.LoginUriView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.capitalize
import com.x8bit.bitwarden.ui.platform.base.util.nullIfAllEqual
import com.x8bit.bitwarden.ui.platform.base.util.orNullIfBlank
import com.x8bit.bitwarden.ui.platform.base.util.orZeroWidthSpace
import com.x8bit.bitwarden.ui.platform.util.toFormattedPattern
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemState
import com.x8bit.bitwarden.ui.vault.feature.item.model.TotpCodeItemData
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
import com.x8bit.bitwarden.ui.vault.model.findVaultCardBrandWithNameOrNull
import java.time.Clock

private const val LAST_UPDATED_DATE_TIME_PATTERN: String = "M/d/yy hh:mm a"
private const val FIDO2_CREDENTIAL_CREATION_DATE_PATTERN: String = "M/d/yy"
private const val FIDO2_CREDENTIAL_CREATION_TIME_PATTERN: String = "h:mm a"

/**
 * Transforms [VaultData] into [VaultState.ViewState].
 */
@Suppress("CyclomaticComplexMethod", "LongMethod", "LongParameterList")
fun CipherView.toViewState(
    previousState: VaultItemState.ViewState.Content?,
    isPremiumUser: Boolean,
    hasMasterPassword: Boolean,
    totpCodeItemData: TotpCodeItemData?,
    clock: Clock = Clock.systemDefaultZone(),
    canDelete: Boolean,
    canAssignToCollections: Boolean,
): VaultItemState.ViewState =
    VaultItemState.ViewState.Content(
        common = VaultItemState.ViewState.Content.Common(
            currentCipher = this,
            name = name,
            requiresReprompt = (reprompt == CipherRepromptType.PASSWORD && hasMasterPassword) &&
                previousState?.common?.requiresReprompt != false,
            customFields = fields.orEmpty().map { it.toCustomField() },
            lastUpdated = revisionDate.toFormattedPattern(
                pattern = LAST_UPDATED_DATE_TIME_PATTERN,
                clock = clock,
            ),
            notes = notes,
            requiresCloneConfirmation = login?.fido2Credentials?.any() ?: false,
            attachments = attachments
                ?.mapNotNull {
                    @Suppress("ComplexCondition")
                    if (it.id == null ||
                        it.fileName == null ||
                        it.size == null ||
                        it.sizeName == null ||
                        it.url == null
                    ) {
                        null
                    } else {
                        VaultItemState.ViewState.Content.Common.AttachmentItem(
                            id = requireNotNull(it.id),
                            title = requireNotNull(it.fileName),
                            displaySize = requireNotNull(it.sizeName),
                            url = requireNotNull(it.url),
                            isLargeFile = try {
                                requireNotNull(it.size).toLong() >= 10485760
                            } catch (exception: NumberFormatException) {
                                false
                            },
                            isDownloadAllowed = isPremiumUser || this.organizationId != null,
                        )
                    }
                }
                .orEmpty(),
            canDelete = canDelete,
            canAssignToCollections = canAssignToCollections,
        ),
        type = when (type) {
            CipherType.LOGIN -> {
                val loginValues = requireNotNull(login)
                VaultItemState.ViewState.Content.ItemType.Login(
                    username = loginValues.username,
                    passwordData = loginValues.password?.let {
                        VaultItemState.ViewState.Content.ItemType.Login.PasswordData(
                            password = it,
                            isVisible = (previousState?.type as?
                                VaultItemState.ViewState.Content.ItemType.Login)
                                ?.passwordData
                                ?.isVisible == true,
                            canViewPassword = viewPassword,
                        )
                    },
                    uris = loginValues.uris.orEmpty().map { it.toUriData() },
                    passwordRevisionDate = loginValues
                        .passwordRevisionDate
                        ?.toFormattedPattern(
                            pattern = LAST_UPDATED_DATE_TIME_PATTERN,
                            clock = clock,
                        ),
                    passwordHistoryCount = passwordHistory?.count(),
                    isPremiumUser = isPremiumUser,
                    canViewTotpCode = isPremiumUser || this.organizationUseTotp,
                    totpCodeItemData = totpCodeItemData,
                    fido2CredentialCreationDateText = loginValues
                        .fido2Credentials
                        ?.firstOrNull()
                        ?.getCreationDateText(clock),
                )
            }

            CipherType.SECURE_NOTE -> {
                VaultItemState.ViewState.Content.ItemType.SecureNote
            }

            CipherType.CARD -> {
                VaultItemState.ViewState.Content.ItemType.Card(
                    cardholderName = card?.cardholderName,
                    number = card?.number?.let {
                        VaultItemState.ViewState.Content.ItemType.Card.NumberData(
                            number = it,
                            isVisible = (previousState?.type
                                as? VaultItemState.ViewState.Content.ItemType.Card)
                                ?.number
                                ?.isVisible == true,
                        )
                    },
                    brand = card?.cardBrand,
                    expiration = card?.expiration,
                    securityCode = card?.code?.let {
                        VaultItemState.ViewState.Content.ItemType.Card.CodeData(
                            code = it,
                            isVisible = (previousState?.type
                                as? VaultItemState.ViewState.Content.ItemType.Card)
                                ?.securityCode
                                ?.isVisible == true,
                        )
                    },
                )
            }

            CipherType.IDENTITY -> {
                VaultItemState.ViewState.Content.ItemType.Identity(
                    username = identity?.username,
                    identityName = identity?.identityName,
                    company = identity?.company,
                    ssn = identity?.ssn,
                    passportNumber = identity?.passportNumber,
                    licenseNumber = identity?.licenseNumber,
                    email = identity?.email,
                    phone = identity?.phone,
                    address = identity?.identityAddress,
                )
            }

            CipherType.SSH_KEY -> {
                val sshKeyValues = requireNotNull(sshKey)
                VaultItemState.ViewState.Content.ItemType.SshKey(
                    name = name,
                    publicKey = sshKeyValues.publicKey,
                    privateKey = sshKeyValues.privateKey,
                    fingerprint = sshKeyValues.fingerprint,
                    showPrivateKey = (previousState?.type as?
                        VaultItemState.ViewState.Content.ItemType.SshKey)
                        ?.showPrivateKey == true,
                )
            }
        },
    )

private fun FieldView.toCustomField(): VaultItemState.ViewState.Content.Common.Custom =
    when (type) {
        FieldType.TEXT -> VaultItemState.ViewState.Content.Common.Custom.TextField(
            name = name.orEmpty(),
            value = value.orZeroWidthSpace(),
            isCopyable = !value.isNullOrBlank(),
        )

        FieldType.HIDDEN -> VaultItemState.ViewState.Content.Common.Custom.HiddenField(
            name = name.orEmpty(),
            value = value.orZeroWidthSpace(),
            isCopyable = !value.isNullOrBlank(),
            isVisible = false,
        )

        FieldType.BOOLEAN -> VaultItemState.ViewState.Content.Common.Custom.BooleanField(
            name = name.orEmpty(),
            value = value?.toBoolean() ?: false,
        )

        FieldType.LINKED -> VaultItemState.ViewState.Content.Common.Custom.LinkedField(
            vaultLinkedFieldType = VaultLinkedFieldType.fromId(requireNotNull(linkedId)),
            name = name.orEmpty(),
        )
    }

private fun LoginUriView.toUriData() =
    VaultItemState.ViewState.Content.ItemType.Login.UriData(
        uri = uri.orZeroWidthSpace(),
        isCopyable = !uri.isNullOrBlank(),
        isLaunchable = !uri.isNullOrBlank(),
    )

private fun Fido2Credential?.getCreationDateText(clock: Clock): Text? =
    this?.let {
        R.string.created_xy.asText(
            creationDate.toFormattedPattern(
                pattern = FIDO2_CREDENTIAL_CREATION_DATE_PATTERN,
                clock = clock,
            ),
            creationDate.toFormattedPattern(
                pattern = FIDO2_CREDENTIAL_CREATION_TIME_PATTERN,
                clock = clock,
            ),
        )
    }

private val IdentityView.identityAddress: String?
    get() = listOfNotNull(
        address1,
        address2,
        address3,
        listOf(city ?: "-", state ?: "-", postalCode ?: "-")
            .nullIfAllEqual("-")
            ?.joinToString(", "),
        country,
    )
        .joinToString("\n")
        .orNullIfBlank()

private val IdentityView.identityName: String?
    get() = listOfNotNull(
        title
            ?.lowercase()
            ?.capitalize(),
        firstName,
        middleName,
        lastName,
    )
        .joinToString(" ")
        .orNullIfBlank()

private val CardView.cardBrand: VaultCardBrand?
    get() = brand
        ?.findVaultCardBrandWithNameOrNull()
        .takeUnless { it == VaultCardBrand.SELECT }

private val CardView.expiration: String?
    get() = listOfNotNull(
        expMonth?.padStart(length = 2, padChar = '0'),
        expYear,
    )
        .joinToString("/")
        .orNullIfBlank()
