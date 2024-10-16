package com.x8bit.bitwarden.data.platform.datasource.network.interceptor

import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import okhttp3.Request
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import javax.inject.Singleton

@Singleton
class AuthTokenInterceptorTest {
    private val authDiskSource = FakeAuthDiskSource()
    private val interceptor: AuthTokenInterceptor = AuthTokenInterceptor(
        authDiskSource = authDiskSource,
    )
    private val request: Request = Request
        .Builder()
        .url("http://localhost")
        .build()

    @Test
    fun `intercept should add the auth token when set`() {
        authDiskSource.userState = USER_STATE
        authDiskSource.storeAccountTokens(userId = USER_ID, ACCOUNT_TOKENS)
        val response = interceptor.intercept(
            chain = FakeInterceptorChain(request = request),
        )
        assertEquals(
            "Bearer $ACCESS_TOKEN",
            response.request.header("Authorization"),
        )
    }

    @Test
    fun `intercept should throw an exception when an auth token is missing`() {
        val throwable = assertThrows(IOException::class.java) {
            interceptor.intercept(
                chain = FakeInterceptorChain(request = request),
            )
        }
        assertEquals(
            "Auth token is missing!",
            throwable.cause?.message,
        )
    }
}

private const val USER_ID: String = "user_id"
private const val ACCESS_TOKEN: String = "access_token"
private val USER_STATE: UserStateJson = UserStateJson(
    activeUserId = USER_ID,
    accounts = mapOf(USER_ID to mockk()),
)
private val ACCOUNT_TOKENS: AccountTokensJson = AccountTokensJson(
    accessToken = ACCESS_TOKEN,
    refreshToken = null,
)
