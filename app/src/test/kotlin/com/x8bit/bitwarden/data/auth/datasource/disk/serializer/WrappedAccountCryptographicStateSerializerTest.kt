package com.x8bit.bitwarden.data.auth.datasource.disk.serializer

import com.bitwarden.core.WrappedAccountCryptographicState
import com.bitwarden.core.di.CoreModule
import io.mockk.mockk
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WrappedAccountCryptographicStateSerializerTest {
    private val json = CoreModule.providesJson(buildInfoManager = mockk(relaxed = true))
    private val serializer = WrappedAccountCryptographicStateSerializer()

    @Test
    fun `serialize V1 produces JSON with v1 type discriminator`() {
        assertEquals(
            buildJsonObject {
                put(key = "type", value = "v1")
                put(key = "privateKey", value = "mockPrivateKey")
            },
            json.encodeToJsonElement(
                serializer = serializer,
                value = WrappedAccountCryptographicState.V1(privateKey = "mockPrivateKey"),
            ),
        )
    }

    @Test
    fun `serialize V2 produces JSON with v2 type discriminator and all properties`() {
        assertEquals(
            buildJsonObject {
                put(key = "type", value = "v2")
                put(key = "privateKey", value = "mockPrivateKey")
                put(key = "signingKey", value = "mockSigningKey")
                put(key = "signedPublicKey", value = "mockSignedPublicKey")
                put(key = "securityState", value = "mockSecurityState")
            },
            json.encodeToJsonElement(
                serializer = serializer,
                value = WrappedAccountCryptographicState.V2(
                    privateKey = "mockPrivateKey",
                    signingKey = "mockSigningKey",
                    signedPublicKey = "mockSignedPublicKey",
                    securityState = "mockSecurityState",
                ),
            ),
        )
    }

    @Test
    fun `serialize V2 with null signedPublicKey omits the signedPublicKey key`() {
        assertEquals(
            buildJsonObject {
                put(key = "type", value = "v2")
                put(key = "privateKey", value = "mockPrivateKey")
                put(key = "signingKey", value = "mockSigningKey")
                put(key = "securityState", value = "mockSecurityState")
            },
            json.encodeToJsonElement(
                serializer = serializer,
                value = WrappedAccountCryptographicState.V2(
                    privateKey = "mockPrivateKey",
                    signingKey = "mockSigningKey",
                    signedPublicKey = null,
                    securityState = "mockSecurityState",
                ),
            ),
        )
    }

    @Test
    fun `deserialize v1 JSON produces a V1 state`() {
        assertEquals(
            WrappedAccountCryptographicState.V1(privateKey = "mockPrivateKey"),
            json.decodeFromString(
                deserializer = serializer,
                string = """
                    {
                      "type": "v1",
                      "privateKey": "mockPrivateKey"
                    }
                """,
            ),
        )
    }

    @Test
    fun `deserialize v2 JSON produces a V2 state`() {
        assertEquals(
            WrappedAccountCryptographicState.V2(
                privateKey = "mockPrivateKey",
                signingKey = "mockSigningKey",
                signedPublicKey = "mockSignedPublicKey",
                securityState = "mockSecurityState",
            ),
            json.decodeFromString(
                deserializer = serializer,
                string = """
                    {
                      "type": "v2",
                      "privateKey": "mockPrivateKey",
                      "signingKey": "mockSigningKey",
                      "signedPublicKey": "mockSignedPublicKey",
                      "securityState": "mockSecurityState"
                    }
                """,
            ),
        )
    }

    @Test
    fun `deserialize v2 JSON with missing signedPublicKey produces a V2 state with null`() {
        assertEquals(
            WrappedAccountCryptographicState.V2(
                privateKey = "mockPrivateKey",
                signingKey = "mockSigningKey",
                signedPublicKey = null,
                securityState = "mockSecurityState",
            ),
            json.decodeFromString(
                deserializer = serializer,
                string = """
                    {
                      "type": "v2",
                      "privateKey": "mockPrivateKey",
                      "signingKey": "mockSigningKey",
                      "securityState": "mockSecurityState"
                    }
                """,
            ),
        )
    }

    @Test
    fun `serialize then deserialize V1 returns an equivalent state`() {
        val original = WrappedAccountCryptographicState.V1(privateKey = "mockPrivateKey")
        assertEquals(
            original,
            json.decodeFromString(
                deserializer = serializer,
                string = json.encodeToString(serializer = serializer, value = original),
            ),
        )
    }

    @Test
    fun `serialize then deserialize V2 returns an equivalent state`() {
        val original = WrappedAccountCryptographicState.V2(
            privateKey = "mockPrivateKey",
            signingKey = "mockSigningKey",
            signedPublicKey = "mockSignedPublicKey",
            securityState = "mockSecurityState",
        )
        assertEquals(
            original,
            json.decodeFromString(
                deserializer = serializer,
                string = json.encodeToString(serializer = serializer, value = original),
            ),
        )
    }
}
