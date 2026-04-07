package com.x8bit.bitwarden.ui.tools.feature.send.addedit.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.text.BitwardenClickableText
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.model.AuthEmail
import com.x8bit.bitwarden.ui.tools.feature.send.addedit.model.SendAuth
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

/**
 * Displays UX for choosing authentication type for a send.
 *
 * @param sendAuth The current authentication configuration.
 * @param onAuthTypeSelect Callback invoked when the authentication type is selected.
 * @param onPasswordChange Callback invoked when the password value changes
 * (only relevant for [SendAuth.Password]).
 * @param onEmailValueChange Callback invoked when an email value changes
 * (only relevant for [SendAuth.Email]).
 * @param onAddNewEmailClick Callback invoked when adding a new email.
 * @param onRemoveEmailClick Callback invoked when removing an email.
 * @param password The current password value (only relevant for [SendAuth.Password]).
 * @param onOpenPasswordGeneratorClick Callback invoked when the Generator button is clicked
 * @param onPasswordCopyClick Callback invoked when the Copy button is clicked
 * @param isEnabled Whether the chooser is enabled.
 * @param isSendsRestrictedByPolicy if sends are restricted by a policy.
 * @param modifier Modifier for the composable.
 */
@Suppress("LongMethod")
@Composable
fun AddEditSendAuthTypeChooser(
    sendAuth: SendAuth,
    onAuthTypeSelect: (SendAuth) -> Unit,
    onPasswordChange: (String) -> Unit,
    onEmailValueChange: (AuthEmail) -> Unit,
    onAddNewEmailClick: () -> Unit,
    onRemoveEmailClick: (AuthEmail) -> Unit,
    onOpenPasswordGeneratorClick: () -> Unit,
    onPasswordCopyClick: (String) -> Unit,
    password: String,
    isEnabled: Boolean,
    isSendsRestrictedByPolicy: Boolean,
    modifier: Modifier = Modifier,
) {
    var shouldShowDialog by rememberSaveable { mutableStateOf(false) }
    // Map option texts to their corresponding SendAuth factory functions
    val textToNoneAuth = stringResource(id = BitwardenString.anyone_with_the_link)
    val textToEmailAuth = stringResource(id = BitwardenString.specific_people)
    val textToPasswordAuth = stringResource(id = BitwardenString.anyone_with_password)

    val options = listOf(
        textToNoneAuth,
        textToEmailAuth,
        textToPasswordAuth,
    )

    Column(modifier = modifier) {
        BitwardenMultiSelectButton(
            label = stringResource(id = BitwardenString.who_can_view),
            isEnabled = isEnabled,
            options = options.toPersistentList(),
            selectedOption = sendAuth.text(),
            onOptionSelected = { selected ->
                val newAuth = when (selected) {
                    textToNoneAuth -> SendAuth.None
                    textToEmailAuth -> SendAuth.Email()
                    textToPasswordAuth -> SendAuth.Password
                    else -> SendAuth.None // fallback
                }
                onAuthTypeSelect(newAuth)
            },
            supportingText = sendAuth.supportingText?.let { it() },
            insets = PaddingValues(top = 6.dp, bottom = 4.dp),
            cardStyle = if (sendAuth is SendAuth.None) {
                CardStyle.Full
            } else {
                CardStyle.Top()
            },
        )

        when (sendAuth) {
            is SendAuth.Email -> {
                SpecificPeopleEmailContent(
                    emails = sendAuth.emails,
                    onEmailValueChange = onEmailValueChange,
                    onAddNewEmailClick = onAddNewEmailClick,
                    onRemoveEmailClick = onRemoveEmailClick,
                )
            }

            is SendAuth.Password -> {
                BitwardenPasswordField(
                    label = stringResource(id = BitwardenString.password),
                    value = password,
                    onValueChange = onPasswordChange,
                    cardStyle = CardStyle.Bottom,
                    passwordFieldTestTag = "SendPasswordEntry",
                    readOnly = isSendsRestrictedByPolicy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    BitwardenStandardIconButton(
                        vectorIconRes = BitwardenDrawable.ic_generate,
                        contentDescription = stringResource(id = BitwardenString.generate_password),
                        onClick = {
                            if (password.isEmpty()) {
                                onOpenPasswordGeneratorClick()
                            } else {
                                shouldShowDialog = true
                            }
                        },
                        modifier = Modifier.testTag(tag = "RegeneratePasswordButton"),
                    )
                    BitwardenStandardIconButton(
                        vectorIconRes = BitwardenDrawable.ic_copy,
                        contentDescription = stringResource(id = BitwardenString.copy_password),
                        isEnabled = password.isNotEmpty(),
                        onClick = {
                            onPasswordCopyClick(password)
                        },
                        modifier = Modifier.testTag(tag = "CopyPasswordButton"),
                    )
                }
            }

            is SendAuth.None -> Unit
        }
    }

    if (shouldShowDialog) {
        BitwardenTwoButtonDialog(
            title = stringResource(id = BitwardenString.password),
            message = stringResource(id = BitwardenString.password_override_alert),
            confirmButtonText = stringResource(id = BitwardenString.yes),
            dismissButtonText = stringResource(id = BitwardenString.no),
            onConfirmClick = {
                shouldShowDialog = false
                onOpenPasswordGeneratorClick()
            },
            onDismissClick = { shouldShowDialog = false },
            onDismissRequest = { shouldShowDialog = false },
        )
    }
}

@Composable
private fun ColumnScope.SpecificPeopleEmailContent(
    emails: ImmutableList<AuthEmail>,
    onEmailValueChange: (AuthEmail) -> Unit,
    onAddNewEmailClick: () -> Unit,
    onRemoveEmailClick: (AuthEmail) -> Unit,
) {
    emails.forEachIndexed { index, authEmail ->
        BitwardenTextField(
            label = stringResource(id = BitwardenString.email),
            value = authEmail.value,
            onValueChange = { onEmailValueChange(authEmail.copy(value = it)) },
            singleLine = false,
            keyboardType = KeyboardType.Email,
            actions = {
                if (index > 0 || authEmail.value.isNotEmpty()) {
                    BitwardenStandardIconButton(
                        vectorIconRes = BitwardenDrawable.ic_delete,
                        contentDescription = stringResource(id = BitwardenString.delete),
                        contentColor = BitwardenTheme.colorScheme.status.error,
                        onClick = {
                            onRemoveEmailClick(authEmail)
                        },
                    )
                }
            },
            textFieldTestTag = "SendEmailEntry",
            cardStyle = CardStyle.Middle(),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    BitwardenClickableText(
        label = stringResource(id = BitwardenString.add_email),
        onClick = onAddNewEmailClick,
        leadingIcon = painterResource(id = BitwardenDrawable.ic_plus_small),
        style = BitwardenTheme.typography.labelMedium,
        innerPadding = PaddingValues(all = 16.dp),
        cornerSize = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag = "AddEditSendAddEmailButton")
            .cardStyle(cardStyle = CardStyle.Bottom, paddingVertical = 0.dp),
    )
}
