package com.x8bit.bitwarden.ui.vault.feature.viewasqrcode.model

import android.os.Parcelable
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import kotlinx.parcelize.Parcelize

/**
 * Represents the different types of QR codes that can be generated.
 */
sealed class QrCodeType(val displayName: Text) : Parcelable {

    /**
     * Plain text QR code.
     */
    @Parcelize
    data object PlainText : QrCodeType(R.string.text.asText())

    /**
     * URL QR code.
     */
    @Parcelize
    data object Url : QrCodeType(R.string.url.asText())

    /**
     * Email QR code.
     */
    @Parcelize
    data object Email : QrCodeType(R.string.email.asText())

    /**
     * Phone number QR code.
     */
    @Parcelize
    data object Phone : QrCodeType(R.string.phone.asText())

    /**
     * SMS QR code.
     */
    @Parcelize
    data object SMS : QrCodeType(R.string.sms.asText())

    /**
     * WiFi network QR code.
     */
    @Parcelize
    data object WiFi : QrCodeType(R.string.wifi.asText())

    /**
     * vCard contact QR code.
     */
    @Parcelize
    data object Contact : QrCodeType(R.string.contact.asText())

    companion object {
        /**
         * List of all available QR code types.
         */
        val ALL = listOf(PlainText, Url, Email, Phone, SMS, WiFi, Contact)
    }
}

/**
 * Represents the configuration options for a QR code.
 */
@Parcelize
data class QrCodeConfig(
    val type: QrCodeType,
    val fields: Map<String, String> = emptyMap()
) : Parcelable
