package com.x8bit.bitwarden.ui.vault.feature.item.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * The privileged access (PAM) state of a gated vault item, as displayed in the item banner.
 */
sealed class VaultItemPamState : Parcelable {

    /**
     * The item is gated and the user can request access.
     *
     * @property isHumanApproval Whether a human approves the request (start, end and reason are
     * collected) or it is approved automatically (only a duration is collected).
     * @property defaultDurationMinutes The suggested lease duration, in minutes.
     * @property maxDurationMinutes The maximum lease duration, in minutes.
     * @property isSlotHeldByAnotherMember Whether another member currently holds the single lease
     * slot.
     * @property slotFreesAt When the slot is expected to free up, if known.
     */
    @Parcelize
    data class CanRequest(
        val isHumanApproval: Boolean,
        val defaultDurationMinutes: Long,
        val maxDurationMinutes: Long,
        val isSlotHeldByAnotherMember: Boolean,
        val slotFreesAt: Instant? = null,
    ) : VaultItemPamState()

    /**
     * An access request is waiting for approval.
     */
    @Parcelize
    data object Pending : VaultItemPamState()

    /**
     * An access request has been approved but access has not started yet.
     *
     * @property requestId The ID of the approved request, used to start or cancel it, or `null`
     * when it is unknown (the actions are then hidden).
     */
    @Parcelize
    data class Approved(val requestId: String?) : VaultItemPamState()

    /**
     * The user has an active lease with [remainingMinutes] left (rounded up) out of
     * [totalMinutes], which is used to draw the progress.
     */
    @Parcelize
    data class Active(
        val remainingMinutes: Long,
        val totalMinutes: Long = remainingMinutes,
    ) : VaultItemPamState()
}

/**
 * The data entered in the request access form.
 *
 * @property isHumanApproval Whether the form collects a window and reason (`true`) or a duration
 * (`false`).
 * @property durationMinutes The requested duration, in minutes, as typed by the user.
 * @property maxDurationMinutes The maximum duration allowed, in minutes.
 * @property startDate The date the access window starts.
 * @property startMinutes The minute of the day the access window starts.
 * @property endDate The date the access window ends.
 * @property endMinutes The minute of the day the access window ends.
 * @property reason The reason for the request.
 */
@Parcelize
data class VaultItemPamRequestForm(
    val isHumanApproval: Boolean,
    val durationMinutes: String,
    val maxDurationMinutes: Long,
    val startDate: LocalDate?,
    val startMinutes: Int,
    val endDate: LocalDate?,
    val endMinutes: Int,
    val reason: String,
) : Parcelable {

    /**
     * The requested duration in minutes, or `null` if the typed value is not a positive number.
     */
    val durationMinutesOrNull: Long?
        get() = durationMinutes.trim().toLongOrNull()?.takeIf { it > 0 }

    /**
     * The durations, in minutes, offered by the duration selector: the presets that fit within
     * [maxDurationMinutes], the maximum itself when it is short enough to be a useful choice, and
     * the currently selected duration.
     */
    val durationOptionsMinutes: List<Long>
        get() {
            val withinMaximum = DURATION_PRESETS_MINUTES.filter { it <= effectiveMaxMinutes }
            val maximum = maxDurationMinutes.takeIf { it in 1..MAX_SELECTABLE_MAXIMUM_MINUTES }
            return (withinMaximum + listOfNotNull(maximum, durationMinutesOrNull))
                .filter { it <= effectiveMaxMinutes }
                .distinct()
                .sorted()
        }

    /**
     * The length of the requested access window in minutes, or `null` when a date is missing.
     */
    val windowMinutesOrNull: Long?
        get() {
            val start = startDate?.let { LocalDateTime.of(it, startMinutes.toLocalTime()) }
            val end = endDate?.let { LocalDateTime.of(it, endMinutes.toLocalTime()) }
            return if (start != null && end != null) {
                Duration.between(start, end).toMinutes()
            } else {
                null
            }
        }

    /**
     * Whether the window ends at or before the moment it starts.
     */
    val isWindowEndBeforeStart: Boolean
        get() = windowMinutesOrNull?.let { it <= 0 } == true

    /**
     * Whether the window is longer than [maxDurationMinutes].
     */
    val isWindowOverMaximum: Boolean
        get() = maxDurationMinutes > 0 &&
            windowMinutesOrNull?.let { it > maxDurationMinutes } == true

    /**
     * Whether the form has everything needed to be submitted.
     */
    val isSubmittable: Boolean
        get() = if (isHumanApproval) {
            startDate != null &&
                endDate != null &&
                !isWindowEndBeforeStart &&
                !isWindowOverMaximum &&
                reason.isNotBlank()
        } else {
            durationMinutesOrNull
                ?.let { maxDurationMinutes <= 0 || it <= maxDurationMinutes } == true
        }

    private val effectiveMaxMinutes: Long
        get() = maxDurationMinutes.takeIf { it > 0 } ?: Long.MAX_VALUE
}

private fun Int.toLocalTime(): LocalTime =
    LocalTime.of(this / MINUTES_PER_HOUR, this % MINUTES_PER_HOUR)

private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = 24L * MINUTES_PER_HOUR
private const val MAX_SELECTABLE_MAXIMUM_MINUTES = 7L * MINUTES_PER_DAY
private const val MINUTES_PER_HOUR_LONG = 60L
private const val FIFTEEN_MINUTES = 15L
private const val THIRTY_MINUTES = 30L
private const val TWO_HOURS = 2 * MINUTES_PER_HOUR_LONG
private const val FOUR_HOURS = 4 * MINUTES_PER_HOUR_LONG
private const val EIGHT_HOURS = 8 * MINUTES_PER_HOUR_LONG
private val DURATION_PRESETS_MINUTES = listOf(
    FIFTEEN_MINUTES,
    THIRTY_MINUTES,
    MINUTES_PER_HOUR_LONG,
    TWO_HOURS,
    FOUR_HOURS,
    EIGHT_HOURS,
    MINUTES_PER_DAY,
)
