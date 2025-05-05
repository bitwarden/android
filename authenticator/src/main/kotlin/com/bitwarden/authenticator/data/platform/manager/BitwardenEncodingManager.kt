package com.bitwarden.authenticator.data.platform.manager

/**
 * An interface for encoding and decoding data.
 */
interface BitwardenEncodingManager {

    /**
     * Decodes '%'-escaped octets in the given string.
     */
    fun uriDecode(value: String): String

    /**
     * Decodes the specified [value], and returns the resulting [ByteArray].
     */
    fun base64Decode(value: String): ByteArray

    /**
     * Encodes the specified [byteArray], and returns the encoded String.
     */
    fun base32Encode(byteArray: ByteArray): String
}
