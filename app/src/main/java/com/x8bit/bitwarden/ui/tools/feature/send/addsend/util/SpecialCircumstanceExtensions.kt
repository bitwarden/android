package com.x8bit.bitwarden.ui.tools.feature.send.addsend.util

import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.AddSendState

/**
 * Determines the initial [AddSendState.ViewState.Content.SendType] based on the data in the
 * [UserState.SpecialCircumstance].
 */
fun UserState.SpecialCircumstance?.toSendType(): AddSendState.ViewState.Content.SendType? =
    when (this) {
        is UserState.SpecialCircumstance.ShareNewSend -> {
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
 * Determines the initial send name based on the data in the [UserState.SpecialCircumstance].
 */
fun UserState.SpecialCircumstance?.toSendName(): String? =
    when (this) {
        is UserState.SpecialCircumstance.ShareNewSend -> {
            when (data) {
                is IntentManager.ShareData.FileSend -> data.fileData.fileName
                is IntentManager.ShareData.TextSend -> data.subject
            }
        }

        else -> null
    }

/**
 * Determines if the [UserState.SpecialCircumstance] requires the app to be closed after completing
 * the send.
 */
fun UserState.SpecialCircumstance?.shouldFinishOnComplete(): Boolean =
    when (this) {
        is UserState.SpecialCircumstance.ShareNewSend -> shouldFinishWhenComplete
        else -> false
    }
