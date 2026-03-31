package com.x8bit.bitwarden.ui.vault.feature.item

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.vault.feature.item.component.itemHeader
import com.x8bit.bitwarden.ui.vault.feature.item.component.vaultItemAttachments
import com.x8bit.bitwarden.ui.vault.feature.item.component.vaultItemCustomFields
import com.x8bit.bitwarden.ui.vault.feature.item.component.vaultItemHistory
import com.x8bit.bitwarden.ui.vault.feature.item.component.vaultItemNotes
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultCommonItemTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultSshKeyItemTypeHandlers

/**
 * The top level content UI state for the [VaultItemScreen] when viewing a SSH key cipher.
 */
@Suppress("LongMethod")
@Composable
fun VaultItemSshKeyContent(
    commonState: VaultItemState.ViewState.Content.Common,
    sshKeyItemState: VaultItemState.ViewState.Content.ItemType.SshKey,
    vaultSshKeyItemTypeHandlers: VaultSshKeyItemTypeHandlers,
    vaultCommonItemTypeHandlers: VaultCommonItemTypeHandlers,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable { mutableStateOf(value = false) }
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            Spacer(Modifier.height(height = 12.dp))
        }
        itemHeader(
            value = commonState.name,
            isFavorite = commonState.favorite,
            isArchived = commonState.archived,
            iconData = commonState.iconData,
            relatedLocations = commonState.relatedLocations,
            iconTestTag = "SshKeyItemNameIcon",
            textFieldTestTag = "SshKeyItemNameEntry",
            isExpanded = isExpanded,
            onExpandClick = { isExpanded = !isExpanded },
            applyIconBackground = commonState.iconData is IconData.Local,
        )

        item(key = "privateKey") {
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenPasswordField(
                label = stringResource(id = BitwardenString.private_key),
                value = sshKeyItemState.privateKey,
                onValueChange = { },
                singleLine = false,
                readOnly = true,
                actions = {
                    BitwardenStandardIconButton(
                        vectorIconRes = BitwardenDrawable.ic_copy,
                        contentDescription = stringResource(id = BitwardenString.copy_private_key),
                        onClick = vaultSshKeyItemTypeHandlers.onCopyPrivateKeyClick,
                        modifier = Modifier.testTag(tag = "SshKeyCopyPrivateKeyButton"),
                    )
                },
                showPassword = sshKeyItemState.showPrivateKey,
                showPasswordTestTag = "ViewPrivateKeyButton",
                showPasswordChange = vaultSshKeyItemTypeHandlers.onShowPrivateKeyClick,
                cardStyle = CardStyle.Top(),
                modifier = Modifier
                    .testTag("SshKeyItemPrivateKeyEntry")
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .animateItem(),
            )
        }

        item(key = "publicKey") {
            BitwardenTextField(
                label = stringResource(id = BitwardenString.public_key),
                value = sshKeyItemState.publicKey,
                onValueChange = { },
                singleLine = false,
                readOnly = true,
                actions = {
                    BitwardenStandardIconButton(
                        vectorIconRes = BitwardenDrawable.ic_copy,
                        contentDescription = stringResource(id = BitwardenString.copy_public_key),
                        onClick = vaultSshKeyItemTypeHandlers.onCopyPublicKeyClick,
                        modifier = Modifier.testTag(tag = "SshKeyCopyPublicKeyButton"),
                    )
                },
                cardStyle = CardStyle.Middle(),
                modifier = Modifier
                    .testTag("SshKeyItemPublicKeyEntry")
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .animateItem(),
            )
        }

        item(key = "fingerprint") {
            BitwardenTextField(
                label = stringResource(id = BitwardenString.fingerprint),
                value = sshKeyItemState.fingerprint,
                onValueChange = { },
                singleLine = false,
                readOnly = true,
                actions = {
                    BitwardenStandardIconButton(
                        vectorIconRes = BitwardenDrawable.ic_copy,
                        contentDescription = stringResource(id = BitwardenString.copy_fingerprint),
                        onClick = vaultSshKeyItemTypeHandlers.onCopyFingerprintClick,
                        modifier = Modifier.testTag(tag = "SshKeyCopyFingerprintButton"),
                    )
                },
                cardStyle = CardStyle.Bottom,
                modifier = Modifier
                    .testTag("SshKeyItemFingerprintEntry")
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .animateItem(),
            )
        }

        vaultItemNotes(
            notes = commonState.notes,
            vaultCommonItemTypeHandlers = vaultCommonItemTypeHandlers,
        )

        vaultItemCustomFields(
            customFields = commonState.customFields,
            vaultCommonItemTypeHandlers = vaultCommonItemTypeHandlers,
        )

        vaultItemAttachments(
            attachments = commonState.attachments,
            vaultCommonItemTypeHandlers = vaultCommonItemTypeHandlers,
        )

        vaultItemHistory(
            commonState = commonState,
            vaultCommonItemTypeHandlers = vaultCommonItemTypeHandlers,
            loginPasswordRevisionDate = null,
        )

        item {
            Spacer(modifier = Modifier.height(88.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
