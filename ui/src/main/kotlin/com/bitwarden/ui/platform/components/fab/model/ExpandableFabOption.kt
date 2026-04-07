package com.bitwarden.ui.platform.components.fab.model

import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.util.Text

/**
 * Represents options displayed when the FAB is expanded.
 */
data class ExpandableFabOption(
    val label: Text,
    val icon: IconData.Local,
    val onFabOptionClick: () -> Unit,
)
