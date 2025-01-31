package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditCardTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditCommonHandlers
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultCardExpirationMonth
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
import com.x8bit.bitwarden.ui.vault.util.longName
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * The UI for adding and editing a card cipher.
 */
@Suppress("LongMethod")
fun LazyListScope.vaultAddEditCardItems(
    commonState: VaultAddEditState.ViewState.Content.Common,
    cardState: VaultAddEditState.ViewState.Content.ItemType.Card,
    commonHandlers: VaultAddEditCommonHandlers,
    cardHandlers: VaultAddEditCardTypeHandlers,
) {
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.cardholder_name),
            value = cardState.cardHolderName,
            onValueChange = cardHandlers.onCardHolderNameTextChange,
            textFieldTestTag = "CardholderNameEntry",
            cardStyle = CardStyle.Top(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
    item {
        var showNumber by rememberSaveable { mutableStateOf(value = false) }
        BitwardenPasswordField(
            label = stringResource(id = R.string.number),
            value = cardState.number,
            onValueChange = cardHandlers.onNumberTextChange,
            showPassword = showNumber,
            showPasswordChange = {
                showNumber = !showNumber
                cardHandlers.onNumberVisibilityChange(showNumber)
            },
            showPasswordTestTag = "ShowCardNumberButton",
            passwordFieldTestTag = "CardNumberEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
    item {
        val resources = LocalContext.current.resources
        BitwardenMultiSelectButton(
            label = stringResource(id = R.string.brand),
            options = VaultCardBrand
                .entries
                .map { it.longName() }
                .toImmutableList(),
            selectedOption = cardState.brand.longName(),
            onOptionSelected = { selectedString ->
                cardHandlers.onBrandSelected(
                    VaultCardBrand
                        .entries
                        .first { it.longName.toString(resources) == selectedString },
                )
            },
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .testTag("CardBrandPicker")
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
    item {
        val resources = LocalContext.current.resources
        BitwardenMultiSelectButton(
            label = stringResource(id = R.string.expiration_month),
            options = VaultCardExpirationMonth
                .entries
                .map { it.value() }
                .toImmutableList(),
            selectedOption = cardState.expirationMonth.value(),
            onOptionSelected = { selectedString ->
                cardHandlers.onExpirationMonthSelected(
                    VaultCardExpirationMonth
                        .entries
                        .first { it.value.toString(resources) == selectedString },
                )
            },
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .testTag("CardExpirationMonthPicker")
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
    item {
        BitwardenTextField(
            label = stringResource(id = R.string.expiration_year),
            value = cardState.expirationYear,
            onValueChange = cardHandlers.onExpirationYearTextChange,
            keyboardType = KeyboardType.Number,
            textFieldTestTag = "CardExpirationYearEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
    item {
        var showSecurityCode by rememberSaveable { mutableStateOf(value = false) }
        BitwardenPasswordField(
            label = stringResource(id = R.string.security_code),
            value = cardState.securityCode,
            onValueChange = cardHandlers.onSecurityCodeTextChange,
            showPassword = showSecurityCode,
            showPasswordChange = {
                showSecurityCode = !showSecurityCode
                cardHandlers.onSecurityCodeVisibilityChange(showSecurityCode)
            },
            keyboardType = KeyboardType.NumberPassword,
            showPasswordTestTag = "CardShowSecurityCodeButton",
            passwordFieldTestTag = "CardSecurityCodeEntry",
            cardStyle = CardStyle.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
    item {
        Spacer(modifier = Modifier.height(height = 16.dp))
        BitwardenListHeaderText(
            label = stringResource(id = R.string.miscellaneous),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(height = 8.dp))
    }
    item {
        BitwardenTextField(
            singleLine = false,
            label = stringResource(id = R.string.notes),
            value = commonState.notes,
            onValueChange = commonHandlers.onNotesTextChange,
            textFieldTestTag = "ItemNotesEntry",
            cardStyle = CardStyle.Full,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    if (commonState.isUnlockWithPasswordEnabled) {
        item {
            Spacer(modifier = Modifier.height(height = 8.dp))
            BitwardenSwitch(
                label = stringResource(id = R.string.password_prompt),
                isChecked = commonState.masterPasswordReprompt,
                onCheckedChange = commonHandlers.onToggleMasterPasswordReprompt,
                actions = {
                    BitwardenStandardIconButton(
                        vectorIconRes = R.drawable.ic_question_circle,
                        contentDescription = stringResource(
                            id = R.string.master_password_re_prompt_help,
                        ),
                        onClick = commonHandlers.onTooltipClick,
                        contentColor = BitwardenTheme.colorScheme.icon.secondary,
                    )
                },
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .testTag("MasterPasswordRepromptToggle")
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
        }
    }

    item {
        Spacer(modifier = Modifier.height(height = 16.dp))
        BitwardenListHeaderText(
            label = stringResource(id = R.string.custom_fields),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .padding(horizontal = 16.dp),
        )
    }

    items(commonState.customFieldData) { customItem ->
        Spacer(modifier = Modifier.height(height = 8.dp))
        VaultAddEditCustomField(
            customField = customItem,
            onCustomFieldValueChange = commonHandlers.onCustomFieldValueChange,
            onCustomFieldAction = commonHandlers.onCustomFieldActionSelect,
            supportedLinkedTypes = persistentListOf(
                VaultLinkedFieldType.CARDHOLDER_NAME,
                VaultLinkedFieldType.EXPIRATION_MONTH,
                VaultLinkedFieldType.EXPIRATION_YEAR,
                VaultLinkedFieldType.SECURITY_CODE,
                VaultLinkedFieldType.BRAND,
                VaultLinkedFieldType.NUMBER,
            ),
            onHiddenVisibilityChanged = commonHandlers.onHiddenFieldVisibilityChange,
            cardStyle = CardStyle.Full,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        Spacer(modifier = Modifier.height(16.dp))
        VaultAddEditCustomFieldsButton(
            onFinishNamingClick = commonHandlers.onAddNewCustomFieldClick,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
}
