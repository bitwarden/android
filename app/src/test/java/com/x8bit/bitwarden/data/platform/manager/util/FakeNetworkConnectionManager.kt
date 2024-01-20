package com.x8bit.bitwarden.data.platform.manager.util

import com.x8bit.bitwarden.data.platform.manager.NetworkConnectionManager

class FakeNetworkConnectionManager(
    override val isNetworkConnected: Boolean,
) : NetworkConnectionManager
