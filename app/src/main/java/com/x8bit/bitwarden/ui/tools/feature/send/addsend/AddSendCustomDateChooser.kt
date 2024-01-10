package com.x8bit.bitwarden.ui.tools.feature.send.addsend

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenDateSelectButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTimeSelectButton
import com.x8bit.bitwarden.ui.platform.util.orNow
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Displays a UI for selecting a customizable date and time.
 *
 * @param currentZonedDateTime The currently selected time, `null` when no time is selected yet.
 * @param dateFormatPattern The pattern to use when displaying the date.
 * @param timeFormatPattern The pattern for displaying the time.
 * @param onDateSelect The callback for being notified of updates to the selected date and time.
 * This will only be `null` when there is no selected time.
 * @param modifier A [Modifier] that you can use to apply custom modifications to the composable.
 */
@Composable
fun AddSendCustomDateChooser(
    currentZonedDateTime: ZonedDateTime?,
    dateFormatPattern: String,
    timeFormatPattern: String,
    onDateSelect: (ZonedDateTime?) -> Unit,
    modifier: Modifier = Modifier,
) {
    // This tracks the date component (year, month, and day) and ignores lower level
    // components.
    var date: ZonedDateTime? by remember { mutableStateOf(currentZonedDateTime) }
    // This tracks just the time component (hours and minutes) and ignores the higher level
    // components. 0 representing midnight and counting up from there.
    var timeMillis: Long by remember {
        mutableStateOf(
            currentZonedDateTime.orNow().let {
                it.hour.hours.inWholeMilliseconds + it.minute.minutes.inWholeMilliseconds
            },
        )
    }
    val derivedDateTimeMillis: ZonedDateTime? by remember {
        derivedStateOf { date?.plus(timeMillis, ChronoUnit.MILLIS) }
    }

    Row(
        modifier = modifier,
    ) {
        BitwardenDateSelectButton(
            modifier = Modifier.weight(1f),
            formatPattern = dateFormatPattern,
            currentZonedDateTime = currentZonedDateTime,
            onDateSelect = {
                date = it
                onDateSelect(derivedDateTimeMillis)
            },
        )
        Spacer(modifier = Modifier.width(16.dp))
        BitwardenTimeSelectButton(
            modifier = Modifier.weight(1f),
            formatPattern = timeFormatPattern,
            currentZonedDateTime = currentZonedDateTime,
            onTimeSelect = { hour, minute ->
                timeMillis = hour.hours.inWholeMilliseconds + minute.minutes.inWholeMilliseconds
                onDateSelect(derivedDateTimeMillis)
            },
        )
    }
}
