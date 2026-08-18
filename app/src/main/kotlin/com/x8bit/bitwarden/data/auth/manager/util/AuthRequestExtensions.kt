package com.x8bit.bitwarden.data.auth.manager.util

import com.bitwarden.core.util.isOverFiveMinutesOld
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequest
import java.time.Clock

/**
 * Whether this request may still be approved or declined, meaning it has not already been
 * approved, not been declined (indicated by it not being approved & having a responseDate), and
 * has not expired (it is under 5 minutes old).
 */
fun AuthRequest.isActionable(clock: Clock): Boolean =
    !requestApproved &&
        responseDate == null &&
        !creationDate.isOverFiveMinutesOld(clock)
