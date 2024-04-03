package com.x8bit.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model

import com.x8bit.bitwarden.authenticator.ui.platform.base.util.Text
import com.x8bit.bitwarden.authenticator.ui.platform.components.fab.ExpandableFabOption
import com.x8bit.bitwarden.authenticator.ui.platform.components.model.IconResource

sealed class ItemListingExpandableFabAction(
    label: Text?,
    icon: IconResource,
    onFabOptionClick: () -> Unit,
) : ExpandableFabOption(label, icon, onFabOptionClick) {

    class ScanQrCode(
        label: Text?,
        icon: IconResource,
        onScanQrCodeClick: () -> Unit,
    ) : ItemListingExpandableFabAction(
        label,
        icon,
        onScanQrCodeClick
    )

    class EnterSetupKey(
        label: Text?,
        icon: IconResource,
        onEnterSetupKeyClick: () -> Unit,
    ) : ItemListingExpandableFabAction(
        label,
        icon,
        onEnterSetupKeyClick
    )
}
