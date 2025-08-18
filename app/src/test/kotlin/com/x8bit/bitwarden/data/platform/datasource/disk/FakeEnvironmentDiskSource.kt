package com.x8bit.bitwarden.data.platform.datasource.disk

import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onSubscription

class FakeEnvironmentDiskSource : EnvironmentDiskSource {
    private val storedEmailVerificationUrls = mutableMapOf<String, EnvironmentUrlDataJson?>()

    override var preAuthEnvironmentUrlData: EnvironmentUrlDataJson? = null
        set(value) {
            field = value
            mutablePreAuthEnvironmentUrlDataFlow.tryEmit(value)
        }

    override val preAuthEnvironmentUrlDataFlow: Flow<EnvironmentUrlDataJson?>
        get() = mutablePreAuthEnvironmentUrlDataFlow
            .onSubscription { emit(preAuthEnvironmentUrlData) }

    override fun getPreAuthEnvironmentUrlDataForEmail(
        userEmail: String,
    ): EnvironmentUrlDataJson? = storedEmailVerificationUrls[userEmail]

    override fun storePreAuthEnvironmentUrlDataForEmail(
        userEmail: String,
        urls: EnvironmentUrlDataJson,
    ) {
        storedEmailVerificationUrls[userEmail] = urls
    }

    private val mutablePreAuthEnvironmentUrlDataFlow =
        bufferedMutableSharedFlow<EnvironmentUrlDataJson?>(replay = 1)
}
