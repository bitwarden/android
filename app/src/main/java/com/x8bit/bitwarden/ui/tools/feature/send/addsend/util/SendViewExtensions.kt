package com.x8bit.bitwarden.ui.tools.feature.send.addsend.util

import com.bitwarden.core.SendType
import com.bitwarden.core.SendView
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
): AddSendState.ViewState.Content =
    AddSendState.ViewState.Content(
        common = AddSendState.ViewState.Content.Common(
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
        ),
        selectedType = when (type) {
            SendType.TEXT -> {
                AddSendState.ViewState.Content.SendType.Text(
                    input = this.text?.text.orEmpty(),
                    isHideByDefaultChecked = this.text?.hidden == true,
                )
            }

            SendType.FILE -> AddSendState.ViewState.Content.SendType.File
        },
    )
