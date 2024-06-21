package com.x8bit.bitwarden.data.util

import androidx.lifecycle.LifecycleOwner

/**
 * A fake implementation of [LifecycleOwner] for testing purposes.
 */
class FakeLifecycleOwner : LifecycleOwner {
    override val lifecycle: FakeLifecycle = FakeLifecycle(lifecycleOwner = this)
}
