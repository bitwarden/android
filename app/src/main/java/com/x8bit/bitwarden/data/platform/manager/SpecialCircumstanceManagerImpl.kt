package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Primary implementation of [SpecialCircumstanceManager].
 */
class SpecialCircumstanceManagerImpl : SpecialCircumstanceManager {
    private val mutableSpecialCircumstanceFlow = MutableStateFlow<SpecialCircumstance?>(null)

    override var specialCircumstance: SpecialCircumstance?
        get() = mutableSpecialCircumstanceFlow.value
        set(value) {
            mutableSpecialCircumstanceFlow.value = value
        }

    override val specialCircumstanceStateFlow: StateFlow<SpecialCircumstance?>
        get() = mutableSpecialCircumstanceFlow
}
