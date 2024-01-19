package com.x8bit.bitwarden.ui.tools.feature.send.util

import com.bitwarden.core.SendView
import com.x8bit.bitwarden.ui.platform.components.model.IconRes
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendStatusIcon
import java.time.Clock

/**
 * Creates the list of trailing label icons to be displayed for a [SendView].
 */
fun SendView.toLabelIcons(clock: Clock = Clock.systemDefaultZone()): List<IconRes> =
    listOfNotNull(
        SendStatusIcon.DISABLED.takeIf { disabled },
        SendStatusIcon.PASSWORD.takeIf { hasPassword },
        SendStatusIcon.MAX_ACCESS_COUNT_REACHED.takeIf {
            maxAccessCount?.let { maxCount -> accessCount >= maxCount } == true
        },
        SendStatusIcon.EXPIRED.takeIf { expirationDate?.isBefore(clock.instant()) == true },
        SendStatusIcon.PENDING_DELETE.takeIf { deletionDate.isBefore(clock.instant()) },
    )
        .map { IconRes(iconRes = it.iconRes, contentDescription = it.contentDescription) }

/**
 * Creates a sharable url from a [SendView].
 */
fun SendView.toSendUrl(
    baseWebSendUrl: String,
): String = "$baseWebSendUrl$accessId/$key"
