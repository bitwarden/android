package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditCommonHandlers
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditSshKeyTypeHandlers

/**
 * The UI for adding and editing a SSH key cipher.
 */
fun LazyListScope.vaultAddEditSshKeyItems(
    commonState: VaultAddEditState.ViewState.Content.Common,
    sshKeyState: VaultAddEditState.ViewState.Content.ItemType.SshKey,
    commonTypeHandlers: VaultAddEditCommonHandlers,
    sshKeyTypeHandlers: VaultAddEditSshKeyTypeHandlers,
) {
    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.name),
            value = commonState.name,
            onValueChange = commonTypeHandlers.onNameTextChange,
            modifier = Modifier
                .testTag("ItemNameEntry")
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.public_key),
            value = sshKeyState.publicKey,
            onValueChange = sshKeyTypeHandlers.onPublicKeyTextChange,
            modifier = Modifier
                .testTag("PublicKeyEntry")
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenPasswordField(
            label = stringResource(id = R.string.private_key),
            value = sshKeyState.privateKey,
            onValueChange = sshKeyTypeHandlers.onPrivateKeyTextChange,
            showPassword = sshKeyState.showPrivateKey,
            showPasswordChange = { sshKeyTypeHandlers.onPrivateKeyVisibilityChange(it) },
            showPasswordTestTag = "ViewPrivateKeyButton",
            modifier = Modifier
                .testTag("PrivateKeyEntry")
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenTextField(
            label = stringResource(id = R.string.fingerprint),
            value = sshKeyState.fingerprint,
            onValueChange = sshKeyTypeHandlers.onFingerprintTextChange,
            modifier = Modifier
                .testTag("FingerprintEntry")
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VaultAddEditSshKeyItems_preview() {
    BitwardenTheme {
        LazyColumn {
            vaultAddEditSshKeyItems(
                commonState = VaultAddEditState.ViewState.Content.Common(
                    name = "SSH Key",
                ),
                sshKeyState = VaultAddEditState.ViewState.Content.ItemType.SshKey(
                    publicKey = "public key",
                    privateKey = "private key",
                    fingerprint = "fingerprint",
                    showPublicKey = false,
                    showPrivateKey = false,
                    showFingerprint = false,
                ),
                commonTypeHandlers = VaultAddEditCommonHandlers(
                    onNameTextChange = { },
                    onFolderSelected = { },
                    onToggleFavorite = { },
                    onToggleMasterPasswordReprompt = { },
                    onNotesTextChange = { },
                    onOwnerSelected = { },
                    onTooltipClick = { },
                    onAddNewCustomFieldClick = { _, _ -> },
                    onCustomFieldValueChange = { },
                    onCustomFieldActionSelect = { _, _ -> },
                    onCollectionSelect = { },
                    onHiddenFieldVisibilityChange = { },
                ),
                sshKeyTypeHandlers = VaultAddEditSshKeyTypeHandlers(
                    onPublicKeyTextChange = { },
                    onPrivateKeyTextChange = { },
                    onPrivateKeyVisibilityChange = { },
                    onFingerprintTextChange = { },
                ),
            )
        }
    }
}
