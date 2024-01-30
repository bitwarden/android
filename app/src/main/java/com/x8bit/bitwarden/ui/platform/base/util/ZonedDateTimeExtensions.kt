package com.x8bit.bitwarden.ui.platform.base.util

import java.time.Duration
import java.time.ZonedDateTime
import java.util.TimeZone

/**
 * Returns a [Boolean] indicating whether this [ZonedDateTime] is five or more minutes old.
 */
@Suppress("MagicNumber")
fun ZonedDateTime.isOverFiveMinutesOld(): Boolean =
    Duration
        .between(
            this,
            ZonedDateTime.now(TimeZone.getDefault().toZoneId()),
        )
        .toMinutes() > 5
