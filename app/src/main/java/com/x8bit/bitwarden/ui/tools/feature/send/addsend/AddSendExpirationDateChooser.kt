package com.x8bit.bitwarden.ui.tools.feature.send.addsend

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.toImmutableList
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Displays UX for choosing expiration date of a send.
 */
@Suppress("LongMethod")
@Composable
fun SendExpirationDateChooser(
    currentZonedDateTime: ZonedDateTime?,
    dateFormatPattern: String,
    timeFormatPattern: String,
    onDateSelect: (ZonedDateTime?) -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val defaultOption = ExpirationOptions.NEVER
    val options = ExpirationOptions.entries.associateWith { it.text() }
    var selectedOption: ExpirationOptions by rememberSaveable { mutableStateOf(defaultOption) }
    Column(
        modifier = modifier,
    ) {
        BitwardenMultiSelectButton(
            label = stringResource(id = R.string.expiration_date),
            isEnabled = isEnabled,
            options = options.values.toImmutableList(),
            selectedOption = selectedOption.text(),
            onOptionSelected = { selected ->
                selectedOption = options.entries.first { it.value == selected }.key
                if (selectedOption != ExpirationOptions.NEVER) {
                    onDateSelect(null)
                } else if (selectedOption != ExpirationOptions.CUSTOM) {
                    onDateSelect(
                        // Add the appropriate milliseconds offset based on the selected option
                        ZonedDateTime.now().plus(selectedOption.offsetMillis, ChronoUnit.MILLIS),
                    )
                }
            },
        )

        AnimatedVisibility(visible = selectedOption == ExpirationOptions.CUSTOM) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                AddSendCustomDateChooser(
                    dateLabel = stringResource(id = R.string.expiration_date),
                    timeLabel = stringResource(id = R.string.expiration_time),
                    currentZonedDateTime = currentZonedDateTime,
                    dateFormatPattern = dateFormatPattern,
                    timeFormatPattern = timeFormatPattern,
                    onDateSelect = onDateSelect,
                    isEnabled = isEnabled,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(id = R.string.expiration_date_info),
            style = BitwardenTheme.typography.bodySmall,
            color = BitwardenTheme.colorScheme.text.secondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
}

private enum class ExpirationOptions(
    val text: Text,
    val offsetMillis: Long,
) {
    NEVER(
        text = R.string.never.asText(),
        offsetMillis = -1L,
    ),
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
