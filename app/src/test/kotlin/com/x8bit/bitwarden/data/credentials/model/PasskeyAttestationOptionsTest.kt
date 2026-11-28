package com.x8bit.bitwarden.data.credentials.model

import com.bitwarden.core.data.util.decodeFromStringOrNull
import com.bitwarden.core.di.CoreModule
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

class PasskeyAttestationOptionsTest {

    private val json = CoreModule.providesJson(buildInfoManager = mockk(relaxed = true))

    @Test
    fun `options without authenticatorSelection should be deserialized`() {
        val result = json.decodeFromStringOrNull<PasskeyAttestationOptions>(
            OPTIONS_WITHOUT_AUTHENTICATOR_SELECTION_JSON,
        )

        assertNotNull(result)
        assertEquals(RELYING_PARTY_ID, result.relyingParty.id)
    }

    @Test
    fun `excludeCredentials should be deserialized from the spec member name`() {
        val result = json.decodeFromStringOrNull<PasskeyAttestationOptions>(OPTIONS_JSON)

        assertNotNull(result)
        assertEquals(
            listOf(EXCLUDED_CREDENTIAL_ID),
            result.excludeCredentials.map { it.id },
        )
    }

    @Test
    fun `excludeCredentials should be serialized with the spec member name`() {
        val options = createOptions(
            excludeCredentials = listOf(
                PublicKeyCredentialDescriptor(
                    type = "public-key",
                    id = EXCLUDED_CREDENTIAL_ID,
                    transports = null,
                ),
            ),
        )

        val result = json.encodeToString(options)

        assertTrue(result.contains("\"excludeCredentials\""))
        assertFalse(result.contains("\"excludedCredentials\""))
    }

    @Test
    fun `cross-platform attachment should be deserialized from the spec value`() {
        val result = json.decodeFromStringOrNull<PasskeyAttestationOptions>(OPTIONS_JSON)

        assertNotNull(result)
        assertEquals(
            PasskeyAttestationOptions
                .AuthenticatorSelectionCriteria
                .AuthenticatorAttachment
                .CROSS_PLATFORM,
            result.authenticatorSelection?.authenticatorAttachment,
        )
    }

    @Test
    fun `cross-platform attachment should be serialized with the spec value`() {
        val options = createOptions(
            authenticatorSelection = PasskeyAttestationOptions.AuthenticatorSelectionCriteria(
                authenticatorAttachment = PasskeyAttestationOptions
                    .AuthenticatorSelectionCriteria
                    .AuthenticatorAttachment
                    .CROSS_PLATFORM,
            ),
        )

        val result = json.encodeToString(options)

        assertTrue(result.contains("\"cross-platform\""))
        assertFalse(result.contains("\"cross_platform\""))
    }

    @Test
    fun `discouraged residentKey should be deserialized from the spec value`() {
        val result = json.decodeFromStringOrNull<PasskeyAttestationOptions>(OPTIONS_JSON)

        assertNotNull(result)
        assertEquals(
            PasskeyAttestationOptions
                .AuthenticatorSelectionCriteria
                .ResidentKeyRequirement
                .DISCOURAGED,
            result.authenticatorSelection?.residentKeyRequirement,
        )
    }

    @Test
    fun `discouraged residentKey should be serialized with the spec value`() {
        val options = createOptions(
            authenticatorSelection = PasskeyAttestationOptions.AuthenticatorSelectionCriteria(
                residentKeyRequirement = PasskeyAttestationOptions
                    .AuthenticatorSelectionCriteria
                    .ResidentKeyRequirement
                    .DISCOURAGED,
            ),
        )

        val result = json.encodeToString(options)

        assertTrue(result.contains("\"discouraged\""))
    }
}

private const val RELYING_PARTY_ID = "www.bitwarden.com"
private const val EXCLUDED_CREDENTIAL_ID = "mockCredentialId"

private fun createOptions(
    authenticatorSelection: PasskeyAttestationOptions.AuthenticatorSelectionCriteria? = null,
    excludeCredentials: List<PublicKeyCredentialDescriptor> = emptyList(),
): PasskeyAttestationOptions = PasskeyAttestationOptions(
    authenticatorSelection = authenticatorSelection,
    challenge = "tZ1rLJ_paLC8IMmg",
    excludeCredentials = excludeCredentials,
    pubKeyCredParams = listOf(
        PasskeyAttestationOptions.PublicKeyCredentialParameters(
            type = "public-key",
            alg = -7.0,
        ),
    ),
    relyingParty = PasskeyAttestationOptions.PublicKeyCredentialRpEntity(
        id = RELYING_PARTY_ID,
        name = "mockRpName",
    ),
    user = PasskeyAttestationOptions.PublicKeyCredentialUserEntity(
        id = "UmhpTE9NOUY",
        name = "mockUserName",
        displayName = "mockDisplayName",
    ),
)

private val OPTIONS_JSON = """
{
  "authenticatorSelection": {
    "authenticatorAttachment": "cross-platform",
    "residentKey": "discouraged",
    "userVerification": "preferred"
  },
  "challenge": "tZ1rLJ_paLC8IMmg",
  "excludeCredentials": [
    {
      "type": "public-key",
      "id": "$EXCLUDED_CREDENTIAL_ID"
    }
  ],
  "pubKeyCredParams": [
    {
      "alg": -7,
      "type": "public-key"
    }
  ],
  "rp": {
    "id": "$RELYING_PARTY_ID",
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

private val OPTIONS_WITHOUT_AUTHENTICATOR_SELECTION_JSON = """
{
  "challenge": "tZ1rLJ_paLC8IMmg",
  "pubKeyCredParams": [
    {
      "alg": -7,
      "type": "public-key"
    }
  ],
  "rp": {
    "id": "$RELYING_PARTY_ID",
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
