package com.bitwarden.ui.platform.components.fab.model

import com.bitwarden.ui.platform.components.icon.model.IconData

/**
 * Models data for an expandable FAB icon.
 */
data class ExpandableFabIcon(
    val icon: IconData.Local,
    val iconRotation: Float,
)
