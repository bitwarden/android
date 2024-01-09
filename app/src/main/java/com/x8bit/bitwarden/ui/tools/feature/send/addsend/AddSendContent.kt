package com.x8bit.bitwarden.ui.tools.feature.send.addsend

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.BitwardenFilledTonalButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.BitwardenSegmentedButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenStepper
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.BitwardenWideSwitch
import com.x8bit.bitwarden.ui.platform.components.SegmentedButtonState
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.handlers.AddSendHandlers

/**
 * Content view for the [AddSendScreen].
 */
@Suppress("LongMethod")
@Composable
fun AddSendContent(
    state: AddSendState.ViewState.Content,
    addSendHandlers: AddSendHandlers,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
    ) {
        BitwardenTextField(
            modifier = Modifier.padding(horizontal = 16.dp),
            label = stringResource(id = R.string.name),
            hint = stringResource(id = R.string.name_info),
            value = state.common.name,
            onValueChange = addSendHandlers.onNamChange,
        )

        Spacer(modifier = Modifier.height(16.dp))
        BitwardenListHeaderText(
            label = stringResource(id = R.string.type),
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))
        BitwardenSegmentedButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            options = listOf(
                SegmentedButtonState(
                    text = stringResource(id = R.string.file),
                    onClick = addSendHandlers.onFileTypeSelect,
                    isChecked = state.selectedType is AddSendState.ViewState.Content.SendType.File,
                ),
                SegmentedButtonState(
                    text = stringResource(id = R.string.text),
                    onClick = addSendHandlers.onTextTypeSelect,
                    isChecked = state.selectedType is AddSendState.ViewState.Content.SendType.Text,
                ),
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))
        when (val type = state.selectedType) {
            is AddSendState.ViewState.Content.SendType.File -> {
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.file),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    text = stringResource(id = R.string.no_file_chosen),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenFilledTonalButton(
                    label = stringResource(id = R.string.choose_file),
                    onClick = addSendHandlers.onChooseFileCLick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.max_file_size),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.type_file_info),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            is AddSendState.ViewState.Content.SendType.Text -> {
                BitwardenTextField(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    label = stringResource(id = R.string.text),
                    hint = stringResource(id = R.string.type_text_info),
                    value = type.input,
                    onValueChange = addSendHandlers.onTextChange,
                )
                Spacer(modifier = Modifier.height(16.dp))
                BitwardenWideSwitch(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    label = stringResource(id = R.string.hide_text_by_default),
                    isChecked = type.isHideByDefaultChecked,
                    onCheckedChange = addSendHandlers.onIsHideByDefaultToggle,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        AddSendOptions(
            state = state,
            addSendHandlers = addSendHandlers,
        )

        Spacer(modifier = Modifier.height(24.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

/**
 * Displays a collapsable set of new send options.
 *
 * @param state The content state.
 * @param addSendHandlers THe handlers various events.
 */
@Suppress("LongMethod")
@Composable
private fun AddSendOptions(
    state: AddSendState.ViewState.Content,
    addSendHandlers: AddSendHandlers,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = if (isExpanded) {
                    stringResource(id = R.string.options_expanded)
                } else {
                    stringResource(id = R.string.options_collapsed)
                },
                onClick = { isExpanded = !isExpanded },
            )
            .padding(16.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = R.string.options),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(end = 8.dp),
        )
        Icon(
            painter = if (isExpanded) {
                painterResource(R.drawable.ic_expand_up)
            } else {
                painterResource(R.drawable.ic_expand_down)
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
    // Hide all content if not expanded:
    AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
        modifier = Modifier.clipToBounds(),
    ) {
        Column {
            SendDeletionDateChooser(
                modifier = Modifier.padding(horizontal = 16.dp),
                dateFormatPattern = state.common.dateFormatPattern,
                timeFormatPattern = state.common.timeFormatPattern,
                currentZonedDateTime = state.common.deletionDate,
                onDateSelect = addSendHandlers.onDeletionDateChange,
            )
            Spacer(modifier = Modifier.height(8.dp))
            SendExpirationDateChooser(
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenStepper(
                label = stringResource(id = R.string.maximum_access_count),
                value = state.common.maxAccessCount,
                onValueChange = addSendHandlers.onMaxAccessCountChange,
                isDecrementEnabled = state.common.maxAccessCount != null,
                modifier = Modifier
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.maximum_access_count_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenPasswordField(
                label = stringResource(id = R.string.new_password),
                hint = stringResource(id = R.string.password_info),
                value = state.common.passwordInput,
                onValueChange = addSendHandlers.onPasswordChange,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenTextField(
                label = stringResource(id = R.string.notes),
                hint = stringResource(id = R.string.notes_info),
                value = state.common.noteInput,
                onValueChange = addSendHandlers.onNoteChange,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            BitwardenWideSwitch(
                modifier = Modifier.padding(horizontal = 16.dp),
                label = stringResource(id = R.string.hide_email),
                isChecked = state.common.isHideEmailChecked,
                onCheckedChange = addSendHandlers.onHideEmailToggle,
            )
            Spacer(modifier = Modifier.height(16.dp))
            BitwardenWideSwitch(
                modifier = Modifier.padding(horizontal = 16.dp),
                label = stringResource(id = R.string.disable_send),
                isChecked = state.common.isDeactivateChecked,
                onCheckedChange = addSendHandlers.onDeactivateSendToggle,
            )
        }
    }
}
