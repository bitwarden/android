package com.x8bit.bitwarden.ui.platform.theme.shape

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * The default [BitwardenShapes] for the app.
 */
val bitwardenShapes: BitwardenShapes = BitwardenShapes(
    actionCard = RoundedCornerShape(size = 12.dp),
    coachmark = RoundedCornerShape(size = 8.dp),
    content = RoundedCornerShape(size = 8.dp),
    contentBottom = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
    contentTop = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
    dialog = RoundedCornerShape(size = 28.dp),
    infoCallout = RoundedCornerShape(size = 8.dp),
    menu = RoundedCornerShape(size = 4.dp),
    snackbar = RoundedCornerShape(size = 8.dp),
)
