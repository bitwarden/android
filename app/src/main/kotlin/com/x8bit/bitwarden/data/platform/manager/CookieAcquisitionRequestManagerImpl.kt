package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.manager.model.CookieAcquisitionRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Singleton

/**
 * Implementation of [CookieAcquisitionRequestManager].
 */
@Singleton
class CookieAcquisitionRequestManagerImpl : CookieAcquisitionRequestManager {

    private val mutableCookieAcquisitionRequestFlow =
        MutableStateFlow<CookieAcquisitionRequest?>(null)

    override val cookieAcquisitionRequestFlow: StateFlow<CookieAcquisitionRequest?> =
        mutableCookieAcquisitionRequestFlow.asStateFlow()

    override fun setPendingCookieAcquisition(data: CookieAcquisitionRequest?) {
        mutableCookieAcquisitionRequestFlow.value = data
    }
}
