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
    state: VaultAddItemState.ViewState.Content,
    isAddItemMode: Boolean,
    onTypeOptionClicked: (VaultAddItemState.ItemTypeOption) -> Unit,
    commonTypeHandlers: VaultAddItemCommonTypeHandlers,
    loginItemTypeHandlers: VaultAddLoginItemTypeHandlers,
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
                    itemType = state.type,
                    onTypeOptionClicked = onTypeOptionClicked,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        when (state.type) {
            is VaultAddItemState.ViewState.Content.ItemType.Login -> {
                addEditLoginItems(
                    commonState = state.common,
                    loginState = state.type,
                    isAddItemMode = isAddItemMode,
                    commonActionHandler = commonTypeHandlers,
                    loginItemTypeHandlers = loginItemTypeHandlers,
                )
            }

            is VaultAddItemState.ViewState.Content.ItemType.Card -> {
                // TODO(BIT-507): Create UI for card-type item creation
            }

            is VaultAddItemState.ViewState.Content.ItemType.Identity -> {
                // TODO(BIT-667): Create UI for identity-type item creation
            }

            is VaultAddItemState.ViewState.Content.ItemType.SecureNotes -> {
                addEditSecureNotesItems(
                    commonState = state.common,
                    isAddItemMode = isAddItemMode,
                    commonTypeHandlers = commonTypeHandlers,
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
    itemType: VaultAddItemState.ViewState.Content.ItemType,
    onTypeOptionClicked: (VaultAddItemState.ItemTypeOption) -> Unit,
    modifier: Modifier,
) {
    val possibleMainStates = VaultAddItemState.ItemTypeOption.entries.toList()
    val optionsWithStrings = possibleMainStates.associateWith { stringResource(id = it.labelRes) }

    BitwardenMultiSelectButton(
        label = stringResource(id = R.string.type),
        options = optionsWithStrings.values.toImmutableList(),
        selectedOption = stringResource(id = itemType.displayStringResId),
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
