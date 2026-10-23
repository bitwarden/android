package com.x8bit.bitwarden.ui.vault.feature.item.component

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.bottomsheet.BitwardenModalBottomSheet
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenTextSelectionButton
import com.bitwarden.ui.platform.components.dialog.BitwardenDatePickerDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTimePickerDialog
import com.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.resource.BitwardenPlurals
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.feature.item.model.VaultItemPamRequestForm
import kotlinx.collections.immutable.toImmutableList
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR

/**
 * The bottom sheet used to request access to a gated item. Automatic approval items ask for a
 * duration; human approval items ask for a start, an end and a required reason.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList", "LongMethod")
@Composable
fun VaultItemPamRequestSheet(
    form: VaultItemPamRequestForm,
    onDurationChange: (String) -> Unit,
    onStartDateChange: (LocalDate?) -> Unit,
    onStartTimeChange: (totalMinutes: Int) -> Unit,
    onEndDateChange: (LocalDate?) -> Unit,
    onEndTimeChange: (totalMinutes: Int) -> Unit,
    onReasonChange: (String) -> Unit,
    onSubmitClick: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    BitwardenModalBottomSheet(
        sheetTitle = stringResource(id = BitwardenString.pam_request_access_dialog_title),
        onDismiss = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(weight = 1f)
                    .fillMaxWidth()
                    .verticalScroll(state = rememberScrollState())
                    .standardHorizontalMargin(),
            ) {
                Spacer(modifier = Modifier.height(height = 4.dp))
                if (form.isHumanApproval) {
                    HumanApprovalFields(
                        form = form,
                        onStartDateChange = onStartDateChange,
                        onStartTimeChange = onStartTimeChange,
                        onEndDateChange = onEndDateChange,
                        onEndTimeChange = onEndTimeChange,
                        onReasonChange = onReasonChange,
                    )
                } else {
                    DurationSelector(
                        form = form,
                        onDurationChange = onDurationChange,
                    )
                }
            }
            BitwardenFilledButton(
                label = stringResource(id = BitwardenString.pam_submit_request),
                onClick = onSubmitClick,
                isEnabled = form.isSubmittable,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(vertical = 16.dp)
                    .navigationBarsPadding()
                    .imePadding()
                    .testTag(tag = "PamSubmitRequestButton"),
            )
        }
    }
}

@Composable
private fun DurationSelector(
    form: VaultItemPamRequestForm,
    onDurationChange: (String) -> Unit,
) {
    val optionsMinutes = form.durationOptionsMinutes
    val options = optionsMinutes.map { durationText(minutes = it) }.toImmutableList()
    BitwardenMultiSelectButton(
        label = stringResource(id = BitwardenString.pam_duration_required_label),
        options = options,
        selectedOption = form.durationMinutesOrNull?.let { durationText(minutes = it) },
        onOptionSelected = { selected ->
            options
                .indexOf(selected)
                .let { optionsMinutes.getOrNull(it) }
                ?.let { onDurationChange(it.toString()) }
        },
        cardStyle = CardStyle.Full,
        textFieldTestTag = "PamDurationField",
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun durationText(minutes: Long): String {
    val days = (minutes / MINUTES_PER_DAY).toInt()
    val hours = (minutes / MINUTES_PER_HOUR).toInt()
    val remainder = (minutes % MINUTES_PER_HOUR).toInt()
    return when {
        minutes % MINUTES_PER_DAY == 0L -> {
            pluralStringResource(
                id = BitwardenPlurals.pam_days_format,
                count = days,
                formatArgs = arrayOf(days),
            )
        }

        hours > 0 && remainder > 0 -> {
            stringResource(
                id = BitwardenString.hours_minutes_format,
                pluralStringResource(
                    id = BitwardenPlurals.hours_format,
                    count = hours,
                    formatArgs = arrayOf(hours),
                ),
                pluralStringResource(
                    id = BitwardenPlurals.minutes_format,
                    count = remainder,
                    formatArgs = arrayOf(remainder),
                ),
            )
        }

        hours > 0 -> {
            pluralStringResource(
                id = BitwardenPlurals.hours_format,
                count = hours,
                formatArgs = arrayOf(hours),
            )
        }

        else -> {
            pluralStringResource(
                id = BitwardenPlurals.minutes_format,
                count = remainder,
                formatArgs = arrayOf(remainder),
            )
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun HumanApprovalFields(
    form: VaultItemPamRequestForm,
    onStartDateChange: (LocalDate?) -> Unit,
    onStartTimeChange: (Int) -> Unit,
    onEndDateChange: (LocalDate?) -> Unit,
    onEndTimeChange: (Int) -> Unit,
    onReasonChange: (String) -> Unit,
) {
    Column {
        DateButton(
            label = stringResource(id = BitwardenString.pam_start_date),
            date = form.startDate,
            onDateSelect = onStartDateChange,
            testTag = "PamStartDateField",
        )
        TimeOfDayButton(
            label = stringResource(id = BitwardenString.pam_start_time),
            totalMinutes = form.startMinutes,
            onTimeSelect = onStartTimeChange,
            cardStyle = CardStyle.Bottom,
            testTag = "PamStartTimeField",
        )
    }
    Column {
        DateButton(
            label = stringResource(id = BitwardenString.pam_end_date),
            date = form.endDate,
            onDateSelect = onEndDateChange,
            testTag = "PamEndDateField",
        )
        TimeOfDayButton(
            label = stringResource(id = BitwardenString.pam_end_time),
            totalMinutes = form.endMinutes,
            onTimeSelect = onEndTimeChange,
            cardStyle = CardStyle.Bottom,
            testTag = "PamEndTimeField",
            errorText = when {
                form.isWindowEndBeforeStart -> {
                    stringResource(id = BitwardenString.pam_the_end_must_be_after_the_start)
                }

                form.isWindowOverMaximum -> {
                    stringResource(
                        id = BitwardenString.pam_the_window_can_be_at_most_format,
                        durationText(minutes = form.maxDurationMinutes),
                    )
                }

                else -> null
            },
        )
    }
    BitwardenTextField(
        label = stringResource(id = BitwardenString.pam_reason_required_label),
        value = form.reason,
        onValueChange = onReasonChange,
        singleLine = false,
        cardStyle = CardStyle.Full,
        textFieldTestTag = "PamReasonField",
        modifier = Modifier
            .fillMaxWidth(),
    )
}

/**
 * A date selector without the clear action of the optional date picker button, since the start
 * and end dates are required.
 */
@Composable
private fun DateButton(
    label: String,
    date: LocalDate?,
    onDateSelect: (LocalDate?) -> Unit,
    testTag: String,
) {
    var shouldShowDialog by rememberSaveable { mutableStateOf(value = false) }
    BitwardenTextSelectionButton(
        label = label,
        selectedOption = date
            ?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
            .orEmpty(),
        onClick = { shouldShowDialog = true },
        cardStyle = CardStyle.Top(dividerPadding = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag = testTag),
    )
    if (shouldShowDialog) {
        BitwardenDatePickerDialog(
            initialDate = date,
            onDateSelect = {
                shouldShowDialog = false
                onDateSelect(it)
            },
            onDismissRequest = { shouldShowDialog = false },
        )
    }
}

@Composable
private fun TimeOfDayButton(
    label: String,
    totalMinutes: Int,
    onTimeSelect: (Int) -> Unit,
    cardStyle: CardStyle,
    testTag: String,
    errorText: String? = null,
) {
    var shouldShowDialog by rememberSaveable { mutableStateOf(value = false) }
    val formattedTime = LocalTime
        .of(totalMinutes / MINUTES_PER_HOUR, totalMinutes % MINUTES_PER_HOUR)
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    BitwardenTextSelectionButton(
        label = label,
        selectedOption = formattedTime,
        onClick = { shouldShowDialog = true },
        cardStyle = cardStyle,
        supportingContent = errorText?.let {
            {
                Text(
                    text = it,
                    style = BitwardenTheme.typography.bodySmall,
                    color = BitwardenTheme.colorScheme.status.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(tag = "PamWindowError"),
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag = testTag),
    )
    if (shouldShowDialog) {
        BitwardenTimePickerDialog(
            initialHour = totalMinutes / MINUTES_PER_HOUR,
            initialMinute = totalMinutes % MINUTES_PER_HOUR,
            onTimeSelect = { hour, minute ->
                shouldShowDialog = false
                onTimeSelect(hour * MINUTES_PER_HOUR + minute)
            },
            onDismissRequest = { shouldShowDialog = false },
            is24Hour = DateFormat.is24HourFormat(LocalContext.current),
        )
    }
}
