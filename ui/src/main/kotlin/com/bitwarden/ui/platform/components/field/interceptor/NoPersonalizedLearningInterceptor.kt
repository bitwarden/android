package com.bitwarden.ui.platform.components.field.interceptor

import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputSession

/**
 * Interceptor that disables the [EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING] flag on text inputs.
 */
@OptIn(ExperimentalComposeUiApi::class)
object NoPersonalizedLearningInterceptor : PlatformTextInputInterceptor {
    override suspend fun interceptStartInputMethod(
        request: PlatformTextInputMethodRequest,
        nextHandler: PlatformTextInputSession,
    ): Nothing {
        val modifiedRequest = PlatformTextInputMethodRequest { outAttrs ->
            request.createInputConnection(outAttrs)
                .also {
                    outAttrs.imeOptions =
                        outAttrs.imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
                }
        }
        nextHandler.startInputMethod(modifiedRequest)
    }
}

/**
 * A [InterceptPlatformTextInput] that disables the [EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING]
 * flag on text inputs to force the keyboard into "Incognito Mode".
 *
 * This is a modified version of the workaround provided by Google here:
 * https://issuetracker.google.com/issues/359257538#comment2
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun IncognitoInput(content: @Composable () -> Unit) {
    InterceptPlatformTextInput(
        interceptor = NoPersonalizedLearningInterceptor,
        content = content,
    )
}
