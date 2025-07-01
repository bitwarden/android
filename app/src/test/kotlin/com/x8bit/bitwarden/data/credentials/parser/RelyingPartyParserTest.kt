package com.x8bit.bitwarden.data.credentials.parser

import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import com.bitwarden.core.di.CoreModule
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

class RelyingPartyParserTest {

    private val relyingPartyParser = RelyingPartyParserImpl(json = CoreModule.providesJson())

    @Test
    fun `parse GetPublicKeyCredentialOption should return relyingPartyId`() {
        val result = relyingPartyParser.parse(
            mockk<GetPublicKeyCredentialOption> {
                every { requestJson } returns DEFAULT_ASSERTION_OPTIONS_JSON
            },
        )

        assertEquals(
            DEFAULT_RELYING_PARTY_ID,
            result,
        )
    }

    @Test
    fun `parse GetPublicKeyCredentialOption should return null if relyingPartyId is missing`() {
        val result = relyingPartyParser.parse(
            mockk<GetPublicKeyCredentialOption> {
                every { requestJson } returns INVALID_ASSERTION_OPTIONS_JSON
            },
        )

        assertNull(result)
    }

    @Test
    fun `parse CreatePublicKeyCredentialRequest should return relyingPartyId`() {
        val result = relyingPartyParser.parse(
            mockk<CreatePublicKeyCredentialRequest> {
                every { requestJson } returns DEFAULT_ATTESTATION_OPTIONS_JSON
            },
        )

        assertEquals(
            DEFAULT_RELYING_PARTY_ID,
            result,
        )
    }

    @Test
    fun `parse CreatePublicKeyCredentialRequest should return null if relyingPartyId is missing`() {
        val result = relyingPartyParser.parse(
            mockk<CreatePublicKeyCredentialRequest> {
                every { requestJson } returns INVALID_ATTESTATION_OPTIONS_JSON
            },
        )

        assertNull(result)
    }

    @Test
    fun `parse BeginGetPublicKeyCredentialOption should return relyingPartyId`() {
        val result = relyingPartyParser.parse(
            mockk<BeginGetPublicKeyCredentialOption> {
                every { requestJson } returns DEFAULT_ASSERTION_OPTIONS_JSON
            },
        )

        assertEquals(
            DEFAULT_RELYING_PARTY_ID,
            result,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `parse BeginGetPublicKeyCredentialOption should return null if relyingPartyId is missing`() {
        val result = relyingPartyParser.parse(
            mockk<BeginGetPublicKeyCredentialOption> {
                every { requestJson } returns INVALID_ASSERTION_OPTIONS_JSON
            },
        )

        assertNull(result)
    }
}

private const val DEFAULT_RELYING_PARTY_ID = "www.bitwarden.com"
private val DEFAULT_ATTESTATION_OPTIONS_JSON = """
{
  "attestation": "direct",
  "authenticatorSelection": {
    "residentKey": "required",
    "userVerification": "preferred"
  },
  "challenge": "tZ1rLJ_paLC8IMmg",
  "excludeCredentials": [],
  "extensions": {
    "credProps": true
  },
  "pubKeyCredParams": [
    {
      "alg": -7,
      "type": "public-key"
    },
    {
      "alg": -257,
      "type": "public-key"
    }
  ],
  "rp": {
    "id": "$DEFAULT_RELYING_PARTY_ID",
    "name": "mockRpName"
  },
  "user": {
    "displayName": "mockDisplayName",
    "id": "UmhpTE9NOUY",
    "name": "mockUserName"
  }
}
"""
    .trimIndent()
private val INVALID_ATTESTATION_OPTIONS_JSON = """
{
  "attestation": "direct",
  "authenticatorSelection": {
    "residentKey": "required",
    "userVerification": "preferred"
  },
  "challenge": "tZ1rLJ_paLC8IMmg",
  "excludeCredentials": [],
  "extensions": {
    "credProps": true
  },
  "pubKeyCredParams": [
    {
      "alg": -7,
      "type": "public-key"
    },
    {
      "alg": -257,
      "type": "public-key"
    }
  ],
  "rp": {
    "name": "mockRpName"
  },
  "user": {
    "displayName": "mockDisplayName",
    "id": "UmhpTE9NOUY",
    "name": "mockUserName"
  }
}
"""
    .trimIndent()
private val DEFAULT_ASSERTION_OPTIONS_JSON = """
{
  "challenge": "FFeZc7g-BPSAPo",
  "allowCredentials": [],
  "timeout": 60000,
  "userVerification": "preferred",
  "rpId": "$DEFAULT_RELYING_PARTY_ID"
}
"""
    .trimIndent()
private val INVALID_ASSERTION_OPTIONS_JSON = """
{
  "challenge": "FFeZc7g-BPSAPo",
  "allowCredentials": [],
  "timeout": 60000,
  "userVerification": "preferred",
}
"""
    .trimIndent()
