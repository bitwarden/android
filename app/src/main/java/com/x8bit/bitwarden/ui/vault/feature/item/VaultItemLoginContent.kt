package com.x8bit.bitwarden.ui.vault.feature.item

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.BitwardenIconButtonWithResource
import com.x8bit.bitwarden.ui.platform.components.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.BitwardenPasswordFieldWithActions
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextFieldWithActions
import com.x8bit.bitwarden.ui.platform.components.BitwardenWideSwitch
import com.x8bit.bitwarden.ui.platform.components.model.IconResource
import com.x8bit.bitwarden.ui.platform.theme.LocalNonMaterialTypography

/**
 * The top level content UI state for the [VaultItemScreen] when viewing a Login cipher.
 */
@Suppress("LongMethod")
@Composable
fun VaultItemLoginContent(
    viewState: VaultItemState.ViewState.Content.Login,
    modifier: Modifier = Modifier,
    loginHandlers: LoginHandlers,
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
        item {
            Spacer(modifier = Modifier.height(8.dp))
            BitwardenTextField(
                label = stringResource(id = R.string.name),
                value = viewState.name,
                onValueChange = { },
                readOnly = true,
                singleLine = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        viewState.username?.let { username ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                UsernameField(
                    username = username,
                    onCopyUsernameClick = loginHandlers.onCopyUsernameClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        viewState.passwordData?.let { passwordData ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                PasswordField(
                    passwordData = passwordData,
                    onShowPasswordClick = loginHandlers.onShowPasswordClick,
                    onCheckForBreachClick = loginHandlers.onCheckForBreachClick,
                    onCopyPasswordClick = loginHandlers.onCopyPasswordClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            TotpField(
                isPremiumUser = viewState.isPremiumUser,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        viewState.uris.takeUnless { it.isEmpty() }?.let { uris ->
            item {
                Spacer(modifier = Modifier.height(4.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.ur_is),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
            items(uris) { uriData ->
                Spacer(modifier = Modifier.height(8.dp))
                UriField(
                    uriData = uriData,
                    onCopyUriClick = loginHandlers.onCopyUriClick,
                    onLaunchUriClick = loginHandlers.onLaunchUriClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        viewState.notes?.let { notes ->
            item {
                Spacer(modifier = Modifier.height(4.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.notes),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                NotesField(
                    notes = notes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        viewState.customFields.takeUnless { it.isEmpty() }?.let { customFields ->
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
                    onCopyCustomHiddenField = loginHandlers.onCopyCustomHiddenField,
                    onCopyCustomTextField = loginHandlers.onCopyCustomTextField,
                    onShowHiddenFieldClick = loginHandlers.onShowHiddenFieldClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            UpdateText(
                header = "${stringResource(id = R.string.date_updated)}: ",
                text = viewState.lastUpdated,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        viewState.passwordRevisionDate?.let { revisionDate ->
            item {
                UpdateText(
                    header = "${stringResource(id = R.string.date_password_updated)}: ",
                    text = revisionDate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        viewState.passwordHistoryCount?.let { passwordHistoryCount ->
            item {
                PasswordHistoryCount(
                    passwordHistoryCount = passwordHistoryCount,
                    onPasswordHistoryClick = loginHandlers.onPasswordHistoryClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(88.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun CustomField(
    customField: VaultItemState.ViewState.Content.Custom,
    onCopyCustomHiddenField: (String) -> Unit,
    onCopyCustomTextField: (String) -> Unit,
    onShowHiddenFieldClick: (VaultItemState.ViewState.Content.Custom.HiddenField, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (customField) {
        is VaultItemState.ViewState.Content.Custom.BooleanField -> {
            BitwardenWideSwitch(
                label = customField.name,
                isChecked = customField.value,
                readOnly = true,
                onCheckedChange = { },
                modifier = modifier,
            )
        }

        is VaultItemState.ViewState.Content.Custom.HiddenField -> {
            BitwardenPasswordFieldWithActions(
                label = customField.name,
                value = customField.value,
                showPasswordChange = { onShowHiddenFieldClick(customField, it) },
                showPassword = customField.isVisible,
                onValueChange = { },
                readOnly = true,
                singleLine = false,
                modifier = modifier,
                actions = {
                    if (customField.isCopyable) {
                        BitwardenIconButtonWithResource(
                            iconRes = IconResource(
                                iconPainter = painterResource(id = R.drawable.ic_copy),
                                contentDescription = stringResource(id = R.string.copy),
                            ),
                            onClick = {
                                onCopyCustomHiddenField(customField.value)
                            },
                        )
                    }
                },
            )
        }

        is VaultItemState.ViewState.Content.Custom.LinkedField -> {
            BitwardenTextField(
                label = customField.name,
                value = customField.type.label(),
                leadingIconResource = IconResource(
                    iconPainter = painterResource(id = R.drawable.ic_linked),
                    contentDescription = stringResource(id = R.string.field_type_linked),
                ),
                onValueChange = { },
                readOnly = true,
                singleLine = false,
                modifier = modifier,
            )
        }

        is VaultItemState.ViewState.Content.Custom.TextField -> {
            BitwardenTextFieldWithActions(
                label = customField.name,
                value = customField.value,
                onValueChange = { },
                readOnly = true,
                singleLine = false,
                modifier = modifier,
                actions = {
                    if (customField.isCopyable) {
                        BitwardenIconButtonWithResource(
                            iconRes = IconResource(
                                iconPainter = painterResource(id = R.drawable.ic_copy),
                                contentDescription = stringResource(id = R.string.copy),
                            ),
                            onClick = { onCopyCustomTextField(customField.value) },
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun NotesField(
    notes: String,
    modifier: Modifier = Modifier,
) {
    BitwardenTextField(
        label = stringResource(id = R.string.notes),
        value = notes,
        onValueChange = { },
        readOnly = true,
        singleLine = false,
        modifier = modifier,
    )
}

@Composable
private fun PasswordField(
    passwordData: VaultItemState.ViewState.Content.PasswordData,
    onShowPasswordClick: (Boolean) -> Unit,
    onCheckForBreachClick: () -> Unit,
    onCopyPasswordClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenPasswordFieldWithActions(
        label = stringResource(id = R.string.password),
        value = passwordData.password,
        showPasswordChange = { onShowPasswordClick(it) },
        showPassword = passwordData.isVisible,
        onValueChange = { },
        readOnly = true,
        singleLine = false,
        actions = {
            BitwardenIconButtonWithResource(
                iconRes = IconResource(
                    iconPainter = painterResource(id = R.drawable.ic_check_mark),
                    contentDescription = stringResource(
                        id = R.string.check_known_data_breaches_for_this_password,
                    ),
                ),
                onClick = onCheckForBreachClick,
            )
            BitwardenIconButtonWithResource(
                iconRes = IconResource(
                    iconPainter = painterResource(id = R.drawable.ic_copy),
                    contentDescription = stringResource(id = R.string.copy_password),
                ),
                onClick = onCopyPasswordClick,
            )
        },
        modifier = modifier,
    )
}

@Composable
private fun PasswordHistoryCount(
    passwordHistoryCount: Int,
    onPasswordHistoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { },
    ) {
        Text(
            text = "${stringResource(id = R.string.password_history)}: ",
            style = LocalNonMaterialTypography.current.labelMediumProminent,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = passwordHistoryCount.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onPasswordHistoryClick),
        )
    }
}

@Composable
private fun TotpField(
    isPremiumUser: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isPremiumUser) {
        // TODO: Insert TOTP values here (BIT-1214)
    } else {
        BitwardenTextField(
            label = stringResource(id = R.string.verification_code_totp),
            value = stringResource(id = R.string.premium_subscription_required),
            enabled = false,
            singleLine = false,
            onValueChange = { },
            readOnly = true,
            modifier = modifier,
        )
    }
}

@Composable
private fun UpdateText(
    header: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) { },
    ) {
        Text(
            text = header,
            style = LocalNonMaterialTypography.current.labelMediumProminent,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun UriField(
    uriData: VaultItemState.ViewState.Content.UriData,
    onCopyUriClick: (String) -> Unit,
    onLaunchUriClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenTextFieldWithActions(
        label = stringResource(id = R.string.uri),
        value = uriData.uri,
        onValueChange = { },
        readOnly = true,
        singleLine = false,
        actions = {
            if (uriData.isLaunchable) {
                BitwardenIconButtonWithResource(
                    iconRes = IconResource(
                        iconPainter = painterResource(id = R.drawable.ic_launch),
                        contentDescription = stringResource(id = R.string.launch),
                    ),
                    onClick = { onLaunchUriClick(uriData.uri) },
                )
            }
            if (uriData.isCopyable) {
                BitwardenIconButtonWithResource(
                    iconRes = IconResource(
                        iconPainter = painterResource(id = R.drawable.ic_copy),
                        contentDescription = stringResource(id = R.string.copy),
                    ),
                    onClick = { onCopyUriClick(uriData.uri) },
                )
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun UsernameField(
    username: String,
    onCopyUsernameClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenTextFieldWithActions(
        label = stringResource(id = R.string.username),
        value = username,
        onValueChange = { },
        readOnly = true,
        singleLine = false,
        actions = {
            BitwardenIconButtonWithResource(
                iconRes = IconResource(
                    iconPainter = painterResource(id = R.drawable.ic_copy),
                    contentDescription = stringResource(id = R.string.copy_username),
                ),
                onClick = onCopyUsernameClick,
            )
        },
        modifier = modifier,
    )
}

/**
 * A class dedicated to handling user interactions related to view login cipher UI.
 * Each lambda corresponds to a specific user action, allowing for easy delegation of
 * logic when user input is detected.
 */
@Suppress("LongParameterList")
class LoginHandlers(
    val onCheckForBreachClick: () -> Unit,
    val onCopyCustomHiddenField: (String) -> Unit,
    val onCopyCustomTextField: (String) -> Unit,
    val onCopyPasswordClick: () -> Unit,
    val onCopyUriClick: (String) -> Unit,
    val onCopyUsernameClick: () -> Unit,
    val onLaunchUriClick: (String) -> Unit,
    val onPasswordHistoryClick: () -> Unit,
    val onShowHiddenFieldClick: (
        VaultItemState.ViewState.Content.Custom.HiddenField,
        Boolean,
    ) -> Unit,
    val onShowPasswordClick: (isVisible: Boolean) -> Unit,
) {
    companion object {
        /**
         * Creates the [LoginHandlers] using the [viewModel] to send the desired actions.
         */
        @Suppress("LongMethod")
        fun create(
            viewModel: VaultItemViewModel,
        ): LoginHandlers =
            LoginHandlers(
                onCheckForBreachClick = {
                    viewModel.trySendAction(VaultItemAction.Login.CheckForBreachClick)
                },
                onCopyCustomHiddenField = {
                    viewModel.trySendAction(VaultItemAction.Login.CopyCustomHiddenFieldClick(it))
                },
                onCopyCustomTextField = {
                    viewModel.trySendAction(VaultItemAction.Login.CopyCustomTextFieldClick(it))
                },
                onCopyPasswordClick = {
                    viewModel.trySendAction(VaultItemAction.Login.CopyPasswordClick)
                },
                onCopyUriClick = {
                    viewModel.trySendAction(VaultItemAction.Login.CopyUriClick(it))
                },
                onCopyUsernameClick = {
                    viewModel.trySendAction(VaultItemAction.Login.CopyUsernameClick)
                },
                onLaunchUriClick = {
                    viewModel.trySendAction(VaultItemAction.Login.LaunchClick(it))
                },
                onPasswordHistoryClick = {
                    viewModel.trySendAction(VaultItemAction.Login.PasswordHistoryClick)
                },
                onShowHiddenFieldClick = { customField, isVisible ->
                    viewModel.trySendAction(
                        VaultItemAction.Login.HiddenFieldVisibilityClicked(
                            isVisible = isVisible,
                            field = customField,
                        ),
                    )
                },
                onShowPasswordClick = {
                    viewModel.trySendAction(VaultItemAction.Login.PasswordVisibilityClicked(it))
                },
            )
    }
}
