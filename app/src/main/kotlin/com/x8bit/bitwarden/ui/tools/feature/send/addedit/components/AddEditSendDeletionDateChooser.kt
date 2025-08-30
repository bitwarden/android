package com.x8bit.bitwarden.ui.tools.feature.send.addedit.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.platform.composition.LocalClock
import kotlinx.collections.immutable.toImmutableList
import java.time.Clock
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Displays UX for choosing deletion date of a send.
 */
@Composable
fun AddEditSendDeletionDateChooser(
    onDateSelect: (ZonedDateTime) -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    clock: Clock = LocalClock.current,
) {
    val options = DeletionOption.entries.associateWith { it.text() }
    var selectedOption: DeletionOption by rememberSaveable {
        mutableStateOf(value = DeletionOption.SEVEN_DAYS)
    }
    BitwardenMultiSelectButton(
        label = stringResource(id = BitwardenString.deletion_date),
        isEnabled = isEnabled,
        options = options.values.toImmutableList(),
        selectedOption = selectedOption.text(),
        onOptionSelected = { selected ->
            selectedOption = options.entries.first { it.value == selected }.key
            onDateSelect(
                ZonedDateTime.now(clock).plus(selectedOption.offsetMillis, ChronoUnit.MILLIS),
            )
        },
        supportingText = stringResource(id = BitwardenString.deletion_date_info),
        insets = PaddingValues(top = 6.dp, bottom = 4.dp),
        cardStyle = CardStyle.Full,
        modifier = modifier,
    )
}

private enum class DeletionOption(
    val text: Text,
    val offsetMillis: Long,
) {
    ONE_HOUR(
        text = BitwardenString.one_hour.asText(),
        offsetMillis = 1.hours.inWholeMilliseconds,
    ),
    ONE_DAY(
        text = BitwardenString.one_day.asText(),
        offsetMillis = 1.days.inWholeMilliseconds,
    ),
    TWO_DAYS(
        text = BitwardenString.two_days.asText(),
        offsetMillis = 2.days.inWholeMilliseconds,
    ),
    THREE_DAYS(
        text = BitwardenString.three_days.asText(),
        offsetMillis = 3.days.inWholeMilliseconds,
    ),
    SEVEN_DAYS(
        text = BitwardenString.seven_days.asText(),
        offsetMillis = 7.days.inWholeMilliseconds,
    ),
    THIRTY_DAYS(
        text = BitwardenString.thirty_days.asText(),
        offsetMillis = 30.days.inWholeMilliseconds,
    ),
}
