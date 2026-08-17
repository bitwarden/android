package com.x8bit.bitwarden.ui.tools.feature.send.addedit.util

import com.bitwarden.send.SendType
import com.bitwarden.send.SendView
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.AddEditSendState
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.model.SendAuth
import java.time.Instant

/**
 * Transforms [SendView] into the [AddEditSendState.ViewState.Content] for a copy of it.
 *
 * A copy is a new Send rather than an edit of this one, so anything that belongs to the original is
 * left behind: it has no share URL or access count yet, it is never created deactivated, and the
 * original password cannot be carried over because it is never readable. [deletionDate] comes from
 * the caller so that a policy-enforced window is applied to the copy.
 *
 * Recipient emails are copied as they are, even when a policy restricts their domains. The user is
 * told which domains are allowed when they save, which is friendlier than silently dropping
 * recipients they chose.
 *
 * @param deletionDate The deletion date to apply to the copy.
 * @param isHideEmailAddressEnabled Whether hiding the sender's email is currently allowed.
 * @param sendAuth The access type to apply to the copy, which the policy may have forced.
 */
fun SendView.toCopyViewState(
    deletionDate: Instant,
    isHideEmailAddressEnabled: Boolean,
    sendAuth: SendAuth,
): AddEditSendState.ViewState.Content =
    AddEditSendState.ViewState.Content(
        common = AddEditSendState.ViewState.Content.Common(
            originalSendView = null,
            name = this.name,
            currentAccessCount = null,
            maxAccessCount = this.maxAccessCount?.toInt(),
            passwordInput = "",
            noteInput = this.notes.orEmpty(),
            isHideEmailChecked = this.hideEmail && isHideEmailAddressEnabled,
            isDeactivateChecked = false,
            deletionDate = deletionDate,
            expirationDate = null,
            sendUrl = null,
            hasPassword = false,
            isHideEmailAddressEnabled = isHideEmailAddressEnabled,
            sendAuth = sendAuth,
        ),
        selectedType = when (type) {
            SendType.TEXT -> {
                AddEditSendState.ViewState.Content.SendType.Text(
                    input = this.text?.text.orEmpty(),
                    isHideByDefaultChecked = this.text?.hidden == true,
                )
            }

            // A file send's attachment cannot be uploaded again, so it is never offered a copy.
            SendType.FILE -> {
                AddEditSendState.ViewState.Content.SendType.File(
                    uri = null,
                    name = null,
                    displaySize = null,
                    sizeBytes = null,
                )
            }

            SendType.ITEM -> TODO("[PM-41095] Support Item SendType")
        },
    )
