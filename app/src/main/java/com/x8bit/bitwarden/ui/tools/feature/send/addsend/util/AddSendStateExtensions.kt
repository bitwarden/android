package com.x8bit.bitwarden.ui.tools.feature.send.addsend.util

import com.bitwarden.send.SendFileView
import com.bitwarden.send.SendTextView
import com.bitwarden.send.SendType
import com.bitwarden.send.SendView
import com.x8bit.bitwarden.ui.platform.base.util.orNullIfBlank
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.AddSendState
import java.time.Clock

/**
 * Transforms [AddSendState] into [SendView].
 */
fun AddSendState.ViewState.Content.toSendView(
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
        expirationDate = common.expirationDate?.toInstant(),
    )

private fun AddSendState.ViewState.Content.SendType.toSendType(): SendType =
    when (this) {
        is AddSendState.ViewState.Content.SendType.File -> SendType.FILE
        is AddSendState.ViewState.Content.SendType.Text -> SendType.TEXT
    }

private fun AddSendState.ViewState.Content.toSendFileView(): SendFileView? =
    (this.selectedType as? AddSendState.ViewState.Content.SendType.File)?.let {
        SendFileView(
            id = null,
            fileName = it.name.orEmpty(),
            size = null,
            sizeName = null,
        )
    }

private fun AddSendState.ViewState.Content.toSendTextView(): SendTextView? =
    (this.selectedType as? AddSendState.ViewState.Content.SendType.Text)?.let {
        SendTextView(
            text = it.input,
            hidden = it.isHideByDefaultChecked,
        )
    }
