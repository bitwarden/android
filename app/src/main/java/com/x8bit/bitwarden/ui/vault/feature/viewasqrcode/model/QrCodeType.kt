package com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.model

import android.os.Parcelable
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import kotlinx.parcelize.Parcelize

/**
 * Represents the different types of QR codes that can be generated.
 */
enum class QrCodeType(val displayName: Text) {
    /**
     * WiFi network QR code.
     */
    WIFI(R.string.wifi.asText()),

    /**
     * URL QR code.
     */
    URL(R.string.url.asText()),

    /**
     * Plain text QR code.
     */
    PLAIN_TEXT(R.string.text.asText()),

    /**
     * Email QR code.
     */
    EMAIL(R.string.email.asText()),

    /**
     * Phone number QR code.
     */
    PHONE(R.string.phone.asText()),

    /**
     * vCard contact QR code.
     */
    CONTACT_VCARD(R.string.contact_vcard.asText()),

    /**
     * meCard contact QR code.
     */
    CONTACT_MECARD(R.string.contact_mecard.asText());

    /**
     * List of field definitions for this QR code type.
     */
    val fields: List<QrCodeTypeField>
        get() = when (this) {
            WIFI -> listOf(
                QrCodeTypeField("ssid", R.string.ssid.asText(), isRequired = true),
                QrCodeTypeField("password", R.string.password.asText(), isRequired = false),
                //QrCodeTypeField("options", R.string.password.asText(), isRequired = false),
            )

            URL -> listOf(
                QrCodeTypeField("url", R.string.url.asText(), isRequired = true)
            )

            PLAIN_TEXT -> listOf(
                QrCodeTypeField("text", R.string.text.asText(), isRequired = true)
            )

            EMAIL -> listOf(
                QrCodeTypeField("email", R.string.email.asText(), isRequired = true),
                QrCodeTypeField("subject", R.string.subject.asText(), isRequired = false),
                QrCodeTypeField("body", R.string.body.asText(), isRequired = false)
            )

            PHONE -> listOf(
                QrCodeTypeField("phone", R.string.phone.asText(), isRequired = true)
            )

            CONTACT_VCARD, CONTACT_MECARD -> listOf(
                QrCodeTypeField("name", R.string.name.asText(), isRequired = true),
                QrCodeTypeField("phone", R.string.phone.asText(), isRequired = false),
                QrCodeTypeField("email", R.string.email.asText(), isRequired = false),
                QrCodeTypeField(
                    key = "organization",
                    displayName = R.string.organization.asText(),
                    isRequired = false
                ),
                QrCodeTypeField("address", R.string.address.asText(), isRequired = false),
                QrCodeTypeField("website", R.string.url.asText(), isRequired = false)
            )
        }
}

/**
 * Defines a field for a QR code type.
 *
 * @property key The unique identifier for this field
 * @property displayName The human-readable label for this field
 * @property isRequired Whether this field is required
 * @property options List of valid options if this is a selection field
 * @property defaultValue Default value for this field
 * @property selectedOption Display text for the selected option (for dropdown fields)
 */
@Parcelize
data class QrCodeTypeField(
    val key: String,
    val displayName: Text,
    val isRequired: Boolean = false,
    var value: Text = "".asText(),
) : Parcelable
