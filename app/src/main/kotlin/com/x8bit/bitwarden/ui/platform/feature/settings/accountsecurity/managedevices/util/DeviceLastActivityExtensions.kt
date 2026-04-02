package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity.managedevices.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.platform.manager.resource.ResourceManager
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Returns a localized string describing how recently this device was active,
 * or null if no activity date is available.
 *
 * Buckets are based on calendar days in the device's local timezone, matching
 * the web client behaviour. Using [java.time.LocalDate] comparison makes this DST-safe without
 * requiring rounding (unlike the JavaScript equivalent).
 */
@Suppress("MagicNumber")
fun Instant?.toLastActivityLabel(clock: Clock, resourceManager: ResourceManager): Text? {
    this ?: return null
    val nowDate = clock.instant().atZone(clock.zone).toLocalDate()
    val activityDate = this.atZone(clock.zone).toLocalDate()
    val daysAgo = ChronoUnit.DAYS.between(activityDate, nowDate)
    val resId = when {
        daysAgo <= 0 -> BitwardenString.today
        daysAgo < 7 -> BitwardenString.past_seven_days
        daysAgo < 14 -> BitwardenString.past_fourteen_days
        daysAgo < 30 -> BitwardenString.past_thirty_days
        else -> BitwardenString.over_thirty_days_ago
    }
    return resourceManager.getString(resId).asText()
}
