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
     * Map of field keys to their definitions for this QR code type.
     */
    val fields: Map<String, QrCodeTypeField>
        get() = when (this) {
            WIFI -> mapOf(
                "ssid" to QrCodeTypeField(R.string.ssid.asText(), isRequired = true),
                "password" to QrCodeTypeField(R.string.password.asText(), isRequired = false),
//                "encryption" to QrCodeTypeField(R.string.encryption_type.asText(), isRequired = false,
//                    options = listOf("WPA", "None"), defaultValue = "WPA"),
//                "hidden" to QrCodeTypeField(R.string.hidden.asText(), isRequired = false,
//                    options = listOf("true", "false"), defaultValue = "false")
            )
            URL -> mapOf(
                "url" to QrCodeTypeField(R.string.url.asText(), isRequired = true)
            )
            PLAIN_TEXT -> mapOf(
                "text" to QrCodeTypeField(R.string.text.asText(), isRequired = true)
            )
            EMAIL -> mapOf(
                "email" to QrCodeTypeField(R.string.email.asText(), isRequired = true),
                "subject" to QrCodeTypeField(R.string.subject.asText(), isRequired = false),
                "body" to QrCodeTypeField(R.string.body.asText(), isRequired = false)
            )
            PHONE -> mapOf(
                "phone" to QrCodeTypeField(R.string.phone.asText(), isRequired = true)
            )
            CONTACT_VCARD, CONTACT_MECARD -> mapOf(
                "name" to QrCodeTypeField(R.string.name.asText(), isRequired = true),
                "phone" to QrCodeTypeField(R.string.phone.asText(), isRequired = false),
                "email" to QrCodeTypeField(R.string.email.asText(), isRequired = false),
                "organization" to QrCodeTypeField(R.string.organization.asText(), isRequired = false),
                "address" to QrCodeTypeField(R.string.address.asText(), isRequired = false),
                "website" to QrCodeTypeField(R.string.url.asText(), isRequired = false)
            )
        }
}

/**
 * Defines a field for a QR code type.
 *
 * @property displayName The human-readable label for this field
 * @property isRequired Whether this field is required
 * @property options List of valid options if this is a selection field
 * @property defaultValue Default value for this field
 */
@Parcelize
data class QrCodeTypeField(
    val displayName: Text,
    val isRequired: Boolean = false,
    val options: List<String> = emptyList(),
    val defaultValue: String = ""
) : Parcelable
