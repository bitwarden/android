package com.x8bit.bitwarden.ui.vault.feature.addedit

import android.Manifest
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.card.BitwardenInfoCalloutCard
import com.x8bit.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.manager.permissions.PermissionsManager
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditCardTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditCommonHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditIdentityTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditLoginTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditSshKeyTypeHandlers
import kotlinx.collections.immutable.toImmutableList

/**
 * The top level content UI state for the [VaultAddEditScreen].
 */
@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
fun VaultAddEditContent(
    state: VaultAddEditState.ViewState.Content,
    isAddItemMode: Boolean,
    typeOptions: List<VaultAddEditState.ItemTypeOption>,
    onTypeOptionClicked: (VaultAddEditState.ItemTypeOption) -> Unit,
    commonTypeHandlers: VaultAddEditCommonHandlers,
    loginItemTypeHandlers: VaultAddEditLoginTypeHandlers,
    identityItemTypeHandlers: VaultAddEditIdentityTypeHandlers,
    cardItemTypeHandlers: VaultAddEditCardTypeHandlers,
    sshKeyItemTypeHandlers: VaultAddEditSshKeyTypeHandlers,
    modifier: Modifier = Modifier,
    permissionsManager: PermissionsManager,
) {
    val launcher = permissionsManager.getLauncher(
        onResult = { isGranted ->
            when (state.type) {
                is VaultAddEditState.ViewState.Content.ItemType.SecureNotes -> Unit
                is VaultAddEditState.ViewState.Content.ItemType.Card -> Unit
                is VaultAddEditState.ViewState.Content.ItemType.Identity -> Unit
                is VaultAddEditState.ViewState.Content.ItemType.SshKey -> Unit
                is VaultAddEditState.ViewState.Content.ItemType.Login -> {
                    loginItemTypeHandlers.onSetupTotpClick(isGranted)
                }
            }
        },
    )

    LazyColumn(modifier = modifier) {
        item {
            if (state.isIndividualVaultDisabled && isAddItemMode) {
                BitwardenInfoCalloutCard(
                    text = stringResource(R.string.personal_ownership_policy_in_effect),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag("PersonalOwnershipPolicyLabel")
                        .fillMaxWidth(),
                )
            }
        }

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
                    entries = typeOptions,
                    itemType = state.type,
                    onTypeOptionClicked = onTypeOptionClicked,
                    modifier = Modifier
                        .testTag("ItemTypePicker")
                        .padding(horizontal = 16.dp),
                )
            }
        }

        when (state.type) {
            is VaultAddEditState.ViewState.Content.ItemType.Login -> {
                vaultAddEditLoginItems(
                    commonState = state.common,
                    loginState = state.type,
                    isAddItemMode = isAddItemMode,
                    commonActionHandler = commonTypeHandlers,
                    loginItemTypeHandlers = loginItemTypeHandlers,
                    onTotpSetupClick = {
                        if (permissionsManager.checkPermission(Manifest.permission.CAMERA)) {
                            loginItemTypeHandlers.onSetupTotpClick(true)
                        } else {
                            launcher.launch(Manifest.permission.CAMERA)
                        }
                    },
                )
            }

            is VaultAddEditState.ViewState.Content.ItemType.Card -> {
                vaultAddEditCardItems(
                    commonState = state.common,
                    cardState = state.type,
                    commonHandlers = commonTypeHandlers,
                    cardHandlers = cardItemTypeHandlers,
                    isAddItemMode = isAddItemMode,
                )
            }

            is VaultAddEditState.ViewState.Content.ItemType.Identity -> {
                vaultAddEditIdentityItems(
                    commonState = state.common,
                    identityState = state.type,
                    isAddItemMode = isAddItemMode,
                    commonTypeHandlers = commonTypeHandlers,
                    identityItemTypeHandlers = identityItemTypeHandlers,
                )
            }

            is VaultAddEditState.ViewState.Content.ItemType.SecureNotes -> {
                vaultAddEditSecureNotesItems(
                    commonState = state.common,
                    isAddItemMode = isAddItemMode,
                    commonTypeHandlers = commonTypeHandlers,
                )
            }

            is VaultAddEditState.ViewState.Content.ItemType.SshKey -> {
                vaultAddEditSshKeyItems(
                    commonState = state.common,
                    sshKeyState = state.type,
                    commonTypeHandlers = commonTypeHandlers,
                    sshKeyTypeHandlers = sshKeyItemTypeHandlers,
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
    entries: List<VaultAddEditState.ItemTypeOption>,
    itemType: VaultAddEditState.ViewState.Content.ItemType,
    onTypeOptionClicked: (VaultAddEditState.ItemTypeOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val optionsWithStrings = entries.associateWith { stringResource(id = it.labelRes) }

    BitwardenMultiSelectButton(
        label = stringResource(id = R.string.type),
        options = optionsWithStrings.values.toImmutableList(),
        selectedOption = stringResource(id = itemType.itemTypeOption.labelRes),
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
