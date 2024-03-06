package com.x8bit.bitwarden.ui.tools.feature.send.addsend

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import kotlinx.collections.immutable.toImmutableList
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

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
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val defaultOption = DeletionOptions.SEVEN_DAYS
    val options = DeletionOptions.entries.associateWith { it.text() }
    var selectedOption: DeletionOptions by rememberSaveable { mutableStateOf(defaultOption) }
    Column(
        modifier = modifier,
    ) {
        BitwardenMultiSelectButton(
            label = stringResource(id = R.string.deletion_date),
            isEnabled = isEnabled,
            options = options.values.toImmutableList(),
            selectedOption = selectedOption.text(),
            onOptionSelected = { selected ->
                selectedOption = options.entries.first { it.value == selected }.key
                if (selectedOption != DeletionOptions.CUSTOM) {
                    onDateSelect(
                        // Add the appropriate milliseconds offset based on the selected option
                        ZonedDateTime.now().plus(selectedOption.offsetMillis, ChronoUnit.MILLIS),
                    )
                }
            },
        )

        AnimatedVisibility(visible = selectedOption == DeletionOptions.CUSTOM) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                AddSendCustomDateChooser(
                    currentZonedDateTime = currentZonedDateTime,
                    dateFormatPattern = dateFormatPattern,
                    timeFormatPattern = timeFormatPattern,
                    onDateSelect = { onDateSelect(requireNotNull(it)) },
                    isEnabled = isEnabled,
                )
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

private enum class DeletionOptions(
    val text: Text,
    val offsetMillis: Long,
) {
    ONE_HOUR(
        text = R.string.one_hour.asText(),
        offsetMillis = 1.hours.inWholeMilliseconds,
    ),
    ONE_DAY(
        text = R.string.one_day.asText(),
        offsetMillis = 1.days.inWholeMilliseconds,
    ),
    TWO_DAYS(
        text = R.string.two_days.asText(),
        offsetMillis = 2.days.inWholeMilliseconds,
    ),
    THREE_DAYS(
        text = R.string.three_days.asText(),
        offsetMillis = 3.days.inWholeMilliseconds,
    ),
    SEVEN_DAYS(
        text = R.string.seven_days.asText(),
        offsetMillis = 7.days.inWholeMilliseconds,
    ),
    THIRTY_DAYS(
        text = R.string.thirty_days.asText(),
        offsetMillis = 30.days.inWholeMilliseconds,
    ),
    CUSTOM(
        text = R.string.custom.asText(),
        offsetMillis = -1L,
    ),
}
