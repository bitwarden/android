package com.bitwarden.network.interceptor

import androidx.annotation.WorkerThread
import com.bitwarden.network.exception.LocalNetworkAccessException
import com.bitwarden.network.provider.PermissionProvider
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Interceptor responsible for determining if the destination of the network request is on the
 * local network or not.
 */
internal class PermissionInterceptor(
    private val permissionProvider: PermissionProvider,
) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        if (permissionProvider.hasLocalNetworkAccessPermission || !chain.isLocalRequest()) {
            return chain.proceed(request = chain.request())
        }
        permissionProvider.acquireLocalNetworkAccessPermission()
        throw LocalNetworkAccessException(message = permissionProvider.errorMessageString)
    }
}

@WorkerThread
@Throws(IOException::class)
private fun Interceptor.Chain.isLocalRequest(): Boolean {
    val host = this.request().url.host
    val address = try {
        InetAddress.getByName(host)
    } catch (uhe: UnknownHostException) {
        // We just rethrow this exception, it was gonna happen anyway.
        throw uhe
    } catch (_: SecurityException) {
        // A security exception has occurred, lets be safe and assume it's a local request.
        return true
    }
    return address.isSiteLocalAddress
}
