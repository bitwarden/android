package com.x8bit.bitwarden.data.autofill.manager.browser

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.net.toUri
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserPackage
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutoFillData
import kotlinx.coroutines.withContext

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
    private val dispatcherManager: DispatcherManager,
) : BrowserThirdPartyAutofillManager {
    override suspend fun getStableBraveAutofillStatus(): BrowserThirdPartyAutoFillData =
        getThirdPartyAutoFillStatusForChannel(BrowserPackage.BRAVE_RELEASE)

    override suspend fun getStableChromeAutofillStatus(): BrowserThirdPartyAutoFillData =
        getThirdPartyAutoFillStatusForChannel(BrowserPackage.CHROME_STABLE)

    override suspend fun getBetaChromeAutofillStatus(): BrowserThirdPartyAutoFillData =
        getThirdPartyAutoFillStatusForChannel(BrowserPackage.CHROME_BETA)

    override suspend fun getStableVivaldiAutofillStatus(): BrowserThirdPartyAutoFillData =
        getThirdPartyAutoFillStatusForChannel(BrowserPackage.VIVALDI_STABLE)

    override suspend fun getDefaultBrowserPackageName(): String? =
        withContext(context = dispatcherManager.io) {
            val intent = Intent(Intent.ACTION_VIEW, "https://example.com".toUri())
            context
                .packageManager
                .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo
                ?.packageName
        }

    private suspend fun getThirdPartyAutoFillStatusForChannel(
        releaseChannel: BrowserPackage,
    ): BrowserThirdPartyAutoFillData = withContext(context = dispatcherManager.io) {
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
            ?.use {
                if (it.moveToFirst()) {
                    thirdPartyEnabled = it
                        .getColumnIndex(THIRD_PARTY_MODE_COLUMN)
                        .takeUnless { columnIndex -> columnIndex == -1 }
                        ?.let { columnIndex -> it.getInt(columnIndex) != 0 }
                        ?: false
                    true
                } else {
                    false
                }
            }
            ?: false
        BrowserThirdPartyAutoFillData(
            isAvailable = isThirdPartyAvailable,
            isThirdPartyEnabled = thirdPartyEnabled,
        )
    }
}
