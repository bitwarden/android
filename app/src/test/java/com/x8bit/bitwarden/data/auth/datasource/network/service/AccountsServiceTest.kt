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
    fun `preLogin should call API`() = runTest {
        val response = MockResponse().setBody(PRE_LOGIN_RESPONSE_JSON)
        server.enqueue(response)
        assertEquals(Result.success(PRE_LOGIN_RESPONSE), service.preLogin(EMAIL))
    }

    companion object {
        private const val EMAIL = "email"
    }
}

private const val PRE_LOGIN_RESPONSE_JSON = """
{
  "kdf": 1,
  "kdfIterations": 1,
  "kdfMemory": 1,
  "kdfParallelism": 1
}        
"""

private val PRE_LOGIN_RESPONSE = PreLoginResponseJson(
    kdf = 1,
    kdfIterations = 1u,
    kdfMemory = 1,
    kdfParallelism = 1,
)
