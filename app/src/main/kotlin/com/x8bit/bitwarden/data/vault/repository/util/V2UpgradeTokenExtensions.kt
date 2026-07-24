package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.core.V2UpgradeToken
import com.bitwarden.network.model.V2UpgradeTokenJson

/**
 * Converts the [V2UpgradeToken] into a [V2UpgradeTokenJson].
 */
fun V2UpgradeToken.toV2UpgradeTokenJson(): V2UpgradeTokenJson =
    V2UpgradeTokenJson(
        wrappedUserKey1 = this.wrappedUserKey1,
        wrappedUserKey2 = this.wrappedUserKey2,
    )

/**
 * Converts the [V2UpgradeTokenJson] into a [V2UpgradeToken].
 */
fun V2UpgradeTokenJson.toV2UpgradeToken(): V2UpgradeToken =
    V2UpgradeToken(
        wrappedUserKey1 = this.wrappedUserKey1,
        wrappedUserKey2 = this.wrappedUserKey2,
    )
