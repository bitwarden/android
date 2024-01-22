package com.x8bit.bitwarden.ui.tools.feature.send.addsend.util

import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.AddSendState

/**
 * Determines the initial [AddSendState.ViewState.Content.SendType] based on the data in the
 * [SpecialCircumstance].
 */
fun SpecialCircumstance?.toSendType(): AddSendState.ViewState.Content.SendType? =
    when (this) {
        is SpecialCircumstance.ShareNewSend -> {
            when (data) {
                is IntentManager.ShareData.FileSend -> AddSendState.ViewState.Content.SendType.File(
                    uri = data.fileData.uri,
                    name = data.fileData.fileName,
                    sizeBytes = data.fileData.sizeBytes,
                    displaySize = null,
                )

                is IntentManager.ShareData.TextSend -> AddSendState.ViewState.Content.SendType.Text(
                    input = data.text,
                    isHideByDefaultChecked = false,
                )
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
                is IntentManager.ShareData.FileSend -> data.fileData.fileName
                is IntentManager.ShareData.TextSend -> data.subject
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
