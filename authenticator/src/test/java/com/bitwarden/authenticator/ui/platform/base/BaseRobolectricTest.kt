package com.bitwarden.authenticator.ui.platform.base

import dagger.hilt.android.testing.HiltTestApplication
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * A base class that can be used for performing tests that use Robolectric and JUnit 4.
 */
@Config(
    application = HiltTestApplication::class,
    sdk = [Config.NEWEST_SDK],
)
@RunWith(RobolectricTestRunner::class)
abstract class BaseRobolectricTest {
    init {
        ShadowLog.stream = System.out
    }
}
