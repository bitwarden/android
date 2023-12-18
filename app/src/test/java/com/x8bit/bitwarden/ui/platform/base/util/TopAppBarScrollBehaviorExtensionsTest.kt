package com.x8bit.bitwarden.ui.platform.base.util

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.graphics.Color
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

@OptIn(ExperimentalMaterial3Api::class)
class TopAppBarScrollBehaviorExtensionsTest {
    @Suppress("MaxLineLength")
    @Test
    fun `toScrolledContainerColor for pinned states should interpolate based on the overlappedFraction`() {
        val expandedColor = Color(
            red = 0f,
            green = 0f,
            blue = 0f,
            alpha = 0f,
        )
        val collapsedColor = Color(
            red = 1f,
            green = 1f,
            blue = 1f,
            alpha = 1f,
        )
        var overlappedFraction = 0f
        val topAppBarScrollBehavior = mockk<TopAppBarScrollBehavior> {
            every { isPinned } returns true
            every { state.overlappedFraction } answers { overlappedFraction }
        }

        overlappedFraction = 0f
        assertEquals(
            expandedColor,
            topAppBarScrollBehavior.toScrolledContainerColor(
                expandedColor = expandedColor,
                collapsedColor = collapsedColor,
            ),
        )

        overlappedFraction = 1f
        assertEquals(
            collapsedColor,
            topAppBarScrollBehavior.toScrolledContainerColor(
                expandedColor = expandedColor,
                collapsedColor = collapsedColor,
            ),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toScrolledContainerColor for pinned states should interpolate based on the collapsedFraction`() {
        val expandedColor = Color(
            red = 0f,
            green = 0f,
            blue = 0f,
            alpha = 0f,
        )
        val collapsedColor = Color(
            red = 1f,
            green = 1f,
            blue = 1f,
            alpha = 1f,
        )
        var collapsedFraction = 0f
        val topAppBarScrollBehavior = mockk<TopAppBarScrollBehavior> {
            every { isPinned } returns false
            every { state.collapsedFraction } answers { collapsedFraction }
        }

        collapsedFraction = 0f
        assertEquals(
            expandedColor,
            topAppBarScrollBehavior.toScrolledContainerColor(
                expandedColor = expandedColor,
                collapsedColor = collapsedColor,
            ),
        )

        collapsedFraction = 1f
        assertEquals(
            collapsedColor,
            topAppBarScrollBehavior.toScrolledContainerColor(
                expandedColor = expandedColor,
                collapsedColor = collapsedColor,
            ),
        )
    }
}
