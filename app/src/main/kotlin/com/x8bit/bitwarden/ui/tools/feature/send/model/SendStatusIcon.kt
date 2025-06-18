package com.x8bit.bitwarden.ui.tools.feature.send.model

import androidx.annotation.DrawableRes
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.R

/**
 * Represents the types of icons to be displayed with the send.
 */
enum class SendStatusIcon(
    @DrawableRes val iconRes: Int,
    val contentDescription: Text,
    val testTag: String,
) {
    DISABLED(
        iconRes = BitwardenDrawable.ic_send_disabled,
        contentDescription = R.string.disabled.asText(),
        testTag = "DisabledSendIcon",
    ),
    PASSWORD(
        iconRes = BitwardenDrawable.ic_key,
        contentDescription = R.string.password.asText(),
        testTag = "PasswordProtectedSendIcon",
    ),
    EXPIRED(
        iconRes = BitwardenDrawable.ic_send_expired,
        contentDescription = R.string.expired.asText(),
        testTag = "ExpiredSendIcon",
    ),
    MAX_ACCESS_COUNT_REACHED(
        iconRes = BitwardenDrawable.ic_send_max_access_count_reached,
        contentDescription = R.string.maximum_access_count_reached.asText(),
        testTag = "MaxAccessSendIcon",
    ),
    PENDING_DELETE(
        iconRes = BitwardenDrawable.ic_send_pending_delete,
        contentDescription = R.string.pending_delete.asText(),
        testTag = "PendingDeletionSendIcon",
    ),
}
