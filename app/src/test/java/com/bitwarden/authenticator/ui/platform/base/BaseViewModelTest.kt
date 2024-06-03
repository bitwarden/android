package com.bitwarden.authenticator.ui.platform.base

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.TurbineContext
import app.cash.turbine.turbineScope
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.extension.RegisterExtension

abstract class BaseViewModelTest {
    @Suppress("unused", "JUnitMalformedDeclaration")
    @RegisterExtension
    protected open val mainDispatcherExtension = MainDispatcherExtension()

    protected suspend fun <S : Any, E : Any, T : BaseViewModel<S, E, *>> T.stateEventFlow(
        backgroundScope: CoroutineScope,
        validate: suspend TurbineContext.(
            stateFlow: ReceiveTurbine<S>,
            eventFlow: ReceiveTurbine<E>,
        ) -> Unit,
    ) {
        turbineScope {
            this.validate(
                this@stateEventFlow.stateFlow.testIn(backgroundScope),
                this@stateEventFlow.eventFlow.testIn(backgroundScope),
            )
        }
    }
}

