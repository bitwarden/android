package com.bitwarden.ui.platform.components.field

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.base.util.nullableTestTag
import com.bitwarden.ui.platform.base.util.tabNavigation
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.bitwarden.ui.platform.components.field.color.bitwardenTextFieldColors
import com.bitwarden.ui.platform.components.field.model.TextToolbarType
import com.bitwarden.ui.platform.components.field.toolbar.BitwardenCutCopyTextToolbar
import com.bitwarden.ui.platform.components.field.toolbar.BitwardenEmptyTextToolbar
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.model.TooltipData
import com.bitwarden.ui.platform.components.row.BitwardenRowOfActions
import com.bitwarden.ui.platform.components.support.BitwardenSupportingContent
import com.bitwarden.ui.platform.components.util.nonLetterColorVisualTransformation
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a Bitwarden-styled password field that hoists show/hide password state to the caller.
 *
 * See overloaded [BitwardenPasswordField] for self managed show/hide state.
 *
 * @param label Label for the text field.
 * @param value Current next on the text field.
 * @param showPassword Whether or not password should be shown.
 * @param showPasswordChange Lambda that is called when user request show/hide be toggled.
 * @param onValueChange Callback that is triggered when the password changes.
 * @param supportingContent An optional supporting content that will appear below the text input.
 * @param supportingContentPadding The padding to be placed on the [supportingContent].
 * @param modifier Modifier for the composable.
 * @param tooltip the optional tooltip to be displayed in the label.
 * @param readOnly `true` if the input should be read-only and not accept user interactions.
 * @param singleLine when `true`, this text field becomes a single line that horizontally scrolls
 * instead of wrapping onto multiple lines.
 * @param showPasswordTestTag The test tag to be used on the show password button (testing tool).
 * @param autoFocus When set to true, the view will request focus after the first recomposition.
 * Setting this to true on multiple fields at once may have unexpected consequences.
 * @param keyboardType The type of keyboard the user has access to when inputting values into
 * the password field.
 * @param imeAction the preferred IME action for the keyboard to have.
 * @param keyboardActions the callbacks of keyboard actions.
 * @param textToolbarType The type of [TextToolbar] to use on the text field.
 * @param passwordFieldTestTag The optional test tag associated with the inner password field.
 * @param cardStyle Indicates the type of card style to be applied.
 * @param actionsPadding Padding to be applied to the [actions] block.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * in the app bar's trailing side. This lambda extends [RowScope], allowing flexibility in
 * defining the layout of the actions.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun BitwardenPasswordField(
    label: String?,
    value: String,
    showPassword: Boolean,
    showPasswordChange: (Boolean) -> Unit,
    onValueChange: (String) -> Unit,
    supportingContent: @Composable (ColumnScope.() -> Unit)?,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
    tooltip: TooltipData? = null,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    showPasswordTestTag: String? = null,
    supportingContentPadding: PaddingValues = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
    autoFocus: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Password,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    textToolbarType: TextToolbarType = TextToolbarType.DEFAULT,
    passwordFieldTestTag: String? = null,
    actionsPadding: PaddingValues = PaddingValues(end = 4.dp),
    actions: @Composable (RowScope.() -> Unit)? = null,
) {
    val focusRequester = remember { FocusRequester() }
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(text = value)) }
    val textFieldValue = textFieldValueState.copy(text = value)
    SideEffect {
        if (textFieldValue.selection != textFieldValueState.selection ||
            textFieldValue.composition != textFieldValueState.composition
        ) {
            textFieldValueState = textFieldValue
        }
    }
    val textToolbar = when (textToolbarType) {
        TextToolbarType.DEFAULT -> BitwardenCutCopyTextToolbar(
            value = textFieldValue,
            onValueChange = onValueChange,
            defaultTextToolbar = LocalTextToolbar.current,
            clipboardManager = LocalClipboard.current.nativeClipboard,
            focusManager = LocalFocusManager.current,
        )

        TextToolbarType.NONE -> BitwardenEmptyTextToolbar
    }
    var lastTextValue by remember(value) { mutableStateOf(value = value) }
    CompositionLocalProvider(value = LocalTextToolbar provides textToolbar) {
        Column(
            modifier = modifier
                .defaultMinSize(minHeight = 60.dp)
                .cardStyle(
                    cardStyle = cardStyle,
                    paddingTop = 6.dp,
                    paddingBottom = 0.dp,
                )
                .tabNavigation()
                .focusRequester(focusRequester = focusRequester),
        ) {
            var focused by remember { mutableStateOf(value = false) }
            TextField(
                colors = bitwardenTextFieldColors(),
                textStyle = BitwardenTheme.typography.sensitiveInfoSmall,
                label = label?.let {
                    {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = it)
                            tooltip?.let { tooltipData ->
                                val targetSize = if (textFieldValue.text.isEmpty() || focused) {
                                    16.dp
                                } else {
                                    12.dp
                                }
                                val size by animateDpAsState(
                                    targetValue = targetSize,
                                    label = "${tooltipData.contentDescription}_animation",
                                )
                                Spacer(modifier = Modifier.width(width = 8.dp))
                                BitwardenStandardIconButton(
                                    vectorIconRes = BitwardenDrawable.ic_question_circle_small,
                                    contentDescription = tooltipData.contentDescription,
                                    onClick = tooltipData.onClick,
                                    contentColor = BitwardenTheme.colorScheme.icon.secondary,
                                    modifier = Modifier.size(size),
                                )
                            }
                        }
                    }
                },
                value = textFieldValue,
                onValueChange = {
                    textFieldValueState = it
                    val stringChangedSinceLastInvocation = lastTextValue != it.text
                    lastTextValue = it.text
                    if (stringChangedSinceLastInvocation) {
                        onValueChange(it.text)
                    }
                },
                visualTransformation = when {
                    !showPassword -> PasswordVisualTransformation()
                    readOnly -> nonLetterColorVisualTransformation()
                    else -> VisualTransformation.None
                },
                singleLine = singleLine,
                readOnly = readOnly,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = imeAction,
                ),
                keyboardActions = keyboardActions,
                trailingIcon = {
                    BitwardenRowOfActions(
                        modifier = Modifier.padding(paddingValues = actionsPadding),
                        actions = {
                            BitwardenStandardIconButton(
                                modifier = Modifier.nullableTestTag(tag = showPasswordTestTag),
                                vectorIconRes = if (showPassword) {
                                    BitwardenDrawable.ic_eye_slash
                                } else {
                                    BitwardenDrawable.ic_eye
                                },
                                contentDescription = stringResource(
                                    id = if (showPassword) {
                                        BitwardenString.hide
                                    } else {
                                        BitwardenString.show
                                    },
                                ),
                                onClick = { showPasswordChange.invoke(!showPassword) },
                            )
                            actions?.invoke(this)
                        },
                    )
                },
                modifier = Modifier
                    .nullableTestTag(tag = passwordFieldTestTag)
                    .fillMaxWidth()
                    .onFocusChanged { focusState -> focused = focusState.isFocused },
            )
            supportingContent
                ?.let { content ->
                    Spacer(modifier = Modifier.height(height = 6.dp))
                    BitwardenHorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                    )
                    BitwardenSupportingContent(
                        cardStyle = null,
                        insets = supportingContentPadding,
                        content = content,
                    )
                }
                ?: Spacer(modifier = Modifier.height(height = 6.dp))
        }
    }
    if (autoFocus) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}

/**
 * Represents a Bitwarden-styled password field that hoists show/hide password state to the caller.
 *
 * See overloaded [BitwardenPasswordField] for self managed show/hide state.
 *
 * @param label Label for the text field.
 * @param value Current next on the text field.
 * @param showPassword Whether or not password should be shown.
 * @param showPasswordChange Lambda that is called when user request show/hide be toggled.
 * @param onValueChange Callback that is triggered when the password changes.
 * @param modifier Modifier for the composable.
 * @param tooltip the optional tooltip to be displayed in the label.
 * @param readOnly `true` if the input should be read-only and not accept user interactions.
 * @param singleLine when `true`, this text field becomes a single line that horizontally scrolls
 * instead of wrapping onto multiple lines.
 * @param supportingText An optional supporting text that will appear below the text input.
 * @param showPasswordTestTag The test tag to be used on the show password button (testing tool).
 * @param autoFocus When set to true, the view will request focus after the first recomposition.
 * Setting this to true on multiple fields at once may have unexpected consequences.
 * @param keyboardType The type of keyboard the user has access to when inputting values into
 * the password field.
 * @param imeAction the preferred IME action for the keyboard to have.
 * @param keyboardActions the callbacks of keyboard actions.
 * @param textToolbarType The type of [TextToolbar] to use on the text field.
 * @param passwordFieldTestTag The optional test tag associated with the inner password field.
 * @param cardStyle Indicates the type of card style to be applied.
 * @param actionsPadding Padding to be applied to the [actions] block.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * in the app bar's trailing side. This lambda extends [RowScope], allowing flexibility in
 * defining the layout of the actions.
 */
@Composable
fun BitwardenPasswordField(
    label: String?,
    value: String,
    showPassword: Boolean,
    showPasswordChange: (Boolean) -> Unit,
    onValueChange: (String) -> Unit,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
    tooltip: TooltipData? = null,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    supportingText: String? = null,
    showPasswordTestTag: String? = null,
    autoFocus: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Password,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    textToolbarType: TextToolbarType = TextToolbarType.DEFAULT,
    passwordFieldTestTag: String? = null,
    actionsPadding: PaddingValues = PaddingValues(end = 4.dp),
    actions: @Composable (RowScope.() -> Unit)? = null,
) {
    BitwardenPasswordField(
        label = label,
        value = value,
        showPassword = showPassword,
        showPasswordChange = showPasswordChange,
        showPasswordTestTag = showPasswordTestTag,
        onValueChange = onValueChange,
        modifier = modifier,
        tooltip = tooltip,
        readOnly = readOnly,
        singleLine = singleLine,
        supportingContent = supportingText?.let {
            {
                Text(
                    text = it,
                    style = BitwardenTheme.typography.bodySmall,
                    color = BitwardenTheme.colorScheme.text.secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        autoFocus = autoFocus,
        keyboardType = keyboardType,
        imeAction = imeAction,
        keyboardActions = keyboardActions,
        textToolbarType = textToolbarType,
        passwordFieldTestTag = passwordFieldTestTag,
        cardStyle = cardStyle,
        actionsPadding = actionsPadding,
        actions = actions,
    )
}

/**
 * Represents a Bitwarden-styled password field that hoists show/hide password state to the caller.
 *
 * See overloaded [BitwardenPasswordField] for self managed show/hide state.
 *
 * @param label Label for the text field.
 * @param value Current next on the text field.
 * @param onValueChange Callback that is triggered when the password changes.
 * @param modifier Modifier for the composable.
 * @param tooltip the optional tooltip to be displayed in the label.
 * @param initialShowPassword The initial state of the show/hide password control. A value of
 * `false` (the default) indicates that that password should begin in the hidden state.
 * @param readOnly `true` if the input should be read-only and not accept user interactions.
 * @param singleLine when `true`, this text field becomes a single line that horizontally scrolls
 * instead of wrapping onto multiple lines.
 * @param supportingContent An optional supporting content that will appear below the text input.
 * @param supportingContentPadding The padding to be placed on the [supportingContent].
 * @param showPasswordTestTag The test tag to be used on the show password button (testing tool).
 * @param autoFocus When set to true, the view will request focus after the first recomposition.
 * Setting this to true on multiple fields at once may have unexpected consequences.
 * @param keyboardType The type of keyboard the user has access to when inputting values into
 * the password field.
 * @param imeAction the preferred IME action for the keyboard to have.
 * @param keyboardActions the callbacks of keyboard actions.
 * @param textToolbarType The type of [TextToolbar] to use on the text field.
 * @param passwordFieldTestTag The optional test tag associated with the inner password field.
 * @param cardStyle Indicates the type of card style to be applied.
 * @param actionsPadding Padding to be applied to the [actions] block.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * in the app bar's trailing side. This lambda extends [RowScope], allowing flexibility in
 * defining the layout of the actions.
 */
@Composable
fun BitwardenPasswordField(
    label: String?,
    value: String,
    onValueChange: (String) -> Unit,
    supportingContent: @Composable (ColumnScope.() -> Unit)?,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
    tooltip: TooltipData? = null,
    initialShowPassword: Boolean = false,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    supportingContentPadding: PaddingValues = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
    showPasswordTestTag: String? = null,
    autoFocus: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Password,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    textToolbarType: TextToolbarType = TextToolbarType.DEFAULT,
    passwordFieldTestTag: String? = null,
    actionsPadding: PaddingValues = PaddingValues(end = 4.dp),
    actions: @Composable (RowScope.() -> Unit)? = null,
) {
    var showPassword by rememberSaveable { mutableStateOf(value = initialShowPassword) }
    BitwardenPasswordField(
        label = label,
        value = value,
        showPassword = showPassword,
        showPasswordChange = { showPassword = !showPassword },
        showPasswordTestTag = showPasswordTestTag,
        onValueChange = onValueChange,
        modifier = modifier,
        tooltip = tooltip,
        readOnly = readOnly,
        singleLine = singleLine,
        supportingContent = supportingContent,
        supportingContentPadding = supportingContentPadding,
        autoFocus = autoFocus,
        keyboardType = keyboardType,
        imeAction = imeAction,
        keyboardActions = keyboardActions,
        textToolbarType = textToolbarType,
        passwordFieldTestTag = passwordFieldTestTag,
        cardStyle = cardStyle,
        actionsPadding = actionsPadding,
        actions = actions,
    )
}

/**
 * Represents a Bitwarden-styled password field that manages the state of a show/hide indicator
 * internally.
 *
 * @param label Label for the text field.
 * @param value Current next on the text field.
 * @param onValueChange Callback that is triggered when the password changes.
 * @param modifier Modifier for the composable.
 * @param tooltip the optional tooltip to be displayed in the label.
 * @param readOnly `true` if the input should be read-only and not accept user interactions.
 * @param singleLine when `true`, this text field becomes a single line that horizontally scrolls
 * instead of wrapping onto multiple lines.
 * @param supportingText An optional supporting text that will appear below the text input.
 * @param initialShowPassword The initial state of the show/hide password control. A value of
 * `false` (the default) indicates that that password should begin in the hidden state.
 * @param showPasswordTestTag The test tag to be used on the show password button (testing tool).
 * @param autoFocus When set to true, the view will request focus after the first recomposition.
 * Setting this to true on multiple fields at once may have unexpected consequences.
 * @param keyboardType The type of keyboard the user has access to when inputting values into
 * the password field.
 * @param imeAction the preferred IME action for the keyboard to have.
 * @param keyboardActions the callbacks of keyboard actions.
 * @param passwordFieldTestTag The optional test tag associated with the inner text field.
 * @param cardStyle Indicates the type of card style to be applied.
 * @param actionsPadding Padding to be applied to the [actions] block.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * in the app bar's trailing side. This lambda extends [RowScope], allowing flexibility in
 * defining the layout of the actions.
 */
@Composable
fun BitwardenPasswordField(
    label: String?,
    value: String,
    onValueChange: (String) -> Unit,
    cardStyle: CardStyle,
    modifier: Modifier = Modifier,
    tooltip: TooltipData? = null,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    supportingText: String? = null,
    initialShowPassword: Boolean = false,
    showPasswordTestTag: String? = null,
    autoFocus: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Password,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    passwordFieldTestTag: String? = null,
    actionsPadding: PaddingValues = PaddingValues(end = 4.dp),
    actions: @Composable (RowScope.() -> Unit)? = null,
) {
    var showPassword by rememberSaveable { mutableStateOf(initialShowPassword) }
    BitwardenPasswordField(
        modifier = modifier,
        label = label,
        value = value,
        showPassword = showPassword,
        showPasswordChange = { showPassword = !showPassword },
        onValueChange = onValueChange,
        tooltip = tooltip,
        readOnly = readOnly,
        singleLine = singleLine,
        supportingText = supportingText,
        showPasswordTestTag = showPasswordTestTag,
        autoFocus = autoFocus,
        keyboardType = keyboardType,
        imeAction = imeAction,
        keyboardActions = keyboardActions,
        passwordFieldTestTag = passwordFieldTestTag,
        cardStyle = cardStyle,
        actionsPadding = actionsPadding,
        actions = actions,
    )
}

@Preview
@Composable
private fun BitwardenPasswordField_preview() {
    BitwardenTheme {
        Column {
            BitwardenPasswordField(
                label = "Label",
                value = "Password",
                onValueChange = {},
                initialShowPassword = false,
                cardStyle = CardStyle.Top(),
            )
            BitwardenPasswordField(
                label = "Label",
                value = "Password",
                onValueChange = {},
                initialShowPassword = true,
                cardStyle = CardStyle.Middle(),
            )
            BitwardenPasswordField(
                label = "Label",
                value = "",
                onValueChange = {},
                initialShowPassword = false,
                cardStyle = CardStyle.Middle(),
            )
            BitwardenPasswordField(
                label = "Label",
                value = "",
                onValueChange = {},
                initialShowPassword = true,
                supportingText = "Hint",
                cardStyle = CardStyle.Bottom,
            )
        }
    }
}
