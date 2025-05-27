package com.x8bit.bitwarden.ui.platform.util

import java.time.ZonedDateTime

/**
 * Returns the current [ZonedDateTime] or [ZonedDateTime.now] if the current one is null.
 */
fun ZonedDateTime?.orNow(): ZonedDateTime = this ?: ZonedDateTime.now()
