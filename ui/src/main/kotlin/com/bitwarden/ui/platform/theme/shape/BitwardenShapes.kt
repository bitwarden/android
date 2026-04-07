package com.bitwarden.ui.platform.theme.shape

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
    val contentMiddle: CornerBasedShape,
    val contentTop: CornerBasedShape,
    val dialog: CornerBasedShape,
    val fab: CornerBasedShape,
    val fabItem: CornerBasedShape,
    val infoCallout: CornerBasedShape,
    val menu: CornerBasedShape,
    val progressIndicator: CornerBasedShape,
    val segmentedControl: CornerBasedShape,
    val snackbar: CornerBasedShape,
    val favicon: CornerBasedShape,
)
