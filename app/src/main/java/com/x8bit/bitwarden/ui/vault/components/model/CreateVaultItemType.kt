package com.x8bit.bitwarden.ui.vault.components.model

import androidx.annotation.StringRes
import com.x8bit.bitwarden.R

/**
 * Enumerated values to represent a create vault item option.
 */
enum class CreateVaultItemType(
    @StringRes val selectionText: Int,
) {
    /**
     * A login cipher.
     */
    LOGIN(R.string.log_in_noun),

    /**
     * A card cipher.
     */
    CARD(R.string.type_card),

    /**
     * A identity cipher.
     */
    IDENTITY(R.string.type_identity),

    /**
     * A secure note cipher.
     */
    SECURE_NOTE(R.string.type_secure_note),

    /**
     * A SSH key cipher.
     */
    SSH_KEY(R.string.type_ssh_key),

    /**
     * A cipher item folder
     */
    FOLDER(R.string.folder),
}
