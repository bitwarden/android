package com.x8bit.bitwarden.ui.platform.feature.rootnav

import androidx.navigation3.runtime.NavKey
import com.x8bit.bitwarden.ui.auth.feature.accountsetup.AccountSetupGraph
import com.x8bit.bitwarden.ui.auth.feature.auth.AuthGraph
import com.x8bit.bitwarden.ui.auth.feature.resetpassword.ResetPasswordGraph
import com.x8bit.bitwarden.ui.platform.feature.vaultunlocked.VaultUnlockedGraph
import com.x8bit.bitwarden.ui.vault.feature.exportitems.ExportItemsGraph
import com.x8bit.bitwarden.ui.vault.feature.migratetomyitems.MigrateToMyItemsGraph
import kotlinx.serialization.Serializable

/**
 * Represents the hierarchy of the Root UI navigation graph and provides type-safe routes to each
 * top-level section of the app.
 *
 * Each section consisting of multiple screens hosts its own nested navigation graph, which is
 * started with the given back stack.
 */
@Serializable
sealed class RootNavGraph : NavKey {
    /**
     * The type-safe route for the splash screen.
     */
    @Serializable
    data object SplashRoute : RootNavGraph()

    /**
     * The type-safe route for the auth section, which hosts the [AuthGraph].
     *
     * @property backStack The initial back stack of the [AuthGraph].
     */
    @Serializable
    data class AuthRoute(
        val backStack: List<AuthGraph>,
    ) : RootNavGraph()

    /**
     * The type-safe route for the remove password screen.
     */
    @Serializable
    data object RemovePasswordRoute : RootNavGraph()

    /**
     * The type-safe route for the reset password section, which hosts the
     * [ResetPasswordGraph].
     */
    @Serializable
    data object ResetPasswordRoute : RootNavGraph()

    /**
     * The type-safe route for the account setup section, which hosts the [AccountSetupGraph].
     *
     * @property backStack The initial back stack of the [AccountSetupGraph].
     */
    @Serializable
    data class AccountSetupRoute(
        val backStack: List<AccountSetupGraph>,
    ) : RootNavGraph()

    /**
     * The type-safe route for the vault unlocked section, which hosts the [VaultUnlockedGraph].
     *
     * @property backStack The initial back stack of the [VaultUnlockedGraph].
     */
    @Serializable
    data class VaultUnlockedRoute(
        val activeUserId: String,
        val backStack: List<VaultUnlockedGraph>,
    ) : RootNavGraph()

    /**
     * The type-safe route for the export items section, which hosts the [ExportItemsGraph].
     *
     * @property backStack The initial back stack of the [ExportItemsGraph].
     */
    @Serializable
    data class ExportItemsRoute(
        val backStack: List<ExportItemsGraph>,
    ) : RootNavGraph()

    /**
     * The type-safe route for the Migrate to My Items section, which hosts the
     * [MigrateToMyItemsGraph].
     */
    @Serializable
    data object MigrateToMyItemsRoute : RootNavGraph()
}
