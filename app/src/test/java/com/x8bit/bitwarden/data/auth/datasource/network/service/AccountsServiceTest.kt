package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.AccountsApi
import com.x8bit.bitwarden.data.auth.datasource.network.api.AuthenticatedAccountsApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.PasswordHintResponseJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.ResendEmailRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.ResetPasswordRequestJson
import com.x8bit.bitwarden.data.auth.datasource.network.model.SetPasswordRequestJson
import com.x8bit.bitwarden.data.platform.base.BaseServiceTest
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.create

class AccountsServiceTest : BaseServiceTest() {

    private val accountsApi: AccountsApi = retrofit.create()
    private val authenticatedAccountsApi: AuthenticatedAccountsApi = retrofit.create()
    private val service = AccountsServiceImpl(
        accountsApi = accountsApi,
        authenticatedAccountsApi = authenticatedAccountsApi,
        json = Json {
            ignoreUnknownKeys = true
        },
    )

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
        val json = ""
        val response = MockResponse().setBody(json)
        server.enqueue(response)
        assertTrue(service.deleteAccount(masterPasswordHash).isSuccess)
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
}
