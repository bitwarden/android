package com.bitwarden.network.interceptor

import androidx.annotation.WorkerThread
import com.bitwarden.network.exception.LocalNetworkAccessException
import com.bitwarden.network.provider.PermissionProvider
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Mask and prefix for the first byte of IPv6 unique local addresses (fc00::/7).
 */
private const val IPV6_UNIQUE_LOCAL_MASK: Int = 0xFE
private const val IPV6_UNIQUE_LOCAL_PREFIX: Int = 0xFC

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
    return address.isSiteLocalAddress || address.isLocalIpv6Address()
}

/**
 * Returns true if this is an IPv6 link-local (fe80::/10) or unique local (fc00::/7) address.
 */
private fun InetAddress.isLocalIpv6Address(): Boolean {
    if (this !is Inet6Address) return false
    val isUniqueLocal =
        (address.first().toInt() and IPV6_UNIQUE_LOCAL_MASK) == IPV6_UNIQUE_LOCAL_PREFIX
    return isLinkLocalAddress || isUniqueLocal
}
