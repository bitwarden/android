package com.x8bit.bitwarden.ui.tools.feature.send.addedit.components

import android.os.Parcelable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.core.data.util.toFormattedDateTimeStyle
import com.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.platform.composition.LocalClock
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.parcelize.Parcelize
import java.time.Clock
import java.time.ZonedDateTime
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Displays a UI for selecting a customizable date and time.
 *
 * @param originalSelection The originally selected time value, this cannot be changed after being
 * set.
 * @param onDateSelect The callback for being notified of updates to the selected date and time.
 * @param isEnabled Whether the button is enabled.
 * @param modifier A [Modifier] that you can use to apply custom modifications to the composable.
 * @param clock The clock used for formatting and timezone purposes.
 */
@Composable
fun AddEditSendCustomDateChooser(
    originalSelection: ZonedDateTime,
    onDateSelect: (ZonedDateTime) -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    clock: Clock = LocalClock.current,
) {
    val originalSelectionOption: CustomDeletionOption.Current = rememberSaveable {
        CustomDeletionOption.Current(time = originalSelection)
    }
    val options = persistentMapOf(
        originalSelectionOption to originalSelectionOption.getText(clock = clock)(),
        CustomDeletionOption.OneHour to CustomDeletionOption.OneHour.getText(clock = clock)(),
        CustomDeletionOption.OneDay to CustomDeletionOption.OneDay.getText(clock = clock)(),
        CustomDeletionOption.TwoDays to CustomDeletionOption.TwoDays.getText(clock = clock)(),
        CustomDeletionOption.ThreeDays to CustomDeletionOption.ThreeDays.getText(clock = clock)(),
        CustomDeletionOption.SevenDays to CustomDeletionOption.SevenDays.getText(clock = clock)(),
        CustomDeletionOption.ThirtyDays to CustomDeletionOption.ThirtyDays.getText(clock = clock)(),
    )
    var currentSelectionOption: CustomDeletionOption by rememberSaveable(originalSelectionOption) {
        mutableStateOf(value = originalSelectionOption)
    }
    BitwardenMultiSelectButton(
        label = stringResource(id = BitwardenString.deletion_date),
        isEnabled = isEnabled,
        options = options.values.toImmutableList(),
        selectedOption = currentSelectionOption.getText(clock = clock).invoke(),
        onOptionSelected = { selected ->
            currentSelectionOption = options.entries.first { it.value == selected }.key
            onDateSelect(
                (currentSelectionOption as? CustomDeletionOption.Current)
                    ?.time
                    ?: ZonedDateTime
                        .now(clock)
                        .plus(currentSelectionOption.offsetMillis, ChronoUnit.MILLIS),
            )
        },
        supportingText = stringResource(id = BitwardenString.deletion_date_info),
        insets = PaddingValues(top = 6.dp, bottom = 4.dp),
        cardStyle = CardStyle.Full,
        modifier = modifier,
    )
}

@Parcelize
private sealed class CustomDeletionOption : Parcelable {
    abstract val offsetMillis: Long
    abstract fun getText(clock: Clock): Text

    @Parcelize
    data class Current(
        val time: ZonedDateTime,
    ) : CustomDeletionOption() {
        override val offsetMillis: Long get() = 0L

        override fun getText(
            clock: Clock,
        ): Text = time
            .toFormattedDateTimeStyle(
                dateStyle = FormatStyle.MEDIUM,
                timeStyle = FormatStyle.SHORT,
                clock = clock,
            )
            .asText()
    }

    @Parcelize
    data object OneHour : CustomDeletionOption() {
        override val offsetMillis: Long get() = 1.hours.inWholeMilliseconds
        override fun getText(clock: Clock): Text = BitwardenString.one_hour.asText()
    }

    @Parcelize
    data object OneDay : CustomDeletionOption() {
        override val offsetMillis: Long get() = 1.days.inWholeMilliseconds
        override fun getText(clock: Clock): Text = BitwardenString.one_day.asText()
    }

    @Parcelize
    data object TwoDays : CustomDeletionOption() {
        override val offsetMillis: Long get() = 2.days.inWholeMilliseconds
        override fun getText(clock: Clock): Text = BitwardenString.two_days.asText()
    }

    @Parcelize
    data object ThreeDays : CustomDeletionOption() {
        override val offsetMillis: Long get() = 3.days.inWholeMilliseconds
        override fun getText(clock: Clock): Text = BitwardenString.three_days.asText()
    }

    @Parcelize
    data object SevenDays : CustomDeletionOption() {
        override val offsetMillis: Long get() = 7.days.inWholeMilliseconds
        override fun getText(clock: Clock): Text = BitwardenString.seven_days.asText()
    }

    @Parcelize
    data object ThirtyDays : CustomDeletionOption() {
        override val offsetMillis: Long get() = 30.days.inWholeMilliseconds
        override fun getText(clock: Clock): Text = BitwardenString.thirty_days.asText()
    }
}
