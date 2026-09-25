package com.x8bit.bitwarden.data.platform.manager.sdk.log

import android.util.Log
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.sdk.LogLevel
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import timber.log.Timber

class SdkLoggerFactoryTest {
    private val buildInfoManager: BuildInfoManager = mockk {
        every { isDevBuild } returns false
    }
    private val recordingTree = RecordingTree()

    private val sdkLoggerFactory: SdkLoggerFactory = SdkLoggerFactoryImpl(
        buildInfoManager = buildInfoManager,
    )

    @BeforeEach
    fun setup() {
        Timber.plant(recordingTree)
    }

    @AfterEach
    fun tearDown() {
        Timber.uproot(recordingTree)
    }

    @Test
    fun `logLevel should be DEBUG when the build is a dev build`() {
        every { buildInfoManager.isDevBuild } returns true

        assertEquals(LogLevel.DEBUG, sdkLoggerFactory.logLevel)
    }

    @Test
    fun `logLevel should be INFO when the build is not a dev build`() {
        every { buildInfoManager.isDevBuild } returns false

        assertEquals(LogLevel.INFO, sdkLoggerFactory.logLevel)
    }

    @Test
    fun `getLogCallback should return a callback that logs with the SDK tag`() {
        sdkLoggerFactory
            .getLogCallback()
            .onLog(level = "INFO", target = "bitwarden_core::target", message = "mockMessage")

        assertEquals(
            listOf(
                LogEntry(
                    priority = Log.INFO,
                    tag = "BitwardenSdkClient",
                    message = "bitwarden_core::target --mockMessage",
                ),
            ),
            recordingTree.logEntries,
        )
    }

    @Test
    fun `getLogCallback should return a callback that maps each SDK level to a log priority`() {
        val callback = sdkLoggerFactory.getLogCallback()

        listOf("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "UNKNOWN").forEach {
            callback.onLog(level = it, target = "mockTarget", message = "mockMessage")
        }

        assertEquals(
            listOf(Log.VERBOSE, Log.DEBUG, Log.INFO, Log.WARN, Log.ERROR, Log.ASSERT),
            recordingTree.logEntries.map { it.priority },
        )
    }
}

/**
 * A [Timber.Tree] that records each log for later verification.
 */
private class RecordingTree : Timber.Tree() {
    val logEntries: MutableList<LogEntry> = mutableListOf()

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        logEntries.add(LogEntry(priority = priority, tag = tag, message = message))
    }
}

/**
 * Models a single log received by the [RecordingTree].
 */
private data class LogEntry(
    val priority: Int,
    val tag: String?,
    val message: String,
)
