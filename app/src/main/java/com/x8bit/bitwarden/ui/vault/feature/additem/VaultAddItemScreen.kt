package com.x8bit.bitwarden.ui.vault.feature.additem

import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.BasicDialogState
import com.x8bit.bitwarden.ui.platform.components.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.LoadingDialogState
import kotlinx.collections.immutable.toImmutableList

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

    val secureNotesTypeHandlers = remember(viewModel) {
        VaultAddSecureNotesItemTypeHandlers.create(viewModel = viewModel)
    }

    when (val dialogState = state.dialog) {
        is VaultAddItemState.DialogState.Loading -> {
            BitwardenLoadingDialog(
                visibilityState = LoadingDialogState.Shown(dialogState.label),
            )
        }

        is VaultAddItemState.DialogState.Error -> BitwardenBasicDialog(
            visibilityState = BasicDialogState.Shown(
                title = R.string.an_error_has_occurred.asText(),
                message = dialogState.message,
            ),
            onDismissRequest = remember(viewModel) {
                { viewModel.trySendAction(VaultAddItemAction.DismissDialog) }
            },
        )

        null -> Unit
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = state.screenDisplayName(),
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
        LazyColumn(
            modifier = Modifier
                .imePadding()
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            item {
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.item_information),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
            if (state.shouldShowTypeSelector) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    TypeOptionsItem(
                        selectedType = state.selectedType,
                        onTypeOptionClicked = remember(viewModel) {
                            { typeOption: VaultAddItemState.ItemTypeOption ->
                                viewModel.trySendAction(
                                    VaultAddItemAction.TypeOptionSelect(typeOption),
                                )
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            when (val selectedType = state.selectedType) {
                is VaultAddItemState.ItemType.Login -> {
                    addEditLoginItems(
                        state = selectedType,
                        loginItemTypeHandlers = loginItemTypeHandlers,
                    )
                }

                is VaultAddItemState.ItemType.Card -> {
                    // TODO(BIT-507): Create UI for card-type item creation
                }

                is VaultAddItemState.ItemType.Identity -> {
                    // TODO(BIT-667): Create UI for identity-type item creation
                }

                is VaultAddItemState.ItemType.SecureNotes -> {
                    addEditSecureNotesItems(
                        state = selectedType,
                        secureNotesTypeHandlers = secureNotesTypeHandlers,
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.navigationBarsPadding())
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
        options = optionsWithStrings.values.toImmutableList(),
        selectedOption = stringResource(id = selectedType.displayStringResId),
        onOptionSelected = { selectedOption ->
            val selectedOptionId =
                optionsWithStrings.entries.first { it.value == selectedOption }.key
            onTypeOptionClicked(selectedOptionId)
        },
        modifier = modifier,
    )
}
