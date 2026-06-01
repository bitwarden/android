package com.bitwarden.core.util

import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Returns a [Boolean] indicating whether this [Instant] is five or more minutes old.
 */
@Suppress("MagicNumber")
fun Instant.isOverFiveMinutesOld(
    clock: Clock = Clock.systemDefaultZone(),
): Boolean =
    Duration
        .between(this, clock.instant())
        .toMinutes() > 5
