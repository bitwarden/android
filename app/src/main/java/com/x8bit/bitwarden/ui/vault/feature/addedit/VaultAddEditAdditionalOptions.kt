package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenExpandingHeader
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.util.persistentListOfNotNull
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditCommonHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.CustomFieldType

/**
 * The collapsable UI for additional options when adding or editing a cipher.
 */
@Suppress("LongMethod")
fun LazyListScope.vaultAddEditAdditionalOptions(
    itemType: VaultAddEditState.ViewState.Content.ItemType,
    commonState: VaultAddEditState.ViewState.Content.Common,
    commonTypeHandlers: VaultAddEditCommonHandlers,
    isAdditionalOptionsExpanded: Boolean,
    onAdditionalOptionsClick: () -> Unit,
) {
    item {
        BitwardenExpandingHeader(
            isExpanded = isAdditionalOptionsExpanded,
            onClick = onAdditionalOptionsClick,
            modifier = Modifier
                .standardHorizontalMargin()
                .fillMaxWidth(),
        )
    }

    if (isAdditionalOptionsExpanded) {
        val isNotes = itemType is VaultAddEditState.ViewState.Content.ItemType.SecureNotes
        if (!isNotes) {
            item(key = "optionalNotes") {
                BitwardenTextField(
                    singleLine = false,
                    label = stringResource(id = R.string.notes),
                    value = commonState.notes,
                    onValueChange = commonTypeHandlers.onNotesTextChange,
                    textFieldTestTag = "ItemNotesEntry",
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                )
            }
        }

        if (commonState.isUnlockWithPasswordEnabled) {
            item(key = "MasterPasswordRepromptToggle") {
                Column(
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                ) {
                    if (!isNotes) {
                        Spacer(modifier = Modifier.height(height = 8.dp))
                    }
                    BitwardenSwitch(
                        label = stringResource(id = R.string.password_prompt),
                        isChecked = commonState.masterPasswordReprompt,
                        onCheckedChange = commonTypeHandlers.onToggleMasterPasswordReprompt,
                        actions = {
                            BitwardenStandardIconButton(
                                vectorIconRes = R.drawable.ic_question_circle_small,
                                contentDescription = stringResource(
                                    id = R.string.master_password_re_prompt_help,
                                ),
                                onClick = commonTypeHandlers.onTooltipClick,
                                contentColor = BitwardenTheme.colorScheme.icon.secondary,
                            )
                        },
                        cardStyle = CardStyle.Full,
                        modifier = Modifier
                            .testTag(tag = "MasterPasswordRepromptToggle")
                            .fillMaxWidth(),
                    )
                }
            }
        }

        item(key = "customFieldsHeader") {
            Column(
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            ) {
                Spacer(modifier = Modifier.height(height = 16.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.custom_fields),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        items(
            items = commonState.customFieldData,
            key = { "customField_${it.itemId}" },
        ) { customItem ->
            Column(
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            ) {
                Spacer(modifier = Modifier.height(height = 8.dp))
                VaultAddEditCustomField(
                    customField = customItem,
                    onCustomFieldValueChange = commonTypeHandlers.onCustomFieldValueChange,
                    onCustomFieldAction = commonTypeHandlers.onCustomFieldActionSelect,
                    onHiddenVisibilityChanged = commonTypeHandlers.onHiddenFieldVisibilityChange,
                    supportedLinkedTypes = itemType.vaultLinkedFieldTypes,
                    cardStyle = CardStyle.Full,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.height(height = 8.dp))
        }

        item(key = "addCustomFieldButton") {
            Column(
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            ) {
                Spacer(modifier = Modifier.height(height = 8.dp))
                VaultAddEditCustomFieldsButton(
                    onFinishNamingClick = commonTypeHandlers.onAddNewCustomFieldClick,
                    options = persistentListOfNotNull(
                        CustomFieldType.TEXT,
                        CustomFieldType.HIDDEN,
                        CustomFieldType.BOOLEAN,
                        CustomFieldType.LINKED.takeIf {
                            itemType.vaultLinkedFieldTypes.isNotEmpty()
                        },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
