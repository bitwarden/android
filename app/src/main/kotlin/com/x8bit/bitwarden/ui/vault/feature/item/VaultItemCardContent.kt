package com.x8bit.bitwarden.ui.vault.feature.item

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.text.BitwardenHyperTextLink
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.feature.item.component.CustomField
import com.x8bit.bitwarden.ui.vault.feature.item.component.itemHeader
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultCardItemTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultCommonItemTypeHandlers
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.util.shortName

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
    var isExpanded by rememberSaveable { mutableStateOf(value = false) }
    val applyIconBackground = cardState.paymentCardBrandIconData == null
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            Spacer(Modifier.height(height = 12.dp))
        }
        itemHeader(
            value = commonState.name,
            isFavorite = commonState.favorite,
            iconData = cardState.paymentCardBrandIconData ?: commonState.iconData,
            relatedLocations = commonState.relatedLocations,
            iconTestTag = "CardItemNameIcon",
            textFieldTestTag = "CardItemNameEntry",
            isExpanded = isExpanded,
            onExpandClick = { isExpanded = !isExpanded },
            applyIconBackground = applyIconBackground,
        )
        item {
            Spacer(modifier = Modifier.height(height = 8.dp))
        }
        cardState.cardholderName?.let { cardholderName ->
            item(key = "cardholderName") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.cardholder_name),
                    value = cardholderName,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "CardholderNameEntry",
                    cardStyle = cardState
                        .propertyList
                        .toListItemCardStyle(
                            index = cardState.propertyList.indexOf(element = cardholderName),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }
        cardState.number?.let { numberData ->
            item(key = "cardNumber") {
                BitwardenPasswordField(
                    label = stringResource(id = BitwardenString.number),
                    value = numberData.number,
                    onValueChange = {},
                    showPassword = numberData.isVisible,
                    showPasswordChange = vaultCardItemTypeHandlers.onShowNumberClick,
                    readOnly = true,
                    singleLine = false,
                    actions = {
                        BitwardenStandardIconButton(
                            vectorIconRes = BitwardenDrawable.ic_copy,
                            contentDescription = stringResource(id = BitwardenString.copy_number),
                            onClick = vaultCardItemTypeHandlers.onCopyNumberClick,
                            modifier = Modifier.testTag(tag = "CardCopyNumberButton"),
                        )
                    },
                    passwordFieldTestTag = "CardNumberEntry",
                    showPasswordTestTag = "CardViewNumberButton",
                    cardStyle = cardState
                        .propertyList
                        .toListItemCardStyle(
                            index = cardState.propertyList.indexOf(element = numberData),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        if (cardState.brand != null && cardState.brand != VaultCardBrand.SELECT) {
            item(key = "cardBrand") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.brand),
                    value = cardState.brand.shortName(),
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "CardBrandEntry",
                    cardStyle = cardState
                        .propertyList
                        .toListItemCardStyle(
                            index = cardState.propertyList.indexOf(element = cardState.brand),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        cardState.expiration?.let { expiration ->
            item(key = "expiration") {
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.expiration),
                    value = expiration,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = false,
                    textFieldTestTag = "CardExpirationEntry",
                    cardStyle = cardState
                        .propertyList
                        .toListItemCardStyle(
                            index = cardState.propertyList.indexOf(element = expiration),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        cardState.securityCode?.let { securityCodeData ->
            item(key = "securityCode") {
                BitwardenPasswordField(
                    label = stringResource(id = BitwardenString.security_code),
                    value = securityCodeData.code,
                    onValueChange = {},
                    showPassword = securityCodeData.isVisible,
                    showPasswordChange = vaultCardItemTypeHandlers.onShowSecurityCodeClick,
                    readOnly = true,
                    singleLine = false,
                    actions = {
                        BitwardenStandardIconButton(
                            vectorIconRes = BitwardenDrawable.ic_copy,
                            contentDescription = stringResource(
                                id = BitwardenString.copy_security_code,
                            ),
                            onClick = vaultCardItemTypeHandlers.onCopySecurityCodeClick,
                            modifier = Modifier.testTag(tag = "CardCopySecurityCodeButton"),
                        )
                    },
                    showPasswordTestTag = "CardViewSecurityCodeButton",
                    passwordFieldTestTag = "CardSecurityCodeEntry",
                    cardStyle = cardState
                        .propertyList
                        .toListItemCardStyle(
                            index = cardState.propertyList.indexOf(element = securityCodeData),
                            dividerPadding = 0.dp,
                        ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        commonState.notes?.let { notes ->
            item(key = "notes") {
                Spacer(modifier = Modifier.height(height = 16.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = BitwardenString.additional_options),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.notes),
                    value = notes,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = false,
                    actions = {
                        BitwardenStandardIconButton(
                            vectorIconRes = BitwardenDrawable.ic_copy,
                            contentDescription = stringResource(id = BitwardenString.copy_notes),
                            onClick = vaultCommonItemTypeHandlers.onCopyNotesClick,
                            modifier = Modifier.testTag(tag = "CipherNotesCopyButton"),
                        )
                    },
                    textFieldTestTag = "CipherNotesLabel",
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        commonState.customFields.takeUnless { it.isEmpty() }?.let { customFields ->
            item(key = "customFieldsHeader") {
                Spacer(modifier = Modifier.height(height = 16.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = BitwardenString.custom_fields),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                )
            }
            itemsIndexed(
                items = customFields,
                key = { index, _ -> "customField_$index" },
            ) { _, customField ->
                Spacer(modifier = Modifier.height(height = 8.dp))
                CustomField(
                    customField = customField,
                    onCopyCustomHiddenField = vaultCommonItemTypeHandlers.onCopyCustomHiddenField,
                    onCopyCustomTextField = vaultCommonItemTypeHandlers.onCopyCustomTextField,
                    onShowHiddenFieldClick = vaultCommonItemTypeHandlers.onShowHiddenFieldClick,
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                )
            }
        }

        commonState.attachments.takeUnless { it?.isEmpty() == true }?.let { attachments ->
            item(key = "attachmentsHeader") {
                Spacer(modifier = Modifier.height(height = 16.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = BitwardenString.attachments),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                )
                Spacer(modifier = Modifier.height(height = 8.dp))
            }
            itemsIndexed(
                items = attachments,
                key = { index, _ -> "attachment_$index" },
            ) { index, attachmentItem ->
                AttachmentItemContent(
                    modifier = Modifier
                        .testTag("CipherAttachment")
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .animateItem(),
                    attachmentItem = attachmentItem,
                    onAttachmentDownloadClick = vaultCommonItemTypeHandlers
                        .onAttachmentDownloadClick,
                    cardStyle = attachments.toListItemCardStyle(index = index),
                )
            }
        }

        item(key = "created") {
            Spacer(modifier = Modifier.height(height = 16.dp))
            Text(
                text = commonState.created(),
                style = BitwardenTheme.typography.bodySmall,
                color = BitwardenTheme.colorScheme.text.secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 12.dp)
                    .animateItem()
                    .testTag("CardItemCreated"),
            )
        }

        item(key = "lastUpdated") {
            Spacer(modifier = Modifier.height(height = 4.dp))
            Text(
                text = commonState.lastUpdated(),
                style = BitwardenTheme.typography.bodySmall,
                color = BitwardenTheme.colorScheme.text.secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(horizontal = 12.dp)
                    .animateItem()
                    .testTag("CardItemLastUpdated"),
            )
        }

        commonState.passwordHistoryCount?.let { passwordHistoryCount ->
            item(key = "passwordHistoryCount") {
                Spacer(modifier = Modifier.height(height = 4.dp))
                BitwardenHyperTextLink(
                    annotatedResId = BitwardenString.password_history_count,
                    args = arrayOf(passwordHistoryCount.toString()),
                    annotationKey = "passwordHistory",
                    accessibilityString = stringResource(id = BitwardenString.password_history),
                    onClick = vaultCommonItemTypeHandlers.onPasswordHistoryClick,
                    style = BitwardenTheme.typography.labelMedium,
                    modifier = Modifier
                        .wrapContentWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 12.dp)
                        .animateItem(),
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(88.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
