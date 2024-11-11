package com.x8bit.bitwarden.ui.tools.feature.generator.handlers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorState.MainType.Username.UsernameType.ForwardedEmailAlias
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorViewModel
import com.x8bit.bitwarden.ui.tools.feature.generator.GeneratorAction.MainType.Username.UsernameType.ForwardedEmailAlias as ForwardedEmailAliasAction

/**
 * A class dedicated to handling user interactions related to forwarded email alias
 * configuration.
 * Each lambda corresponds to a specific user action, allowing for easy delegation of
 * logic when user input is detected.
 */
@Suppress("LongParameterList")
data class ForwardedEmailAliasHandlers(
    val onServiceChange: (ForwardedEmailAlias.ServiceTypeOption) -> Unit,
    val onAddyIoAccessTokenTextChange: (String) -> Unit,
    val onAddyIoDomainNameTextChange: (String) -> Unit,
    val onDuckDuckGoApiKeyTextChange: (String) -> Unit,
    val onFastMailApiKeyTextChange: (String) -> Unit,
    val onFirefoxRelayAccessTokenTextChange: (String) -> Unit,
    val onForwardEmailApiKeyTextChange: (String) -> Unit,
    val onForwardEmailDomainNameTextChange: (String) -> Unit,
    val onSimpleLoginApiKeyTextChange: (String) -> Unit,
) {
    @Suppress("UndocumentedPublicClass")
    companion object {
        /**
         * Creates an instance of [ForwardedEmailAliasHandlers] by binding actions to the provided
         * [GeneratorViewModel].
         */
        fun create(
            viewModel: GeneratorViewModel,
        ): ForwardedEmailAliasHandlers = ForwardedEmailAliasHandlers(
            onServiceChange = { newServiceTypeOption ->
                viewModel.trySendAction(
                    ForwardedEmailAliasAction.ServiceTypeOptionSelect(
                        serviceTypeOption = newServiceTypeOption,
                    ),
                )
            },
            onAddyIoAccessTokenTextChange = { newAccessToken ->
                viewModel.trySendAction(
                    ForwardedEmailAliasAction.AddyIo.AccessTokenTextChange(
                        accessToken = newAccessToken,
                    ),
                )
            },
            onAddyIoDomainNameTextChange = { newDomainName ->
                viewModel.trySendAction(
                    ForwardedEmailAliasAction.AddyIo.DomainTextChange(domain = newDomainName),
                )
            },
            onDuckDuckGoApiKeyTextChange = { newApiKey ->
                viewModel.trySendAction(
                    ForwardedEmailAliasAction.DuckDuckGo.ApiKeyTextChange(apiKey = newApiKey),
                )
            },
            onFastMailApiKeyTextChange = { newApiKey ->
                viewModel.trySendAction(
                    ForwardedEmailAliasAction.FastMail.ApiKeyTextChange(apiKey = newApiKey),
                )
            },
            onFirefoxRelayAccessTokenTextChange = { newAccessToken ->
                viewModel.trySendAction(
                    ForwardedEmailAliasAction.FirefoxRelay.AccessTokenTextChange(
                        accessToken = newAccessToken,
                    ),
                )
            },
            onForwardEmailApiKeyTextChange = { newApiKey ->
                viewModel.trySendAction(
                    ForwardedEmailAliasAction.ForwardEmail.ApiKeyTextChange(apiKey = newApiKey),
                )
            },
            onForwardEmailDomainNameTextChange = { newDomainName ->
                viewModel.trySendAction(
                    ForwardedEmailAliasAction.ForwardEmail.DomainNameTextChange(
                        domainName = newDomainName,
                    ),
                )
            },
            onSimpleLoginApiKeyTextChange = { newApiKey ->
                viewModel.trySendAction(
                    ForwardedEmailAliasAction.SimpleLogin.ApiKeyTextChange(apiKey = newApiKey),
                )
            },
        )
    }
}

/**
 * Helper function to remember a [ForwardedEmailAliasHandlers] instance in a [Composable] scope.
 */
@Composable
fun rememberForwardedEmailAliasHandlers(
    viewModel: GeneratorViewModel,
): ForwardedEmailAliasHandlers =
    remember(viewModel) {
        ForwardedEmailAliasHandlers.create(viewModel = viewModel)
    }
