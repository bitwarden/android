package com.x8bit.bitwarden.ui.tools.feature.send

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenDateSelectButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTimeSelectButton
import kotlinx.collections.immutable.toImmutableList
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Displays UX for choosing deletion date of a send.
 */
@Suppress("LongMethod")
@Composable
fun SendDeletionDateChooser(
    currentZonedDateTime: ZonedDateTime,
    dateFormatPattern: String,
    timeFormatPattern: String,
    onDateSelect: (ZonedDateTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    val customOption = stringResource(id = R.string.custom)
    val options = listOf(
        stringResource(id = R.string.one_hour),
        stringResource(id = R.string.one_day),
        stringResource(id = R.string.two_days),
        stringResource(id = R.string.three_days),
        stringResource(id = R.string.seven_days),
        stringResource(id = R.string.thirty_days),
        customOption,
    )
    val defaultOption = stringResource(id = R.string.seven_days)
    var selectedOption: String by rememberSaveable { mutableStateOf(defaultOption) }
    Column(
        modifier = modifier,
    ) {
        BitwardenMultiSelectButton(
            label = stringResource(id = R.string.deletion_date),
            options = options.toImmutableList(),
            selectedOption = selectedOption,
            onOptionSelected = { selectedOption = it },
        )

        AnimatedVisibility(visible = selectedOption == customOption) {
            // This tracks the date component (year, month, and day) and ignores lower level
            // components.
            var date: ZonedDateTime by remember {
                mutableStateOf(currentZonedDateTime)
            }
            // This tracks just the time component (hours and minutes) and ignores the higher level
            // components. 0 representing midnight and counting up from there.
            var timeMillis: Long by remember {
                mutableStateOf(
                    currentZonedDateTime.hour.hours.inWholeMilliseconds +
                        currentZonedDateTime.minute.minutes.inWholeMilliseconds,
                )
            }
            val derivedDateTimeMillis: ZonedDateTime by remember {
                derivedStateOf { date.plus(timeMillis, ChronoUnit.MILLIS) }
            }

            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Row {
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
                            timeMillis = hour.hours.inWholeMilliseconds +
                                minute.minutes.inWholeMilliseconds
                            onDateSelect(derivedDateTimeMillis)
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(id = R.string.deletion_date_info),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
}
