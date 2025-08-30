package com.x8bit.bitwarden.data.platform.manager

import android.content.Context
import com.x8bit.bitwarden.R

/**
 * Primary implementation of [ResourceCacheManager].
 */
class ResourceCacheManagerImpl(
    private val context: Context,
) : ResourceCacheManager {
    override val domainExceptionSuffixes: List<String> by lazy {
        context
            .resources
            .getStringArray(R.array.exception_suffixes)
            .toList()
    }

    override val domainNormalSuffixes: List<String> by lazy {
        context
            .resources
            .getStringArray(R.array.normal_suffixes)
            .toList()
    }

    override val domainWildCardSuffixes: List<String> by lazy {
        context
            .resources
            .getStringArray(R.array.wild_card_suffixes)
            .toList()
    }
}
