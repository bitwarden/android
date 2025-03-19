package com.x8bit.bitwarden.ui.vault.feature.item.util

import androidx.annotation.DrawableRes
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
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.util.toFormattedPattern
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemState
import com.x8bit.bitwarden.ui.vault.feature.item.model.TotpCodeItemData
import com.x8bit.bitwarden.ui.vault.feature.item.model.VaultItemLocation
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toLoginIconData
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
import com.x8bit.bitwarden.ui.vault.model.findVaultCardBrandWithNameOrNull
import kotlinx.collections.immutable.ImmutableList
import java.time.Clock

private const val LAST_UPDATED_DATE_TIME_PATTERN: String = "M/d/yy hh:mm a"
private const val FIDO2_CREDENTIAL_CREATION_DATE_PATTERN: String = "M/d/yy"
private const val FIDO2_CREDENTIAL_CREATION_TIME_PATTERN: String = "h:mm a"

/**
 * Transforms [VaultData] into [VaultItemState.ViewState].
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
    canEdit: Boolean,
    baseIconUrl: String,
    isIconLoadingDisabled: Boolean,
    relatedLocations: ImmutableList<VaultItemLocation>,
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
                            } catch (_: NumberFormatException) {
                                false
                            },
                            isDownloadAllowed = isPremiumUser || this.organizationId != null,
                        )
                    }
                }
                .orEmpty(),
            canDelete = canDelete,
            canAssignToCollections = canAssignToCollections,
            canEdit = canEdit,
            favorite = this.favorite,
            passwordHistoryCount = passwordHistory?.count(),
            iconData = this.toIconData(
                baseIconUrl = baseIconUrl,
                isIconLoadingDisabled = isIconLoadingDisabled,
            ),
            relatedLocations = relatedLocations,
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
                    paymentCardBrandIconData = card?.paymentCardBrandIconRes?.let {
                        IconData.Local(iconRes = it)
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

private fun CipherView.toIconData(
    baseIconUrl: String,
    isIconLoadingDisabled: Boolean,
): IconData {
    return when (this.type) {
        CipherType.LOGIN -> {
            login?.uris.toLoginIconData(
                baseIconUrl = baseIconUrl,
                isIconLoadingDisabled = isIconLoadingDisabled,
                usePasskeyDefaultIcon = false,
            )
        }

        CipherType.CARD -> {
            card?.paymentCardBrandIconRes
                ?.let { IconData.Local(iconRes = it) }
                ?: IconData.Local(type.iconRes)
        }

        else -> {
            IconData.Local(iconRes = this.type.iconRes)
        }
    }
}

@get:DrawableRes
private val CipherType.iconRes: Int
    get() = when (this) {
        CipherType.SECURE_NOTE -> R.drawable.ic_note
        CipherType.CARD -> R.drawable.ic_payment_card
        CipherType.IDENTITY -> R.drawable.ic_id_card
        CipherType.SSH_KEY -> R.drawable.ic_ssh_key
        CipherType.LOGIN -> R.drawable.ic_globe
    }

@get:DrawableRes
private val CardView.paymentCardBrandIconRes: Int?
    get() = when (this.cardBrand) {
        VaultCardBrand.VISA -> R.drawable.ic_payment_card_brand_visa
        VaultCardBrand.MASTERCARD -> R.drawable.ic_payment_card_brand_mastercard
        VaultCardBrand.AMEX -> R.drawable.ic_payment_card_brand_amex
        VaultCardBrand.DISCOVER -> R.drawable.ic_payment_card_brand_discover
        VaultCardBrand.DINERS_CLUB -> R.drawable.ic_payment_card_brand_diners_club
        VaultCardBrand.JCB -> R.drawable.ic_payment_card_brand_jcb
        VaultCardBrand.MAESTRO -> R.drawable.ic_payment_card_brand_maestro
        VaultCardBrand.UNIONPAY -> R.drawable.ic_payment_card_brand_union_pay
        VaultCardBrand.RUPAY -> R.drawable.ic_payment_card_brand_ru_pay
        VaultCardBrand.SELECT,
        VaultCardBrand.OTHER,
        null,
            -> null
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
