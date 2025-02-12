package com.bitwarden.authenticator.data.platform.datasource.disk

import android.content.SharedPreferences
import com.bitwarden.authenticator.data.platform.datasource.disk.BaseDiskSource.Companion.BASE_KEY
import com.bitwarden.authenticator.data.platform.datasource.disk.model.FeatureFlagsConfiguration
import com.bitwarden.authenticator.data.platform.repository.util.bufferedMutableSharedFlow
import com.bitwarden.authenticator.data.platform.util.decodeFromStringOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val KEY_FEATURE_FLAGS = "$BASE_KEY:featureFlags"

/**
 * Primary implementation of [FeatureFlagDiskSource].
 */
class FeatureFlagDiskSourceImpl(
    sharedPreferences: SharedPreferences,
    private val json: Json,
) : BaseDiskSource(sharedPreferences = sharedPreferences),
    FeatureFlagDiskSource {

    private val mutableFeatureFlagsConfigurationFlow =
        bufferedMutableSharedFlow<FeatureFlagsConfiguration?>(replay = 1)

    override val featureFlagsConfigurationFlow: Flow<FeatureFlagsConfiguration?>
        get() = mutableFeatureFlagsConfigurationFlow.onSubscription {
            emit(featureFlagsConfiguration)
        }

    override var featureFlagsConfiguration: FeatureFlagsConfiguration?
        get() = getString(key = KEY_FEATURE_FLAGS)
            ?.let { json.decodeFromStringOrNull(it) }
        set(value) {
            putString(
                key = KEY_FEATURE_FLAGS,
                value = value.let { json.encodeToString(it) },
            )
            mutableFeatureFlagsConfigurationFlow.tryEmit(value)
        }
}
