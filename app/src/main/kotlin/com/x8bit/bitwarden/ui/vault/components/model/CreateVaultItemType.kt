package com.x8bit.bitwarden.ui.vault.components.model

import androidx.annotation.StringRes
import com.bitwarden.ui.platform.resource.BitwardenString

/**
 * Enumerated values to represent a create vault item option.
 */
enum class CreateVaultItemType(
    @field:StringRes val selectionText: Int,
) {
    /**
     * A login cipher.
     */
    LOGIN(BitwardenString.log_in_noun),

    /**
     * A card cipher.
     */
    CARD(BitwardenString.type_card),

    /**
     * A identity cipher.
     */
    IDENTITY(BitwardenString.type_identity),

    /**
     * A secure note cipher.
     */
    SECURE_NOTE(BitwardenString.type_secure_note),

    /**
     * A SSH key cipher.
     */
    SSH_KEY(BitwardenString.type_ssh_key),

    /**
     * A bank account cipher.
     */
    BANK_ACCOUNT(BitwardenString.type_bank_account),

    /**
     * A driver's license cipher.
     */
    DRIVERS_LICENSE(BitwardenString.type_drivers_license),

    /**
     * A passport cipher.
     */
    PASSPORT(BitwardenString.type_passport),

    /**
     * A cipher item folder
     */
    FOLDER(BitwardenString.folder),
}
