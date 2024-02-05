package com.x8bit.bitwarden.ui.platform.base

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * A base class that can be used for performing tests that use Robolectric and JUnit 4.
 */
@Config(
    application = HiltTestApplication::class,
    sdk = [Config.NEWEST_SDK],
)
@RunWith(AndroidJUnit4::class)
abstract class BaseRobolectricTest {
    init {
        ShadowLog.stream = System.out
    }
}
