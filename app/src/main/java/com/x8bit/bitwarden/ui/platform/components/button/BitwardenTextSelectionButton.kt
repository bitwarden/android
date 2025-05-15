package com.x8bit.bitwarden.ui.platform.components.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.cardStyle
import com.x8bit.bitwarden.ui.platform.base.util.nullableTestTag
import com.x8bit.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.x8bit.bitwarden.ui.platform.components.field.color.bitwardenTextFieldButtonColors
import com.x8bit.bitwarden.ui.platform.components.model.TooltipData
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenRowOfActions
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.util.persistentListOfNotNull

/**
 * A button which uses a read-only text field for layout and style purposes.
 */
@Suppress("LongMethod")
@Composable
fun BitwardenTextSelectionButton(
    label: String,
    selectedOption: String?,
    onClick: () -> Unit,
    cardStyle: CardStyle?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
    tooltip: TooltipData? = null,
    insets: PaddingValues = PaddingValues(),
    textFieldTestTag: String? = null,
    semanticRole: Role = Role.Button,
    actionsPadding: PaddingValues = PaddingValues(end = 4.dp),
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .clearAndSetSemantics {
                role = semanticRole
                contentDescription = supportingText
                    ?.let { "$selectedOption. $label. $it" }
                    ?: "$selectedOption. $label"
                customActions = persistentListOfNotNull(
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
            }
            .cardStyle(
                cardStyle = cardStyle,
                paddingTop = 6.dp,
                paddingBottom = 0.dp,
                onClick = onClick,
                clickEnabled = enabled,
            )
            .padding(paddingValues = insets),
    ) {
        TextField(
            textStyle = BitwardenTheme.typography.bodyLarge,
            readOnly = true,
            enabled = false,
            label = {
                Row {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    tooltip?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        BitwardenStandardIconButton(
                            vectorIconRes = R.drawable.ic_question_circle_small,
                            contentDescription = it.contentDescription,
                            onClick = it.onClick,
                            contentColor = BitwardenTheme.colorScheme.icon.secondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            },
            trailingIcon = {
                BitwardenRowOfActions(
                    modifier = Modifier.padding(paddingValues = actionsPadding),
                    actions = {
                        Icon(
                            painter = rememberVectorPainter(id = R.drawable.ic_chevron_down),
                            contentDescription = null,
                            tint = BitwardenTheme.colorScheme.icon.primary,
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        )
                        actions()
                    },
                )
            },
            value = selectedOption.orEmpty(),
            onValueChange = {},
            colors = bitwardenTextFieldButtonColors(),
            modifier = Modifier
                .nullableTestTag(tag = textFieldTestTag)
                .fillMaxWidth(),
        )
        supportingText
            ?.let { content ->
                Spacer(modifier = Modifier.height(height = 6.dp))
                BitwardenHorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp),
                )
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    content = {
                        Text(
                            text = content,
                            style = BitwardenTheme.typography.bodySmall,
                            color = BitwardenTheme.colorScheme.text.secondary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                )
            }
            ?: Spacer(modifier = Modifier.height(height = cardStyle?.let { 6.dp } ?: 0.dp))
    }
}

@Preview
@Composable
private fun BitwardenTextSelectionButton_preview() {
    BitwardenTheme {
        BitwardenTextSelectionButton(
            label = "Folder",
            selectedOption = "No Folder",
            onClick = {},
            cardStyle = CardStyle.Full,
        )
    }
}
