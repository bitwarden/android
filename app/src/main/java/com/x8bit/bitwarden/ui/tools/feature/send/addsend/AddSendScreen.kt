package com.x8bit.bitwarden.ui.tools.feature.send.addsend

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BitwardenFilledTonalButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenSegmentedButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenStepper
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.BitwardenWideSwitch
import com.x8bit.bitwarden.ui.platform.components.SegmentedButtonState
import com.x8bit.bitwarden.ui.tools.feature.send.SendDeletionDateChooser
import com.x8bit.bitwarden.ui.tools.feature.send.SendExpirationDateChooser

/**
 * Displays new send UX.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSendScreen(
    viewModel: NewSendViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is AddSendEvent.NavigateBack -> onNavigateBack()
            is AddSendEvent.ShowToast -> {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.add_send),
                navigationIcon = painterResource(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(AddSendAction.CloseClick) }
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = R.string.save),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(AddSendAction.SaveClick) }
                        },
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues = innerPadding),
        ) {
            BitwardenTextField(
                modifier = Modifier.padding(horizontal = 16.dp),
                label = stringResource(id = R.string.name),
                hint = stringResource(id = R.string.name_info),
                value = state.name,
                onValueChange = remember(viewModel) {
                    { viewModel.trySendAction(AddSendAction.NameChange(it)) }
                },
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
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(AddSendAction.FileTypeClick) }
                        },
                        isChecked = state.selectedType is AddSendState.SendType.File,
                    ),
                    SegmentedButtonState(
                        text = stringResource(id = R.string.text),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(AddSendAction.TextTypeClick) }
                        },
                        isChecked = state.selectedType is AddSendState.SendType.Text,
                    ),
                ),
            )
            Spacer(modifier = Modifier.height(16.dp))
            when (val type = state.selectedType) {
                is AddSendState.SendType.File -> {
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
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(AddSendAction.ChooseFileClick) }
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

                is AddSendState.SendType.Text -> {
                    BitwardenTextField(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        label = stringResource(id = R.string.text),
                        hint = stringResource(id = R.string.type_text_info),
                        value = type.input,
                        onValueChange = remember(viewModel) {
                            { viewModel.trySendAction(AddSendAction.TextChange(it)) }
                        },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BitwardenWideSwitch(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        label = stringResource(id = R.string.hide_text_by_default),
                        isChecked = type.isHideByDefaultChecked,
                        onCheckedChange = remember(viewModel) {
                            { viewModel.trySendAction(AddSendAction.HideByDefaultToggle(it)) }
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            NewSendOptions(
                state = state,
                onMaxAccessCountChange = remember(viewModel) {
                    { viewModel.trySendAction(AddSendAction.MaxAccessCountChange(it)) }
                },
                onPasswordChange = remember(viewModel) {
                    { viewModel.trySendAction(AddSendAction.PasswordChange(it)) }
                },
                onNoteChange = remember(viewModel) {
                    { viewModel.trySendAction(AddSendAction.NoteChange(it)) }
                },
                onHideEmailChecked = remember(viewModel) {
                    { viewModel.trySendAction(AddSendAction.HideMyEmailToggle(it)) }
                },
                onDeactivateSendChecked = remember(viewModel) {
                    { viewModel.trySendAction(AddSendAction.DeactivateThisSendToggle(it)) }
                },
            )
            Spacer(modifier = Modifier.height(24.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

/**
 * Displays a collapsable set of new send options.
 *
 * @param state state.
 * @param onMaxAccessCountChange called when max access count changes.
 * @param onPasswordChange called when the password changes.
 * @param onNoteChange called when the notes changes.
 * @param onHideEmailChecked called when hide email is checked.
 * @param onDeactivateSendChecked called when deactivate send is checked.
 */
@Suppress("LongMethod")
@Composable
private fun NewSendOptions(
    state: AddSendState,
    onMaxAccessCountChange: (Int) -> Unit,
    onPasswordChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onHideEmailChecked: (Boolean) -> Unit,
    onDeactivateSendChecked: (Boolean) -> Unit,
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
            )
            Spacer(modifier = Modifier.height(8.dp))
            SendExpirationDateChooser(
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenStepper(
                label = stringResource(id = R.string.maximum_access_count),
                value = state.maxAccessCount,
                onValueChange = onMaxAccessCountChange,
                isDecrementEnabled = state.maxAccessCount != null,
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
                value = state.passwordInput,
                onValueChange = onPasswordChange,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenTextField(
                label = stringResource(id = R.string.notes),
                hint = stringResource(id = R.string.notes_info),
                value = state.noteInput,
                onValueChange = onNoteChange,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            BitwardenWideSwitch(
                modifier = Modifier.padding(horizontal = 16.dp),
                label = stringResource(id = R.string.hide_email),
                isChecked = state.isHideEmailChecked,
                onCheckedChange = onHideEmailChecked,
            )
            Spacer(modifier = Modifier.height(16.dp))
            BitwardenWideSwitch(
                modifier = Modifier.padding(horizontal = 16.dp),
                label = stringResource(id = R.string.disable_send),
                isChecked = state.isDeactivateChecked,
                onCheckedChange = onDeactivateSendChecked,
            )
        }
    }
}
