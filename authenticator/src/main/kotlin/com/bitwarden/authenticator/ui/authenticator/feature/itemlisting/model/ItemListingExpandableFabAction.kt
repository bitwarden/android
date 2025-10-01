package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model

import androidx.compose.material3.ExtendedFloatingActionButton
import com.bitwarden.ui.platform.components.fab.ExpandableFabOption
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.util.Text

/**
 * Models [ExpandableFabOption]s that can be triggered by the [ExtendedFloatingActionButton].
 */
sealed class ItemListingExpandableFabAction(
    label: Text,
    icon: IconData.Local,
    onFabOptionClick: () -> Unit,
) : ExpandableFabOption(label, icon, onFabOptionClick) {

    /**
     * Indicates the Scan QR code button was clicked.
     */
    class ScanQrCode(
        label: Text,
        icon: IconData.Local,
        onScanQrCodeClick: () -> Unit,
    ) : ItemListingExpandableFabAction(
        label = label,
        icon = icon,
        onFabOptionClick = onScanQrCodeClick,
    )

    /**
     * Indicates the Enter Key button was clicked.
     */
    class EnterSetupKey(
        label: Text,
        icon: IconData.Local,
        onEnterSetupKeyClick: () -> Unit,
    ) : ItemListingExpandableFabAction(
        label = label,
        icon = icon,
        onFabOptionClick = onEnterSetupKeyClick,
    )
}
