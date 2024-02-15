package com.x8bit.bitwarden.ui.platform.manager.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import com.x8bit.bitwarden.WebAuthCallbackActivity
import com.x8bit.bitwarden.data.autofill.util.toPendingIntentMutabilityFlag
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * The default implementation of the [NfcManager].
 */
@OmitFromCoverage
class NfcManagerImpl(
    private val activity: Activity,
) : NfcManager {
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    private val supportsNfc: Boolean get() = nfcAdapter?.isEnabled == true

    override fun start() {
        if (!supportsNfc) return
        nfcAdapter?.enableForegroundDispatch(
            activity,
            PendingIntent.getActivity(
                activity,
                1,
                Intent(activity, WebAuthCallbackActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
                ),
                PendingIntent.FLAG_UPDATE_CURRENT.toPendingIntentMutabilityFlag(),
            ),
            arrayOf(
                IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                    // Register for all NDEF tags starting with http or https
                    addDataScheme("http")
                    addDataScheme("https")
                },
            ),
            null,
        )
    }

    override fun stop() {
        if (!supportsNfc) return
        nfcAdapter?.disableForegroundDispatch(activity)
    }
}
