package com.x8bit.bitwarden.ui.vault.feature.importlogins.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.ClickableTextHighlight
import com.x8bit.bitwarden.ui.platform.base.util.createClickableAnnotatedString
import com.x8bit.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.x8bit.bitwarden.ui.platform.components.card.BitwardenContentCard
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.feature.importlogins.model.InstructionStep
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Reusable component for each step of the import logins flow.
 */
@Suppress("LongMethod")
@Composable
fun ImportLoginsInstructionStep(
    stepText: String,
    stepTitle: String,
    instructions: ImmutableList<InstructionStep>,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = stepText,
            style = BitwardenTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(12.dp))
        Text(text = stepTitle, style = BitwardenTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        BitwardenContentCard(
            contentItems = instructions,
            bottomDividerPaddingStart = 48.dp,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        ) { instructionStep ->
            InstructionRowItem(
                instructionStep = instructionStep,
                modifier = modifier
                    .padding(all = 12.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = createClickableAnnotatedString(
                mainString = stringResource(R.string.need_help_check_out_import_help),
                highlights = listOf(
                    ClickableTextHighlight(
                        textToHighlight = stringResource(R.string.import_help_highlight),
                        onTextClick = onHelpClick,
                    ),
                ),
            ),
            style = BitwardenTheme.typography.bodySmall,
            color = BitwardenTheme.colorScheme.text.secondary,
            modifier = Modifier.standardHorizontalMargin(),
        )
        Spacer(Modifier.height(24.dp))
        BitwardenFilledButton(
            label = stringResource(R.string.continue_text),
            onClick = onContinueClick,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(Modifier.height(12.dp))
        BitwardenOutlinedButton(
            label = stringResource(R.string.back),
            onClick = onBackClick,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Preview
@Composable
private fun ImportLoginsInstructionStep_preview() {
    BitwardenTheme {
        Column(modifier = Modifier.background(BitwardenTheme.colorScheme.background.primary)) {
            ImportLoginsInstructionStep(
                stepText = "Step text",
                stepTitle = "Step title",
                instructions = persistentListOf(
                    InstructionStep(
                        stepNumber = 1,
                        instructionText = buildAnnotatedString {
                            append("Step text 1")
                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = BitwardenTheme.typography.bodyMedium.fontFamily,
                                ),
                            ) {
                                append(" with bold text")
                            }
                        },
                        additionalText = null,
                    ),
                    InstructionStep(
                        stepNumber = 2,
                        instructionText = buildAnnotatedString {
                            append("Step text 2")
                        },
                        additionalText = "Added deets",
                    ),
                    InstructionStep(
                        stepNumber = 3,
                        instructionText = buildAnnotatedString {
                            append("Step text 3")
                        },
                    ),
                ),
                onBackClick = {},
                onContinueClick = {},
                onHelpClick = {},
            )
        }
    }
}
