package com.x8bit.bitwarden.data.auth.manager

import com.bitwarden.ui.platform.model.TotpData

/**
 * Default in memory implementation for [AddTotpItemFromAuthenticatorManager].
 */
class AddTotpItemFromAuthenticatorManagerImpl : AddTotpItemFromAuthenticatorManager {

    override var pendingAddTotpLoginItemData: TotpData? = null
}
