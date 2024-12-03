package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.autofill.accessibility.manager.FakeAccessibilityEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillEnabledManagerImpl
import com.x8bit.bitwarden.data.platform.datasource.disk.util.FakeSettingsDiskSource
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReviewPromptManagerTest {

    private val autofillEnabledManager: AutofillEnabledManager = AutofillEnabledManagerImpl()
    private val fakeAccessibilityEnabledManager = FakeAccessibilityEnabledManager()
    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val fakeSettingsDiskSource = FakeSettingsDiskSource()

    private val reviewPromptManager = ReviewPromptManagerImpl(
        autofillEnabledManager = autofillEnabledManager,
        accessibilityEnabledManager = fakeAccessibilityEnabledManager,
        authDiskSource = fakeAuthDiskSource,
        settingsDiskSource = fakeSettingsDiskSource,
    )

    @Test
    fun `incrementAddCipherAction increments stored value as expected`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        reviewPromptManager.registerAddCipherAction()
        assertEquals(
            1,
            fakeSettingsDiskSource.getAddCipherActionCount(),
        )
        reviewPromptManager.registerAddCipherAction()
        assertEquals(
            2,
            fakeSettingsDiskSource.getAddCipherActionCount(),
        )
        reviewPromptManager.registerAddCipherAction()
        assertEquals(
            3,
            fakeSettingsDiskSource.getAddCipherActionCount(),
        )
        reviewPromptManager.registerAddCipherAction()
        // Should not increment over 3.
        assertEquals(
            3,
            fakeSettingsDiskSource.getAddCipherActionCount(),
        )
    }

    @Test
    fun `incrementCopyGeneratedResultAction increments stored value as expected`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        reviewPromptManager.registerGeneratedResultAction()
        assertEquals(
            1,
            fakeSettingsDiskSource.getGeneratedResultActionCount(),
        )
        reviewPromptManager.registerGeneratedResultAction()
        assertEquals(
            2,
            fakeSettingsDiskSource.getGeneratedResultActionCount(),
        )
        reviewPromptManager.registerGeneratedResultAction()
        assertEquals(
            3,
            fakeSettingsDiskSource.getGeneratedResultActionCount(),
        )
        reviewPromptManager.registerGeneratedResultAction()
        assertEquals(
            3,
            fakeSettingsDiskSource.getGeneratedResultActionCount(),
        )
    }

    @Test
    fun `incrementCreateSendAction increments stored value as expected`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        reviewPromptManager.registerCreateSendAction()
        assertEquals(
            1,
            fakeSettingsDiskSource.getCreateSendActionCount(),
        )
        reviewPromptManager.registerCreateSendAction()
        assertEquals(
            2,
            fakeSettingsDiskSource.getCreateSendActionCount(),
        )
        reviewPromptManager.registerCreateSendAction()
        assertEquals(
            3,
            fakeSettingsDiskSource.getCreateSendActionCount(),
        )
        reviewPromptManager.registerCreateSendAction()
        assertEquals(
            3,
            fakeSettingsDiskSource.getCreateSendActionCount(),
        )
    }

    @Test
    fun `shouldPromptForAppReview should default to false if no active user`() {
        assertFalse(reviewPromptManager.shouldPromptForAppReview())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `shouldPromptForAppReview should return true if one auto fill service is enabled and one actions requirement is met`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        fakeAccessibilityEnabledManager.isAccessibilityEnabled = true
        autofillEnabledManager.isAutofillEnabled = false
        fakeSettingsDiskSource.storeGeneratedResultActionCount(count = 0)
        fakeSettingsDiskSource.storeCreateSendActionCount(count = 0)
        fakeSettingsDiskSource.storeAddCipherActionCount(count = 4)
        assertTrue(reviewPromptManager.shouldPromptForAppReview())
    }

    @Test
    fun `shouldPromptForAppReview should return false if no auto fill service is enabled`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        fakeAccessibilityEnabledManager.isAccessibilityEnabled = false
        autofillEnabledManager.isAutofillEnabled = false
        fakeSettingsDiskSource.storeGeneratedResultActionCount(count = 0)
        fakeSettingsDiskSource.storeCreateSendActionCount(count = 0)
        fakeSettingsDiskSource.storeAddCipherActionCount(count = 4)
        assertFalse(reviewPromptManager.shouldPromptForAppReview())
    }

    @Test
    fun `shouldPromptForAppReview should return false if no action count is met`() {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        fakeAccessibilityEnabledManager.isAccessibilityEnabled = true
        autofillEnabledManager.isAutofillEnabled = true
        fakeSettingsDiskSource.storeGeneratedResultActionCount(count = 1)
        fakeSettingsDiskSource.storeCreateSendActionCount(count = 0)
        fakeSettingsDiskSource.storeAddCipherActionCount(count = 2)
        assertFalse(reviewPromptManager.shouldPromptForAppReview())
    }
}

private const val USER_ID = "user_id"
private val MOCK_USER_STATE = mockk<UserStateJson>() {
    every { activeUserId } returns USER_ID
}
