package com.x8bit.bitwarden.data.auth.manager

import com.bitwarden.ui.platform.model.TotpData

/**
 * Manager for keeping track of requests from the Bitwarden Authenticator app to add a TOTP
 * item.
 */
interface AddTotpItemFromAuthenticatorManager {

    /**
     * Current pending [TotpData] to be added from the Authenticator app.
     */
    var pendingAddTotpLoginItemData: TotpData?
}
