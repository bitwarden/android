package com.x8bit.bitwarden.ui.tools.feature.send.model

import androidx.annotation.DrawableRes
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText

/**
 * Represents the types of icons to be displayed with the send.
 */
enum class SendStatusIcon(
    @DrawableRes val iconRes: Int,
    val contentDescription: Text,
    val testTag: String,
) {
    DISABLED(
        iconRes = R.drawable.ic_send_disabled,
        contentDescription = R.string.disabled.asText(),
        testTag = "DisabledSendIcon",
    ),
    PASSWORD(
        iconRes = R.drawable.ic_key,
        contentDescription = R.string.password.asText(),
        testTag = "PasswordProtectedSendIcon",
    ),
    EXPIRED(
        iconRes = R.drawable.ic_send_expired,
        contentDescription = R.string.expired.asText(),
        testTag = "ExpiredSendIcon",
    ),
    MAX_ACCESS_COUNT_REACHED(
        iconRes = R.drawable.ic_send_max_access_count_reached,
        contentDescription = R.string.maximum_access_count_reached.asText(),
        testTag = "MaxAccessSendIcon",
    ),
    PENDING_DELETE(
        iconRes = R.drawable.ic_send_pending_delete,
        contentDescription = R.string.pending_delete.asText(),
        testTag = "PendingDeletionSendIcon",
    ),
}
