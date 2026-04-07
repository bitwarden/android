package com.x8bit.bitwarden.ui.tools.feature.send.model

import androidx.annotation.DrawableRes
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText

/**
 * Represents the types of icons to be displayed with the send.
 */
enum class SendStatusIcon(
    @field:DrawableRes val iconRes: Int,
    val contentDescription: Text,
    val testTag: String,
) {
    DISABLED(
        iconRes = BitwardenDrawable.ic_send_disabled,
        contentDescription = BitwardenString.disabled.asText(),
        testTag = "DisabledSendIcon",
    ),
    PASSWORD(
        iconRes = BitwardenDrawable.ic_key,
        contentDescription = BitwardenString.password.asText(),
        testTag = "PasswordProtectedSendIcon",
    ),
    EXPIRED(
        iconRes = BitwardenDrawable.ic_send_expired,
        contentDescription = BitwardenString.expired.asText(),
        testTag = "ExpiredSendIcon",
    ),
    MAX_ACCESS_COUNT_REACHED(
        iconRes = BitwardenDrawable.ic_send_max_access_count_reached,
        contentDescription = BitwardenString.maximum_access_count_reached.asText(),
        testTag = "MaxAccessSendIcon",
    ),
    PENDING_DELETE(
        iconRes = BitwardenDrawable.ic_send_pending_delete,
        contentDescription = BitwardenString.pending_delete.asText(),
        testTag = "PendingDeletionSendIcon",
    ),
}
