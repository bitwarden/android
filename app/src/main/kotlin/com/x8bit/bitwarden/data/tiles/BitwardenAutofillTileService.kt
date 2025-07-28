package com.x8bit.bitwarden.data.tiles

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.Keep
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.AccessibilityActivity
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityAutofillManager
import com.x8bit.bitwarden.data.autofill.accessibility.model.AccessibilityAction
import com.x8bit.bitwarden.data.autofill.accessibility.util.isAccessibilityServiceEnabled
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
        if (!applicationContext.isAccessibilityServiceEnabled) {
            showDialog(getAccessibilityServiceRequiredDialog())
            return
        }
        accessibilityAutofillManager.accessibilityAction = AccessibilityAction.AttemptParseUri
        val intent = Intent(applicationContext, AccessibilityActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (!isBuildVersionAtLeast(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) {
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

    private fun getAccessibilityServiceRequiredDialog(): Dialog =
        AlertDialog.Builder(this)
            .setMessage(BitwardenString.autofill_tile_accessibility_required)
            .setCancelable(true)
            .setPositiveButton(BitwardenString.okay) { dialog, _ -> dialog.cancel() }
            .create()
}
