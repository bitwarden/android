package com.x8bit.bitwarden.ui.vault.feature.importlogins.components

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.card.BitwardenContentCard
import com.x8bit.bitwarden.ui.platform.components.content.BitwardenContentBlock
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.feature.importlogins.model.InstructionStep
import kotlinx.collections.immutable.persistentListOf

/**
 * Row item for the content card of the import logins screen instructions.
 */
@Composable
fun InstructionRowItem(
    instructionStep: InstructionStep,
    modifier: Modifier = Modifier,
) {
    BitwardenContentBlock(
        modifier = modifier,
        headerText = instructionStep.instructionText,
        headerTextStyle = BitwardenTheme.typography.bodyMedium,
        subtitleText = instructionStep.additionalText,
        subtitleTextStyle = BitwardenTheme.typography.labelSmall,
    )
}

@Suppress("MagicNumber")
@get:DrawableRes
private val InstructionStep.imageRes: Int
    get() = when (this.stepNumber) {
        1 -> R.drawable.ic_number1
        2 -> R.drawable.ic_number2
        3 -> R.drawable.ic_number3
        4 -> R.drawable.ic_number4
        else -> error(
            "Invalid step number, if new step is required please add drawable asset for it.",
        )
    }

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InstructionCard_preview() {
    BitwardenTheme {
        Surface {
            BitwardenContentCard(
                contentItems = persistentListOf(
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
            ) {
                InstructionRowItem(it)
            }
        }
    }
}
