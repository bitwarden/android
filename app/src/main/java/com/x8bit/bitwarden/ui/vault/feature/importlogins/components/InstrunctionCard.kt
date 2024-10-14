package com.x8bit.bitwarden.ui.vault.feature.importlogins.components

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.vault.feature.importlogins.model.InstructionStep

/**
 * Reusable component for the import logins instruction card.
 */
@Composable
fun InstructionCard(
    instructionSteps: List<InstructionStep>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = BitwardenTheme.colorScheme.background.secondary,
                shape = BitwardenTheme.shapes.infoCard,
            ),
    ) {
        instructionSteps.forEachIndexed { index, step ->
            InstructionRow(
                instructionStep = step,
                modifier = modifier
                    .padding(all = 12.dp),
            )
            if (index < instructionSteps.lastIndex) {
                BitwardenHorizontalDivider(
                    modifier = Modifier.padding(start = 48.dp),
                )
            }
        }
    }
}

@Composable
private fun InstructionRow(
    instructionStep: InstructionStep,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = rememberVectorPainter(instructionStep.imageRes),
            contentDescription = null,
            tint = BitwardenTheme.colorScheme.icon.secondary,
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = instructionStep.instructionText,
                style = BitwardenTheme.typography.bodyMedium,
            )
            instructionStep.additionalText?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    style = BitwardenTheme.typography.labelSmall,
                    color = BitwardenTheme.colorScheme.text.secondary,
                )
            }
        }
    }
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
            InstructionCard(
                instructionSteps = listOf(
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
            )
        }
    }
}
