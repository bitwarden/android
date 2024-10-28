package com.x8bit.bitwarden.ui.platform.theme.shape

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Immutable

/**
 * Defines all the shapes for the app.
 */
@Immutable
data class BitwardenShapes(
    val actionCard: CornerBasedShape,
    val bottomSheet: CornerBasedShape,
    val coachmark: CornerBasedShape,
    val content: CornerBasedShape,
    val contentBottom: CornerBasedShape,
    val contentTop: CornerBasedShape,
    val dialog: CornerBasedShape,
    val fab: CornerBasedShape,
    val infoCallout: CornerBasedShape,
    val menu: CornerBasedShape,
    val segmentedControl: CornerBasedShape,
    val snackbar: CornerBasedShape,
)
