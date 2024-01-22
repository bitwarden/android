package com.x8bit.bitwarden.ui.tools.feature.send.addsend.util

import android.net.Uri
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.tools.feature.send.addsend.AddSendState
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SpecialCircumstanceExtensionsTest {

    @Test
    fun `toSendType with TextSend should return Text SendType with correct text`() {
        val text = "Share Text"
        val expected = AddSendState.ViewState.Content.SendType.Text(
            input = text,
            isHideByDefaultChecked = false,
        )
        val specialCircumstance = SpecialCircumstance.ShareNewSend(
            data = IntentManager.ShareData.TextSend(
                subject = "",
                text = text,
            ),
            shouldFinishWhenComplete = false,
        )

        val result = specialCircumstance.toSendType()

        assertEquals(expected, result)
    }

    @Test
    fun `toSendType with FileSend should return File SendType with correct data`() {
        val uri = mockk<Uri>()
        val fileName = "Share Name"
        val sizeBytes = 100L
        val expected = AddSendState.ViewState.Content.SendType.File(
            uri = uri,
            name = fileName,
            sizeBytes = sizeBytes,
            displaySize = null,
        )
        val specialCircumstance = SpecialCircumstance.ShareNewSend(
            data = IntentManager.ShareData.FileSend(
                fileData = IntentManager.FileData(
                    fileName = fileName,
                    uri = uri,
                    sizeBytes = sizeBytes,
                ),
            ),
            shouldFinishWhenComplete = false,
        )

        val result = specialCircumstance.toSendType()

        assertEquals(expected, result)
    }

    @Test
    fun `toSendType with null SpecialCircumstance should return null`() {
        val specialCircumstance: SpecialCircumstance? = null
        assertNull(specialCircumstance.toSendType())
    }

    @Test
    fun `toSendName with TextSend should return subject`() {
        val subject = "Subject"
        val specialCircumstance = SpecialCircumstance.ShareNewSend(
            data = IntentManager.ShareData.TextSend(
                subject = subject,
                text = "",
            ),
            shouldFinishWhenComplete = false,
        )

        val result = specialCircumstance.toSendName()

        assertEquals(subject, result)
    }

    @Test
    fun `toSendName with FileSend should return file name`() {
        val fileName = "File Name"
        val specialCircumstance = SpecialCircumstance.ShareNewSend(
            data = IntentManager.ShareData.FileSend(
                fileData = IntentManager.FileData(
                    fileName = fileName,
                    uri = mockk(),
                    sizeBytes = 0L,
                ),
            ),
            shouldFinishWhenComplete = false,
        )

        val result = specialCircumstance.toSendName()

        assertEquals(fileName, result)
    }

    @Test
    fun `toSendName with null SpecialCircumstance should return null`() {
        val specialCircumstance: SpecialCircumstance? = null
        assertNull(specialCircumstance.toSendName())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `shouldFinishOnComplete with ShareNewSend shouldFinishWhenComplete true should return true`() {
        val specialCircumstance = SpecialCircumstance.ShareNewSend(
            data = mockk(),
            shouldFinishWhenComplete = true,
        )
        assertTrue(specialCircumstance.shouldFinishOnComplete())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `shouldFinishOnComplete with ShareNewSend shouldFinishWhenComplete false should return false`() {
        val specialCircumstance = SpecialCircumstance.ShareNewSend(
            data = mockk(),
            shouldFinishWhenComplete = false,
        )
        assertFalse(specialCircumstance.shouldFinishOnComplete())
    }

    @Test
    fun `shouldFinishOnComplete with null SpecialCircumstance should return false`() {
        val specialCircumstance: SpecialCircumstance? = null
        assertFalse(specialCircumstance.shouldFinishOnComplete())
    }
}
