package com.bitwarden.authenticator.ui.authenticator.feature.util

import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.repository.model.AuthenticatorItem
import com.bitwarden.authenticator.data.authenticator.repository.model.SharedVerificationCodesState
import com.bitwarden.authenticator.ui.platform.components.listitem.model.SharedCodesDisplayState
import com.bitwarden.authenticator.ui.platform.components.listitem.model.VerificationCodeDisplayItem
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SharedVerificationCodesStateTest {

    @Test
    fun `toSharedCodesDisplayState on empty list should return empty list`() {
        val state = SharedVerificationCodesState.Success(emptyList())
        val expected = SharedCodesDisplayState.Codes(persistentListOf())
        assertEquals(
            expected,
            state.toSharedCodesDisplayState(ALERT_THRESHOLD),
        )
    }

    @Test
    fun `toSharedCodesDisplayState should return list of sections grouped by account`() {
        val state = SharedVerificationCodesState.Success(
            items = listOf(
                VerificationCodeItem(
                    code = "123456",
                    periodSeconds = 30,
                    timeLeftSeconds = 10,
                    issueTime = 100L,
                    id = "123",
                    issuer = null,
                    label = null,
                    source = AuthenticatorItem.Source.Shared(
                        userId = "user1",
                        nameOfUser = "John Appleseed",
                        email = "John@test.com",
                        environmentLabel = "bitwarden.com",
                    ),
                ),
                VerificationCodeItem(
                    code = "987654",
                    periodSeconds = 30,
                    timeLeftSeconds = 10,
                    issueTime = 100L,
                    id = "987",
                    issuer = "issuer",
                    label = "accountName",
                    source = AuthenticatorItem.Source.Shared(
                        userId = "user1",
                        nameOfUser = "Jane Doe",
                        email = "Jane@test.com",
                        environmentLabel = "bitwarden.eu",
                    ),
                ),
            ),
        )
        val expected = SharedCodesDisplayState.Codes(
            sections = persistentListOf(
                SharedCodesDisplayState.SharedCodesAccountSection(
                    id = "user1",
                    label = BitwardenString.shared_accounts_header.asText(
                        "John@test.com",
                        "bitwarden.com",
                        1,
                    ),
                    codes = persistentListOf(
                        VerificationCodeDisplayItem(
                            authCode = "123456",
                            periodSeconds = 30,
                            timeLeftSeconds = 10,
                            id = "123",
                            title = "--",
                            subtitle = null,
                            favorite = false,
                            showOverflow = false,
                            alertThresholdSeconds = ALERT_THRESHOLD,
                            showMoveToBitwarden = false,
                        ),
                    ),
                    isExpanded = true,
                ),
                SharedCodesDisplayState.SharedCodesAccountSection(
                    id = "user1",
                    label = BitwardenString.shared_accounts_header.asText(
                        "Jane@test.com",
                        "bitwarden.eu",
                        1,
                    ),
                    codes = persistentListOf(
                        VerificationCodeDisplayItem(
                            authCode = "987654",
                            periodSeconds = 30,
                            timeLeftSeconds = 10,
                            id = "987",
                            title = "issuer",
                            subtitle = "accountName",
                            favorite = false,
                            showOverflow = false,
                            alertThresholdSeconds = ALERT_THRESHOLD,
                            showMoveToBitwarden = false,
                        ),
                    ),
                    isExpanded = true,
                ),
            ),
        )
        assertEquals(
            expected,
            state.toSharedCodesDisplayState(ALERT_THRESHOLD),
        )
    }

    @Test
    fun `toSharedCodesDisplayState should return list of sections maintaining expanded state`() {
        val state = SharedVerificationCodesState.Success(
            items = listOf(
                VerificationCodeItem(
                    code = "123456",
                    periodSeconds = 30,
                    timeLeftSeconds = 10,
                    issueTime = 100L,
                    id = "123",
                    issuer = null,
                    label = null,
                    source = AuthenticatorItem.Source.Shared(
                        userId = "user1",
                        nameOfUser = "John Appleseed",
                        email = "John@test.com",
                        environmentLabel = "bitwarden.com",
                    ),
                ),
                VerificationCodeItem(
                    code = "987654",
                    periodSeconds = 30,
                    timeLeftSeconds = 10,
                    issueTime = 100L,
                    id = "987",
                    issuer = "issuer",
                    label = "accountName",
                    source = AuthenticatorItem.Source.Shared(
                        userId = "user1",
                        nameOfUser = "Jane Doe",
                        email = "Jane@test.com",
                        environmentLabel = "bitwarden.eu",
                    ),
                ),
            ),
        )
        val expected = SharedCodesDisplayState.Codes(
            sections = persistentListOf(
                SharedCodesDisplayState.SharedCodesAccountSection(
                    id = "user1",
                    label = BitwardenString.shared_accounts_header.asText(
                        "John@test.com",
                        "bitwarden.com",
                        1,
                    ),
                    codes = persistentListOf(
                        VerificationCodeDisplayItem(
                            authCode = "123456",
                            periodSeconds = 30,
                            timeLeftSeconds = 10,
                            id = "123",
                            title = "--",
                            subtitle = null,
                            favorite = false,
                            showOverflow = false,
                            alertThresholdSeconds = ALERT_THRESHOLD,
                            showMoveToBitwarden = false,
                        ),
                    ),
                    isExpanded = false,
                ),
                SharedCodesDisplayState.SharedCodesAccountSection(
                    id = "user1",
                    label = BitwardenString.shared_accounts_header.asText(
                        "Jane@test.com",
                        "bitwarden.eu",
                        1,
                    ),
                    codes = persistentListOf(
                        VerificationCodeDisplayItem(
                            authCode = "987654",
                            periodSeconds = 30,
                            timeLeftSeconds = 10,
                            id = "987",
                            title = "issuer",
                            subtitle = "accountName",
                            favorite = false,
                            showOverflow = false,
                            alertThresholdSeconds = ALERT_THRESHOLD,
                            showMoveToBitwarden = false,
                        ),
                    ),
                    isExpanded = false,
                ),
            ),
        )
        assertEquals(
            expected,
            state.toSharedCodesDisplayState(
                alertThresholdSeconds = ALERT_THRESHOLD,
                currentSections = expected.sections.map {
                    it.copy(label = "junk to show that it does update the other values".asText())
                },
            ),
        )
    }
}

private const val ALERT_THRESHOLD = 7
