package com.x8bit.bitwarden.ui.tools.feature.send.addedit.util

import com.bitwarden.ui.platform.manager.share.model.ShareData
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.AddEditSendState

/**
 * Determines the initial [AddEditSendState.ViewState.Content.SendType] based on the data in the
 * [SpecialCircumstance].
 */
fun SpecialCircumstance?.toSendType(): AddEditSendState.ViewState.Content.SendType? =
    when (this) {
        is SpecialCircumstance.ShareNewSend -> {
            when (data) {
                is ShareData.FileSend -> {
                    AddEditSendState.ViewState.Content.SendType.File(
                        uri = data.fileData.uri,
                        name = data.fileData.fileName,
                        sizeBytes = data.fileData.sizeBytes,
                        displaySize = null,
                    )
                }

                is ShareData.TextSend -> {
                    AddEditSendState.ViewState.Content.SendType.Text(
                        input = data.text,
                        isHideByDefaultChecked = false,
                    )
                }
            }
        }

        else -> null
    }

/**
 * Determines the initial send name based on the data in the [SpecialCircumstance].
 */
fun SpecialCircumstance?.toSendName(): String? =
    when (this) {
        is SpecialCircumstance.ShareNewSend -> {
            when (data) {
                is ShareData.FileSend -> data.fileData.fileName
                is ShareData.TextSend -> data.subject
            }
        }

        else -> null
    }

/**
 * Determines if the [SpecialCircumstance] requires the app to be closed after completing the send.
 */
fun SpecialCircumstance?.shouldFinishOnComplete(): Boolean =
    when (this) {
        is SpecialCircumstance.ShareNewSend -> shouldFinishWhenComplete
        else -> false
    }
