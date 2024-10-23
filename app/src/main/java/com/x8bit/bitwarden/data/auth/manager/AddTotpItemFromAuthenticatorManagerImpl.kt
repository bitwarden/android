package com.x8bit.bitwarden.data.auth.manager

import com.x8bit.bitwarden.ui.vault.model.TotpData

/**
 * Default in memory implementation for [AddTotpItemFromAuthenticatorManager].
 */
class AddTotpItemFromAuthenticatorManagerImpl : AddTotpItemFromAuthenticatorManager {

    override var pendingAddTotpLoginItemData: TotpData? = null
}
