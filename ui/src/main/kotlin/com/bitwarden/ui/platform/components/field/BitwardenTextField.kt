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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.base.util.nullableTestTag
import com.bitwarden.ui.platform.base.util.simpleVerticalScrollbar
import com.bitwarden.ui.platform.base.util.toPx
import com.bitwarden.ui.platform.base.util.withLineBreaksAtWidth
import com.bitwarden.ui.platform.components.appbar.color.bitwardenMenuItemColors
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.bitwarden.ui.platform.components.field.color.bitwardenTextFieldColors
import com.bitwarden.ui.platform.components.field.model.TextToolbarType
import com.bitwarden.ui.platform.components.field.toolbar.BitwardenCutCopyTextToolbar
import com.bitwarden.ui.platform.components.field.toolbar.BitwardenEmptyTextToolbar
import com.bitwarden.ui.platform.components.icon.BitwardenIcon
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.model.TooltipData
import com.bitwarden.ui.platform.components.row.BitwardenRowOfActions
import com.bitwarden.ui.platform.components.support.BitwardenSupportingContent
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Component that allows the user to input text. This composable will manage the state of
 * the user's input.
 *
 * @param label label for the text field.
 * @param value current next on the text field.
 * @param modifier modifier for the composable.
 * @param onValueChange callback that is triggered when the input of the text field changes.
 * @param tooltip the optional tooltip to be displayed in the label.
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 * the [value] is empty.
 * @param leadingIconData the optional resource for the leading icon on the text field.
 * @param supportingText optional supporting text that will appear below the text input.
 * @param singleLine when `true`, this text field becomes a single line that horizontally scrolls
 * instead of wrapping onto multiple lines.
 * @param readOnly `true` if the input should be read-only and not accept user interactions.
 * @param enabled Whether or not the text field is enabled.
 * @param textStyle An optional style that may be used to override the default used.
 * @param textColor An optional color that may be used to override the text color.
 * @param shouldAddCustomLineBreaks If `true`, line breaks will be inserted to allow for filling
 * an entire line before breaking. `false` by default.
 * @param visualTransformation Transforms the visual representation of the input [value].
 * @param keyboardType the preferred type of keyboard input.
 * @param keyboardActions the callbacks of keyboard actions.
 * @param imeAction the preferred IME action for the keyboard to have.
 * @param textToolbarType The type of [TextToolbar] to use on the text field.
 * @param textFieldTestTag The optional test tag associated with the inner text field.
 * @param cardStyle Indicates the type of card style to be applied.
 * @param actionsPadding Padding to be applied to the [actions] block.
 * @param actionsTestTag The test tag to use for the row of actions, or null if there is none.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * in the app bar's trailing side. This lambda extends [RowScope], allowing flexibility in
 * defining the layout of the actions.
 */
@Composable
fun BitwardenTextField(
    label: String?,
    value: String,
    onValueChange: (String) -> Unit,
    cardStyle: CardStyle?,
    modifier: Modifier = Modifier,
    tooltip: TooltipData? = null,
    placeholder: String? = null,
    leadingIconData: IconData? = null,
    supportingText: String? = null,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    textStyle: TextStyle = BitwardenTheme.typography.bodyLarge,
    textColor: Color = BitwardenTheme.colorScheme.text.primary,
    shouldAddCustomLineBreaks: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    imeAction: ImeAction = ImeAction.Default,
    isError: Boolean = false,
    autoFocus: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textToolbarType: TextToolbarType = TextToolbarType.DEFAULT,
    autoCompleteOptions: ImmutableList<String> = persistentListOf(),
    textFieldTestTag: String? = null,
    actionsPadding: PaddingValues = PaddingValues(end = 4.dp),
    actionsTestTag: String? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
) {
    BitwardenTextField(
        modifier = modifier,
        label = label,
        value = value,
        onValueChange = onValueChange,
        tooltip = tooltip,
        placeholder = placeholder,
        leadingIconData = leadingIconData,
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
        singleLine = singleLine,
        readOnly = readOnly,
        enabled = enabled,
        textStyle = textStyle,
        textColor = textColor,
        shouldAddCustomLineBreaks = shouldAddCustomLineBreaks,
        keyboardType = keyboardType,
        keyboardActions = keyboardActions,
        imeAction = imeAction,
        isError = isError,
        autoFocus = autoFocus,
        visualTransformation = visualTransformation,
        textToolbarType = textToolbarType,
        autoCompleteOptions = autoCompleteOptions,
        textFieldTestTag = textFieldTestTag,
        cardStyle = cardStyle,
        actionsPadding = actionsPadding,
        actionsTestTag = actionsTestTag,
        actions = actions,
    )
}

/**
 * Component that allows the user to input text. This composable will manage the state of
 * the user's input.
 *
 * @param label label for the text field.
 * @param value current next on the text field.
 * @param modifier modifier for the composable.
 * @param tooltip the optional tooltip to be displayed in the label.
 * @param onValueChange callback that is triggered when the input of the text field changes.
 * @param supportingContent An optional supporting content composable that will appear below the
 * text input.
 * @param supportingContentPadding The padding to be placed on the [supportingContent].
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 * the [value] is empty.
 * @param leadingIconData the optional resource for the leading icon on the text field.
 * @param singleLine when `true`, this text field becomes a single line that horizontally scrolls
 * instead of wrapping onto multiple lines.
 * @param readOnly `true` if the input should be read-only and not accept user interactions.
 * @param enabled Whether or not the text field is enabled.
 * @param textStyle An optional style that may be used to override the default used.
 * @param textColor An optional color that may be used to override the text color.
 * @param shouldAddCustomLineBreaks If `true`, line breaks will be inserted to allow for filling
 * an entire line before breaking. `false` by default.
 * @param visualTransformation Transforms the visual representation of the input [value].
 * @param keyboardType the preferred type of keyboard input.
 * @param keyboardActions the callbacks of keyboard actions.
 * @param imeAction the preferred IME action for the keyboard to have.
 * @param textToolbarType The type of [TextToolbar] to use on the text field.
 * @param textFieldTestTag The optional test tag associated with the inner text field.
 * @param cardStyle Indicates the type of card style to be applied.
 * @param actionsPadding Padding to be applied to the [actions] block.
 * @param actionsTestTag The test tag to use for the row of actions, or null if there is none.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * in the app bar's trailing side. This lambda extends [RowScope], allowing flexibility in
 * defining the layout of the actions.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitwardenTextField(
    label: String?,
    value: String,
    onValueChange: (String) -> Unit,
    supportingContent: (@Composable ColumnScope.() -> Unit)?,
    cardStyle: CardStyle?,
    modifier: Modifier = Modifier,
    tooltip: TooltipData? = null,
    supportingContentPadding: PaddingValues = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
    placeholder: String? = null,
    leadingIconData: IconData? = null,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    textStyle: TextStyle = BitwardenTheme.typography.bodyLarge,
    textColor: Color = BitwardenTheme.colorScheme.text.primary,
    shouldAddCustomLineBreaks: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    imeAction: ImeAction = ImeAction.Default,
    isError: Boolean = false,
    autoFocus: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textToolbarType: TextToolbarType = TextToolbarType.DEFAULT,
    autoCompleteOptions: ImmutableList<String> = persistentListOf(),
    textFieldTestTag: String? = null,
    actionsPadding: PaddingValues = PaddingValues(end = 4.dp),
    actionsTestTag: String? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
) {
    var widthPx by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val formattedText = if (shouldAddCustomLineBreaks) {
        value.withLineBreaksAtWidth(
            // Adjust for built in padding
            widthPx = widthPx - 32.dp.toPx(),
            monospacedTextStyle = textStyle,
        )
    } else {
        value
    }
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(text = formattedText)) }
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
        var hasFocused by remember { mutableStateOf(value = false) }
        val filteredAutoCompleteList = autoCompleteOptions
            .filter { it.startsWith(textFieldValue.text) && it != textFieldValue.text }
            .toImmutableList()
        val isDropDownExpanded = filteredAutoCompleteList.isNotEmpty() && hasFocused
        ExposedDropdownMenuBox(
            expanded = isDropDownExpanded,
            onExpandedChange = {
                hasFocused = !hasFocused
                focusRequester.requestFocus()
            },
            modifier = modifier.defaultMinSize(minHeight = 60.dp),
        ) {
            Column(
                modifier = Modifier
                    .onGloballyPositioned { widthPx = it.size.width }
                    .onFocusEvent { focusState -> hasFocused = focusState.hasFocus }
                    .cardStyle(
                        cardStyle = cardStyle,
                        paddingTop = 6.dp,
                        paddingBottom = 0.dp,
                    )
                    .fillMaxWidth()
                    .semantics {
                        customActions = listOfNotNull(
                            tooltip?.let {
                                CustomAccessibilityAction(
                                    label = it.contentDescription,
                                    action = {
                                        it.onClick()
                                        true
                                    },
                                )
                            },
                        )
                    },
            ) {
                var focused by remember { mutableStateOf(false) }

                TextField(
                    colors = bitwardenTextFieldColors(textColor = textColor),
                    enabled = enabled,
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
                    leadingIcon = leadingIconData?.let { iconData ->
                        {
                            BitwardenIcon(
                                iconData = iconData,
                                tint = BitwardenTheme.colorScheme.icon.primary,
                            )
                        }
                    },
                    placeholder = placeholder?.let { { Text(text = it, style = textStyle) } },
                    onValueChange = {
                        hasFocused = true
                        textFieldValueState = it
                        val stringChangedSinceLastInvocation = lastTextValue != it.text
                        lastTextValue = it.text
                        if (stringChangedSinceLastInvocation) {
                            onValueChange(it.text)
                        }
                    },
                    singleLine = singleLine,
                    readOnly = readOnly,
                    textStyle = textStyle,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = keyboardType,
                        imeAction = imeAction,
                    ),
                    keyboardActions = keyboardActions,
                    trailingIcon = actions?.let {
                        {
                            BitwardenRowOfActions(
                                actions = it,
                                modifier = Modifier
                                    .nullableTestTag(tag = actionsTestTag)
                                    .padding(paddingValues = actionsPadding),
                            )
                        }
                    },
                    isError = isError,
                    visualTransformation = visualTransformation,
                    modifier = Modifier
                        .nullableTestTag(tag = textFieldTestTag)
                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable)
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            focused = focusState.isFocused
                            if (focused) {
                                textFieldValueState = textFieldValueState.copy(
                                    selection = TextRange(textFieldValueState.text.length),
                                )
                            }
                        },
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
                    ?: Spacer(modifier = Modifier.height(height = cardStyle?.let { 6.dp } ?: 0.dp))
            }
            val scrollState = rememberScrollState()
            ExposedDropdownMenu(
                expanded = isDropDownExpanded,
                shape = BitwardenTheme.shapes.menu,
                containerColor = BitwardenTheme.colorScheme.background.primary,
                onDismissRequest = { hasFocused = false },
                scrollState = scrollState,
                modifier = Modifier.simpleVerticalScrollbar(state = scrollState),
            ) {
                filteredAutoCompleteList.forEach {
                    DropdownMenuItem(
                        colors = bitwardenMenuItemColors(),
                        text = { Text(text = it, style = textStyle) },
                        onClick = { onValueChange(it) },
                    )
                }
            }
        }
    }
    if (autoFocus) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}

@Preview
@Composable
private fun BitwardenTextField_preview() {
    BitwardenTheme {
        Column {
            BitwardenTextField(
                label = "Label",
                value = "Input",
                onValueChange = {},
                supportingText = "Hint",
                cardStyle = CardStyle.Top(),
            )
            BitwardenTextField(
                label = "Label",
                value = "",
                onValueChange = {},
                supportingText = "Hint",
                cardStyle = CardStyle.Bottom,
            )
        }
    }
}
