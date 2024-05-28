package com.bitwarden.authenticator.data.platform.manager

import android.net.Uri
import com.google.common.io.BaseEncoding

/**
 * Default implementation of [BitwardenEncodingManager].
 */
class BitwardenEncodingManagerImpl : BitwardenEncodingManager {
    override fun uriDecode(value: String): String = Uri.decode(value)

    override fun base64Decode(value: String): ByteArray = BaseEncoding.base64().decode(value)

    override fun base32Encode(byteArray: ByteArray): String =
        BaseEncoding.base32().encode(byteArray)
}
