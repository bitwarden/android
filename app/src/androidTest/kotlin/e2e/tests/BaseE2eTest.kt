package e2e.tests

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.x8bit.bitwarden.MainActivity
import data.TestDataReader
import org.junit.Rule

open class BaseE2eTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    // Workaround to find Compose UI elements on Espresso tests
    @get:Rule
    val composeTestRule: ComposeTestRule = createEmptyComposeRule()

    val testData = TestDataReader.getTestData("TestData.json")
}
