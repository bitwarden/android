package com.x8bit.bitwarden.ui.tools.feature.send.addedit.util

import com.bitwarden.send.SendFileView
import com.bitwarden.send.SendTextView
import com.bitwarden.send.SendType
import com.bitwarden.send.SendView
import com.bitwarden.ui.platform.base.util.orNullIfBlank
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.AddEditSendState
import java.time.Clock

/**
 * Transforms [AddEditSendState] into [SendView].
 */
fun AddEditSendState.ViewState.Content.toSendView(
    clock: Clock,
): SendView =
    SendView(
        id = common.originalSendView?.id,
        accessId = common.originalSendView?.accessId,
        name = common.name,
        notes = common.noteInput.orNullIfBlank(),
        key = common.originalSendView?.key,
        newPassword = common.passwordInput.orNullIfBlank(),
        hasPassword = false,
        type = selectedType.toSendType(),
        file = toSendFileView(),
        text = toSendTextView(),
        maxAccessCount = common.maxAccessCount?.toUInt(),
        accessCount = 0U,
        disabled = common.isDeactivateChecked,
        hideEmail = common.isHideEmailChecked,
        revisionDate = clock.instant(),
        deletionDate = common.deletionDate.toInstant(),
        expirationDate = common.expirationDate?.let {
            // We no longer support expiration dates but is a send has one already,
            // we just update it to match the deletion date.
            common.deletionDate.toInstant()
        },
    )

private fun AddEditSendState.ViewState.Content.SendType.toSendType(): SendType =
    when (this) {
        is AddEditSendState.ViewState.Content.SendType.File -> SendType.FILE
        is AddEditSendState.ViewState.Content.SendType.Text -> SendType.TEXT
    }

private fun AddEditSendState.ViewState.Content.toSendFileView(): SendFileView? =
    (this.selectedType as? AddEditSendState.ViewState.Content.SendType.File)?.let {
        SendFileView(
            id = null,
            fileName = it.name.orEmpty(),
            size = null,
            sizeName = null,
        )
    }

private fun AddEditSendState.ViewState.Content.toSendTextView(): SendTextView? =
    (this.selectedType as? AddEditSendState.ViewState.Content.SendType.Text)?.let {
        SendTextView(
            text = it.input,
            hidden = it.isHideByDefaultChecked,
        )
    }
