package com.x8bit.bitwarden.ui.vault.feature.importlogins.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import com.bitwarden.ui.platform.base.util.annotatedStringResource
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toAnnotatedString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.x8bit.bitwarden.ui.platform.components.card.BitwardenContentCard
import com.x8bit.bitwarden.ui.platform.components.model.ContentBlockData
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
    ctaText: String = stringResource(R.string.continue_text),
    instructions: ImmutableList<ContentBlockData>,
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
            modifier = Modifier
                .standardHorizontalMargin(),
            contentHeaderTextStyle = BitwardenTheme.typography.bodyMedium,
            contentSubtitleTextStyle = BitwardenTheme.typography.labelSmall,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = annotatedStringResource(
                id = R.string.need_help_check_out_import_help,
                onAnnotationClick = { onHelpClick() },
            ),
            style = BitwardenTheme.typography.bodySmall,
            color = BitwardenTheme.colorScheme.text.secondary,
            modifier = Modifier.standardHorizontalMargin(),
        )
        Spacer(Modifier.height(24.dp))
        BitwardenFilledButton(
            label = ctaText,
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
                    ContentBlockData(
                        iconVectorResource = R.drawable.ic_number1,
                        headerText = buildAnnotatedString {
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
                        subtitleText = null,
                    ),
                    ContentBlockData(
                        iconVectorResource = R.drawable.ic_number2,
                        headerText = buildAnnotatedString {
                            append("Step text 2")
                        },
                        subtitleText = "Added deets".toAnnotatedString(),
                    ),
                    ContentBlockData(
                        iconVectorResource = R.drawable.ic_number3,
                        headerText = buildAnnotatedString {
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
