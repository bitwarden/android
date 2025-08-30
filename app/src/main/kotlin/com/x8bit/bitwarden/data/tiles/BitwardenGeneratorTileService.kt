package com.x8bit.bitwarden.data.tiles

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.Keep
import androidx.core.net.toUri
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.x8bit.bitwarden.MainActivity

/**
 * A service for handling the Password Generator quick settings tile.
 */
@Keep
@OmitFromCoverage
class BitwardenGeneratorTileService : TileService() {

    override fun onClick() {
        if (isLocked) {
            unlockAndRun { launchGenerator() }
        } else {
            launchGenerator()
        }
    }

    private fun launchGenerator() {
        val intent = Intent(applicationContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .setData("bitwarden://password_generator".toUri())
        if (!isBuildVersionAtLeast(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) {
            @Suppress("DEPRECATION")
            @SuppressLint("StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        } else {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }
    }
}
