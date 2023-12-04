package com.x8bit.bitwarden.ui.vault.feature.item.util

import com.bitwarden.core.CipherRepromptType
import com.bitwarden.core.CipherType
import com.bitwarden.core.CipherView
import com.bitwarden.core.FieldType
import com.bitwarden.core.FieldView
import com.bitwarden.core.LoginUriView
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.orZeroWidthSpace
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemState
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultState
import java.time.format.DateTimeFormatter
import java.util.TimeZone

private val dateTimeFormatter = DateTimeFormatter
    .ofPattern("M/d/yy hh:mm a")
    .withZone(TimeZone.getDefault().toZoneId())

/**
 * Transforms [VaultData] into [VaultState.ViewState].
 */
fun CipherView.toViewState(
    isPremiumUser: Boolean,
): VaultItemState.ViewState =
    when (type) {
        CipherType.LOGIN -> {
            val loginValues = requireNotNull(this.login)
            VaultItemState.ViewState.Content.Login(
                name = this.name,
                username = loginValues.username,
                passwordData = loginValues.password?.let {
                    VaultItemState.ViewState.Content.PasswordData(password = it, isVisible = false)
                },
                isPremiumUser = isPremiumUser,
                requiresReprompt = this.reprompt == CipherRepromptType.PASSWORD,
                customFields = this.fields.orEmpty().map { it.toCustomField() },
                uris = loginValues.uris.orEmpty().map { it.toUriData() },
                lastUpdated = dateTimeFormatter.format(this.revisionDate),
                passwordRevisionDate = loginValues.passwordRevisionDate?.let {
                    dateTimeFormatter.format(it)
                },
                passwordHistoryCount = this.passwordHistory?.count(),
                totp = loginValues.totp,
                notes = this.notes,
            )
        }

        CipherType.SECURE_NOTE -> VaultItemState.ViewState.Error(
            message = "Not yet implemented.".asText(),
        )

        CipherType.CARD -> VaultItemState.ViewState.Error(
            message = "Not yet implemented.".asText(),
        )

        CipherType.IDENTITY -> VaultItemState.ViewState.Error(
            message = "Not yet implemented.".asText(),
        )
    }

private fun FieldView.toCustomField(): VaultItemState.ViewState.Content.Custom =
    when (type) {
        FieldType.TEXT -> VaultItemState.ViewState.Content.Custom.TextField(
            name = name.orEmpty(),
            value = value.orZeroWidthSpace(),
            isCopyable = !value.isNullOrBlank(),
        )

        FieldType.HIDDEN -> VaultItemState.ViewState.Content.Custom.HiddenField(
            name = name.orEmpty(),
            value = value.orZeroWidthSpace(),
            isCopyable = !value.isNullOrBlank(),
            isVisible = false,
        )

        FieldType.BOOLEAN -> VaultItemState.ViewState.Content.Custom.BooleanField(
            name = name.orEmpty(),
            value = value?.toBoolean() ?: false,
        )

        FieldType.LINKED -> VaultItemState.ViewState.Content.Custom.LinkedField(
            id = requireNotNull(linkedId),
            name = name.orEmpty(),
        )
    }

private fun LoginUriView.toUriData() =
    VaultItemState.ViewState.Content.UriData(
        uri = uri.orZeroWidthSpace(),
        isCopyable = !uri.isNullOrBlank(),
        isLaunchable = !uri.isNullOrBlank(),
    )
