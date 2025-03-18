package com.x8bit.bitwarden.ui.vault.feature.viewasqrcode

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.ui.platform.base.util.composableWithSlideTransitions
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType

private const val VAULT_ITEM_ID = "vault_item_id"

private const val LOGIN: String = "login"
private const val CARD: String = "card"
private const val IDENTITY: String = "identity"
private const val SECURE_NOTE: String = "secure_note"
private const val SSH_KEY: String = "ssh_key"
private const val CIPHER_TYPE: String = "vault_item_type"

private const val VIEW_AS_QR_CODE_PREFIX: String = "view_as_qr_code"

private const val VIEW_AS_QR_CODE_ROUTE: String =
    VIEW_AS_QR_CODE_PREFIX +
        "/{$VAULT_ITEM_ID}" +
        "?$CIPHER_TYPE={$CIPHER_TYPE}"

/**
 * Class to retrieve view as QR code arguments from the [SavedStateHandle].
 */
@OmitFromCoverage
data class ViewAsQrCodeArgs(
    val vaultItemId: String,
    val vaultItemCipherType: VaultItemCipherType,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        vaultItemId = checkNotNull(savedStateHandle.get<String>(VAULT_ITEM_ID)),
        vaultItemCipherType = requireNotNull(savedStateHandle.get<String>(CIPHER_TYPE))
            .toVaultItemCipherType(),
    )
}

/**
 * Add the view as QR code screen to the nav graph.
 */
fun NavGraphBuilder.viewAsQrCodeDestination(
    onNavigateBack: () -> Unit,
) {
    composableWithSlideTransitions(
        route = VIEW_AS_QR_CODE_ROUTE,
        arguments = listOf(
            navArgument(VAULT_ITEM_ID) { type = NavType.StringType },
            navArgument(CIPHER_TYPE) { type = NavType.StringType },
        ),
    ) {
        ViewAsQrCodeScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}

/**
 * Navigate to the view as QR code screen.
 */
fun NavController.navigateToViewAsQrCode(
    args: ViewAsQrCodeArgs,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = "$VIEW_AS_QR_CODE_PREFIX/${args.vaultItemId}" +
            "?$CIPHER_TYPE=${args.vaultItemCipherType.toTypeString()}",
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
            "Cipher Type string arguments for ViewAsQrCodeNavigation must match!",
        )
    }
