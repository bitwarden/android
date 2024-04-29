package com.x8bit.bitwarden.data.autofill.builder

import android.annotation.SuppressLint
import android.os.Build
import android.service.autofill.FillRequest
import android.service.autofill.SaveInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository

/**
 * The primary implementation of [SaveInfoBuilder].This is used for converting autofill data into
 * a save info.
 */
class SaveInfoBuilderImpl(
    val settingsRepository: SettingsRepository,
) : SaveInfoBuilder {

    @SuppressLint("InlinedApi")
    override fun build(
        autofillAppInfo: AutofillAppInfo,
        autofillPartition: AutofillPartition,
        fillRequest: FillRequest,
        packageName: String?,
    ): SaveInfo? {
        // Make sure that the save prompt is possible.
        val canPerformSaveRequest = autofillPartition.canPerformSaveRequest
        if (settingsRepository.isAutofillSavePromptDisabled || !canPerformSaveRequest) return null

        // Docs state that password fields cannot be reliably saved
        // in Compat mode since they show as masked values.
        val isInCompatMode = if (autofillAppInfo.sdkInt >= Build.VERSION_CODES.Q) {
            // Attempt to automatically establish compat request mode on Android 10+
            (fillRequest.flags or FillRequest.FLAG_COMPATIBILITY_MODE_REQUEST) == fillRequest.flags
        } else {
            COMPAT_BROWSERS.contains(packageName)
        }

        // If login and compat mode, the password might be obfuscated,
        // in which case we should skip the save request.
        return if (autofillPartition is AutofillPartition.Login && isInCompatMode) {
            null
        } else {
            SaveInfo
                .Builder(
                    autofillPartition.saveType,
                    autofillPartition.requiredSaveIds.toTypedArray(),
                )
                .apply {
                    // setOptionalIds will throw an IllegalArgumentException if the array is empty
                    autofillPartition
                        .optionalSaveIds
                        .takeUnless { it.isEmpty() }
                        ?.let { setOptionalIds(it.toTypedArray()) }
                    if (isInCompatMode) setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
                }
                .build()
        }
    }
}

/**
 * These browsers function using the compatibility shim for the Autofill Framework.
 *
 * Ensure that these entries are sorted alphabetically and keep this list synchronized with the
 * values in /xml/autofill_service_configuration.xml and
 * /xml-v30/autofill_service_configuration.xml.
 */
private val COMPAT_BROWSERS: List<String> = listOf(
    "alook.browser",
    "alook.browser.google",
    "app.vanadium.browser",
    "com.amazon.cloud9",
    "com.android.browser",
    "com.android.chrome",
    "com.android.htmlviewer",
    "com.avast.android.secure.browser",
    "com.avg.android.secure.browser",
    "com.brave.browser",
    "com.brave.browser_beta",
    "com.brave.browser_default",
    "com.brave.browser_dev",
    "com.brave.browser_nightly",
    "com.chrome.beta",
    "com.chrome.canary",
    "com.chrome.dev",
    "com.cookiegames.smartcookie",
    "com.cookiejarapps.android.smartcookieweb",
    "com.ecosia.android",
    "com.google.android.apps.chrome",
    "com.google.android.apps.chrome_dev",
    "com.google.android.captiveportallogin",
    "com.iode.firefox",
    "com.jamal2367.styx",
    "com.kiwibrowser.browser",
    "com.kiwibrowser.browser.dev",
    "com.lemurbrowser.exts",
    "com.microsoft.emmx",
    "com.microsoft.emmx.beta",
    "com.microsoft.emmx.canary",
    "com.microsoft.emmx.dev",
    "com.mmbox.browser",
    "com.mmbox.xbrowser",
    "com.mycompany.app.soulbrowser",
    "com.naver.whale",
    "com.neeva.app",
    "com.opera.browser",
    "com.opera.browser.beta",
    "com.opera.gx",
    "com.opera.mini.native",
    "com.opera.mini.native.beta",
    "com.opera.touch",
    "com.qflair.browserq",
    "com.qwant.liberty",
    "com.rainsee.create",
    "com.sec.android.app.sbrowser",
    "com.sec.android.app.sbrowser.beta",
    "com.stoutner.privacybrowser.free",
    "com.stoutner.privacybrowser.standard",
    "com.vivaldi.browser",
    "com.vivaldi.browser.snapshot",
    "com.vivaldi.browser.sopranos",
    "com.yandex.browser",
    "com.yjllq.internet",
    "com.yjllq.kito",
    "com.yujian.ResideMenuDemo",
    "com.z28j.feel",
    "idm.internet.download.manager",
    "idm.internet.download.manager.adm.lite",
    "idm.internet.download.manager.plus",
    "io.github.forkmaintainers.iceraven",
    "mark.via",
    "mark.via.gp",
    "net.dezor.browser",
    "net.slions.fulguris.full.download",
    "net.slions.fulguris.full.download.debug",
    "net.slions.fulguris.full.playstore",
    "net.slions.fulguris.full.playstore.debug",
    "org.adblockplus.browser",
    "org.adblockplus.browser.beta",
    "org.bromite.bromite",
    "org.bromite.chromium",
    "org.chromium.chrome",
    "org.codeaurora.swe.browser",
    "org.cromite.cromite",
    "org.gnu.icecat",
    "org.mozilla.fenix",
    "org.mozilla.fenix.nightly",
    "org.mozilla.fennec_aurora",
    "org.mozilla.fennec_fdroid",
    "org.mozilla.firefox",
    "org.mozilla.firefox_beta",
    "org.mozilla.reference.browser",
    "org.mozilla.rocket",
    "org.torproject.torbrowser",
    "org.torproject.torbrowser_alpha",
    "org.ungoogled.chromium.extensions.stable",
    "org.ungoogled.chromium.stable",
    "us.spotco.fennec_dos",
)
