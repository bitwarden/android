package com.x8bit.bitwarden.ui.vault.feature.item

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenHiddenPasswordField
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.x8bit.bitwarden.ui.platform.components.field.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.header.BitwardenListHeaderText
import com.x8bit.bitwarden.ui.platform.components.indicator.BitwardenCircularCountdownIndicator
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.components.model.TooltipData
import com.x8bit.bitwarden.ui.platform.components.text.BitwardenClickableText
import com.x8bit.bitwarden.ui.platform.components.text.BitwardenHyperTextLink
import com.x8bit.bitwarden.ui.vault.feature.item.component.CustomField
import com.x8bit.bitwarden.ui.vault.feature.item.component.VaultItemUpdateText
import com.x8bit.bitwarden.ui.vault.feature.item.component.itemHeader
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultCommonItemTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.item.handlers.VaultLoginItemTypeHandlers
import com.x8bit.bitwarden.ui.vault.feature.item.model.TotpCodeItemData

private const val AUTH_CODE_SPACING_INTERVAL = 3

/**
 * The top level content UI state for the [VaultItemScreen] when viewing a Login cipher.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun VaultItemLoginContent(
    commonState: VaultItemState.ViewState.Content.Common,
    loginItemState: VaultItemState.ViewState.Content.ItemType.Login,
    vaultCommonItemTypeHandlers: VaultCommonItemTypeHandlers,
    vaultLoginItemTypeHandlers: VaultLoginItemTypeHandlers,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable { mutableStateOf(value = false) }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
    ) {
        item {
            Spacer(Modifier.height(height = 12.dp))
        }
        itemHeader(
            value = commonState.name,
            isFavorite = commonState.favorite,
            iconData = commonState.iconData,
            relatedLocations = commonState.relatedLocations,
            iconTestTag = "LoginItemNameIcon",
            textFieldTestTag = "LoginItemNameEntry",
            isExpanded = isExpanded,
            onExpandClick = { isExpanded = !isExpanded },
            applyIconBackground = commonState.iconData is IconData.Local,
        )
        if (loginItemState.hasLoginCredentials) {
            item(key = "loginCredentialsHeader") {
                Spacer(modifier = Modifier.height(height = 16.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.login_credentials),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                )
                Spacer(modifier = Modifier.height(height = 8.dp))
            }
        }

        loginItemState.username?.let { username ->
            item(key = "username") {
                UsernameField(
                    username = username,
                    onCopyUsernameClick = vaultLoginItemTypeHandlers.onCopyUsernameClick,
                    cardStyle = loginItemState
                        .passwordData
                        ?.let { CardStyle.Top(dividerPadding = 0.dp) }
                        ?: CardStyle.Full,
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth()
                        .animateItem(),
                )
            }
        }

        loginItemState.passwordData?.let { passwordData ->
            item(key = "passwordData") {
                PasswordField(
                    passwordData = passwordData,
                    onShowPasswordClick = vaultLoginItemTypeHandlers.onShowPasswordClick,
                    onCheckForBreachClick = vaultLoginItemTypeHandlers.onCheckForBreachClick,
                    onCopyPasswordClick = vaultLoginItemTypeHandlers.onCopyPasswordClick,
                    cardStyle = loginItemState
                        .username
                        ?.let { CardStyle.Bottom }
                        ?: CardStyle.Full,
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth()
                        .animateItem(),
                )
            }
        }

        loginItemState.fido2CredentialCreationDateText?.let { creationDate ->
            item(key = "creationDate") {
                Spacer(modifier = Modifier.height(8.dp))
                Fido2CredentialField(
                    creationDate = creationDate(),
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth()
                        .animateItem(),
                )
            }
        }

        loginItemState.totpCodeItemData?.let { totpCodeItemData ->
            item(key = "totpCode") {
                Spacer(modifier = Modifier.height(8.dp))
                TotpField(
                    totpCodeItemData = totpCodeItemData,
                    enabled = loginItemState.canViewTotpCode,
                    onCopyTotpClick = vaultLoginItemTypeHandlers.onCopyTotpCodeClick,
                    onAuthenticatorHelpToolTipClick = vaultLoginItemTypeHandlers
                        .onAuthenticatorHelpToolTipClick,
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth()
                        .animateItem(),
                )
            }
        }

        loginItemState.uris.takeUnless { it.isEmpty() }?.let { uris ->
            item(key = "urisHeader") {
                Spacer(modifier = Modifier.height(height = 16.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.autofill_options),
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                )
                Spacer(modifier = Modifier.height(height = 8.dp))
            }

            itemsIndexed(
                items = uris,
                key = { index, _ -> "uri_$index" },
            ) { index, uriData ->
                UriField(
                    uriData = uriData,
                    onCopyUriClick = vaultLoginItemTypeHandlers.onCopyUriClick,
                    onLaunchUriClick = vaultLoginItemTypeHandlers.onLaunchUriClick,
                    cardStyle = uris.toListItemCardStyle(index = index, dividerPadding = 0.dp),
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth()
                        .animateItem(),
                )
            }
        }

        commonState.notes?.let { notes ->
            item(key = "notes") {
                Spacer(modifier = Modifier.height(height = 16.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.additional_options),
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .animateItem(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                NotesField(
                    notes = notes,
                    onCopyAction = vaultCommonItemTypeHandlers.onCopyNotesClick,
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .fillMaxWidth()
                        .animateItem(),
                )
            }
        }

        commonState.customFields.takeUnless { it.isEmpty() }?.let { customFields ->
            item(key = "customFieldsHeader") {
                Spacer(modifier = Modifier.height(height = 16.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.custom_fields),
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
                        .standardHorizontalMargin()
                        .fillMaxWidth()
                        .animateItem(),
                )
            }
        }

        commonState.attachments.takeUnless { it?.isEmpty() == true }?.let { attachments ->
            item(key = "attachmentsHeader") {
                Spacer(modifier = Modifier.height(height = 16.dp))
                BitwardenListHeaderText(
                    label = stringResource(id = R.string.attachments),
                    modifier = Modifier
                        .standardHorizontalMargin()
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
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
                        .standardHorizontalMargin()
                        .fillMaxWidth()
                        .animateItem(),
                    attachmentItem = attachmentItem,
                    cardStyle = attachments.toListItemCardStyle(index = index),
                    onAttachmentDownloadClick = vaultCommonItemTypeHandlers
                        .onAttachmentDownloadClick,
                )
            }
        }

        item(key = "lastUpdated") {
            Spacer(modifier = Modifier.height(16.dp))
            VaultItemUpdateText(
                header = "${stringResource(id = R.string.date_updated)}: ",
                text = commonState.lastUpdated,
                modifier = Modifier
                    .standardHorizontalMargin()
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
                    .animateItem(),
            )
        }

        loginItemState.passwordRevisionDate?.let { revisionDate ->
            item(key = "revisionDate") {
                Spacer(modifier = Modifier.height(height = 4.dp))
                VaultItemUpdateText(
                    header = "${stringResource(id = R.string.date_password_updated)}: ",
                    text = revisionDate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin()
                        .padding(horizontal = 12.dp)
                        .animateItem(),
                )
            }
        }
        commonState.passwordHistoryCount?.let { passwordHistoryCount ->
            item(key = "passwordHistoryCount") {
                Spacer(modifier = Modifier.height(height = 4.dp))
                BitwardenHyperTextLink(
                    annotatedResId = R.string.password_history_count,
                    args = arrayOf(passwordHistoryCount.toString()),
                    annotationKey = "passwordHistory",
                    accessibilityString = stringResource(id = R.string.password_history),
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

@Composable
private fun Fido2CredentialField(
    creationDate: String,
    modifier: Modifier = Modifier,
) {
    BitwardenTextField(
        label = stringResource(id = R.string.passkey),
        value = creationDate,
        onValueChange = { },
        readOnly = true,
        singleLine = true,
        cardStyle = CardStyle.Full,
        modifier = modifier,
    )
}

@Composable
private fun NotesField(
    notes: String,
    onCopyAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BitwardenTextField(
        label = stringResource(id = R.string.notes),
        value = notes,
        onValueChange = { },
        readOnly = true,
        singleLine = false,
        actions = {
            BitwardenStandardIconButton(
                vectorIconRes = R.drawable.ic_copy,
                contentDescription = stringResource(id = R.string.copy_notes),
                onClick = onCopyAction,
                modifier = Modifier.testTag(tag = "CipherNotesCopyButton"),
            )
        },
        textFieldTestTag = "CipherNotesLabel",
        cardStyle = CardStyle.Full,
        modifier = modifier,
    )
}

@Composable
private fun PasswordField(
    passwordData: VaultItemState.ViewState.Content.ItemType.Login.PasswordData,
    onShowPasswordClick: (Boolean) -> Unit,
    onCheckForBreachClick: () -> Unit,
    onCopyPasswordClick: () -> Unit,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
) {
    if (passwordData.canViewPassword) {
        BitwardenPasswordField(
            label = stringResource(id = R.string.password),
            value = passwordData.password,
            showPasswordChange = { onShowPasswordClick(it) },
            showPassword = passwordData.isVisible,
            onValueChange = { },
            readOnly = true,
            singleLine = false,
            actions = {
                BitwardenStandardIconButton(
                    vectorIconRes = R.drawable.ic_copy,
                    contentDescription = stringResource(id = R.string.copy_password),
                    onClick = onCopyPasswordClick,
                    modifier = Modifier.testTag(tag = "LoginCopyPasswordButton"),
                )
            },
            supportingContentPadding = PaddingValues(),
            supportingContent = {
                BitwardenClickableText(
                    label = stringResource(id = R.string.check_password_for_data_breaches),
                    style = BitwardenTheme.typography.labelMedium,
                    onClick = onCheckForBreachClick,
                    innerPadding = PaddingValues(all = 16.dp),
                    cornerSize = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(tag = "LoginCheckPasswordButton"),
                )
            },
            showPasswordTestTag = "LoginViewPasswordButton",
            passwordFieldTestTag = "LoginPasswordEntry",
            cardStyle = cardStyle,
            modifier = modifier,
        )
    } else {
        BitwardenHiddenPasswordField(
            label = stringResource(id = R.string.password),
            value = passwordData.password,
            passwordFieldTestTag = "LoginPasswordEntry",
            cardStyle = cardStyle,
            modifier = modifier,
        )
    }
}

@Composable
private fun TotpField(
    totpCodeItemData: TotpCodeItemData,
    enabled: Boolean,
    onCopyTotpClick: () -> Unit,
    onAuthenticatorHelpToolTipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (enabled) {
        BitwardenTextField(
            label = stringResource(id = R.string.authenticator_key),
            value = totpCodeItemData.verificationCode
                .chunked(AUTH_CODE_SPACING_INTERVAL)
                .joinToString(" "),
            onValueChange = { },
            textStyle = BitwardenTheme.typography.sensitiveInfoSmall,
            readOnly = true,
            singleLine = true,
            tooltip = TooltipData(
                onClick = onAuthenticatorHelpToolTipClick,
                contentDescription = stringResource(id = R.string.authenticator_key_help),
            ),
            actions = {
                BitwardenCircularCountdownIndicator(
                    timeLeftSeconds = totpCodeItemData.timeLeftSeconds,
                    periodSeconds = totpCodeItemData.periodSeconds,
                )
                BitwardenStandardIconButton(
                    vectorIconRes = R.drawable.ic_copy,
                    contentDescription = stringResource(id = R.string.copy_totp),
                    onClick = onCopyTotpClick,
                    modifier = Modifier.testTag(tag = "LoginCopyTotpButton"),
                )
            },
            textFieldTestTag = "LoginTotpEntry",
            cardStyle = CardStyle.Full,
            modifier = modifier,
        )
    } else {
        BitwardenTextField(
            label = stringResource(id = R.string.authenticator_key),
            value = "",
            tooltip = TooltipData(
                onClick = onAuthenticatorHelpToolTipClick,
                contentDescription = stringResource(id = R.string.authenticator_key_help),
            ),
            supportingText = stringResource(id = R.string.premium_subscription_required),
            enabled = false,
            singleLine = false,
            onValueChange = { },
            readOnly = true,
            cardStyle = CardStyle.Full,
            modifier = modifier,
        )
    }
}

@Composable
private fun UriField(
    uriData: VaultItemState.ViewState.Content.ItemType.Login.UriData,
    onCopyUriClick: (String) -> Unit,
    onLaunchUriClick: (String) -> Unit,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
) {
    BitwardenTextField(
        label = stringResource(id = R.string.website_uri),
        value = uriData.uri,
        onValueChange = { },
        readOnly = true,
        singleLine = false,
        actions = {
            if (uriData.isLaunchable) {
                BitwardenStandardIconButton(
                    vectorIconRes = R.drawable.ic_external_link,
                    contentDescription = stringResource(id = R.string.launch),
                    onClick = { onLaunchUriClick(uriData.uri) },
                    modifier = Modifier.testTag(tag = "LoginLaunchUriButton"),
                )
            }
            if (uriData.isCopyable) {
                BitwardenStandardIconButton(
                    vectorIconRes = R.drawable.ic_copy,
                    contentDescription = stringResource(id = R.string.copy),
                    onClick = { onCopyUriClick(uriData.uri) },
                    modifier = Modifier.testTag(tag = "LoginCopyUriButton"),
                )
            }
        },
        textFieldTestTag = "LoginUriEntry",
        cardStyle = cardStyle,
        modifier = modifier,
    )
}

@Composable
private fun UsernameField(
    username: String,
    onCopyUsernameClick: () -> Unit,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
) {
    BitwardenTextField(
        label = stringResource(id = R.string.username),
        value = username,
        onValueChange = { },
        readOnly = true,
        singleLine = false,
        actions = {
            BitwardenStandardIconButton(
                vectorIconRes = R.drawable.ic_copy,
                contentDescription = stringResource(id = R.string.copy_username),
                onClick = onCopyUsernameClick,
                modifier = Modifier.testTag(tag = "LoginCopyUsernameButton"),
            )
        },
        textFieldTestTag = "LoginUsernameEntry",
        cardStyle = cardStyle,
        modifier = modifier,
    )
}
