package com.x8bit.bitwarden.data.platform.manager.model

/**
 * Model to encapsulate different states for a user's first time experience.
 */
data class FirstTimeState(
    val showImportLoginsCard: Boolean,
    val showImportLoginsCardInSettings: Boolean,
    val showSetupUnlockCard: Boolean,
    val showSetupAutofillCard: Boolean,
) {
    /**
     * Constructs a [FirstTimeState] accepting nullable values. If a value is null, the default
     * is used.
     */
    constructor(
        showImportLoginsCard: Boolean? = null,
        showSetupUnlockCard: Boolean? = null,
        showSetupAutofillCard: Boolean? = null,
        showImportLoginsCardInSettings: Boolean? = null,
    ) : this(
        showImportLoginsCard = showImportLoginsCard ?: true,
        showSetupUnlockCard = showSetupUnlockCard ?: false,
        showSetupAutofillCard = showSetupAutofillCard ?: false,
        showImportLoginsCardInSettings = showImportLoginsCardInSettings ?: false,
    )
}
