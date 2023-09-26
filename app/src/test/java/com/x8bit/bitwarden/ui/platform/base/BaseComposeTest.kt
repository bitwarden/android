package com.x8bit.bitwarden.ui.platform.base

import androidx.compose.ui.test.junit4.createComposeRule
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * A base class that can be used for performing Compose-layer testing using Robolectric, Compose
 * Testing, and JUnit 4.
 */
@Config(
    application = HiltTestApplication::class,
    sdk = [Config.NEWEST_SDK],
)
@RunWith(RobolectricTestRunner::class)
abstract class BaseComposeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    init {
        ShadowLog.stream = System.out
    }
}
