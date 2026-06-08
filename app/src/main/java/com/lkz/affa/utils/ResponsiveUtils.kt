package com.lkz.affa.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Equivalent of Flutter's ResponsiveUtils.
 * Provides breakpoint helpers based on the current window width.
 *
 * In Compose there is no BuildContext — use [LocalConfiguration] instead.
 * All helpers are @Composable so they can read the composition-local config
 * reactively (recompose automatically on orientation/window size change).
 *
 * Usage:
 * ```
 * val columns = ResponsiveUtils.getGridColumnCount()
 * if (ResponsiveUtils.isWide()) { ... }
 * ```
 */
object ResponsiveUtils {

    private const val MOBILE_BREAKPOINT = 600
    private const val TABLET_BREAKPOINT = 1024

    @Composable
    fun screenWidthDp(): Int = LocalConfiguration.current.screenWidthDp

    /** Width < 600dp */
    @Composable
    fun isMobile(): Boolean = screenWidthDp() < MOBILE_BREAKPOINT

    /** 600dp ≤ Width < 1024dp */
    @Composable
    fun isTablet(): Boolean = screenWidthDp() in MOBILE_BREAKPOINT until TABLET_BREAKPOINT

    /** Width ≥ 1024dp */
    @Composable
    fun isDesktop(): Boolean = screenWidthDp() >= TABLET_BREAKPOINT

    /** Width ≥ 600dp (tablet or wider) */
    @Composable
    fun isWide(): Boolean = screenWidthDp() >= MOBILE_BREAKPOINT

    /**
     * Returns the number of grid columns based on current screen width.
     * Matches Flutter's getGridColumnCount breakpoints exactly.
     */
    @Composable
    fun getGridColumnCount(): Int = when {
        screenWidthDp() < 450  -> 3
        screenWidthDp() < 850  -> 4
        screenWidthDp() < 1200 -> 5
        screenWidthDp() < 1500 -> 6
        else                   -> 7
    }

    /**
     * Standard poster aspect ratio (width / height = 0.7).
     * Constant — does not depend on screen size.
     */
    const val CARD_ASPECT_RATIO = 0.7f


    fun isMobile(widthDp: Int): Boolean = widthDp < MOBILE_BREAKPOINT
    fun isTablet(widthDp: Int): Boolean = widthDp in MOBILE_BREAKPOINT until TABLET_BREAKPOINT
    fun isDesktop(widthDp: Int): Boolean = widthDp >= TABLET_BREAKPOINT
    fun isWide(widthDp: Int): Boolean = widthDp >= MOBILE_BREAKPOINT

    fun getGridColumnCount(widthDp: Int): Int = when {
        widthDp < 450  -> 3
        widthDp < 850  -> 4
        widthDp < 1200 -> 5
        widthDp < 1500 -> 6
        else           -> 7
    }
}
