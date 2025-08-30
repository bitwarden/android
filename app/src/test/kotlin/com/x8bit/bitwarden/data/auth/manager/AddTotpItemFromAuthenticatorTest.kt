package com.x8bit.bitwarden.data.auth.manager

import com.x8bit.bitwarden.ui.vault.model.TotpData
import io.mockk.mockk
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull

class AddTotpItemFromAuthenticatorTest {

    @Test
    fun `pendingAddTotpLoginItemData should start as null and keep value when set`() {
        val manager = AddTotpItemFromAuthenticatorManagerImpl()
        assertNull(manager.pendingAddTotpLoginItemData)

        val totpData: TotpData = mockk()
        manager.pendingAddTotpLoginItemData = totpData
        assertEquals(
            totpData,
            manager.pendingAddTotpLoginItemData,
        )

        manager.pendingAddTotpLoginItemData = null
        assertNull(manager.pendingAddTotpLoginItemData)
    }
}
