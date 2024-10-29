package com.x8bit.bitwarden.ui.platform.components.bottomsheet

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.union
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.x8bit.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A reusable modal bottom sheet that applies provides a bottom sheet layout with the
 * standard [BitwardenScaffold] and [BitwardenTopAppBar] and expected scrolling behavior with
 * passed in [sheetContent]
 *
 * @param sheetTitle The title to display in the [BitwardenTopAppBar]
 * @param onDismiss The action to perform when the bottom sheet is dismissed will also be performed
 * when the "close" icon is clicked, caller must handle any desired animation or hiding of the
 * bottom sheet.
 * @param showBottomSheet Whether or not to show the bottom sheet, by default this is true assuming
 * the showing/hiding will be handled by the caller.
 * @param sheetContent Content to display in the bottom sheet. The content is passed the padding
 * from the containing [BitwardenScaffold].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitwardenModalBottomSheet(
    sheetTitle: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showBottomSheet: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(),
    sheetContent: @Composable (PaddingValues) -> Unit,
) {
    if (!showBottomSheet) return
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        dragHandle = null,
        sheetState = sheetState,
        contentWindowInsets = {
            WindowInsets(left = 0, top = 0, right = 0, bottom = 0)
                .union(WindowInsets.displayCutout)
        },
        containerColor = Color.Transparent,
    ) {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        BitwardenScaffold(
            topBar = {
                BitwardenTopAppBar(
                    title = sheetTitle,
                    navigationIcon = NavigationIcon(
                        navigationIcon = rememberVectorPainter(R.drawable.ic_close),
                        onNavigationIconClick = onDismiss,
                        navigationIconContentDescription = stringResource(R.string.close),
                    ),
                    scrollBehavior = scrollBehavior,
                    minimunHeight = 64.dp,
                )
            },
            modifier = Modifier
                // We apply the shape here due to implementation of the ModalBottomSheet applying
                // the insets to the content but the shape to an outer container.
                .clip(shape = BitwardenTheme.shapes.bottomSheet)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .fillMaxSize(),
        ) { paddingValues ->
            sheetContent(paddingValues)
        }
    }
}
