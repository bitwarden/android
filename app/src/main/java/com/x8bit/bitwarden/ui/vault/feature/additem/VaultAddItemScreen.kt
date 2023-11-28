package com.x8bit.bitwarden.ui.vault.feature.additem

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.EventsEffect
import com.x8bit.bitwarden.ui.platform.components.BitwardenFilledTonalButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenFilledTonalButtonWithIcon
import com.x8bit.bitwarden.ui.platform.components.BitwardenIconButtonWithResource
import com.x8bit.bitwarden.ui.platform.components.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenPasswordFieldWithActions
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.components.BitwardenSwitchWithActions
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextFieldWithActions
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.model.IconResource

/**
 * Top level composable for the vault add item screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun VaultAddItemScreen(
    onNavigateBack: () -> Unit,
    viewModel: VaultAddItemViewModel = hiltViewModel(),
) {

    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is VaultAddItemEvent.ShowToast -> {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }

            VaultAddItemEvent.NavigateBack -> onNavigateBack.invoke()
        }
    }

    val loginItemTypeHandlers = remember(viewModel) {
        VaultAddLoginItemTypeHandlers.create(viewModel = viewModel)
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = R.string.add_item),
                navigationIcon = painterResource(id = R.drawable.ic_close),
                navigationIconContentDescription = stringResource(id = R.string.close),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(VaultAddItemAction.CloseClick) }
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    BitwardenTextButton(
                        label = stringResource(id = R.string.save),
                        onClick = remember(viewModel) {
                            { viewModel.trySendAction(VaultAddItemAction.SaveClick) }
                        },
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            BitwardenListHeaderText(
                label = stringResource(id = R.string.item_information),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            TypeOptionsItem(
                selectedType = state.selectedType,
                onTypeOptionClicked = remember(viewModel) {
                    { typeOption: VaultAddItemState.ItemTypeOption ->
                        viewModel.trySendAction(VaultAddItemAction.TypeOptionSelect(typeOption))
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            when (val selectedType = state.selectedType) {
                is VaultAddItemState.ItemType.Login -> {
                    AddLoginTypeItemContent(
                        state = selectedType,
                        loginItemTypeHandlers = loginItemTypeHandlers,
                    )
                }

                VaultAddItemState.ItemType.Card -> {
                    // TODO(BIT-507): Create UI for card-type item creation
                }

                VaultAddItemState.ItemType.Identity -> {
                    // TODO(BIT-667): Create UI for identity-type item creation
                }

                VaultAddItemState.ItemType.SecureNotes -> {
                    // TODO(BIT-666): Create UI for secure notes type item creation
                }
            }
        }
    }
}

@Composable
private fun TypeOptionsItem(
    selectedType: VaultAddItemState.ItemType,
    onTypeOptionClicked: (VaultAddItemState.ItemTypeOption) -> Unit,
    modifier: Modifier,
) {
    val possibleMainStates = VaultAddItemState.ItemTypeOption.values().toList()

    val optionsWithStrings =
        possibleMainStates.associateBy({ it }, { stringResource(id = it.labelRes) })

    BitwardenMultiSelectButton(
        label = stringResource(id = R.string.type),
        options = optionsWithStrings.values.toList(),
        selectedOption = stringResource(id = selectedType.displayStringResId),
        onOptionSelected = { selectedOption ->
            val selectedOptionId =
                optionsWithStrings.entries.first { it.value == selectedOption }.key
            onTypeOptionClicked(selectedOptionId)
        },
        modifier = modifier,
    )
}

@Suppress("LongMethod")
@Composable
private fun AddLoginTypeItemContent(
    state: VaultAddItemState.ItemType.Login,
    loginItemTypeHandlers: VaultAddLoginItemTypeHandlers,
) {
    Spacer(modifier = Modifier.height(8.dp))
    BitwardenTextField(
        label = stringResource(id = R.string.name),
        value = state.name,
        onValueChange = loginItemTypeHandlers.onNameTextChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )

    Spacer(modifier = Modifier.height(8.dp))
    BitwardenTextFieldWithActions(
        label = stringResource(id = R.string.username),
        value = state.username,
        onValueChange = loginItemTypeHandlers.onUsernameTextChange,
        actions = {
            BitwardenIconButtonWithResource(
                iconRes = IconResource(
                    iconPainter = painterResource(id = R.drawable.ic_generator),
                    contentDescription = stringResource(id = R.string.generate_username),
                ),
                onClick = loginItemTypeHandlers.onOpenUsernameGeneratorClick,
            )
        },
        modifier = Modifier.padding(horizontal = 16.dp),
    )

    Spacer(modifier = Modifier.height(8.dp))
    BitwardenPasswordFieldWithActions(
        label = stringResource(id = R.string.password),
        value = state.password,
        onValueChange = loginItemTypeHandlers.onPasswordTextChange,
        modifier = Modifier
            .padding(horizontal = 16.dp),
    ) {
        BitwardenIconButtonWithResource(
            iconRes = IconResource(
                iconPainter = painterResource(id = R.drawable.ic_check_mark),
                contentDescription = stringResource(id = R.string.check_password),
            ),
            onClick = loginItemTypeHandlers.onPasswordCheckerClick,
        )
        BitwardenIconButtonWithResource(
            iconRes = IconResource(
                iconPainter = painterResource(id = R.drawable.ic_generator),
                contentDescription = stringResource(id = R.string.generate_password),
            ),
            onClick = loginItemTypeHandlers.onOpenPasswordGeneratorClick,
        )
    }

    Spacer(modifier = Modifier.height(24.dp))
    BitwardenListHeaderText(
        label = stringResource(id = R.string.authenticator_key),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )

    Spacer(modifier = Modifier.height(16.dp))
    BitwardenFilledTonalButtonWithIcon(
        label = stringResource(id = R.string.setup_totp),
        icon = painterResource(id = R.drawable.ic_light_bulb),
        onClick = loginItemTypeHandlers.onSetupTotpClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )

    Spacer(modifier = Modifier.height(24.dp))
    BitwardenListHeaderText(
        label = stringResource(id = R.string.ur_is),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )

    Spacer(modifier = Modifier.height(8.dp))
    BitwardenTextFieldWithActions(
        label = stringResource(id = R.string.uri),
        value = state.uri,
        onValueChange = loginItemTypeHandlers.onUriTextChange,
        actions = {
            BitwardenIconButtonWithResource(
                iconRes = IconResource(
                    iconPainter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = stringResource(id = R.string.options),
                ),
                onClick = loginItemTypeHandlers.onUriSettingsClick,
            )
        },
        modifier = Modifier.padding(horizontal = 16.dp),
    )

    Spacer(modifier = Modifier.height(16.dp))
    BitwardenFilledTonalButton(
        label = stringResource(id = R.string.new_uri),
        onClick = loginItemTypeHandlers.onAddNewUriClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )

    Spacer(modifier = Modifier.height(24.dp))
    BitwardenListHeaderText(
        label = stringResource(id = R.string.miscellaneous),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )

    Spacer(modifier = Modifier.height(8.dp))
    BitwardenMultiSelectButton(
        label = stringResource(id = R.string.folder),
        options = state.availableFolders,
        selectedOption = state.folder,
        onOptionSelected = loginItemTypeHandlers.onFolderTextChange,
        modifier = Modifier.padding(horizontal = 16.dp),
    )

    Spacer(modifier = Modifier.height(16.dp))
    BitwardenSwitch(
        label = stringResource(
            id = R.string.favorite,
        ),
        isChecked = state.favorite,
        onCheckedChange = loginItemTypeHandlers.onToggleFavorite,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )

    Spacer(modifier = Modifier.height(16.dp))
    BitwardenSwitchWithActions(
        label = stringResource(id = R.string.password_prompt),
        isChecked = state.masterPasswordReprompt,
        onCheckedChange = loginItemTypeHandlers.onToggleMasterPasswordReprompt,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        actions = {
            IconButton(onClick = loginItemTypeHandlers.onTooltipClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_tooltip),
                    tint = MaterialTheme.colorScheme.onSurface,
                    contentDescription = stringResource(
                        id = R.string.master_password_re_prompt_help,
                    ),
                )
            }
        },
    )

    Spacer(modifier = Modifier.height(24.dp))
    BitwardenListHeaderText(
        label = stringResource(id = R.string.notes),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )

    Spacer(modifier = Modifier.height(8.dp))
    BitwardenTextField(
        label = stringResource(id = R.string.notes),
        value = state.notes,
        onValueChange = loginItemTypeHandlers.onNotesTextChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )

    Spacer(modifier = Modifier.height(24.dp))
    BitwardenListHeaderText(
        label = stringResource(id = R.string.custom_fields),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )

    Spacer(modifier = Modifier.height(16.dp))
    BitwardenFilledTonalButton(
        label = stringResource(id = R.string.new_custom_field),
        onClick = loginItemTypeHandlers.onAddNewCustomFieldClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )

    Spacer(modifier = Modifier.height(24.dp))
    BitwardenListHeaderText(
        label = stringResource(id = R.string.ownership),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )

    Spacer(modifier = Modifier.height(8.dp))
    BitwardenMultiSelectButton(
        label = stringResource(id = R.string.who_owns_this_item),
        options = state.availableOwners,
        selectedOption = state.ownership,
        onOptionSelected = loginItemTypeHandlers.onOwnershipTextChange,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
    Spacer(modifier = Modifier.height(24.dp))
    Spacer(modifier = Modifier.navigationBarsPadding())
}
