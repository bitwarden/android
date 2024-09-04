package com.x8bit.bitwarden.data.tiles

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.Keep
import com.x8bit.bitwarden.AccessibilityActivity
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityAutofillManager
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * A service for handling the Autofill quick settings tile.
 */
@AndroidEntryPoint
@Keep
@OmitFromCoverage
class BitwardenAutofillTileService : TileService() {
    @Inject
    lateinit var accessibilityAutofillManager: AccessibilityAutofillManager

    override fun onClick() {
        if (isLocked) {
            unlockAndRun { launchAutofill() }
        } else {
            launchAutofill()
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun launchAutofill() {
        accessibilityAutofillManager.isAccessibilityTileClicked = true
        val intent = Intent(applicationContext, AccessibilityActivity::class.java)
        if (isBuildVersionBelow(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        } else {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    applicationContext,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }
    }
}
