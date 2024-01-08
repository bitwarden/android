package com.x8bit.bitwarden.ui.tools.feature.send.addsend.util

import com.bitwarden.core.SendFileView
import com.bitwarden.core.SendTextView
import com.bitwarden.core.SendType
import com.bitwarden.core.SendView
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.AddSendState
import java.time.Instant

/**
 * Transforms [AddSendState] into [SendView].
 */
// TODO: The 'key' needs to be updated in order to get the save operation to work (BIT-480)
fun AddSendState.ViewState.Content.toSendView(): SendView =
    SendView(
        id = null,
        accessId = null,
        name = common.name,
        notes = common.noteInput,
        key = "",
        password = common.passwordInput.takeUnless { it.isBlank() },
        type = selectedType.toSendType(),
        file = toSendFileView(),
        text = toSendTextView(),
        maxAccessCount = common.maxAccessCount?.toUInt(),
        accessCount = 0U,
        disabled = common.isDeactivateChecked,
        hideEmail = common.isHideEmailChecked,
        revisionDate = Instant.now(),
        deletionDate = Instant.now(),
        expirationDate = null,
    )

private fun AddSendState.ViewState.Content.SendType.toSendType(): SendType =
    when (this) {
        AddSendState.ViewState.Content.SendType.File -> SendType.FILE
        is AddSendState.ViewState.Content.SendType.Text -> SendType.TEXT
    }

private fun AddSendState.ViewState.Content.toSendFileView(): SendFileView? =
    (this.selectedType as? AddSendState.ViewState.Content.SendType.File)?.let {
        // TODO: Add support for these properties in order to save a file (BIT-480)
        SendFileView(
            id = "",
            fileName = "",
            size = "",
            sizeName = "",
        )
    }

private fun AddSendState.ViewState.Content.toSendTextView(): SendTextView? =
    (this.selectedType as? AddSendState.ViewState.Content.SendType.Text)?.let {
        SendTextView(
            text = it.input,
            hidden = it.isHideByDefaultChecked,
        )
    }
