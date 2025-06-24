package com.x8bit.bitwarden.data.autofill.manager.browser

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.bitwarden.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutoFillData

private const val CONTENT_PROVIDER_NAME = ".AutofillThirdPartyModeContentProvider"
private const val THIRD_PARTY_MODE_COLUMN = "autofill_third_party_state"
private const val THIRD_PARTY_MODE_ACTIONS_URI_PATH = "autofill_third_party_mode"

/**
 * Default implementation of the [BrowserThirdPartyAutofillManager] which uses a [ContentResolver]
 * to determine if the installed browser packages support and enable third party autofill services.
 *
 * Based off of [this blog post](https://android-developers.googleblog.com/2025/02/chrome-3p-autofill-services-update.html)
 */
@OmitFromCoverage
class BrowserThirdPartyAutofillManagerImpl(
    private val context: Context,
) : BrowserThirdPartyAutofillManager {
    override val stableBraveAutofillStatus: BrowserThirdPartyAutoFillData
        get() = getThirdPartyAutoFillStatusForChannel(BrowserPackage.BRAVE_RELEASE)
    override val stableChromeAutofillStatus: BrowserThirdPartyAutoFillData
        get() = getThirdPartyAutoFillStatusForChannel(BrowserPackage.CHROME_STABLE)
    override val betaChromeAutofillStatus: BrowserThirdPartyAutoFillData
        get() = getThirdPartyAutoFillStatusForChannel(BrowserPackage.CHROME_BETA)

    private fun getThirdPartyAutoFillStatusForChannel(
        releaseChannel: BrowserPackage,
    ): BrowserThirdPartyAutoFillData {
        val uri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(releaseChannel.packageName + CONTENT_PROVIDER_NAME)
            .path(THIRD_PARTY_MODE_ACTIONS_URI_PATH)
            .build()
        val cursor = context
            .contentResolver
            .query(
                /* uri = */ uri,
                /* projection = */ arrayOf(THIRD_PARTY_MODE_COLUMN),
                /* selection = */ null,
                /* selectionArgs = */ null,
                /* sortOrder = */ null,
            )
        var thirdPartyEnabled = false
        val isThirdPartyAvailable = cursor
            ?.let {
                it.moveToFirst()
                val columnIndex = it.getColumnIndex(THIRD_PARTY_MODE_COLUMN)
                thirdPartyEnabled = it.getInt(columnIndex) != 0
                it.close()
                true
            }
            ?: false
        return BrowserThirdPartyAutoFillData(
            isAvailable = isThirdPartyAvailable,
            isThirdPartyEnabled = thirdPartyEnabled,
        )
    }
}
