package com.x8bit.bitwarden.data.auth.manager.util

import com.bitwarden.core.util.isOverFiveMinutesOld
import com.x8bit.bitwarden.data.auth.manager.model.AuthRequest
import java.time.Clock

/**
 * Whether this request may still be approved or declined
 * and has not expired (it is under 5 minutes old).
 */
fun AuthRequest.isActionable(clock: Clock): Boolean =
    !requestApproved &&
        responseDate == null &&
        !creationDate.isOverFiveMinutesOld(clock)

/**
 * Filters out [AuthRequest]s that match one of the following criteria:
 * * The request has been approved.
 * * The request has been declined (indicated by it not being approved & having a responseDate).
 * * The request has expired (it is at least 5 minutes old).
 */
fun List<AuthRequest>.filterRespondedAndExpired(clock: Clock): List<AuthRequest> =
    filter { it.isActionable(clock = clock) }
