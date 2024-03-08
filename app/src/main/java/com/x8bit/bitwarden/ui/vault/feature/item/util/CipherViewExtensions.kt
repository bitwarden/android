package com.x8bit.bitwarden.ui.vault.feature.item.util

import com.bitwarden.core.CardView
import com.bitwarden.core.CipherRepromptType
import com.bitwarden.core.CipherType
import com.bitwarden.core.CipherView
import com.bitwarden.core.FieldType
import com.bitwarden.core.FieldView
import com.bitwarden.core.IdentityView
import com.bitwarden.core.LoginUriView
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.util.capitalize
import com.x8bit.bitwarden.ui.platform.base.util.nullIfAllEqual
import com.x8bit.bitwarden.ui.platform.base.util.orNullIfBlank
import com.x8bit.bitwarden.ui.platform.base.util.orZeroWidthSpace
import com.x8bit.bitwarden.ui.platform.util.toFormattedPattern
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemState
import com.x8bit.bitwarden.ui.vault.feature.item.model.TotpCodeItemData
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultState
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
import com.x8bit.bitwarden.ui.vault.model.findVaultCardBrandWithNameOrNull
import java.util.TimeZone

private const val DATE_TIME_PATTERN: String = "M/d/yy hh:mm a"

/**
 * Transforms [VaultData] into [VaultState.ViewState].
 */
@Suppress("LongMethod")
fun CipherView.toViewState(
    isPremiumUser: Boolean,
    totpCodeItemData: TotpCodeItemData?,
): VaultItemState.ViewState =
    VaultItemState.ViewState.Content(
        common = VaultItemState.ViewState.Content.Common(
            currentCipher = this,
            name = name,
            requiresReprompt = reprompt == CipherRepromptType.PASSWORD,
            customFields = fields.orEmpty().map { it.toCustomField() },
            lastUpdated = revisionDate.toFormattedPattern(
                pattern = DATE_TIME_PATTERN,
                zone = TimeZone.getDefault().toZoneId(),
            ),
            notes = notes,
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
        ),
        type = when (type) {
            CipherType.LOGIN -> {
                val loginValues = requireNotNull(login)
                VaultItemState.ViewState.Content.ItemType.Login(
                    username = loginValues.username,
                    passwordData = loginValues.password?.let {
                        VaultItemState.ViewState.Content.ItemType.Login.PasswordData(
                            password = it,
                            isVisible = false,
                            canViewPassword = viewPassword,
                        )
                    },
                    uris = loginValues.uris.orEmpty().map { it.toUriData() },
                    passwordRevisionDate = loginValues
                        .passwordRevisionDate
                        ?.toFormattedPattern(
                            pattern = DATE_TIME_PATTERN,
                            zone = TimeZone.getDefault().toZoneId(),
                        ),
                    passwordHistoryCount = passwordHistory?.count(),
                    isPremiumUser = isPremiumUser,
                    totpCodeItemData = totpCodeItemData,
                )
            }

            CipherType.SECURE_NOTE -> {
                VaultItemState.ViewState.Content.ItemType.SecureNote
            }

            CipherType.CARD -> {
                VaultItemState.ViewState.Content.ItemType.Card(
                    cardholderName = card?.cardholderName,
                    number = card?.number,
                    brand = card?.cardBrand,
                    expiration = card?.expiration,
                    securityCode = card?.code,
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
