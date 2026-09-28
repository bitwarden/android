package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bitwarden.ui.platform.components.button.model.BitwardenButtonData
import com.bitwarden.ui.platform.components.card.BitwardenActionCard
import com.bitwarden.ui.platform.components.icon.BitwardenIcon
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.vault.feature.vault.handlers.VaultHandlers

/**
 * The action card for the vault screen.
 */
@Suppress("LongMethod")
@Composable
fun VaultActionCard(
    actionCardState: VaultState.ActionCardState,
    vaultHandlers: VaultHandlers,
    modifier: Modifier = Modifier,
) {
    when (actionCardState) {
        VaultState.ActionCardState.UpgradedToPremium -> {
            BitwardenActionCard(
                cardTitle = stringResource(id = BitwardenString.upgraded_to_premium),
                cardSubtitle = stringResource(
                    id = BitwardenString.you_now_have_access_to_all_advanced_security_features,
                ),
                actionButton = BitwardenButtonData(
                    label = BitwardenString.learn_more.asText(),
                    onClick = { vaultHandlers.actionCardClick(actionCardState) },
                    isExternalLink = true,
                ),
                leadingContent = {
                    Icon(
                        painter = rememberVectorPainter(id = BitwardenDrawable.ic_star),
                        contentDescription = null,
                        tint = BitwardenTheme.colorScheme.icon.secondary,
                    )
                },
                onDismissClick = { vaultHandlers.dismissActionCardClick(actionCardState) },
                modifier = modifier,
            )
        }

        VaultState.ActionCardState.UpgradePremium -> {
            BitwardenActionCard(
                cardTitle = stringResource(
                    id = BitwardenString.unlock_advanced_security_features,
                ),
                cardSubtitle = stringResource(
                    id = BitwardenString
                        .a_premium_plan_gives_you_more_tools_to_stay_secure_and_in_control,
                ),
                actionButton = BitwardenButtonData(
                    label = BitwardenString.learn_more.asText(),
                    onClick = { vaultHandlers.actionCardClick(actionCardState) },
                ),
                onDismissClick = { vaultHandlers.dismissActionCardClick(actionCardState) },
                modifier = modifier,
            )
        }

        VaultState.ActionCardState.PremiumNeedsAttention -> {
            BitwardenActionCard(
                cardTitle = stringResource(id = BitwardenString.your_subscription_needs_attention),
                cardSubtitle = stringResource(id = BitwardenString.check_your_plan_for_details),
                actionButton = BitwardenButtonData(
                    label = BitwardenString.view_plan.asText(),
                    onClick = { vaultHandlers.actionCardClick(actionCardState) },
                ),
                modifier = modifier,
            )
        }

        VaultState.ActionCardState.IntroducingArchive -> {
            BitwardenActionCard(
                cardTitle = stringResource(id = BitwardenString.introducing_archive),
                cardSubtitle = stringResource(
                    id = BitwardenString.keep_items_you_dont_need_right_now_safe_but_out_sight,
                ),
                actionButton = BitwardenButtonData(
                    label = BitwardenString.go_to_archive.asText(),
                    onClick = { vaultHandlers.actionCardClick(actionCardState) },
                ),
                leadingContent = {
                    Icon(
                        painter = rememberVectorPainter(id = BitwardenDrawable.ic_archive),
                        contentDescription = null,
                        tint = BitwardenTheme.colorScheme.icon.secondary,
                    )
                },
                onDismissClick = { vaultHandlers.dismissActionCardClick(actionCardState) },
                modifier = modifier,
            )
        }

        VaultState.ActionCardState.ImportItems -> {
            BitwardenActionCard(
                cardTitle = stringResource(id = BitwardenString.import_saved_logins),
                cardSubtitle = stringResource(id = BitwardenString.use_a_computer_to_import_logins),
                actionButton = BitwardenButtonData(
                    label = BitwardenString.get_started.asText(),
                    onClick = { vaultHandlers.actionCardClick(actionCardState) },
                ),
                onDismissClick = { vaultHandlers.dismissActionCardClick(actionCardState) },
                modifier = modifier,
            )
        }

        is VaultState.ActionCardState.VaultPolicyBanner -> {
            BitwardenActionCard(
                cardTitle = actionCardState.title?.invoke(),
                cardSubtitle = actionCardState.message(),
                actionButton = actionCardState.button?.let {
                    BitwardenButtonData(
                        label = it,
                        onClick = { vaultHandlers.actionCardClick(actionCardState) },
                    )
                },
                onDismissClick = { vaultHandlers.dismissActionCardClick(actionCardState) }
                    .takeIf { actionCardState.button == null },
                leadingContent = {
                    BitwardenIcon(
                        iconData = IconData.Local(iconRes = BitwardenDrawable.ic_info_circle),
                        tint = BitwardenTheme.colorScheme.icon.secondary,
                    )
                },
                modifier = modifier,
            )
        }
    }
}
