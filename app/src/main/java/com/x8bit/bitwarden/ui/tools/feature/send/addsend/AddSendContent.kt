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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.cardBackground
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.x8bit.bitwarden.ui.platform.components.card.BitwardenInfoCalloutCard
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.stepper.BitwardenStepper
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManager
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.handlers.AddSendHandlers

/**
 * Content view for the [AddSendScreen].
 */
@Suppress("LongMethod")
@Composable
fun AddSendContent(
    state: AddSendState.ViewState.Content,
    policyDisablesSend: Boolean,
    policySendOptionsInEffect: Boolean,
    isAddMode: Boolean,
    isShared: Boolean,
    addSendHandlers: AddSendHandlers,
    permissionsManager: PermissionsManager,
    modifier: Modifier = Modifier,
) {
    val chooseFileCameraPermissionLauncher = permissionsManager.getLauncher { isGranted ->
        addSendHandlers.onChooseFileClick(isGranted)
    }
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(height = 12.dp))
        if (policyDisablesSend) {
            BitwardenInfoCalloutCard(
                text = stringResource(id = R.string.send_disabled_warning),
                modifier = Modifier
                    .standardHorizontalMargin()
                    .fillMaxWidth()
                    .testTag("SendPolicyInEffectLabel"),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (policySendOptionsInEffect) {
            BitwardenInfoCalloutCard(
                text = stringResource(id = R.string.send_options_policy_in_effect),
                modifier = Modifier
                    .testTag(tag = "SendPolicyInEffectLabel")
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (state.selectedType is AddSendState.ViewState.Content.SendType.Text) {
            BitwardenTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
                label = stringResource(id = R.string.text_to_share),
                readOnly = policyDisablesSend,
                value = state.selectedType.input,
                singleLine = false,
                onValueChange = addSendHandlers.onTextChange,
                textFieldTestTag = "SendTextContentEntry",
                cardStyle = CardStyle.Full,
            )
            Spacer(modifier = Modifier.height(height = 16.dp))
        }

        BitwardenListHeaderText(
            label = stringResource(id = R.string.send_details),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(height = 8.dp))
        BitwardenTextField(
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
            label = stringResource(id = R.string.send_name_required),
            readOnly = policyDisablesSend,
            value = state.common.name,
            onValueChange = addSendHandlers.onNamChange,
            textFieldTestTag = "SendNameEntry",
            cardStyle = CardStyle.Full,
        )

        when (val type = state.selectedType) {
            is AddSendState.ViewState.Content.SendType.File -> {
                Spacer(modifier = Modifier.height(height = 16.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.file),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(height = 8.dp))

                if (isShared) {
                    Text(
                        text = type.name.orEmpty(),
                        color = BitwardenTheme.colorScheme.text.primary,
                        style = BitwardenTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .standardHorizontalMargin()
                            .padding(horizontal = 16.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.max_file_size),
                        color = BitwardenTheme.colorScheme.text.secondary,
                        style = BitwardenTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .standardHorizontalMargin()
                            .padding(horizontal = 16.dp),
                    )
                } else if (isAddMode) {
                    type.name?.let {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .standardHorizontalMargin()
                                .defaultMinSize(minHeight = 60.dp)
                                .cardBackground(cardStyle = CardStyle.Full)
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 12.dp,
                                )
                                .testTag(tag = "SendCurrentFileNameLabel"),
                            text = it,
                            color = BitwardenTheme.colorScheme.text.primary,
                            style = BitwardenTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    BitwardenOutlinedButton(
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
                            .testTag(tag = "SendChooseFileButton")
                            .fillMaxWidth()
                            .standardHorizontalMargin(),
                    )
                    Spacer(modifier = Modifier.height(height = 8.dp))
                    Text(
                        text = stringResource(id = R.string.max_file_size),
                        color = BitwardenTheme.colorScheme.text.secondary,
                        style = BitwardenTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .standardHorizontalMargin()
                            .padding(horizontal = 16.dp),
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .standardHorizontalMargin()
                            .defaultMinSize(minHeight = 60.dp)
                            .cardBackground(cardStyle = CardStyle.Full)
                            .padding(
                                horizontal = 16.dp,
                                vertical = 12.dp,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = type.name.orEmpty(),
                            color = BitwardenTheme.colorScheme.text.primary,
                            style = BitwardenTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = type.displaySize.orEmpty(),
                            color = BitwardenTheme.colorScheme.text.secondary,
                            style = BitwardenTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            is AddSendState.ViewState.Content.SendType.Text -> {
                Spacer(modifier = Modifier.height(height = 8.dp))
                BitwardenSwitch(
                    modifier = Modifier
                        .testTag(tag = "SendHideTextByDefaultToggle")
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                    label = stringResource(id = R.string.hide_text_by_default),
                    isChecked = type.isHideByDefaultChecked,
                    onCheckedChange = addSendHandlers.onIsHideByDefaultToggle,
                    readOnly = policyDisablesSend,
                    cardStyle = CardStyle.Full,
                )
            }
        }

        Spacer(modifier = Modifier.height(height = 8.dp))
        SendDeletionDateChooser(
            modifier = Modifier
                .testTag("SendDeletionOptionsPicker")
                .fillMaxWidth()
                .standardHorizontalMargin(),
            dateFormatPattern = state.common.dateFormatPattern,
            timeFormatPattern = state.common.timeFormatPattern,
            currentZonedDateTime = state.common.deletionDate,
            onDateSelect = addSendHandlers.onDeletionDateChange,
            isEnabled = !policyDisablesSend,
        )
        Spacer(modifier = Modifier.height(8.dp))
        AddSendOptions(
            state = state,
            sendRestrictionPolicy = policyDisablesSend,
            isAddMode = isAddMode,
            addSendHandlers = addSendHandlers,
        )
        Spacer(modifier = Modifier.height(height = 12.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

/**
 * Displays a collapsable set of new send options.
 *
 * @param state The content state.
 * @param sendRestrictionPolicy When `true`, indicates that there's a policy preventing the user
 * from editing or creating sends.
 * @param isAddMode When `true`, indicates that we are creating a new send and `false` when editing
 * an existing send.
 * @param addSendHandlers THe handlers various events.
 */
@Suppress("LongMethod")
@Composable
private fun AddSendOptions(
    state: AddSendState.ViewState.Content,
    sendRestrictionPolicy: Boolean,
    isAddMode: Boolean,
    addSendHandlers: AddSendHandlers,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .testTag("SendShowHideOptionsButton")
            .fillMaxWidth()
            .clickable(
                onClickLabel = if (isExpanded) {
                    stringResource(id = R.string.options_expanded)
                } else {
                    stringResource(id = R.string.options_collapsed)
                },
                onClick = { isExpanded = !isExpanded },
            )
            .minimumInteractiveComponentSize()
            .padding(top = 16.dp, bottom = 8.dp)
            .standardHorizontalMargin()
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = R.string.additional_options),
            color = BitwardenTheme.colorScheme.text.interaction,
            style = BitwardenTheme.typography.labelLarge,
            modifier = Modifier.padding(end = 8.dp),
        )
        Icon(
            painter = rememberVectorPainter(
                if (isExpanded) {
                    R.drawable.ic_chevron_up_small
                } else {
                    R.drawable.ic_chevron_down_small
                },
            ),
            contentDescription = null,
            tint = BitwardenTheme.colorScheme.icon.secondary,
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
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenStepper(
                label = stringResource(id = R.string.maximum_access_count),
                supportingTextContent = {
                    Text(
                        text = stringResource(id = R.string.maximum_access_count_info),
                        style = BitwardenTheme.typography.bodySmall,
                        color = BitwardenTheme.colorScheme.text.secondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                value = state.common.maxAccessCount,
                onValueChange = addSendHandlers.onMaxAccessCountChange,
                isDecrementEnabled = state.common.maxAccessCount != null && !sendRestrictionPolicy,
                isIncrementEnabled = !sendRestrictionPolicy,
                range = 0..Int.MAX_VALUE,
                textFieldReadOnly = sendRestrictionPolicy,
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .testTag("SendMaxAccessCountEntry")
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenPasswordField(
                label = stringResource(id = R.string.new_password),
                supportingText = stringResource(id = R.string.password_info),
                readOnly = sendRestrictionPolicy,
                value = state.common.passwordInput,
                onValueChange = addSendHandlers.onPasswordChange,
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .testTag("SendNewPasswordEntry")
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
            BitwardenSwitch(
                modifier = Modifier
                    .testTag("SendHideEmailSwitch")
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
                label = stringResource(id = R.string.hide_email),
                isChecked = state.common.isHideEmailChecked,
                onCheckedChange = addSendHandlers.onHideEmailToggle,
                readOnly = sendRestrictionPolicy,
                enabled = state.common.isHideEmailChecked || state.common.isHideEmailAddressEnabled,
                cardStyle = CardStyle.Full,
            )
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenTextField(
                label = stringResource(id = R.string.private_notes),
                readOnly = sendRestrictionPolicy,
                value = state.common.noteInput,
                singleLine = false,
                onValueChange = addSendHandlers.onNoteChange,
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
        }
    }
}
