package com.x8bit.bitwarden.data.autofill

import android.os.CancellationSignal
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BitwardenAutofillServiceTests {
    private lateinit var bitwardenAutofillService: BitwardenAutofillService

    @BeforeEach
    fun setup() {
        bitwardenAutofillService = BitwardenAutofillService()
    }

    @Nested
    inner class OnFillRequest {
        @Test
        fun `nothing happens`() {
            // Setup
            val cancellationSignal: CancellationSignal = mockk()
            val fillCallback: FillCallback = mockk()
            val fillRequest: FillRequest = mockk()

            // Test
            bitwardenAutofillService.onFillRequest(
                cancellationSignal = cancellationSignal,
                fillCallback = fillCallback,
                request = fillRequest,
            )
        }
    }

    @Nested
    inner class OnSaveRequest {
        @Test
        fun `nothing happens`() {
            // Setup
            val saverRequest: SaveRequest = mockk()
            val saveCallback: SaveCallback = mockk()

            // Test
            bitwardenAutofillService.onSaveRequest(
                saveCallback = saveCallback,
                saverRequest = saverRequest,
            )
        }
    }
}
