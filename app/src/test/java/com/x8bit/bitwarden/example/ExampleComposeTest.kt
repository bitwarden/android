package com.x8bit.bitwarden.example

import androidx.compose.material3.Button
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Example showing that Compose tests using "junit" imports and Roboelectric work.
 */
@Config(
    application = HiltTestApplication::class,
    sdk = [Config.NEWEST_SDK],
)
@RunWith(RobolectricTestRunner::class)
class ExampleComposeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `the onClick callback should be correctly triggered when performing a click`() {
        var isClicked = false
        composeTestRule.setContent {
            Button(
                onClick = { isClicked = true },
            ) {
                // Empty
            }
        }

        assertFalse(isClicked)

        composeTestRule.onRoot().performClick()

        assertTrue(isClicked)
    }
}
