package com.x8bit.bitwarden.e2e

import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import com.x8bit.bitwarden.e2e.config.E2eConfig
import com.x8bit.bitwarden.ui.platform.util.ScreenTags
import com.x8bit.bitwarden.e2e.seeder.LoginItem
import com.x8bit.bitwarden.e2e.seeder.SeededAccount
import com.x8bit.bitwarden.e2e.seeder.SeederClient
import com.x8bit.bitwarden.e2e.seeder.addLoginItem
import com.x8bit.bitwarden.e2e.seeder.createFreeUser
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

private const val HARNESS_PACKAGE = "com.bitwarden.testharness.dev"
private const val HARNESS_ACTIVITY = "$HARNESS_PACKAGE/com.bitwarden.testharness.MainActivity"
private const val PROVIDER_SERVICE = "com.x8bit.bitwarden.Autofill.CredentialProviderService"
private const val PASSKEY_USERNAME = "passkey-user"
private const val PASSKEY_RP_ID = "passkeys.example.com"
private const val PASSKEY_ORIGIN = "https://$PASSKEY_RP_ID"
private const val LOGIN_TIMEOUT_MS = 60_000L
private const val TIMEOUT_MS = 30_000L
private const val POLL_MS = 250L
private const val TAG = "CredentialManagerTest"

/**
 * QA-2937 spike: cross-app Credential Manager flows. The `:testharness` app asks for a password or
 * passkey, the system picker offers Bitwarden, and the harness shows what it received.
 *
 * Unlike [BaseE2eTest], this drives Bitwarden through UiAutomator too: a Compose test rule in
 * Bitwarden's process stops the app's other activities (here, the provider's MainActivity) from
 * drawing, so the credential flow never renders.
 */
@RunWith(AndroidJUnit4::class)
class CredentialManagerTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val appPackage = instrumentation.targetContext.packageName
    private val config = E2eConfig.fromInstrumentationArguments()
    private val seeder = SeederClient(config)

    @After
    fun deleteSeededData() {
        seeder.deleteSeededData()
    }

    @Test
    fun getPasswordFromHarness() {
        val account = seeder.createFreeUser()
        val item = LoginItem(
            name = "Harness-Login",
            username = "harness-user",
            password = "Harness-Password-1",
            uri = "androidapp://$HARNESS_PACKAGE",
        )
        seeder.addLoginItem(account, item)
        setBitwardenAsCredentialProvider()
        logIn(account)

        start(HARNESS_ACTIVITY, forceStop = true)
        waitFor(By.res("CredentialManagerCategory")).click()
        waitFor(By.res("GetPasswordRow")).click()
        waitFor(By.res("GetPaswordExecuteButton")).click()

        // The system picker (GMS on API 37) preselects the only entry; Continue confirms it.
        waitFor(By.text(item.username))
        waitFor(By.text("Continue")).click()

        val resultText = waitFor(By.textContains(item.password)).text
        check("Username: ${item.username}" in resultText) {
            "Harness result doesn't show the seeded username:\n$resultText"
        }
    }

    @Test
    fun createAndUsePasskeyFromHarness() {
        val account = seeder.createFreeUser()
        setBitwardenAsCredentialProvider()
        logIn(account)

        // Create: the harness sets an origin, so Bitwarden treats it as a privileged app and asks
        // to trust it; user verification is the master password (the emulator has no screen lock).
        start(HARNESS_ACTIVITY, forceStop = true)
        waitFor(By.res("CredentialManagerCategory")).click()
        waitFor(By.res("CreatePasskeyRow")).click()
        enterText(tag = "PasskeyUsernameField", value = PASSKEY_USERNAME)
        enterText(tag = "PasskeyRelyingPartyIdField", value = PASSKEY_RP_ID)
        enterText(tag = "PasskeyOriginField", value = PASSKEY_ORIGIN)
        waitFor(By.res("PasskeyExecuteButton")).click()

        waitFor(By.text(PASSKEY_USERNAME))
        waitFor(By.text("Continue")).click()
        waitFor(By.res("AcceptAlertButton").hasDescendant(By.desc("Trust"))).click()
        waitFor(By.desc("Save passkey as new login")).click()
        // A one-time "Bitwarden Autofill Service" tip can appear before the item editor.
        val saveOrTip = waitForAny(By.res("SaveButton"), By.res("AcceptAlertButton"))
        if (saveOrTip.resourceName == "AcceptAlertButton") saveOrTip.click()
        waitFor(By.res("SaveButton")).click()
        verifyMasterPassword(account)

        waitFor(By.textContains("SUCCESS: Passkey created successfully"))

        // Use: the same harness asks for an assertion for the passkey it just created.
        device.pressBack()
        waitFor(By.res("GetPasskeyRow")).click()
        enterText(tag = "PasskeyRelyingPartyIdField", value = PASSKEY_RP_ID)
        enterText(tag = "PasskeyOriginField", value = PASSKEY_ORIGIN)
        waitFor(By.res("GetPasskeyExecuteButton")).click()

        waitFor(By.text(PASSKEY_USERNAME))
        waitFor(By.text("Continue")).click()
        verifyMasterPassword(account)

        val resultText = waitFor(
            By.textContains("SUCCESS: Passkey authenticated successfully"),
        ).text
        check("\"authenticatorData\"" in resultText && "\"signature\"" in resultText) {
            "Harness result has no assertion:\n$resultText"
        }
    }

    private fun verifyMasterPassword(account: SeededAccount) {
        enterText(tag = "AlertInputField", value = account.password, isPassword = true)
        waitFor(By.res("AcceptAlertButton")).click()
    }

    private fun setBitwardenAsCredentialProvider() {
        val component = "$appPackage/$PROVIDER_SERVICE"
        shell("settings put secure credential_service $component")
        shell("settings put secure credential_service_primary $component")
    }

    private fun logIn(account: SeededAccount) {
        // Not force-stopped: Bitwarden's process is this test's process.
        start("$appPackage/com.x8bit.bitwarden.MainActivity", forceStop = false)
        waitFor(By.desc("I understand")).click()
        waitFor(By.res("ChooseLoginButton")).click()
        waitFor(By.res("RegionSelectorDropdown")).click()
        waitFor(By.text("Self-hosted")).click()
        enterText(tag = "ServerUrlEntry", value = config.serverUrl)
        waitFor(By.res("SaveButton")).click()
        enterText(tag = "EmailAddressEntry", value = account.email)
        waitFor(By.res("ContinueButton")).click()
        enterText(tag = "MasterPasswordEntry", value = account.password, isPassword = true)
        waitFor(By.res("LogInWithMasterPasswordButton")).click()

        // With Bitwarden as the autofill service, a browser-autofill prompt can follow login.
        val vaultOrPrompt = waitForAny(
            By.res(ScreenTags.VAULT),
            By.res("DismissAlertButton"),
            timeoutMs = LOGIN_TIMEOUT_MS,
        )
        if (vaultOrPrompt.resourceName == "DismissAlertButton") {
            vaultOrPrompt.click()
            waitFor(By.res(ScreenTags.VAULT))
        }
    }

    /**
     * Types [value] into the field tagged [tag]. Some tags sit on a wrapper around the EditText.
     * Password fields read back masked, so only other fields are checked.
     */
    private fun enterText(tag: String, value: String, isPassword: Boolean = false) {
        val node = waitFor(By.res(tag))
        val field = node.findObject(By.clazz("android.widget.EditText")) ?: node
        field.text = value
        if (!isPassword) {
            waitForAny(By.res(tag).text(value), By.res(tag).hasDescendant(By.text(value)))
        }
    }

    private fun start(component: String, forceStop: Boolean) {
        // The shell isn't subject to package visibility, so the test APK needs no <queries> entry.
        val stop = if (forceStop) "-S " else ""
        // Bitwarden's MainActivity enforces its intent filter: explicit starts need MAIN/LAUNCHER.
        val output = shell(
            "am start -W $stop-a android.intent.action.MAIN -c android.intent.category.LAUNCHER " +
                "-n $component",
        )
        check("Status: ok" in output) { "Couldn't start $component:\n$output" }
    }

    private fun waitFor(selector: BySelector): UiObject2 = waitForAny(selector)

    /**
     * Returns the first node matching any of [selectors], waiting up to [timeoutMs].
     */
    private fun waitForAny(vararg selectors: BySelector, timeoutMs: Long = TIMEOUT_MS): UiObject2 {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            selectors.firstNotNullOfOrNull { device.findObject(it) }?.let { return it }
            SystemClock.sleep(POLL_MS)
        }
        val dump = ByteArrayOutputStream().also { device.dumpWindowHierarchy(it) }
        Log.e(TAG, "Not found: ${selectors.toList()}\n$dump")
        error("Timed out waiting for ${selectors.toList()}")
    }

    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(command),
        ).use { it.readBytes().decodeToString() }
}
