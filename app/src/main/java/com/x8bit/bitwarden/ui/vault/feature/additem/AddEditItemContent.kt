package com.x8bit.bitwarden.ui.vault.feature.additem

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.BitwardenMultiSelectButton
import kotlinx.collections.immutable.toImmutableList

/**
 * The top level content UI state for the [VaultAddItemScreen].
 */
@Composable
fun AddEditItemContent(
    viewState: VaultAddItemState.ViewState.Content,
    isAddItemMode: Boolean,
    onTypeOptionClicked: (VaultAddItemState.ItemTypeOption) -> Unit,
    loginItemTypeHandlers: VaultAddLoginItemTypeHandlers,
    secureNotesTypeHandlers: VaultAddSecureNotesItemTypeHandlers,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        item {
            BitwardenListHeaderText(
                label = stringResource(id = R.string.item_information),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
        if (isAddItemMode) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                TypeOptionsItem(
                    content = viewState,
                    onTypeOptionClicked = onTypeOptionClicked,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        when (viewState) {
            is VaultAddItemState.ViewState.Content.Login -> {
                addEditLoginItems(
                    state = viewState,
                    isAddItemMode = isAddItemMode,
                    loginItemTypeHandlers = loginItemTypeHandlers,
                )
            }

            is VaultAddItemState.ViewState.Content.Card -> {
                // TODO(BIT-507): Create UI for card-type item creation
            }

            is VaultAddItemState.ViewState.Content.Identity -> {
                // TODO(BIT-667): Create UI for identity-type item creation
            }

            is VaultAddItemState.ViewState.Content.SecureNotes -> {
                addEditSecureNotesItems(
                    state = viewState,
                    isAddItemMode = isAddItemMode,
                    secureNotesTypeHandlers = secureNotesTypeHandlers,
                )
            }
        }

        item {
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun TypeOptionsItem(
    content: VaultAddItemState.ViewState.Content,
    onTypeOptionClicked: (VaultAddItemState.ItemTypeOption) -> Unit,
    modifier: Modifier,
) {
    val possibleMainStates = VaultAddItemState.ItemTypeOption.entries.toList()
        // TODO: Add support for Card Type items (BIT-668)
        .filterNot { it == VaultAddItemState.ItemTypeOption.CARD }
        // TODO: Add support for Identity Type items (BIT-667)
        .filterNot { it == VaultAddItemState.ItemTypeOption.IDENTITY }
    val optionsWithStrings = possibleMainStates.associateWith { stringResource(id = it.labelRes) }

    BitwardenMultiSelectButton(
        label = stringResource(id = R.string.type),
        options = optionsWithStrings.values.toImmutableList(),
        selectedOption = stringResource(id = content.displayStringResId),
        onOptionSelected = { selectedOption ->
            val selectedOptionId = optionsWithStrings
                .entries
                .first { it.value == selectedOption }
                .key
            onTypeOptionClicked(selectedOptionId)
        },
        modifier = modifier,
    )
}
