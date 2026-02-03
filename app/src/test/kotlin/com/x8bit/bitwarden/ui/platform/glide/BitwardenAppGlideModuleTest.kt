package com.x8bit.bitwarden.ui.platform.glide

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Test class for [BitwardenAppGlideModule] to verify mTLS configuration is properly applied
 * to Glide without requiring a real mTLS server.
 *
 * These tests verify the module's structure and that it can be instantiated.
 * Full integration testing requires running the app and checking logcat for
 * "BitwardenGlide" logs when images are loaded.
 */
class BitwardenAppGlideModuleTest {

    @Test
    fun `BitwardenAppGlideModule should be instantiable`() {
        // Verify the module can be created
        val module = BitwardenAppGlideModule()

        assertNotNull(module)
    }

    @Test
    fun `BitwardenAppGlideModule should have EntryPoint interface for Hilt dependency injection`() {
        // Verify the Hilt EntryPoint interface exists for accessing CertificateManager
        val entryPointInterface = BitwardenAppGlideModule::class.java
            .declaredClasses
            .firstOrNull { it.simpleName == "BitwardenGlideEntryPoint" }

        assertNotNull(entryPointInterface)
    }

    @Test
    fun `BitwardenGlideEntryPoint should declare certificateManager method`() {
        // Verify the EntryPoint has the required method to access CertificateManager
        val entryPointInterface = BitwardenAppGlideModule::class.java
            .declaredClasses
            .firstOrNull { it.simpleName == "BitwardenGlideEntryPoint" }

        val methods = requireNotNull(entryPointInterface).declaredMethods
        val hasCertificateManagerMethod = methods.any { it.name == "certificateManager" }

        assertTrue(hasCertificateManagerMethod)
    }
}
