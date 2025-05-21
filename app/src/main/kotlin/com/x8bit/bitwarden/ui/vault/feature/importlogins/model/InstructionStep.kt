package com.x8bit.bitwarden.ui.vault.feature.importlogins.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString

/**
 * Models a single instruction step to be displayed in the import login instructions card.
 */
@Immutable
data class InstructionStep(
    val stepNumber: Int,
    val instructionText: AnnotatedString,
    val additionalText: String? = null,
)
