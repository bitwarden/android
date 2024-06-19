package com.x8bit.bitwarden.ui.tools.feature.send.addsend.util

import com.bitwarden.send.SendType
import com.bitwarden.send.SendView
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.AddSendState
import com.x8bit.bitwarden.ui.tools.feature.send.util.toSendUrl
import java.time.Clock
import java.time.ZonedDateTime

/**
 * Transforms [SendView] into [AddSendState.ViewState.Content].
 */
fun SendView.toViewState(
    clock: Clock,
    baseWebSendUrl: String,
    isHideEmailAddressEnabled: Boolean,
): AddSendState.ViewState.Content =
    AddSendState.ViewState.Content(
        common = AddSendState.ViewState.Content.Common(
            originalSendView = this,
            name = this.name,
            currentAccessCount = this.accessCount.toInt(),
            maxAccessCount = this.maxAccessCount?.toInt(),
            // We do not set the password here
            // We only allow them to create new passwords, not view old ones
            passwordInput = "",
            noteInput = this.notes.orEmpty(),
            isHideEmailChecked = this.hideEmail,
            isDeactivateChecked = this.disabled,
            deletionDate = ZonedDateTime.ofInstant(this.deletionDate, clock.zone),
            expirationDate = this.expirationDate?.let { ZonedDateTime.ofInstant(it, clock.zone) },
            sendUrl = this.toSendUrl(baseWebSendUrl),
            hasPassword = this.hasPassword,
            isHideEmailAddressEnabled = isHideEmailAddressEnabled,
        ),
        selectedType = when (type) {
            SendType.TEXT -> {
                AddSendState.ViewState.Content.SendType.Text(
                    input = this.text?.text.orEmpty(),
                    isHideByDefaultChecked = this.text?.hidden == true,
                )
            }

            SendType.FILE -> {
                val fileView = requireNotNull(this.file)
                AddSendState.ViewState.Content.SendType.File(
                    uri = null,
                    name = fileView.fileName,
                    displaySize = fileView.sizeName,
                    sizeBytes = null,
                )
            }
        },
    )
