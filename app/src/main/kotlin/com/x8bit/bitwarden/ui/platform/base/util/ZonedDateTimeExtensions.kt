package com.x8bit.bitwarden.ui.platform.base.util

import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Returns a [Boolean] indicating whether this [ZonedDateTime] is five or more minutes old.
 */
@Suppress("MagicNumber")
fun ZonedDateTime.isOverFiveMinutesOld(
    clock: Clock = Clock.systemDefaultZone(),
): Boolean =
    Duration
        .between(this.toInstant(), clock.instant())
        .toMinutes() > 5
