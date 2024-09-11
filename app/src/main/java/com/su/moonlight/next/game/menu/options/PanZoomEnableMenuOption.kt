package com.su.moonlight.next.game.menu.options

import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.PanToolAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.limelight.Game
import com.su.moonlight.next.App
import com.su.moonlight.next.R

class PanZoomEnableMenuOption(game: Game) :
    MenuOption("",
        true,
        Icons.Rounded.PanToolAlt,
        Runnable { game.toggleZoomMode() }
    ) {

    @Composable
    override fun MenuUI() {
        CompositionLocalProvider(
            LocalContentColor provides Color.White
        ) {
            MenuOptionTile(icon = icon, label = label, backgroundColor = Color(0xAA607D8B))
        }
    }

    override val isAutoCloseMenu: Boolean = false
}