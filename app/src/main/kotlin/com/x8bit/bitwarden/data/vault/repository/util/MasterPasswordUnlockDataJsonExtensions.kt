package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.core.MasterPasswordUnlockData
import com.bitwarden.network.model.MasterPasswordUnlockDataJson
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toKdf

/**
 * Converts [MasterPasswordUnlockDataJson] to [MasterPasswordUnlockData]
 */
fun MasterPasswordUnlockDataJson.toSdkMasterPasswordUnlock(): MasterPasswordUnlockData =
    MasterPasswordUnlockData(
        kdf = kdf.toKdf(),
        masterKeyWrappedUserKey = masterKeyWrappedUserKey,
        salt = salt,
    )
