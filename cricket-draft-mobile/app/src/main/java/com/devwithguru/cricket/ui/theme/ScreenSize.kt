package com.devwithguru.cricket.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

// ─── Screen Breakpoints ─────────────────────────────────────
enum class ScreenSize {
    Small,   // < 360dp width (small phones like Galaxy A03)
    Medium,  // 360-399dp (standard phones like Pixel 5)
    Large,   // 400-499dp (large phones like Galaxy S24)
    XLarge   // 500dp+ (tablets, foldables)
}

// ─── Screen Config ──────────────────────────────────────────
@Immutable
data class ScreenConfig(
    val width: Dp,
    val height: Dp,
    val density: Float,
    val screenSize: ScreenSize,
    val isCompact: Boolean,   // Small screens
    val isMedium: Boolean,    // Standard phones
    val isLarge: Boolean,     // Large phones
    val isTablet: Boolean     // Tablets/foldables
) {
    // ─── Spacing ──────────────────────────────────────────
    val horizontalPadding: Dp
        get() = when (screenSize) {
            ScreenSize.Small -> 12.dp
            ScreenSize.Medium -> 16.dp
            ScreenSize.Large -> 20.dp
            ScreenSize.XLarge -> 24.dp
        }

    val verticalPadding: Dp
        get() = when (screenSize) {
            ScreenSize.Small -> 8.dp
            ScreenSize.Medium -> 12.dp
            ScreenSize.Large -> 16.dp
            ScreenSize.XLarge -> 20.dp
        }

    val cardPadding: Dp
        get() = when (screenSize) {
            ScreenSize.Small -> 10.dp
            ScreenSize.Medium -> 14.dp
            ScreenSize.Large -> 16.dp
            ScreenSize.XLarge -> 20.dp
        }

    val sectionSpacing: Dp
        get() = when (screenSize) {
            ScreenSize.Small -> 12.dp
            ScreenSize.Medium -> 16.dp
            ScreenSize.Large -> 20.dp
            ScreenSize.XLarge -> 24.dp
        }

    val avatarSize: Dp
        get() = when (screenSize) {
            ScreenSize.Small -> 36.dp
            ScreenSize.Medium -> 44.dp
            ScreenSize.Large -> 48.dp
            ScreenSize.XLarge -> 52.dp
        }

    val iconSize: Dp
        get() = when (screenSize) {
            ScreenSize.Small -> 14.dp
            ScreenSize.Medium -> 16.dp
            ScreenSize.Large -> 18.dp
            ScreenSize.XLarge -> 20.dp
        }

    // ─── Responsive Text Sizes ────────────────────────────
    // Display: hero numbers, scores, big counters
    val displayTextSize: TextUnit
        get() = when (screenSize) {
            ScreenSize.Small -> 24.sp
            ScreenSize.Medium -> 28.sp
            ScreenSize.Large -> 32.sp
            ScreenSize.XLarge -> 36.sp
        }

    // Heading: screen titles, section headers
    val headingTextSize: TextUnit
        get() = when (screenSize) {
            ScreenSize.Small -> 15.sp
            ScreenSize.Medium -> 17.sp
            ScreenSize.Large -> 19.sp
            ScreenSize.XLarge -> 21.sp
        }

    // Subheading: card titles, list item titles
    val subheadingTextSize: TextUnit
        get() = when (screenSize) {
            ScreenSize.Small -> 13.sp
            ScreenSize.Medium -> 14.sp
            ScreenSize.Large -> 15.sp
            ScreenSize.XLarge -> 16.sp
        }

    // Title: tab titles, toolbar titles (was titleTextSize)
    val titleTextSize: TextUnit
        get() = when (screenSize) {
            ScreenSize.Small -> 14.sp
            ScreenSize.Medium -> 16.sp
            ScreenSize.Large -> 18.sp
            ScreenSize.XLarge -> 20.sp
        }

    // Body: main readable text, descriptions
    val bodyTextSize: TextUnit
        get() = when (screenSize) {
            ScreenSize.Small -> 12.sp
            ScreenSize.Medium -> 13.sp
            ScreenSize.Large -> 14.sp
            ScreenSize.XLarge -> 15.sp
        }

    // Button: action buttons, chips
    val buttonTextSize: TextUnit
        get() = when (screenSize) {
            ScreenSize.Small -> 11.sp
            ScreenSize.Medium -> 12.sp
            ScreenSize.Large -> 13.sp
            ScreenSize.XLarge -> 14.sp
        }

    // Caption: timestamps, helper text, fine print
    val captionTextSize: TextUnit
        get() = when (screenSize) {
            ScreenSize.Small -> 10.sp
            ScreenSize.Medium -> 11.sp
            ScreenSize.Large -> 12.sp
            ScreenSize.XLarge -> 12.sp
        }

    // Score: live match score numbers (larger for visibility)
    val scoreTextSize: TextUnit
        get() = when (screenSize) {
            ScreenSize.Small -> 20.sp
            ScreenSize.Medium -> 22.sp
            ScreenSize.Large -> 26.sp
            ScreenSize.XLarge -> 30.sp
        }
}

// ─── Local Composition ──────────────────────────────────────
val LocalScreenConfig = androidx.compose.runtime.staticCompositionLocalOf<ScreenConfig> {
    error("No ScreenConfig provided")
}

// ─── Composable Extension ───────────────────────────────────
val screenConfig: ScreenConfig
    @Composable
    @ReadOnlyComposable
    get() = LocalScreenConfig.current

// ─── Setup Function ─────────────────────────────────────────
@Composable
fun provideScreenConfig(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current.density
    val widthDp = configuration.screenWidthDp.dp
    val heightDp = configuration.screenHeightDp.dp
    val widthPx = configuration.screenWidthDp

    val screenSize = when {
        widthPx < 360 -> ScreenSize.Small
        widthPx < 400 -> ScreenSize.Medium
        widthPx < 500 -> ScreenSize.Large
        else -> ScreenSize.XLarge
    }

    val config = ScreenConfig(
        width = widthDp,
        height = heightDp,
        density = density,
        screenSize = screenSize,
        isCompact = screenSize == ScreenSize.Small,
        isMedium = screenSize == ScreenSize.Medium,
        isLarge = screenSize == ScreenSize.Large,
        isTablet = screenSize == ScreenSize.XLarge
    )

    androidx.compose.runtime.CompositionLocalProvider(
        LocalScreenConfig provides config
    ) {
        content()
    }
}

// ─── Responsive Modifier Helpers ────────────────────────────
object Responsive {
    /**
     * Returns a Dp value that scales based on screen size.
     * Use for padding, margins, gaps, etc.
     */
    @Composable
    @ReadOnlyComposable
    fun dp(small: Float, medium: Float, large: Float, xlarge: Float = large * 1.1f): Dp {
        val config = LocalScreenConfig.current
        return when (config.screenSize) {
            ScreenSize.Small -> small.dp
            ScreenSize.Medium -> medium.dp
            ScreenSize.Large -> large.dp
            ScreenSize.XLarge -> xlarge.dp
        }
    }

    /**
     * Returns a fractional width (0.0 to 1.0) for columns.
     * e.g., Responsive.columnWidth(0.48f) for 2-column grid
     */
    @Composable
    @ReadOnlyComposable
    fun columnWidth(columns: Int): Float {
        val config = LocalScreenConfig.current
        val horizontalPadding = config.horizontalPadding.value * 2
        val availableWidth = config.width.value - horizontalPadding
        return when {
            config.isTablet && columns <= 3 -> 1f / min(columns, 3)
            config.isLarge && columns <= 2 -> 1f / min(columns, 2)
            else -> 1f
        }
    }

    /**
     * For LazyColumn/LazyRow: Use fillMaxWidth() always.
     * For grids: Use weight() with responsive column count.
     */
    @Composable
    @ReadOnlyComposable
    fun gridColumns(): Int {
        val config = LocalScreenConfig.current
        return when (config.screenSize) {
            ScreenSize.Small -> 1
            ScreenSize.Medium -> 1
            ScreenSize.Large -> 2
            ScreenSize.XLarge -> 2
        }
    }
}
