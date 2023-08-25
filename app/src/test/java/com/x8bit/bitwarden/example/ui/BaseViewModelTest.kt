package com.x8bit.bitwarden.example.ui

import com.x8bit.bitwarden.example.MainDispatcherExtension
import org.junit.jupiter.api.extension.RegisterExtension

abstract class BaseViewModelTest {
    @RegisterExtension
    protected open val mainDispatcherExtension = MainDispatcherExtension()
}
