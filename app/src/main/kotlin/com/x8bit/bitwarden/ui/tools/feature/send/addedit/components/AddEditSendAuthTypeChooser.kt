package com.x8bit.bitwarden.ui.tools.feature.send.addedit.components

import androidx.compose.foundation.layout.Column
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
import com.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.text.BitwardenClickableText
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendAuthType
import kotlinx.collections.immutable.toImmutableList

/**
 * Displays UX for choosing authentication type for a send.
 *
 * @param onAuthTypeSelect Callback invoked when the authentication type is selected.
 * @param onPasswordChange Callback invoked when the password value changes
 * (only relevant for [SendAuthType.PASSWORD]).
 * @param onEmailValueChange Callback invoked when the email list changes
 * (only relevant for [SendAuthType.EMAIL]).
 * @param onOpenPasswordGeneratorClick Callback invoked when the Generator button is clicked
 * @param onPasswordCopyClick Callback invoked when the Copy button is clicked
 * @param password The current password value (only relevant for [SendAuthType.PASSWORD]).
 * @param emails The list of emails (only relevant for [SendAuthType.EMAIL]).
 * @param isEnabled Whether the chooser is enabled.
 * @param sendRestrictionPolicy if sends are restricted by a policy.
 * @param modifier Modifier for the composable.
 */
@Suppress("LongMethod")
@Composable
fun AddEditSendAuthTypeChooser(
    onAuthTypeSelect: (SendAuthType) -> Unit,
    onPasswordChange: (String) -> Unit,
    onEmailValueChange: (String, Int) -> Unit,
    onAddNewEmailClick: () -> Unit,
    onRemoveEmailClick: (Int) -> Unit,
    onOpenPasswordGeneratorClick: () -> Unit,
    onPasswordCopyClick: (String) -> Unit,
    onShowDialog: () -> Unit,
    password: String,
    emails: List<String>,
    isEnabled: Boolean,
    isPremium: Boolean,
    hasPassword: Boolean,
    sendRestrictionPolicy: Boolean,
    modifier: Modifier = Modifier,
) {
    val options = SendAuthType.entries
        .filter { authType ->
            authType != SendAuthType.EMAIL || isPremium
        }
        .associateWith { it.text() }
    var selectedOption: SendAuthType by rememberSaveable {
        mutableStateOf(
            value = when {
                hasPassword -> SendAuthType.PASSWORD
                emails.isNotEmpty() -> SendAuthType.EMAIL
                else -> SendAuthType.NONE
            },
        )
    }
    Column(modifier = modifier) {
        BitwardenMultiSelectButton(
            label = stringResource(id = BitwardenString.who_can_view),
            isEnabled = isEnabled,
            options = options.values.toImmutableList(),
            selectedOption = selectedOption.text(),
            onOptionSelected = { selected ->
                selectedOption = options.entries.first { it.value == selected }.key
                onAuthTypeSelect(selectedOption)
            },
            supportingText = selectedOption.supportingTextRes?.let { stringResource(id = it) },
            insets = PaddingValues(top = 6.dp, bottom = 4.dp),
            cardStyle = CardStyle.Top(),
        )

        when (selectedOption) {
            SendAuthType.EMAIL -> {
                SpecificPeopleEmailContent(
                    emails = emails,
                    onEmailValueChange = onEmailValueChange,
                    onAddNewEmailClick = onAddNewEmailClick,
                    onRemoveEmailClick = onRemoveEmailClick,
                )
            }

            SendAuthType.PASSWORD -> {
                BitwardenPasswordField(
                    label = stringResource(id = BitwardenString.password),
                    value = password,
                    onValueChange = onPasswordChange,
                    cardStyle = CardStyle.Bottom,
                    passwordFieldTestTag = "SendPasswordEntry",
                    readOnly = sendRestrictionPolicy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    BitwardenStandardIconButton(
                        vectorIconRes = BitwardenDrawable.ic_generate,
                        contentDescription = stringResource(id = BitwardenString.generate_password),
                        onClick = {
                            if (password.isEmpty()) {
                                onOpenPasswordGeneratorClick()
                            } else {
                                onShowDialog()
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

            else -> Unit
        }
    }
}

@Composable
private fun SpecificPeopleEmailContent(
    emails: List<String>,
    onEmailValueChange: (String, Int) -> Unit,
    onAddNewEmailClick: () -> Unit,
    onRemoveEmailClick: (Int) -> Unit,
) {
    val emailsToDisplay = emails.ifEmpty { listOf("") }
    emailsToDisplay.forEachIndexed { index, email ->
        BitwardenTextField(
            label = stringResource(id = BitwardenString.email),
            value = email,
            onValueChange = { onEmailValueChange(it, index) },
            singleLine = false,
            keyboardType = KeyboardType.Email,
            actions = {
                if (index > 0 || email.isNotEmpty()) {
                    BitwardenStandardIconButton(
                        vectorIconRes = BitwardenDrawable.ic_delete,
                        contentDescription = stringResource(id = BitwardenString.delete),
                        contentColor = BitwardenTheme.colorScheme.status.error,
                        onClick = {
                            onRemoveEmailClick(index)
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
        onClick = {
            onAddNewEmailClick()
        },
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
