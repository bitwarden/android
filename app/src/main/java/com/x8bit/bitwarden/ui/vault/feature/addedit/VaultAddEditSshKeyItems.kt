package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditCommonHandlers

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
                .padding(horizontal = 16.dp),
        )
    }

    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenPasswordField(
            label = stringResource(id = R.string.public_key),
            value = sshKeyState.publicKey,
            onValueChange = sshKeyTypeHandlers.onPublicKeyTextChange,
            showPassword = sshKeyState.showPublicKey,
            showPasswordChange = { sshKeyTypeHandlers.onPublicKeyVisibilityChange(it) },
            showPasswordTestTag = "ViewPublicKeyButton",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }

    item {
        Spacer(modifier = Modifier.height(8.dp))
        BitwardenPasswordField(
            label = stringResource(id = R.string.fingerprint),
            value = sshKeyState.fingerprint,
            onValueChange = sshKeyTypeHandlers.onFingerprintTextChange,
            showPassword = sshKeyState.showFingerprint,
            showPasswordChange = { sshKeyTypeHandlers.onFingerprintVisibilityChange(it) },
            showPasswordTestTag = "ViewFingerprintButton",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
}

data class VaultAddEditSshKeyTypeHandlers(
    val onPublicKeyTextChange: (String) -> Unit,
    val onPublicKeyVisibilityChange: (Boolean) -> Unit,
    val onPrivateKeyTextChange: (String) -> Unit,
    val onPrivateKeyVisibilityChange: (Boolean) -> Unit,
    val onFingerprintTextChange: (String) -> Unit,
    val onFingerprintVisibilityChange: (Boolean) -> Unit,
) {
    companion object {
        fun create(viewModel: VaultAddEditViewModel): VaultAddEditSshKeyTypeHandlers =
            VaultAddEditSshKeyTypeHandlers(
                onPublicKeyTextChange = { newPublicKey ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.SshKeyType.PublicKeyTextChange(
                            publicKey = newPublicKey,
                        ),
                    )
                },
                onPublicKeyVisibilityChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.SshKeyType.PublicKeyVisibilityChange(
                            isVisible = it
                        ),
                    )
                },
                onPrivateKeyTextChange = { newPrivateKey ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.SshKeyType.PrivateKeyTextChange(
                            privateKey = newPrivateKey,
                        ),
                    )
                },
                onPrivateKeyVisibilityChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.SshKeyType.PrivateKeyVisibilityChange(
                            isVisible = it
                        ),
                    )
                },
                onFingerprintTextChange = { newFingerprint ->
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.SshKeyType.FingerprintTextChange(
                            fingerprint = newFingerprint,
                        ),
                    )
                },
                onFingerprintVisibilityChange = {
                    viewModel.trySendAction(
                        VaultAddEditAction.ItemType.SshKeyType.FingerprintVisibilityChange(
                            isVisible = it
                        ),
                    )
                },
            )
    }
}
