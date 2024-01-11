package com.x8bit.bitwarden.ui.vault.feature.item

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.BitwardenIconButtonWithResource
import com.x8bit.bitwarden.ui.platform.components.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.BitwardenPasswordFieldWithActions
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.model.IconResource
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultCardItemTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultCommonItemTypeHandlers
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand

/**
 * The top level content UI state for the [VaultItemScreen] when viewing a Card cipher.
 */
@Suppress("LongMethod")
@Composable
fun VaultItemCardContent(
    commonState: VaultItemState.ViewState.Content.Common,
    cardState: VaultItemState.ViewState.Content.ItemType.Card,
    vaultCommonItemTypeHandlers: VaultCommonItemTypeHandlers,
    vaultCardItemTypeHandlers: VaultCardItemTypeHandlers,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item {
            BitwardenListHeaderText(
                label = stringResource(id = R.string.item_information),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenTextField(
                label = stringResource(id = R.string.name),
                value = commonState.name,
                onValueChange = { },
                readOnly = true,
                singleLine = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
        cardState.cardholderName?.let { cardholderName ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenTextField(
                    label = stringResource(id = R.string.cardholder_name),
                    value = cardholderName,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
        cardState.number?.let { number ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenPasswordFieldWithActions(
                    label = stringResource(id = R.string.number),
                    value = number,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    actions = {
                        BitwardenIconButtonWithResource(
                            iconRes = IconResource(
                                iconPainter = painterResource(id = R.drawable.ic_copy),
                                contentDescription = stringResource(id = R.string.copy_number),
                            ),
                            onClick = vaultCardItemTypeHandlers.onCopyNumberClick,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        if (cardState.brand != null && cardState.brand != VaultCardBrand.SELECT) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenTextField(
                    label = stringResource(id = R.string.brand),
                    value = cardState.brand.value(),
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        cardState.expiration?.let { expiration ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenTextField(
                    label = stringResource(id = R.string.expiration),
                    value = expiration,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        cardState.securityCode?.let { securityCode ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenPasswordFieldWithActions(
                    label = stringResource(id = R.string.security_code),
                    value = securityCode,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    actions = {
                        BitwardenIconButtonWithResource(
                            iconRes = IconResource(
                                iconPainter = painterResource(id = R.drawable.ic_copy),
                                contentDescription = stringResource(
                                    id = R.string.copy_security_code,
                                ),
                            ),
                            onClick = vaultCardItemTypeHandlers.onCopySecurityCodeClick,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        commonState.notes?.let { notes ->
            item {
                Spacer(modifier = Modifier.height(4.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.notes),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenTextField(
                    label = stringResource(id = R.string.notes),
                    value = notes,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        commonState.customFields.takeUnless { it.isEmpty() }?.let { customFields ->
            item {
                Spacer(modifier = Modifier.height(4.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.custom_fields),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
            items(customFields) { customField ->
                Spacer(modifier = Modifier.height(8.dp))
                CustomField(
                    customField = customField,
                    onCopyCustomHiddenField = vaultCommonItemTypeHandlers.onCopyCustomHiddenField,
                    onCopyCustomTextField = vaultCommonItemTypeHandlers.onCopyCustomTextField,
                    onShowHiddenFieldClick = vaultCommonItemTypeHandlers.onShowHiddenFieldClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            VaultItemUpdateText(
                header = "${stringResource(id = R.string.date_updated)}: ",
                text = commonState.lastUpdated,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
        item {
            Spacer(modifier = Modifier.height(88.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
