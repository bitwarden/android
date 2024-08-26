package com.x8bit.bitwarden.data.tiles

import android.annotation.SuppressLint
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.Keep
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Runnable
import javax.inject.Inject

/**
 * A service for handling the My Vault quick settings tile.
 */
@AndroidEntryPoint
@Keep
@OmitFromCoverage
class BitwardenVaultTileService : TileService() {
    @Inject
    lateinit var intentManager: IntentManager

    override fun onClick() {
        if (isLocked) {
            unlockAndRun(Runnable { launchVault() })
        } else {
            launchVault()
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun launchVault() {
        val intent = intentManager.createTileIntent("bitwarden://my_vault")

        if (isBuildVersionBelow(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) {
            startActivityAndCollapse(intent)
        } else {
            startActivityAndCollapse(intentManager.createTilePendingIntent(0, intent))
        }
    }
}
