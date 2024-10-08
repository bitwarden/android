package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Primary implementation of [SpecialCircumstanceManager].
 */
class SpecialCircumstanceManagerImpl(
    authRepository: AuthRepository,
    dispatcherManager: DispatcherManager,
) : SpecialCircumstanceManager {
    private val mutableSpecialCircumstanceFlow = MutableStateFlow<SpecialCircumstance?>(null)
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    init {
        authRepository
            .userStateFlow
            .filter {
                it?.activeAccount?.isLoggedIn == true
            }
            .onEach { _ ->
                if (specialCircumstance is SpecialCircumstance.RegistrationEvent) {
                    specialCircumstance = null
                }
            }
            .launchIn(unconfinedScope)
    }

    override var specialCircumstance: SpecialCircumstance?
        get() = mutableSpecialCircumstanceFlow.value
        set(value) {
            mutableSpecialCircumstanceFlow.value = value
        }

    override val specialCircumstanceStateFlow: StateFlow<SpecialCircumstance?>
        get() = mutableSpecialCircumstanceFlow
}
