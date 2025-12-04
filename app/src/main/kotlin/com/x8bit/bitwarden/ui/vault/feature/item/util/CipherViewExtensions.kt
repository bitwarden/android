package com.x8bit.bitwarden.ui.vault.feature.item.util

import androidx.annotation.DrawableRes
import com.bitwarden.core.data.util.toFormattedDateTimeStyle
import com.bitwarden.ui.platform.base.util.nullIfAllEqual
import com.bitwarden.ui.platform.base.util.orNullIfBlank
import com.bitwarden.ui.platform.base.util.orZeroWidthSpace
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.bitwarden.vault.CardView
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.Fido2Credential
import com.bitwarden.vault.FieldType
import com.bitwarden.vault.FieldView
import com.bitwarden.vault.IdentityView
import com.bitwarden.vault.LoginUriView
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemState
import com.x8bit.bitwarden.ui.vault.feature.item.model.TotpCodeItemData
import com.x8bit.bitwarden.ui.vault.feature.item.model.VaultItemLocation
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toLoginIconData
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
import com.x8bit.bitwarden.ui.vault.model.findVaultCardBrandWithNameOrNull
import kotlinx.collections.immutable.ImmutableList
import java.time.Clock
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Transforms [VaultData] into [VaultItemState.ViewState].
 */
@Suppress("CyclomaticComplexMethod", "LongMethod", "LongParameterList")
fun CipherView.toViewState(
    previousState: VaultItemState.ViewState.Content?,
    isPremiumUser: Boolean,
    totpCodeItemData: TotpCodeItemData?,
    clock: Clock = Clock.systemDefaultZone(),
    canDelete: Boolean,
    canRestore: Boolean,
    canAssignToCollections: Boolean,
    canEdit: Boolean,
    baseIconUrl: String,
    isIconLoadingDisabled: Boolean,
    relatedLocations: ImmutableList<VaultItemLocation>,
    hasOrganizations: Boolean,
): VaultItemState.ViewState =
    VaultItemState.ViewState.Content(
        common = VaultItemState.ViewState.Content.Common(
            currentCipher = this,
            name = name,
            customFields = fields.orEmpty().map { fieldView ->
                fieldView.toCustomField(
                    previousState = previousState
                        ?.common
                        ?.customFields
                        ?.find { it.id == fieldView.hashCode().toString() },
                )
            },
            created = BitwardenString.created.asText(
                creationDate.toFormattedDateTimeStyle(
                    dateStyle = FormatStyle.MEDIUM,
                    timeStyle = FormatStyle.SHORT,
                    clock = clock,
                ),
            ),
            lastUpdated = BitwardenString.last_edited.asText(
                revisionDate.toFormattedDateTimeStyle(
                    dateStyle = FormatStyle.MEDIUM,
                    timeStyle = FormatStyle.SHORT,
                    clock = clock,
                ),
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
            canRestore = canRestore,
            canAssignToCollections = canAssignToCollections,
            canEdit = canEdit,
            favorite = this.favorite,
            passwordHistoryCount = passwordHistory?.count(),
            iconData = this.toIconData(
                baseIconUrl = baseIconUrl,
                isIconLoadingDisabled = isIconLoadingDisabled,
            ),
            relatedLocations = relatedLocations,
            hasOrganizations = hasOrganizations,
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
                        ?.toFormattedDateTimeStyle(
                            dateStyle = FormatStyle.MEDIUM,
                            timeStyle = FormatStyle.SHORT,
                            clock = clock,
                        )
                        ?.let { BitwardenString.password_last_updated.asText(it) },
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

/**
 * Transforms [FieldView] into [VaultItemState.ViewState.Content.Common.Custom].
 */
fun FieldView.toCustomField(
    previousState: VaultItemState.ViewState.Content.Common.Custom?,
): VaultItemState.ViewState.Content.Common.Custom =
    when (type) {
        FieldType.TEXT -> VaultItemState.ViewState.Content.Common.Custom.TextField(
            id = this.hashCode().toString(),
            name = name.orEmpty(),
            value = value.orZeroWidthSpace(),
            isCopyable = !value.isNullOrBlank(),
        )

        FieldType.HIDDEN -> VaultItemState.ViewState.Content.Common.Custom.HiddenField(
            id = this.hashCode().toString(),
            name = name.orEmpty(),
            value = value.orZeroWidthSpace(),
            isCopyable = !value.isNullOrBlank(),
            isVisible = (previousState as?
                VaultItemState.ViewState.Content.Common.Custom.HiddenField)
                ?.isVisible
                ?: false,
        )

        FieldType.BOOLEAN -> VaultItemState.ViewState.Content.Common.Custom.BooleanField(
            id = this.hashCode().toString(),
            name = name.orEmpty(),
            value = value?.toBoolean() ?: false,
        )

        FieldType.LINKED -> VaultItemState.ViewState.Content.Common.Custom.LinkedField(
            id = this.hashCode().toString(),
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

private fun Fido2Credential.getCreationDateText(clock: Clock): Text =
    BitwardenString.created_x.asText(
        this.creationDate.toFormattedDateTimeStyle(
            dateStyle = FormatStyle.MEDIUM,
            timeStyle = FormatStyle.SHORT,
            clock = clock,
        ),
    )

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
        CipherType.SECURE_NOTE -> BitwardenDrawable.ic_note
        CipherType.CARD -> BitwardenDrawable.ic_payment_card
        CipherType.IDENTITY -> BitwardenDrawable.ic_id_card
        CipherType.SSH_KEY -> BitwardenDrawable.ic_ssh_key
        CipherType.LOGIN -> BitwardenDrawable.ic_globe
    }

@get:DrawableRes
private val CardView.paymentCardBrandIconRes: Int?
    get() = when (this.cardBrand) {
        VaultCardBrand.VISA -> BitwardenDrawable.ic_payment_card_brand_visa
        VaultCardBrand.MASTERCARD -> BitwardenDrawable.ic_payment_card_brand_mastercard
        VaultCardBrand.AMEX -> BitwardenDrawable.ic_payment_card_brand_amex
        VaultCardBrand.DISCOVER -> BitwardenDrawable.ic_payment_card_brand_discover
        VaultCardBrand.DINERS_CLUB -> BitwardenDrawable.ic_payment_card_brand_diners_club
        VaultCardBrand.JCB -> BitwardenDrawable.ic_payment_card_brand_jcb
        VaultCardBrand.MAESTRO -> BitwardenDrawable.ic_payment_card_brand_maestro
        VaultCardBrand.UNIONPAY -> BitwardenDrawable.ic_payment_card_brand_union_pay
        VaultCardBrand.RUPAY -> BitwardenDrawable.ic_payment_card_brand_ru_pay
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
            ?.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            },
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
