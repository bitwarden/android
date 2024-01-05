package com.x8bit.bitwarden.ui.vault.model

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.SELECT_TEXT

/**
 * Defines all available title options for identities.
 */
enum class VaultIdentityTitle(val value: Text) {
    SELECT(value = SELECT_TEXT),
    MR(value = R.string.mr.asText()),
    MRS(value = R.string.mrs.asText()),
    MS(value = R.string.ms.asText()),
    MX(value = R.string.mx.asText()),
    DR(value = R.string.dr.asText()),
}
