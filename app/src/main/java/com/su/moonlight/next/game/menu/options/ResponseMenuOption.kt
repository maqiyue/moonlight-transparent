package com.su.moonlight.next.game.menu.options

import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddChart
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.limelight.Game
import com.su.moonlight.next.App
import com.su.moonlight.next.R
import com.su.moonlight.next.defaultTileBackgroundColor
import com.su.moonlight.next.enablePerformanceOverlayTileBackgroundColor
import com.su.moonlight.next.enableZoomModeEnabledTileBackgroundColor

abstract class BaseResponseMenuOption(
    private val label: String,
    private val icon: ImageVector,
    private val isEnable: Boolean,
    private val respColor: Color,
    runnable: Runnable
) :
    MenuPanelOption(runnable) {

    @Composable
    override fun MenuUI() {
        val color = if (isEnable) Color.White else Color.Unspecified
        val backgroundColor =
            if (isEnable) respColor else defaultTileBackgroundColor
        CompositionLocalProvider(
            LocalContentColor provides color
        ) {
            MenuOptionTile(
                icon,
                label,
                backgroundColor
            )
        }
    }
}

class PerformanceOverlayMenuOption(game: Game) : BaseResponseMenuOption(
    App.ins.resources.getString(R.string.game_menu_hud),
    Icons.Rounded.AddChart,
    game.isEnablePerformanceOverlay,
    enablePerformanceOverlayTileBackgroundColor,
    Runnable { game.showHUD() }
)

class PanZoomModeMenuOption(game: Game) : BaseResponseMenuOption(
    App.ins.resources.getString(if (game.isZoomModeEnabled) R.string.game_menu_disable_zoom_mode else R.string.game_menu_enable_zoom_mode),
    Icons.Rounded.PanTool,
    game.isZoomModeEnabled,
    enableZoomModeEnabledTileBackgroundColor,
    Runnable { game.toggleZoomMode() }
)