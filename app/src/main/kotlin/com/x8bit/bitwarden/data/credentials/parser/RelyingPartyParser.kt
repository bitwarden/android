package com.x8bit.bitwarden.data.credentials.parser

import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption

/**
 * A tool for parsing relying party data from the Credential Manager requests.
 */
interface RelyingPartyParser {
    /**
     * Parse the relying party ID from the [GetPublicKeyCredentialOption].
     */
    fun parse(getPublicKeyCredentialOption: GetPublicKeyCredentialOption): String?

    /**
     * Parse the relying party ID from the [CreatePublicKeyCredentialRequest].
     */
    fun parse(createPublicKeyCredentialRequest: CreatePublicKeyCredentialRequest): String?

    /**
     * Parse the relying party ID from the [BeginGetPublicKeyCredentialOption].
     */
    fun parse(beginGetPublicKeyCredentialOption: BeginGetPublicKeyCredentialOption): String?
}
