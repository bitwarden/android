package com.x8bit.bitwarden.ui.vault.model

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.vault.feature.addedit.util.SELECT_TEXT

/**
 * Defines all available title options for identities.
 */
enum class VaultIdentityTitle(val value: Text) {
    SELECT(value = SELECT_TEXT),
    MR(value = BitwardenString.mr.asText()),
    MRS(value = BitwardenString.mrs.asText()),
    MS(value = BitwardenString.ms.asText()),
    MX(value = BitwardenString.mx.asText()),
    DR(value = BitwardenString.dr.asText()),
}
