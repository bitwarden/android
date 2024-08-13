package com.x8bit.bitwarden.data.platform.datasource.disk

import android.content.SharedPreferences
import com.x8bit.bitwarden.data.auth.datasource.disk.model.EnvironmentUrlDataJson
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.platform.util.decodeFromStringOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val PRE_AUTH_URLS_KEY = "preAuthEnvironmentUrls"
private const val EMAIL_VERIFICATION_URLS = "emailVerificationUrls"

/**
 * Primary implementation of [EnvironmentDiskSource].
 */
class EnvironmentDiskSourceImpl(
    sharedPreferences: SharedPreferences,
    private val json: Json,
) : BaseDiskSource(sharedPreferences = sharedPreferences),
    EnvironmentDiskSource {
    override var preAuthEnvironmentUrlData: EnvironmentUrlDataJson?
        get() = getString(key = PRE_AUTH_URLS_KEY)?.let { json.decodeFromStringOrNull(it) }
        set(value) {
            putString(
                key = PRE_AUTH_URLS_KEY,
                value = value?.let { json.encodeToString(it) },
            )
            mutableEnvironmentUrlDataFlow.tryEmit(value)
        }

    override val preAuthEnvironmentUrlDataFlow: Flow<EnvironmentUrlDataJson?>
        get() = mutableEnvironmentUrlDataFlow
            .onSubscription { emit(preAuthEnvironmentUrlData) }

    private val mutableEnvironmentUrlDataFlow =
        bufferedMutableSharedFlow<EnvironmentUrlDataJson?>(replay = 1)

    override fun getPreAuthEnvironmentUrlDataForEmail(
        userEmail: String,
    ): EnvironmentUrlDataJson? =
        getString(key = EMAIL_VERIFICATION_URLS.appendIdentifier(userEmail))
            ?.let {
                json.decodeFromStringOrNull(it)
            }

    override fun storePreAuthEnvironmentUrlDataForEmail(
        userEmail: String,
        urls: EnvironmentUrlDataJson,
    ) {
        putString(
            key = EMAIL_VERIFICATION_URLS.appendIdentifier(userEmail),
            value = json.encodeToString(urls),
        )
    }
}
