package com.limelight.next.game.menu.options

import androidx.annotation.DrawableRes
import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.rounded.AddChart
import androidx.compose.material.icons.rounded.KeyboardAlt
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.KeyboardCommandKey
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Input
import androidx.compose.material.icons.rounded.KeyboardHide
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.limelight.Game
import com.limelight.R
import com.limelight.preferences.PreferenceConfiguration

import com.limelight.next.defaultTileBackgroundColor
import com.limelight.next.enablePerformanceOverlayTileBackgroundColor
import com.limelight.next.enableZoomModeEnabledTileBackgroundColor
import com.moonlight.next.game.menu.GameMenuPanel

abstract class BaseResponseMenuOption : MenuPanelOption {
    constructor(
        label: String,
        icon: ImageVector,
        isEnable: Boolean,
        respColor: Color,
        runnable: Runnable
    ) : super(runnable) {
        this.label = label
        this.icon = icon
        this.iconRes = null
        this.isEnable = isEnable
        this.respColor = respColor
    }

    constructor(
        label: String,
        @DrawableRes iconRes: Int,
        isEnable: Boolean,
        respColor: Color,
        runnable: Runnable
    ) : super(runnable) {
        this.label = label
        this.icon = null
        this.iconRes = iconRes
        this.isEnable = isEnable
        this.respColor = respColor
    }

    private val label: String
    private val icon: ImageVector?
    private val iconRes: Int?
    private val isEnable: Boolean
    private val respColor: Color

    @Composable
    override fun MenuUI() {
        val color = if (isEnable) Color.White else Color.Unspecified
        val backgroundColor = if (isEnable) respColor else defaultTileBackgroundColor
        CompositionLocalProvider(LocalContentColor provides color) {
            if (iconRes != null) {
                MenuOptionTile(iconRes = iconRes, label = label, backgroundColor = backgroundColor)
            } else {
                MenuOptionTile(icon = icon!!, label = label, backgroundColor = backgroundColor)
            }
        }
    }
}

class PerformanceOverlayMenuOption(game: Game) : BaseResponseMenuOption(
    game.resources.getString(R.string.game_menu_hud),
    Icons.Rounded.AddChart,
    game.isPerfOverlayEnabled(),
    enablePerformanceOverlayTileBackgroundColor,
    Runnable { game.toggleHUD() }
)

class PanZoomModeMenuOption(game: Game) : BaseResponseMenuOption(
    game.resources.getString(if (game.isZoomModeEnabled) R.string.game_menu_disable_zoom_mode else R.string.game_menu_enable_zoom_mode),
    Icons.Rounded.PanTool,
    game.isZoomModeEnabled,
    enableZoomModeEnabledTileBackgroundColor,
    Runnable { game.toggleZoomMode() }
)

class VirtualKeyboardMenuOption(game: Game) : BaseResponseMenuOption(
    game.getString(R.string.game_menu_toggle_virtual_keyboard_model),
    Icons.Rounded.KeyboardHide,
    true,
    Color(0xAA607D8B),
    Runnable { game.showHidekeyBoardLayoutController() }
)

class SendKeysMenuOption(game: Game, gameMenuPanel: GameMenuPanel, ) : BaseResponseMenuOption(
    game.getString(R.string.game_menu_send_keys),
    Icons.Default.Computer,
    true,
    Color(0xAA607D8B),
    Runnable { gameMenuPanel.showSpecialKeysMenu() }
)

class FloatingKeyboardMenuOption(game: Game) : BaseResponseMenuOption(
    game.getString(R.string.game_menu_toggle_floating_keyboard),
    R.drawable.multimodal_hand_eye_24px,
    game.isFloatingKeyboardShowing(),
    Color(0xCCEF754D),
    Runnable { game.toggleFloatingKeyboard() }
)

class ThreeFingerKeyboardMenuOption(game: Game) : BaseResponseMenuOption(
    game.getString(R.string.game_menu_toggle_three_finger_keyboard),
    R.drawable.trackpad_input_3_24px,
    game.isThreeFingerKeyboardEnabled(),
    Color(0xAABC94F3),
    Runnable { game.toggleThreeFingerKeyboard() }
)