package com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.util

import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.authenticator.data.authenticator.repository.model.AuthenticatorItem
import com.bitwarden.authenticator.data.authenticator.repository.model.SharedVerificationCodesState
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model.SharedCodesDisplayState
import com.bitwarden.authenticator.ui.authenticator.feature.itemlisting.model.VerificationCodeDisplayItem
import com.bitwarden.authenticator.ui.platform.base.util.asText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SharedVerificationCodesStateTest {

    @Test
    fun `toSharedCodesDisplayState on empty list should return empty list`() {
        val state = SharedVerificationCodesState.Success(emptyList())
        val expected = SharedCodesDisplayState.Codes(emptyList())
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
            sections = listOf(
                SharedCodesDisplayState.SharedCodesAccountSection(
                    label = R.string.shared_accounts_header.asText(
                        "John@test.com",
                        "bitwarden.com",
                    ),
                    codes = listOf(
                        VerificationCodeDisplayItem(
                            authCode = "123456",
                            periodSeconds = 30,
                            timeLeftSeconds = 10,
                            id = "123",
                            title = "--",
                            subtitle = null,
                            favorite = false,
                            allowLongPressActions = false,
                            alertThresholdSeconds = ALERT_THRESHOLD,
                            showMoveToBitwarden = false,
                        ),
                    ),
                ),
                SharedCodesDisplayState.SharedCodesAccountSection(
                    label = R.string.shared_accounts_header.asText(
                        "Jane@test.com",
                        "bitwarden.eu",
                    ),
                    codes = listOf(
                        VerificationCodeDisplayItem(
                            authCode = "987654",
                            periodSeconds = 30,
                            timeLeftSeconds = 10,
                            id = "987",
                            title = "issuer",
                            subtitle = "accountName",
                            favorite = false,
                            allowLongPressActions = false,
                            alertThresholdSeconds = ALERT_THRESHOLD,
                            showMoveToBitwarden = false,
                        ),
                    ),
                ),
            ),
        )
        assertEquals(
            expected,
            state.toSharedCodesDisplayState(ALERT_THRESHOLD),
        )
    }
}

private const val ALERT_THRESHOLD = 7
