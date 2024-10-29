package com.x8bit.bitwarden.ui.vault.feature.item

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
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
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
                    .testTag("SshKeyItemNameEntry")
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        sshKeyItemState.publicKey?.let { publicKey ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenTextField(
                    label = stringResource(id = R.string.public_key),
                    value = publicKey,
                    onValueChange = { },
                    singleLine = false,
                    readOnly = true,
                    modifier = Modifier
                        .testTag("SshKeyItemPublicKeyEntry")
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        sshKeyItemState.privateKey?.let { privateKey ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenPasswordField(
                    label = stringResource(id = R.string.private_key),
                    value = privateKey,
                    onValueChange = { },
                    singleLine = false,
                    readOnly = true,
                    showPassword = sshKeyItemState.showPrivateKey,
                    showPasswordTestTag = "ViewPrivateKeyButton",
                    showPasswordChange = vaultSshKeyItemTypeHandlers.onShowPrivateKeyClick,
                    modifier = Modifier
                        .testTag("SshKeyItemPrivateKeyEntry")
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        sshKeyItemState.fingerprint?.let { fingerprint ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BitwardenTextField(
                    label = stringResource(id = R.string.fingerprint),
                    value = fingerprint,
                    onValueChange = { },
                    singleLine = false,
                    readOnly = true,
                    modifier = Modifier
                        .testTag("SshKeyItemFingerprintEntry")
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
                    .padding(horizontal = 16.dp)
                    .testTag("SshKeyItemLastUpdated"),
            )
        }

        item {
            Spacer(modifier = Modifier.height(88.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
