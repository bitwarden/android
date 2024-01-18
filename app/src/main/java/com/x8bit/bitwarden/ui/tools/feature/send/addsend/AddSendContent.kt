package com.x8bit.bitwarden.ui.tools.feature.send.addsend

import android.Manifest
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
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
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.BitwardenWideSwitch
import com.x8bit.bitwarden.ui.platform.components.SegmentedButtonState
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManager
import com.x8bit.bitwarden.ui.platform.theme.LocalNonMaterialTypography
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.handlers.AddSendHandlers

/**
 * Content view for the [AddSendScreen].
 */
@Suppress("LongMethod")
@Composable
fun AddSendContent(
    state: AddSendState.ViewState.Content,
    isAddMode: Boolean,
    addSendHandlers: AddSendHandlers,
    permissionsManager: PermissionsManager,
    modifier: Modifier = Modifier,
) {
    val chooseFileCameraPermissionLauncher = permissionsManager.getLauncher { isGranted ->
        addSendHandlers.onChooseFileClick(isGranted)
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
    ) {
        BitwardenTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            label = stringResource(id = R.string.name),
            hint = stringResource(id = R.string.name_info),
            value = state.common.name,
            onValueChange = addSendHandlers.onNamChange,
        )

        if (isAddMode) {
            Spacer(modifier = Modifier.height(16.dp))
            BitwardenListHeaderText(
                label = stringResource(id = R.string.type),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
                        isChecked = state.isFileType,
                    ),
                    SegmentedButtonState(
                        text = stringResource(id = R.string.text),
                        onClick = addSendHandlers.onTextTypeSelect,
                        isChecked = state.isTextType,
                    ),
                ),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        when (val type = state.selectedType) {
            is AddSendState.ViewState.Content.SendType.File -> {
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.file),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (isAddMode) {
                    Text(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        text = type.name ?: stringResource(id = R.string.no_file_chosen),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BitwardenFilledTonalButton(
                        label = stringResource(id = R.string.choose_file),
                        onClick = {
                            if (permissionsManager.checkPermission(Manifest.permission.CAMERA)) {
                                addSendHandlers.onChooseFileClick(true)
                            } else {
                                chooseFileCameraPermissionLauncher.launch(
                                    Manifest.permission.CAMERA,
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.max_file_size),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.type_file_info),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        Text(
                            text = type.name.orEmpty(),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = type.displaySize.orEmpty(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            is AddSendState.ViewState.Content.SendType.Text -> {
                BitwardenTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    label = stringResource(id = R.string.text),
                    hint = stringResource(id = R.string.type_text_info),
                    value = type.input,
                    singleLine = false,
                    onValueChange = addSendHandlers.onTextChange,
                )
                Spacer(modifier = Modifier.height(16.dp))
                BitwardenWideSwitch(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    label = stringResource(id = R.string.hide_text_by_default),
                    isChecked = type.isHideByDefaultChecked,
                    onCheckedChange = addSendHandlers.onIsHideByDefaultToggle,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        AddSendOptions(
            state = state,
            isAddMode = isAddMode,
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
 * @param isAddMode When `true`, indicates that we are creating a new send and `false` when editing
 * an existing send.
 * @param addSendHandlers THe handlers various events.
 */
@Suppress("LongMethod")
@Composable
private fun AddSendOptions(
    state: AddSendState.ViewState.Content,
    isAddMode: Boolean,
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
            if (isAddMode) {
                SendDeletionDateChooser(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    dateFormatPattern = state.common.dateFormatPattern,
                    timeFormatPattern = state.common.timeFormatPattern,
                    currentZonedDateTime = state.common.deletionDate,
                    onDateSelect = addSendHandlers.onDeletionDateChange,
                )
                Spacer(modifier = Modifier.height(8.dp))
                SendExpirationDateChooser(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    dateFormatPattern = state.common.dateFormatPattern,
                    timeFormatPattern = state.common.timeFormatPattern,
                    currentZonedDateTime = state.common.expirationDate,
                    onDateSelect = addSendHandlers.onExpirationDateChange,
                )
            } else {
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.deletion_date),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                AddSendCustomDateChooser(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    dateFormatPattern = state.common.dateFormatPattern,
                    timeFormatPattern = state.common.timeFormatPattern,
                    currentZonedDateTime = state.common.deletionDate,
                    onDateSelect = { addSendHandlers.onDeletionDateChange(requireNotNull(it)) },
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.deletion_date_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(16.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.expiration_date),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                AddSendCustomDateChooser(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    dateFormatPattern = state.common.dateFormatPattern,
                    timeFormatPattern = state.common.timeFormatPattern,
                    currentZonedDateTime = state.common.expirationDate,
                    onDateSelect = addSendHandlers.onExpirationDateChange,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.expiration_date_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    BitwardenTextButton(
                        label = stringResource(id = R.string.clear),
                        onClick = addSendHandlers.onClearExpirationDateClick,
                        isEnabled = state.common.expirationDate != null,
                        modifier = Modifier.wrapContentWidth(),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenStepper(
                label = stringResource(id = R.string.maximum_access_count),
                value = state.common.maxAccessCount,
                onValueChange = addSendHandlers.onMaxAccessCountChange,
                isDecrementEnabled = state.common.maxAccessCount != null,
                range = 0..Int.MAX_VALUE,
                textFieldReadOnly = false,
                modifier = Modifier
                    .fillMaxWidth()
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
            if (!isAddMode) {
                state.common.currentAccessCount?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.current_access_count) + ":",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = it.toString(),
                            style = LocalNonMaterialTypography.current.bodySmallProminent,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            BitwardenPasswordField(
                label = stringResource(id = R.string.new_password),
                hint = stringResource(id = R.string.password_info),
                value = state.common.passwordInput,
                onValueChange = addSendHandlers.onPasswordChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenTextField(
                label = stringResource(id = R.string.notes),
                hint = stringResource(id = R.string.notes_info),
                value = state.common.noteInput,
                singleLine = false,
                onValueChange = addSendHandlers.onNoteChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            BitwardenWideSwitch(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                label = stringResource(id = R.string.hide_email),
                isChecked = state.common.isHideEmailChecked,
                onCheckedChange = addSendHandlers.onHideEmailToggle,
            )
            Spacer(modifier = Modifier.height(16.dp))
            BitwardenWideSwitch(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                label = stringResource(id = R.string.disable_send),
                isChecked = state.common.isDeactivateChecked,
                onCheckedChange = addSendHandlers.onDeactivateSendToggle,
            )
        }
    }
}
