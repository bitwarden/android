package com.x8bit.bitwarden.example

import androidx.compose.material3.Button
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.x8bit.bitwarden.example.ui.BaseComposeTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Example showing that Compose tests using "junit" imports and Robolectric work.
 */
class ExampleComposeTest : BaseComposeTest() {
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
