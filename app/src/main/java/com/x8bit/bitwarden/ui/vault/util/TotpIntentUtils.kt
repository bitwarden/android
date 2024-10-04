package com.x8bit.bitwarden.ui.vault.util

import android.content.Intent
import com.x8bit.bitwarden.ui.vault.model.TotpData

/**
 * Checks if the given [Intent] contains data for a TOTP. The [TotpData] will be returned when the
 * correct data is present or `null` if data is invalid or missing.
 */
fun Intent.getTotpDataOrNull(): TotpData? = this.data?.getTotpDataOrNull()
