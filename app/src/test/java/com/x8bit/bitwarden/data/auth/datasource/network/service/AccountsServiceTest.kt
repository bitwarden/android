package com.x8bit.bitwarden.data.auth.datasource.network.service

import com.x8bit.bitwarden.data.auth.datasource.network.api.AccountsApi
import com.x8bit.bitwarden.data.auth.datasource.network.model.PreLoginResponseJson
import com.x8bit.bitwarden.data.platform.base.BaseServiceTest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import retrofit2.create

class AccountsServiceTest : BaseServiceTest() {

    private val accountsApi: AccountsApi = retrofit.create()
    private val service = AccountsServiceImpl(accountsApi)

    @Test
    fun `preLogin with unknown kdf type be failure`() = runTest {
        val json = """
            {
              "kdf": 2,
              "kdfIterations": 1,
            }
            """
        val response = MockResponse().setBody(json)
        server.enqueue(response)
        assert(service.preLogin(EMAIL).isFailure)
    }

    @Test
    fun `preLogin Argon2 without memory property should be failure`() = runTest {
        val json = """
            {
              "kdf": 1,
              "kdfIterations": 1,
              "kdfParallelism": 1
            }
            """
        val response = MockResponse().setBody(json)
        server.enqueue(response)
        assert(service.preLogin(EMAIL).isFailure)
    }

    @Test
    fun `preLogin Argon2 without parallelism property should be failure`() = runTest {
        val json = """
            {
              "kdf": 1,
              "kdfIterations": 1,
              "kdfMemory": 1
            }
            """
        val response = MockResponse().setBody(json)
        server.enqueue(response)
        assert(service.preLogin(EMAIL).isFailure)
    }

    @Test
    fun `preLogin Argon2 should be success`() = runTest {
        val json = """
            {
              "kdf": 1,
              "kdfIterations": 1,
              "kdfMemory": 1,
              "kdfParallelism": 1
            }
            """
        val expectedResponse = PreLoginResponseJson(
            kdfParams = PreLoginResponseJson.KdfParams.Argon2ID(
                iterations = 1u,
                memory = 1u,
                parallelism = 1u,
            ),
        )
        val response = MockResponse().setBody(json)
        server.enqueue(response)
        assertEquals(Result.success(expectedResponse), service.preLogin(EMAIL))
    }

    @Test
    fun `preLogin Pbkdf2 should be success`() = runTest {
        val json = """
            {
              "kdf": 0,
              "kdfIterations": 1
            }
            """
        val expectedResponse = PreLoginResponseJson(
            kdfParams = PreLoginResponseJson.KdfParams.Pbkdf2(1u),
        )
        val response = MockResponse().setBody(json)
        server.enqueue(response)
        assertEquals(Result.success(expectedResponse), service.preLogin(EMAIL))
    }

    companion object {
        private const val EMAIL = "email"
    }
}
