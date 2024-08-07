package com.x8bit.bitwarden.ui.platform.feature.tiles

import android.annotation.SuppressLint
import android.os.Build
import android.service.quicksettings.TileService
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManagerImpl
import kotlinx.coroutines.Runnable

class GeneratorTileService: TileService() {
    private val tileIntentManager: IntentManager = IntentManagerImpl(this)

    override fun onTileAdded() {
        super.onTileAdded()
    }

    override fun onStartListening() {
        super.onStartListening()
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
    }

    override fun onClick() {
        super.onClick()

        if (isLocked) {
            unlockAndRun(Runnable { launchGenerator(); })
        } else {
            launchGenerator()
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("StartActivityAndCollapseDeprecated")
    fun launchGenerator() {
        val intent = tileIntentManager.createTileIntent("bitwarden://password_generator");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(tileIntentManager.createTilePendingIntent(intent))
        } else {
            startActivityAndCollapse(intent)
        }
    }
}
