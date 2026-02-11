package com.x8bit.bitwarden.ui.tools.feature.send.addedit.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Displays UX for choosing authentication type for a send.
 *
 * @param selectedAuthType The currently selected authentication type from the ViewModel.
 * @param onAuthTypeSelect Callback invoked when the authentication type is selected.
 * @param onPasswordChange Callback invoked when the password value changes
 * (only relevant for [SendAuthType.PASSWORD]).
 * @param onEmailValueChange Callback invoked when the email list changes
 * (only relevant for [SendAuthType.EMAIL]).
 * @param password The current password value (only relevant for [SendAuthType.PASSWORD]).
 * @param emails The list of emails (only relevant for [SendAuthType.EMAIL]).
 * @param isEnabled Whether the chooser is enabled.
 * @param modifier Modifier for the composable.
 */
@Composable
fun AddEditSendAuthTypeChooser(
    selectedAuthType: SendAuthType,
    onAuthTypeSelect: (SendAuthType) -> Unit,
    onPasswordChange: (String) -> Unit,
    onEmailValueChange: (String, Int) -> Unit,
    onAddNewEmailClick: () -> Unit,
    onRemoveEmailClick: (Int) -> Unit,
    password: String,
    emails: ImmutableList<String>,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val options = SendAuthType.entries.associateWith { it.text() }
    Column(modifier = modifier) {
        BitwardenMultiSelectButton(
            label = stringResource(id = BitwardenString.who_can_view),
            isEnabled = isEnabled,
            options = options.values.toImmutableList(),
            selectedOption = selectedAuthType.text(),
            onOptionSelected = { selected ->
                val newAuthType = options.entries.first { it.value == selected }.key
                onAuthTypeSelect(newAuthType)
            },
            supportingText = selectedAuthType.supportingText?.invoke(),
            insets = PaddingValues(top = 6.dp, bottom = 4.dp),
            cardStyle = if (selectedAuthType == SendAuthType.NONE) {
                CardStyle.Full
            } else {
                CardStyle.Top()
            },
        )

        when (selectedAuthType) {
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
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            else -> Unit
        }
    }
}

@Composable
private fun ColumnScope.SpecificPeopleEmailContent(
    emails: ImmutableList<String>,
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
