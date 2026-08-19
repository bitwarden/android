package com.x8bit.bitwarden.ui.tools.feature.send.addedit.util

import com.bitwarden.network.model.SendAccessTypeJson
import com.bitwarden.send.SendType
import com.bitwarden.send.SendView
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.AddEditSendState
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.model.AddEditSendType
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.model.AuthEmail
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.model.SendAuth
import com.x8bit.bitwarden.ui.tools.feature.send.util.toSendUrl
import kotlinx.collections.immutable.toImmutableList
import java.time.Clock
import java.time.temporal.ChronoUnit

/**
 * The deletion window applied to a new Send when no deletion date is enforced by policy (7 days).
 */
internal const val DEFAULT_DELETION_HOURS: Long = 7 * 24L

/**
 * Returns the [SendAuth] this access type restricts a Send to, preserving [current] when it already
 * matches the restricted type so that any emails already entered are kept.
 */
internal fun SendAccessTypeJson.toEnforcedSendAuth(current: SendAuth): SendAuth = when (this) {
    // Every option stays available, so the current selection is left alone.
    SendAccessTypeJson.ANY -> current
    SendAccessTypeJson.PASSWORD_PROTECTED -> SendAuth.Password
    SendAccessTypeJson.SPECIFIC_PEOPLE -> current as? SendAuth.Email ?: SendAuth.Email()
}

/**
 * Returns the [SendAuth] describing who this Send is shared with.
 */
fun SendView.toSendAuth(): SendAuth = when {
    hasPassword -> SendAuth.Password
    emails.isNotEmpty() -> {
        SendAuth.Email(emails = this.emails.map { AuthEmail(value = it) }.toImmutableList())
    }

    else -> SendAuth.None
}

/**
 * Maps this loaded [SendView] into the content for whichever mode the add/edit screen is in: the
 * send's own values when editing it, or the values a compliant copy of it should start from.
 *
 * @param state The state being updated, which is the in-flight value rather than the view model's
 * `state` property so that the mapping sees the same policy data as the update it belongs to.
 * @param clock Used to derive the copy's deletion date from the enforced window.
 * @param baseWebSendUrl The base URL used to build an existing send's share URL.
 * @param isHideEmailAddressEnabled Whether hiding the sender's email is currently allowed.
 */
fun SendView.toAddEditViewState(
    state: AddEditSendState,
    clock: Clock,
    baseWebSendUrl: String,
    isHideEmailAddressEnabled: Boolean,
): AddEditSendState.ViewState.Content = when (state.addEditSendType) {
    is AddEditSendType.CopyItem -> {
        toCopyViewState(
            deletionDate = clock.instant().plus(
                state.enforcedDeletionHours?.toLong() ?: DEFAULT_DELETION_HOURS,
                ChronoUnit.HOURS,
            ),
            isHideEmailAddressEnabled = isHideEmailAddressEnabled,
            sendAuth = state
                .enforcedWhoCanAccess
                ?.toEnforcedSendAuth(current = this.toSendAuth())
                ?: this.toSendAuth(),
        )
    }

    AddEditSendType.AddItem,
    is AddEditSendType.EditItem,
        -> {
        toViewState(
            baseWebSendUrl = baseWebSendUrl,
            isHideEmailAddressEnabled = isHideEmailAddressEnabled,
        )
    }
}

/**
 * Transforms [SendView] into [AddEditSendState.ViewState.Content].
 */
fun SendView.toViewState(
    baseWebSendUrl: String,
    isHideEmailAddressEnabled: Boolean,
): AddEditSendState.ViewState.Content =
    AddEditSendState.ViewState.Content(
        common = AddEditSendState.ViewState.Content.Common(
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
            deletionDate = this.deletionDate,
            expirationDate = this.expirationDate,
            sendUrl = this.toSendUrl(baseWebSendUrl),
            hasPassword = this.hasPassword,
            isHideEmailAddressEnabled = isHideEmailAddressEnabled,
            sendAuth = this.toSendAuth(),
        ),
        selectedType = when (type) {
            SendType.TEXT -> {
                AddEditSendState.ViewState.Content.SendType.Text(
                    input = this.text?.text.orEmpty(),
                    isHideByDefaultChecked = this.text?.hidden == true,
                )
            }

            SendType.FILE -> {
                val fileView = requireNotNull(this.file)
                AddEditSendState.ViewState.Content.SendType.File(
                    uri = null,
                    name = fileView.fileName,
                    displaySize = fileView.sizeName,
                    sizeBytes = null,
                )
            }

            SendType.ITEM -> TODO("[PM-41095] Support Item SendType")
        },
    )
