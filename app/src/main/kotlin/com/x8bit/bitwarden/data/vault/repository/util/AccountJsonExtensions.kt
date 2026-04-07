package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.exporters.Account
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson

/**
 * Converts a [AccountJson] to a [Account] for use in the SDK.
 */
fun AccountJson.toSdkAccount(): Account = Account(
    id = profile.userId,
    email = profile.email,
    name = profile.name,
)
