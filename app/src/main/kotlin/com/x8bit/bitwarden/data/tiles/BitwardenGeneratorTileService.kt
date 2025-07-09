package com.x8bit.bitwarden.data.tiles

import android.annotation.SuppressLint
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.Keep
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * A service for handling the Password Generator quick settings tile.
 */
@AndroidEntryPoint
@Keep
@OmitFromCoverage
class BitwardenGeneratorTileService : TileService() {
    @Inject
    lateinit var intentManager: IntentManager

    override fun onClick() {
        if (isLocked) {
            unlockAndRun { launchGenerator() }
        } else {
            launchGenerator()
        }
    }

    private fun launchGenerator() {
        val intent = intentManager.createTileIntent("bitwarden://password_generator")
        if (!isBuildVersionAtLeast(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) {
            @Suppress("DEPRECATION")
            @SuppressLint("StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        } else {
            startActivityAndCollapse(intentManager.createTilePendingIntent(0, intent))
        }
    }
}
