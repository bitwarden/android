@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.vault.feature.item

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.bitwarden.core.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditArgs
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType

private const val LOGIN: String = "login"
private const val CARD: String = "card"
private const val IDENTITY: String = "identity"
private const val SECURE_NOTE: String = "secure_note"
private const val SSH_KEY: String = "ssh_key"
private const val VAULT_ITEM_CIPHER_TYPE: String = "vault_item_cipher_type"

private const val VAULT_ITEM_PREFIX = "vault_item"
private const val VAULT_ITEM_ID = "vault_item_id"
private const val VAULT_ITEM_ROUTE = "$VAULT_ITEM_PREFIX/{$VAULT_ITEM_ID}" +
    "?$VAULT_ITEM_CIPHER_TYPE={$VAULT_ITEM_CIPHER_TYPE}"

/**
 * Class to retrieve vault item arguments from the [SavedStateHandle].
 */
data class VaultItemArgs(
    val vaultItemId: String,
    val cipherType: VaultItemCipherType,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        vaultItemId = checkNotNull(savedStateHandle.get<String>(VAULT_ITEM_ID)),
        cipherType = requireNotNull(savedStateHandle.get<String>(VAULT_ITEM_CIPHER_TYPE))
            .toVaultItemCipherType(),
    )
}

/**
 * Add the vault item screen to the nav graph.
 */
fun NavGraphBuilder.vaultItemDestination(
    onNavigateBack: () -> Unit,
    onNavigateToVaultEditItem: (args: VaultAddEditArgs) -> Unit,
    onNavigateToMoveToOrganization: (vaultItemId: String, showOnlyCollections: Boolean) -> Unit,
    onNavigateToAttachments: (vaultItemId: String) -> Unit,
    onNavigateToPasswordHistory: (vaultItemId: String) -> Unit,
) {
    composableWithSlideTransitions(
        route = VAULT_ITEM_ROUTE,
        arguments = listOf(
            navArgument(VAULT_ITEM_ID) { type = NavType.StringType },
            navArgument(VAULT_ITEM_CIPHER_TYPE) { type = NavType.StringType },
        ),
    ) {
        VaultItemScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToVaultAddEditItem = onNavigateToVaultEditItem,
            onNavigateToMoveToOrganization = onNavigateToMoveToOrganization,
            onNavigateToAttachments = onNavigateToAttachments,
            onNavigateToPasswordHistory = onNavigateToPasswordHistory,
        )
    }
}

/**
 * Navigate to the vault item screen.
 */
fun NavController.navigateToVaultItem(
    args: VaultItemArgs,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = "$VAULT_ITEM_PREFIX/${args.vaultItemId}" +
            "?$VAULT_ITEM_CIPHER_TYPE=${args.cipherType.toTypeString()}",
        navOptions = navOptions,
    )
}

private fun VaultItemCipherType.toTypeString(): String =
    when (this) {
        VaultItemCipherType.LOGIN -> LOGIN
        VaultItemCipherType.CARD -> CARD
        VaultItemCipherType.IDENTITY -> IDENTITY
        VaultItemCipherType.SECURE_NOTE -> SECURE_NOTE
        VaultItemCipherType.SSH_KEY -> SSH_KEY
    }

private fun String.toVaultItemCipherType(): VaultItemCipherType =
    when (this) {
        LOGIN -> VaultItemCipherType.LOGIN
        CARD -> VaultItemCipherType.CARD
        IDENTITY -> VaultItemCipherType.IDENTITY
        SECURE_NOTE -> VaultItemCipherType.SECURE_NOTE
        SSH_KEY -> VaultItemCipherType.SSH_KEY
        else -> throw IllegalStateException(
            "Edit Item string arguments for VaultAddEditNavigation must match!",
        )
    }
