package com.bitwarden.authenticator.ui.platform.feature.settings.appearance.model

import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.base.util.Text
import com.bitwarden.authenticator.ui.platform.base.util.asText

/**
 * Represents the languages supported by the app.
 */
enum class AppLanguage(
    val localeName: String?,
    val text: Text,
) {
    DEFAULT(
        localeName = null,
        text = R.string.default_system.asText(),
    ),
    AFRIKAANS(
        localeName = "af",
        text = "Afrikaans".asText(),
    ),
    BELARUSIAN(
        localeName = "be",
        text = "Беларуская".asText(),
    ),
    BULGARIAN(
        localeName = "bg",
        text = "български".asText(),
    ),
    CATALAN(
        localeName = "ca",
        text = "català".asText(),
    ),
    CZECH(
        localeName = "cs",
        text = "čeština".asText(),
    ),
    DANISH(
        localeName = "da",
        text = "Dansk".asText(),
    ),
    GERMAN(
        localeName = "de",
        text = "Deutsch".asText(),
    ),
    GREEK(
        localeName = "el",
        text = "Ελληνικά".asText(),
    ),
    ENGLISH(
        localeName = "en",
        text = "English".asText(),
    ),
    ENGLISH_BRITISH(
        localeName = "en-GB",
        text = "English (British)".asText(),
    ),
    SPANISH(
        localeName = "es",
        text = "Español".asText(),
    ),
    ESTONIAN(
        localeName = "et",
        text = "eesti".asText(),
    ),
    PERSIAN(
        localeName = "fa",
        text = "فارسی".asText(),
    ),
    FINNISH(
        localeName = "fi",
        text = "suomi".asText(),
    ),
    FRENCH(
        localeName = "fr",
        text = "Français".asText(),
    ),
    HINDI(
        localeName = "hi",
        text = "हिन्दी".asText(),
    ),
    CROATIAN(
        localeName = "hr",
        text = "hrvatski".asText(),
    ),
    HUNGARIAN(
        localeName = "hu",
        text = "magyar".asText(),
    ),
    INDONESIAN(
        localeName = "in",
        text = "Bahasa Indonesia".asText(),
    ),
    ITALIAN(
        localeName = "it",
        text = "Italiano".asText(),
    ),
    HEBREW(
        localeName = "iw",
        text = "עברית".asText(),
    ),
    JAPANESE(
        localeName = "ja",
        text = "日本語".asText(),
    ),
    KOREAN(
        localeName = "ko",
        text = "한국어".asText(),
    ),
    LATVIAN(
        localeName = "lv",
        text = "Latvietis".asText(),
    ),
    MALAYALAM(
        localeName = "ml",
        text = "മലയാളം".asText(),
    ),
    NORWEGIAN(
        localeName = "nb",
        text = "norsk (bokmål)".asText(),
    ),
    DUTCH(
        localeName = "nl",
        text = "Nederlands".asText(),
    ),
    POLISH(
        localeName = "pl",
        text = "Polski".asText(),
    ),
    PORTUGUESE_BRAZILIAN(
        localeName = "pt-BR",
        text = "Português do Brasil".asText(),
    ),
    PORTUGUESE(
        localeName = "pt-PT",
        text = "Português".asText(),
    ),
    ROMANIAN(
        localeName = "ro",
        text = "română".asText(),
    ),
    RUSSIAN(
        localeName = "ru",
        text = "русский".asText(),
    ),
    SLOVAK(
        localeName = "sk",
        text = "slovenčina".asText(),
    ),
    SWEDISH(
        localeName = "sv",
        text = "svenska".asText(),
    ),
    THAI(
        localeName = "th",
        text = "ไทย".asText(),
    ),
    TURKISH(
        localeName = "tr",
        text = "Türkçe".asText(),
    ),
    UKRAINIAN(
        localeName = "uk",
        text = "українська".asText(),
    ),
    VIETNAMESE(
        localeName = "vi",
        text = "Tiếng Việt".asText(),
    ),
    CHINESE_SIMPLIFIED(
        localeName = "zh-CN",
        text = "中文（中国大陆）".asText(),
    ),
    CHINESE_TRADITIONAL(
        localeName = "zh-TW",
        text = "中文（台灣）".asText(),
    ),
}
