package com.x8bit.bitwarden.ui.vault.feature.addedit

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.cardStyle
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.coachmark.CoachMarkActionText
import com.x8bit.bitwarden.ui.platform.components.coachmark.CoachMarkScope
import com.x8bit.bitwarden.ui.platform.components.coachmark.model.CoachMarkHighlightShape
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenHiddenPasswordField
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.model.TooltipData
import com.x8bit.bitwarden.ui.platform.components.text.BitwardenClickableText
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.feature.addedit.handlers.VaultAddEditLoginTypeHandlers

/**
 * The UI for adding and editing a login cipher.
 */
@Suppress("LongMethod", "LongParameterList")
fun LazyListScope.vaultAddEditLoginItems(
    coachMarkScope: CoachMarkScope<AddEditItemCoachMark>,
    loginState: VaultAddEditState.ViewState.Content.ItemType.Login,
    loginItemTypeHandlers: VaultAddEditLoginTypeHandlers,
    windowAdaptiveInfo: WindowAdaptiveInfo,
    onTotpSetupClick: () -> Unit,
    onNextCoachMark: () -> Unit,
    onPreviousCoachMark: () -> Unit,
    onCoachMarkTourComplete: () -> Unit,
    onCoachMarkDismissed: () -> Unit,
) = coachMarkScope.run {
    item {
        Spacer(modifier = Modifier.height(height = 16.dp))
        BitwardenListHeaderText(
            label = stringResource(id = R.string.login_credentials),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .padding(horizontal = 16.dp),
        )
    }

    item {
        Spacer(modifier = Modifier.height(8.dp))
        UsernameRow(
            username = loginState.username,
            loginItemTypeHandlers = loginItemTypeHandlers,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    item(key = AddEditItemCoachMark.GENERATE_PASSWORD) {
        PasswordRow(
            password = loginState.password,
            canViewPassword = loginState.canViewPassword,
            loginItemTypeHandlers = loginItemTypeHandlers,
            onGenerateCoachMarkActionClick = onNextCoachMark,
            onCoachMarkDismissed = onCoachMarkDismissed,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }

    loginState.fido2CredentialCreationDateTime?.let { creationDateTime ->
        item {
            Spacer(modifier = Modifier.height(8.dp))
            PasskeyField(
                creationDateTime = creationDateTime,
                canRemovePasskey = loginState.canRemovePasskey,
                loginItemTypeHandlers = loginItemTypeHandlers,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
        }
    }

    item {
        Spacer(modifier = Modifier.height(height = 8.dp))
    }

    coachMarkHighlightItem(
        key = AddEditItemCoachMark.TOTP,
        title = R.string.coachmark_2_of_3.asText(),
        description = R.string.you_ll_only_need_to_set_up_authenticator_key.asText(),
        onDismiss = onCoachMarkDismissed,
        leftAction = {
            CoachMarkActionText(
                actionLabel = stringResource(R.string.back),
                onActionClick = onPreviousCoachMark,
            )
        },
        rightAction = {
            CoachMarkActionText(
                actionLabel = stringResource(R.string.next),
                onActionClick = onNextCoachMark,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .standardHorizontalMargin(windowAdaptiveInfo = windowAdaptiveInfo),
    ) {
        TotpRow(
            totpKey = loginState.totp,
            canViewTotp = loginState.canViewPassword,
            loginItemTypeHandlers = loginItemTypeHandlers,
            onTotpSetupClick = onTotpSetupClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    item {
        Spacer(modifier = Modifier.height(height = 16.dp))
        BitwardenListHeaderText(
            label = stringResource(id = R.string.autofill_options),
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(height = 8.dp))
    }

    coachMarkHighlightItems(
        key = AddEditItemCoachMark.URI,
        title = R.string.coachmark_3_of_3.asText(),
        description = R.string.you_must_add_a_web_address_to_use_autofill_to_access_this_account
            .asText(),
        leftAction = {
            CoachMarkActionText(
                actionLabel = stringResource(R.string.back),
                onActionClick = onPreviousCoachMark,
            )
        },
        onDismiss = onCoachMarkDismissed,
        rightAction = {
            CoachMarkActionText(
                actionLabel = stringResource(R.string.done_text),
                onActionClick = onCoachMarkTourComplete,
            )
        },
        trailingContentIsBottomCard = true,
        trailingStaticContent = {
            BitwardenClickableText(
                label = stringResource(id = R.string.add_website),
                onClick = loginItemTypeHandlers.onAddNewUriClick,
                leadingIcon = painterResource(id = R.drawable.ic_plus_small),
                style = BitwardenTheme.typography.labelMedium,
                innerPadding = PaddingValues(all = 16.dp),
                cornerSize = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(tag = "LoginAddNewUriButton")
                    .cardStyle(cardStyle = CardStyle.Bottom, paddingVertical = 0.dp),
            )
        },
        items = loginState.uriList,
        modifier = Modifier
            .standardHorizontalMargin(windowAdaptiveInfo = windowAdaptiveInfo),
    ) { uriItem, cardStyle ->
        VaultAddEditUriItem(
            uriItem = uriItem,
            onUriValueChange = loginItemTypeHandlers.onUriValueChange,
            onUriItemRemoved = loginItemTypeHandlers.onRemoveUriClick,
            cardStyle = cardStyle,
            modifier = Modifier
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun UsernameRow(
    username: String,
    loginItemTypeHandlers: VaultAddEditLoginTypeHandlers,
    modifier: Modifier = Modifier,
) {
    var shouldShowDialog by rememberSaveable { mutableStateOf(false) }

    BitwardenTextField(
        label = stringResource(id = R.string.username),
        value = username,
        onValueChange = loginItemTypeHandlers.onUsernameTextChange,
        actions = {
            BitwardenStandardIconButton(
                vectorIconRes = R.drawable.ic_generate,
                contentDescription = stringResource(id = R.string.generate_username),
                onClick = {
                    if (username.isEmpty()) {
                        loginItemTypeHandlers.onOpenUsernameGeneratorClick()
                    } else {
                        shouldShowDialog = true
                    }
                },
                modifier = Modifier.testTag(tag = "GenerateUsernameButton"),
            )
        },
        textFieldTestTag = "LoginUsernameEntry",
        cardStyle = CardStyle.Top(dividerPadding = 0.dp),
        modifier = modifier,
    )

    if (shouldShowDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = R.string.username),
            message = stringResource(
                id = R.string.are_you_sure_you_want_to_overwrite_the_current_username,
            ),
            confirmButtonText = stringResource(id = R.string.yes),
            dismissButtonText = stringResource(id = R.string.no),
            onConfirmClick = {
                shouldShowDialog = false
                loginItemTypeHandlers.onOpenUsernameGeneratorClick()
            },
            onDismissClick = {
                shouldShowDialog = false
            },
            onDismissRequest = {
                shouldShowDialog = false
            },
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun CoachMarkScope<AddEditItemCoachMark>.PasswordRow(
    password: String,
    canViewPassword: Boolean,
    loginItemTypeHandlers: VaultAddEditLoginTypeHandlers,
    onGenerateCoachMarkActionClick: () -> Unit,
    onCoachMarkDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var shouldShowDialog by rememberSaveable { mutableStateOf(false) }

    if (canViewPassword) {
        var shouldShowPassword by remember { mutableStateOf(false) }
        BitwardenPasswordField(
            label = stringResource(id = R.string.password),
            value = password,
            onValueChange = loginItemTypeHandlers.onPasswordTextChange,
            showPassword = shouldShowPassword,
            showPasswordChange = {
                shouldShowPassword = !shouldShowPassword
                loginItemTypeHandlers.onPasswordVisibilityChange(shouldShowPassword)
            },
            showPasswordTestTag = "ViewPasswordButton",
            passwordFieldTestTag = "LoginPasswordEntry",
            supportingContentPadding = PaddingValues(),
            supportingContent = {
                BitwardenClickableText(
                    label = stringResource(id = R.string.check_password_for_data_breaches),
                    style = BitwardenTheme.typography.labelMedium,
                    onClick = loginItemTypeHandlers.onPasswordCheckerClick,
                    innerPadding = PaddingValues(all = 16.dp),
                    cornerSize = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(tag = "CheckPasswordButton"),
                )
            },
            cardStyle = CardStyle.Bottom,
            modifier = modifier,
        ) {
            CoachMarkHighlight(
                key = AddEditItemCoachMark.GENERATE_PASSWORD,
                title = stringResource(R.string.coachmark_1_of_3),
                description = stringResource(
                    R.string.use_this_button_to_generate_a_new_unique_password,
                ),
                shape = CoachMarkHighlightShape.Oval,
                onDismiss = onCoachMarkDismissed,
                leftAction = null,
                rightAction = {
                    CoachMarkActionText(
                        actionLabel = stringResource(R.string.next),
                        onActionClick = onGenerateCoachMarkActionClick,
                    )
                },
            ) {
                BitwardenStandardIconButton(
                    vectorIconRes = R.drawable.ic_generate,
                    contentDescription = stringResource(id = R.string.generate_password),
                    onClick = {
                        if (password.isEmpty()) {
                            loginItemTypeHandlers.onOpenPasswordGeneratorClick()
                        } else {
                            shouldShowDialog = true
                        }
                    },
                    modifier = Modifier.testTag(tag = "RegeneratePasswordButton"),
                )
            }

            if (shouldShowDialog) {
                BitwardenTwoButtonDialog(
                    title = stringResource(id = R.string.password),
                    message = stringResource(id = R.string.password_override_alert),
                    confirmButtonText = stringResource(id = R.string.yes),
                    dismissButtonText = stringResource(id = R.string.no),
                    onConfirmClick = {
                        shouldShowDialog = false
                        loginItemTypeHandlers.onOpenPasswordGeneratorClick()
                    },
                    onDismissClick = {
                        shouldShowDialog = false
                    },
                    onDismissRequest = {
                        shouldShowDialog = false
                    },
                )
            }
        }
    } else {
        BitwardenHiddenPasswordField(
            label = stringResource(id = R.string.password),
            value = password,
            passwordFieldTestTag = "LoginPasswordEntry",
            cardStyle = CardStyle.Bottom,
            modifier = modifier,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun TotpRow(
    totpKey: String?,
    canViewTotp: Boolean,
    loginItemTypeHandlers: VaultAddEditLoginTypeHandlers,
    onTotpSetupClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenTextField(
        label = stringResource(id = R.string.authenticator_key),
        value = totpKey.orEmpty(),
        onValueChange = {},
        readOnly = true,
        singleLine = true,
        actions = {
            totpKey?.let {
                BitwardenStandardIconButton(
                    vectorIconRes = R.drawable.ic_clear,
                    contentDescription = stringResource(id = R.string.delete),
                    onClick = loginItemTypeHandlers.onClearTotpKeyClick,
                )
                BitwardenStandardIconButton(
                    vectorIconRes = R.drawable.ic_copy,
                    contentDescription = stringResource(id = R.string.copy_totp),
                    onClick = { loginItemTypeHandlers.onCopyTotpKeyClick(totpKey) },
                )
            }
        },
        tooltip = TooltipData(
            onClick = loginItemTypeHandlers.onAuthenticatorHelpToolTipClick,
            contentDescription = stringResource(id = R.string.authenticator_key_help),
        ),
        supportingContentPadding = PaddingValues(),
        supportingContent = {
            BitwardenClickableText(
                label = stringResource(id = R.string.set_up_authenticator_key),
                onClick = onTotpSetupClick,
                leadingIcon = painterResource(id = R.drawable.ic_camera_small),
                style = BitwardenTheme.typography.labelMedium,
                innerPadding = PaddingValues(all = 16.dp),
                isEnabled = canViewTotp,
                cornerSize = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("SetupTotpButton"),
            )
        },
        textFieldTestTag = "LoginTotpEntry",
        cardStyle = CardStyle.Full,
        modifier = modifier
            .fillMaxWidth()
            .focusProperties { canFocus = false },
    )
}

@Composable
private fun PasskeyField(
    creationDateTime: Text,
    canRemovePasskey: Boolean,
    loginItemTypeHandlers: VaultAddEditLoginTypeHandlers,
    modifier: Modifier = Modifier,
) {
    BitwardenTextField(
        label = stringResource(id = R.string.passkey),
        value = creationDateTime.invoke(),
        onValueChange = { },
        readOnly = true,
        singleLine = true,
        actions = {
            if (canRemovePasskey) {
                BitwardenStandardIconButton(
                    vectorIconRes = R.drawable.ic_minus,
                    contentDescription = stringResource(id = R.string.remove_passkey),
                    onClick = loginItemTypeHandlers.onClearFido2CredentialClick,
                    modifier = Modifier.testTag(tag = "RemovePasskeyButton"),
                )
            }
        },
        cardStyle = CardStyle.Full,
        modifier = modifier,
    )
}
