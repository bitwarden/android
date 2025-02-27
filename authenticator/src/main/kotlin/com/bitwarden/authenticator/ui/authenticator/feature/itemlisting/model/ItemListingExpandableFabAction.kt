package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model

import androidx.compose.material3.ExtendedFloatingActionButton
import com.bitwarden.authenticator.ui.platform.base.util.Text
import com.bitwarden.authenticator.ui.platform.components.fab.ExpandableFabOption
import com.bitwarden.authenticator.ui.platform.components.model.IconResource

/**
 * Models [ExpandableFabOption]s that can be triggered by the [ExtendedFloatingActionButton].
 */
sealed class ItemListingExpandableFabAction(
    label: Text?,
    icon: IconResource,
    onFabOptionClick: () -> Unit,
) : ExpandableFabOption(label, icon, onFabOptionClick) {

    /**
     * Indicates the Scan QR code button was clicked.
     */
    class ScanQrCode(
        label: Text?,
        icon: IconResource,
        onScanQrCodeClick: () -> Unit,
    ) : ItemListingExpandableFabAction(
        label,
        icon,
        onScanQrCodeClick,
    )

    /**
     * Indicates the Enter Key button was clicked.
     */
    class EnterSetupKey(
        label: Text?,
        icon: IconResource,
        onEnterSetupKeyClick: () -> Unit,
    ) : ItemListingExpandableFabAction(
        label,
        icon,
        onEnterSetupKeyClick,
    )
}
