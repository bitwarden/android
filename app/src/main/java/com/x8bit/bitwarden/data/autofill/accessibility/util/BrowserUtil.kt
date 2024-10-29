package com.x8bit.bitwarden.data.autofill.accessibility.util

import com.x8bit.bitwarden.data.autofill.accessibility.model.Browser

/**
 * Determines if the [String] receiver is a package name for a supported browser and returns that
 * [Browser] if it is a match.
 */
fun String.getSupportedBrowserOrNull(): Browser? =
    ACCESSIBILITY_SUPPORTED_BROWSERS.find { it.packageName == this@getSupportedBrowserOrNull }

/**
 * A list of supported browsers and the field ID used to find the url bar.
 *
 * This list should be kept in order and match the list of compatibility browsers in the
 * autofill_service_configuration.xml.
 */
private val ACCESSIBILITY_SUPPORTED_BROWSERS = listOf(
    Browser(packageName = "alook.browser", urlFieldId = "search_fragment_input_view"),
    Browser(packageName = "alook.browser.google", urlFieldId = "search_fragment_input_view"),
    Browser(packageName = "app.vanadium.browser", urlFieldId = "url_bar"),
    Browser(packageName = "com.amazon.cloud9", urlFieldId = "url"),
    Browser(packageName = "com.android.browser", urlFieldId = "url"),
    Browser(packageName = "com.android.chrome", urlFieldId = "url_bar"),
    // "com.android.htmlviewer": Doesn't have a URL bar
    Browser(packageName = "com.avast.android.secure.browser", urlFieldId = "editor"),
    Browser(packageName = "com.avg.android.secure.browser", urlFieldId = "editor"),
    Browser(packageName = "com.brave.browser", urlFieldId = "url_bar"),
    Browser(packageName = "com.brave.browser_beta", urlFieldId = "url_bar"),
    Browser(packageName = "com.brave.browser_default", urlFieldId = "url_bar"),
    Browser(packageName = "com.brave.browser_dev", urlFieldId = "url_bar"),
    Browser(packageName = "com.brave.browser_nightly", urlFieldId = "url_bar"),
    Browser(packageName = "com.chrome.beta", urlFieldId = "url_bar"),
    Browser(packageName = "com.chrome.canary", urlFieldId = "url_bar"),
    Browser(packageName = "com.chrome.dev", urlFieldId = "url_bar"),
    Browser(packageName = "com.cookiegames.smartcookie", urlFieldId = "search"),
    Browser(
        packageName = "com.cookiejarapps.android.smartcookieweb",
        urlFieldId = "mozac_browser_toolbar_url_view",
    ),
    Browser(packageName = "com.duckduckgo.mobile.android", urlFieldId = "omnibarTextInput"),
    Browser(packageName = "com.ecosia.android", urlFieldId = "url_bar"),
    Browser(packageName = "com.google.android.apps.chrome", urlFieldId = "url_bar"),
    Browser(packageName = "com.google.android.apps.chrome_dev", urlFieldId = "url_bar"),
    // "com.google.android.captiveportallogin": URL displayed in ActionBar subtitle without viewId
    Browser(packageName = "com.iode.firefox", urlFieldId = "mozac_browser_toolbar_url_view"),
    Browser(packageName = "com.jamal2367.styx", urlFieldId = "search"),
    Browser(packageName = "com.kiwibrowser.browser", urlFieldId = "url_bar"),
    Browser(packageName = "com.kiwibrowser.browser.dev", urlFieldId = "url_bar"),
    Browser(packageName = "com.microsoft.emmx", urlFieldId = "url_bar"),
    Browser(packageName = "com.microsoft.emmx.beta", urlFieldId = "url_bar"),
    Browser(packageName = "com.microsoft.emmx.canary", urlFieldId = "url_bar"),
    Browser(packageName = "com.microsoft.emmx.dev", urlFieldId = "url_bar"),
    Browser(packageName = "com.mmbox.browser", urlFieldId = "search_box"),
    Browser(packageName = "com.mmbox.xbrowser", urlFieldId = "search_box"),
    Browser(packageName = "com.mycompany.app.soulbrowser", urlFieldId = "edit_text"),
    Browser(packageName = "com.naver.whale", urlFieldId = "url_bar"),
    Browser(packageName = "com.neeva.app", urlFieldId = "full_url_text_view"),
    Browser(packageName = "com.opera.browser", urlFieldId = "url_field"),
    Browser(packageName = "com.opera.browser.beta", urlFieldId = "url_field"),
    Browser(packageName = "com.opera.gx", urlFieldId = "addressbarEdit"),
    Browser(packageName = "com.opera.mini.native", urlFieldId = "url_field"),
    Browser(packageName = "com.opera.mini.native.beta", urlFieldId = "url_field"),
    Browser(packageName = "com.opera.touch", urlFieldId = "addressbarEdit"),
    Browser(packageName = "com.qflair.browserq", urlFieldId = "url"),
    Browser(
        packageName = "com.qwant.liberty",
        // 2nd = Legacy (before v4)
        possibleUrlFieldIds = listOf("mozac_browser_toolbar_url_view", "url_bar_title"),
    ),
    Browser(packageName = "com.rainsee.create", urlFieldId = "search_box"),
    Browser(packageName = "com.sec.android.app.sbrowser", urlFieldId = "location_bar_edit_text"),
    Browser(
        packageName = "com.sec.android.app.sbrowser.beta",
        urlFieldId = "location_bar_edit_text",
    ),
    Browser(packageName = "com.stoutner.privacybrowser.free", urlFieldId = "url_edittext"),
    Browser(packageName = "com.stoutner.privacybrowser.standard", urlFieldId = "url_edittext"),
    Browser(packageName = "com.vivaldi.browser", urlFieldId = "url_bar"),
    Browser(packageName = "com.vivaldi.browser.snapshot", urlFieldId = "url_bar"),
    Browser(packageName = "com.vivaldi.browser.sopranos", urlFieldId = "url_bar"),
    Browser(
        packageName = "com.yandex.browser",
        possibleUrlFieldIds = listOf(
            "bro_omnibar_address_title_text",
            "bro_omnibox_collapsed_title",
        ),
        urlExtractor = {
            // 0 = Regular Space, 1 = No-break space (00A0)
            it.split(' ', ' ').firstOrNull()
        },
    ),
    Browser(packageName = "com.yjllq.internet", urlFieldId = "search_box"),
    Browser(packageName = "com.yjllq.kito", urlFieldId = "search_box"),
    Browser(packageName = "com.yujian.ResideMenuDemo", urlFieldId = "search_box"),
    Browser(packageName = "com.z28j.feel", urlFieldId = "g2"),
    Browser(packageName = "idm.internet.download.manager", urlFieldId = "search"),
    Browser(packageName = "idm.internet.download.manager.adm.lite", urlFieldId = "search"),
    Browser(packageName = "idm.internet.download.manager.plus", urlFieldId = "search"),
    Browser(
        packageName = "io.github.forkmaintainers.iceraven",
        urlFieldId = "mozac_browser_toolbar_url_view",
    ),
    Browser(packageName = "mark.via", urlFieldId = "am,an"),
    Browser(packageName = "mark.via.gp", urlFieldId = "as"),
    Browser(packageName = "net.dezor.browser", urlFieldId = "url_bar"),
    Browser(packageName = "net.slions.fulguris.full.download", urlFieldId = "search"),
    Browser(packageName = "net.slions.fulguris.full.download.debug", urlFieldId = "search"),
    Browser(packageName = "net.slions.fulguris.full.playstore", urlFieldId = "search"),
    Browser(packageName = "net.slions.fulguris.full.playstore.debug", urlFieldId = "search"),
    Browser(
        packageName = "org.adblockplus.browser",
        // 2nd = Legacy (before v2)
        possibleUrlFieldIds = listOf("url_bar", "url_bar_title"),
    ),
    Browser(
        packageName = "org.adblockplus.browser.beta",
        // 2nd = Legacy (before v2)
        possibleUrlFieldIds = listOf("url_bar", "url_bar_title"),
    ),
    Browser(packageName = "org.bromite.bromite", urlFieldId = "url_bar"),
    Browser(packageName = "org.bromite.chromium", urlFieldId = "url_bar"),
    Browser(packageName = "org.chromium.chrome", urlFieldId = "url_bar"),
    Browser(packageName = "org.codeaurora.swe.browser", urlFieldId = "url_bar"),
    Browser(packageName = "org.cromite.cromite", urlFieldId = "url_bar"),
    Browser(
        packageName = "org.gnu.icecat",
        // 2nd = Anticipation
        possibleUrlFieldIds = listOf("url_bar_title", "mozac_browser_toolbar_url_view"),
    ),
    Browser(packageName = "org.mozilla.fenix", urlFieldId = "mozac_browser_toolbar_url_view"),
    // [DEPRECATED ENTRY]
    Browser(
        packageName = "org.mozilla.fenix.nightly",
        urlFieldId = "mozac_browser_toolbar_url_view",
    ),
    // [DEPRECATED ENTRY]
    Browser(
        packageName = "org.mozilla.fennec_aurora",
        possibleUrlFieldIds = listOf("mozac_browser_toolbar_url_view", "url_bar_title"),
    ),
    Browser(
        packageName = "org.mozilla.fennec_fdroid",
        // 2nd = Legacy
        possibleUrlFieldIds = listOf("mozac_browser_toolbar_url_view", "url_bar_title"),
    ),
    Browser(
        packageName = "org.mozilla.firefox",
        // 2nd = Legacy
        possibleUrlFieldIds = listOf("mozac_browser_toolbar_url_view", "url_bar_title"),
    ),
    Browser(
        packageName = "org.mozilla.firefox_beta",
        // 2nd = Legacy
        possibleUrlFieldIds = listOf("mozac_browser_toolbar_url_view", "url_bar_title"),
    ),
    Browser(
        packageName = "org.mozilla.focus",
        // 2nd = Legacy
        possibleUrlFieldIds = listOf("mozac_browser_toolbar_url_view", "display_url"),
    ),
    Browser(
        packageName = "org.mozilla.focus.beta",
        // 2nd = Legacy
        possibleUrlFieldIds = listOf("mozac_browser_toolbar_url_view", "display_url"),
    ),
    Browser(
        packageName = "org.mozilla.focus.nightly",
        // 2nd = Legacy
        possibleUrlFieldIds = listOf("mozac_browser_toolbar_url_view", "display_url"),
    ),
    Browser(
        packageName = "org.mozilla.klar",
        // 2nd = Legacy
        possibleUrlFieldIds = listOf("mozac_browser_toolbar_url_view", "display_url"),
    ),
    Browser(
        packageName = "org.mozilla.reference.browser",
        urlFieldId = "mozac_browser_toolbar_url_view",
    ),
    Browser(packageName = "org.mozilla.rocket", urlFieldId = "display_url"),
    Browser(
        packageName = "org.torproject.torbrowser",
        // 2nd = Legacy (before v10.0.3)
        possibleUrlFieldIds = listOf("mozac_browser_toolbar_url_view", "url_bar_title"),
    ),
    Browser(
        packageName = "org.torproject.torbrowser_alpha",
        // 2nd = Legacy (before v10.0a8)
        possibleUrlFieldIds = listOf("mozac_browser_toolbar_url_view", "url_bar_title"),
    ),
    Browser(packageName = "org.ungoogled.chromium.extensions.stable", urlFieldId = "url_bar"),
    Browser(packageName = "org.ungoogled.chromium.stable", urlFieldId = "url_bar"),
    Browser(
        packageName = "us.spotco.fennec_dos",
        // 2nd = Legacy
        possibleUrlFieldIds = listOf("mozac_browser_toolbar_url_view", "url_bar_title"),
    ),

    // [Section B] Entries only present here
    // TODO: Test the compatibility of these with Autofill Framework
    Browser(packageName = "acr.browser.barebones", urlFieldId = "search"),
    Browser(packageName = "acr.browser.lightning", urlFieldId = "search"),
    Browser(packageName = "com.feedback.browser.wjbrowser", urlFieldId = "addressbar_url"),
    Browser(packageName = "com.ghostery.android.ghostery", urlFieldId = "search_field"),
    Browser(packageName = "com.htc.sense.browser", urlFieldId = "title"),
    Browser(packageName = "com.jerky.browser2", urlFieldId = "enterUrl"),
    Browser(packageName = "com.ksmobile.cb", urlFieldId = "address_bar_edit_text"),
    Browser(packageName = "com.lemurbrowser.exts", urlFieldId = "url_bar"),
    Browser(packageName = "com.linkbubble.playstore", urlFieldId = "url_text"),
    Browser(packageName = "com.mx.browser", urlFieldId = "address_editor_with_progress"),
    Browser(packageName = "com.mx.browser.tablet", urlFieldId = "address_editor_with_progress"),
    Browser(packageName = "com.nubelacorp.javelin", urlFieldId = "enterUrl"),
    Browser(packageName = "jp.co.fenrir.android.sleipnir", urlFieldId = "url_text"),
    Browser(packageName = "jp.co.fenrir.android.sleipnir_black", urlFieldId = "url_text"),
    Browser(packageName = "jp.co.fenrir.android.sleipnir_test", urlFieldId = "url_text"),
    Browser(packageName = "mobi.mgeek.TunnyBrowser", urlFieldId = "title"),
    Browser(packageName = "org.iron.srware", urlFieldId = "url_bar"),
)
