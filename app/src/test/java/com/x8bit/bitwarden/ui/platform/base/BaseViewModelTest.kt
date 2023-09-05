package com.x8bit.bitwarden.ui.platform.base

import org.junit.jupiter.api.extension.RegisterExtension

abstract class BaseViewModelTest {
    @RegisterExtension
    protected open val mainDispatcherExtension = MainDispatcherExtension()
}
