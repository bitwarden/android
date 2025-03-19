package com.bitwarden.authenticator.data.platform.datasource.network.interceptor

import android.os.Build
import com.bitwarden.authenticator.BuildConfig
import com.bitwarden.authenticator.ui.platform.base.BaseRobolectricTest
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Test

class HeadersInterceptorTest : BaseRobolectricTest() {

    private val headersInterceptors = HeadersInterceptor()

    @Test
    fun `intercept should modify original request to include custom headers`() {
        // We reference the real BuildConfig here, since we don't want the test to break on every
        // version bump. We are also doing the same thing for Build when the SDK gets incremented.
        val versionName = BuildConfig.VERSION_NAME
        val buildType = BuildConfig.BUILD_TYPE
        val release = Build.VERSION.RELEASE
        val sdk = Build.VERSION.SDK_INT
        val originalRequest = Request.Builder().url("http://www.fake.com/").build()
        val chain = FakeInterceptorChain(originalRequest)

        val response = headersInterceptors.intercept(chain)

        val request = response.request
        @Suppress("MaxLineLength")
        assertEquals(
            "Bitwarden_Mobile/$versionName ($buildType) (Android $release; SDK $sdk; Model robolectric)",
            request.header("User-Agent"),
        )
        assertEquals("mobile", request.header("Bitwarden-Client-Name"))
        assertEquals(versionName, request.header("Bitwarden-Client-Version"))
        assertEquals("0", request.header("Device-Type"))
    }
}
