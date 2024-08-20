package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.AuthenticatedAccountsApi
import com.x8bit.bitwarden.data.auth.datasource.network.api.AuthenticatedKeyConnectorApi
import com.x8bit.bitwarden.data.auth.datasource.network.api.UnauthenticatedAccountsApi
import com.x8bit.bitwarden.data.auth.datasource.network.api.UnauthenticatedKeyConnectorApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.KdfTypeJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KeyConnectorKeyRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.KeyConnectorMasterKeyResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.PasswordHintResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.ResendEmailRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.ResetPasswordRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.SetPasswordRequestJson
import com.x8bit.bitwarden.data.platform.base.BaseServiceTest
import com.x8bit.bitwarden.data.platform.util.asSuccess
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.create

class AccountsServiceTest : BaseServiceTest() {

    private val unauthenticatedAccountsApi: UnauthenticatedAccountsApi = retrofit.create()
    private val authenticatedAccountsApi: AuthenticatedAccountsApi = retrofit.create()
    private val unauthenticatedKeyConnectorApi: UnauthenticatedKeyConnectorApi = retrofit.create()
    private val authenticatedKeyConnectorApi: AuthenticatedKeyConnectorApi = retrofit.create()
    private val service = AccountsServiceImpl(
        unauthenticatedAccountsApi = unauthenticatedAccountsApi,
        authenticatedAccountsApi = authenticatedAccountsApi,
        unauthenticatedKeyConnectorApi = unauthenticatedKeyConnectorApi,
        authenticatedKeyConnectorApi = authenticatedKeyConnectorApi,
        json = json,
    )

    @Test
    fun `convertToKeyConnector with empty response is success`() = runTest {
        val response = MockResponse().setBody("")
        server.enqueue(response)

        val result = service.convertToKeyConnector()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `createAccountKeys with empty response is success`() = runTest {
        val publicKey = "publicKey"
        val encryptedPrivateKey = "encryptedPrivateKey"
        val json = ""
        val response = MockResponse().setBody(json)
        server.enqueue(response)

        val result = service.createAccountKeys(
            publicKey = publicKey,
            encryptedPrivateKey = encryptedPrivateKey,
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun `deleteAccount with empty response is success`() = runTest {
        val masterPasswordHash = "37y4d8r379r4789nt387r39k3dr87nr93"
        val oneTimePassword = null
        val json = ""
        val response = MockResponse().setBody(json)
        server.enqueue(response)
        assertTrue(service.deleteAccount(masterPasswordHash, oneTimePassword).isSuccess)
    }

    @Test
    fun `requestOtp success should return Success`() = runTest {
        val response = MockResponse().setResponseCode(200)
        server.enqueue(response)

        val result = service.requestOneTimePasscode()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `requestOtp failure should return Failure`() = runTest {
        val response = MockResponse().setResponseCode(400)
        server.enqueue(response)

        val result = service.requestOneTimePasscode()

        assertTrue(result.isFailure)
    }

    @Test
    fun `verifyOtp success should return Success`() = runTest {
        val response = MockResponse().setResponseCode(200)
        server.enqueue(response)

        val result = service.verifyOneTimePasscode("passcode")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `verifyOtp failure should return Failure`() = runTest {
        val response = MockResponse().setResponseCode(400)
        server.enqueue(response)

        val result = service.verifyOneTimePasscode("passcode")

        assertTrue(result.isFailure)
    }

    @Test
    fun `requestPasswordHint success should return Success`() = runTest {
        val email = "test@example.com"
        val response = MockResponse().setResponseCode(200).setBody("{}")
        server.enqueue(response)

        val result = service.requestPasswordHint(email)

        assertTrue(result.isSuccess)
        assertEquals(PasswordHintResponseJson.Success, result.getOrNull())
    }

    @Test
    fun `resendVerificationCodeEmail with empty response is success`() = runTest {
        val response = MockResponse().setBody("")
        server.enqueue(response)
        val result = service.resendVerificationCodeEmail(
            body = ResendEmailRequestJson(
                deviceIdentifier = "3",
                email = "example@email.com",
                passwordHash = "37y4d8r379r4789nt387r39k3dr87nr93",
                ssoToken = null,
            ),
        )
        assertTrue(result.isSuccess)
    }

    @Test
    fun `resetPassword with empty response is success`() = runTest {
        val response = MockResponse().setBody("")
        server.enqueue(response)
        val result = service.resetPassword(
            body = ResetPasswordRequestJson(
                currentPasswordHash = "",
                newPasswordHash = "",
                passwordHint = null,
                key = "",
            ),
        )
        assertTrue(result.isSuccess)
    }

    @Test
    fun `resetPassword with empty response and null current password is success`() = runTest {
        val response = MockResponse().setBody("")
        server.enqueue(response)
        val result = service.resetPassword(
            body = ResetPasswordRequestJson(
                currentPasswordHash = null,
                newPasswordHash = "",
                passwordHint = null,
                key = "",
            ),
        )
        assertTrue(result.isSuccess)
    }

    @Test
    fun `setPassword with empty response is success`() = runTest {
        val response = MockResponse().setBody("")
        server.enqueue(response)
        val result = service.setPassword(
            body = SetPasswordRequestJson(
                passwordHash = "passwordHash",
                passwordHint = "passwordHint",
                organizationIdentifier = "organizationId",
                kdfIterations = 7,
                kdfMemory = 1,
                kdfParallelism = 2,
                kdfType = null,
                key = "encryptedUserKey",
                keys = RegisterRequestJson.Keys(
                    publicKey = "public",
                    encryptedPrivateKey = "private",
                ),
            ),
        )
        assertTrue(result.isSuccess)
    }

    @Test
    fun `setKeyConnectorKey with token and empty response is success`() = runTest {
        val response = MockResponse().setBody("")
        server.enqueue(response)
        val result = service.setKeyConnectorKey(
            accessToken = "token",
            body = KeyConnectorKeyRequestJson(
                organizationIdentifier = "organizationId",
                kdfIterations = 7,
                kdfMemory = 1,
                kdfParallelism = 2,
                kdfType = KdfTypeJson.ARGON2_ID,
                userKey = "encryptedUserKey",
                keys = KeyConnectorKeyRequestJson.Keys(
                    publicKey = "public",
                    encryptedPrivateKey = "private",
                ),
            ),
        )
        assertTrue(result.isSuccess)
    }

    @Test
    fun `getMasterKeyFromKeyConnector with token and empty response is success`() = runTest {
        val masterKey = "masterKey"
        val response = MockResponse().setBody("""{ "key": "$masterKey" }""")
        server.enqueue(response)
        val result = service.getMasterKeyFromKeyConnector(
            url = "$url/test",
            accessToken = "token",
        )
        assertEquals(
            KeyConnectorMasterKeyResponseJson(masterKey = masterKey).asSuccess(),
            result,
        )
    }

    @Test
    fun `storeMasterKeyToKeyConnector without token success should return Success`() = runTest {
        val response = MockResponse()
        server.enqueue(response)
        val result = service.storeMasterKeyToKeyConnector(
            url = "$url/test",
            masterKey = "masterKey",
        )
        assertEquals(Unit.asSuccess(), result)
    }

    @Test
    fun `storeMasterKeyToKeyConnector with token success should return Success`() = runTest {
        val response = MockResponse()
        server.enqueue(response)
        val result = service.storeMasterKeyToKeyConnector(
            url = "$url/test",
            masterKey = "masterKey",
            accessToken = "token",
        )
        assertEquals(Unit.asSuccess(), result)
    }
}
