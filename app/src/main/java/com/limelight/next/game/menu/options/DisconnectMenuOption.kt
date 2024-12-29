package com.limelight.next.game.menu.options

import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixOff
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.limelight.Game
import com.limelight.R



class DisconnectMenuOption(game: Game) :
    MenuOption(game.resources.getString(R.string.game_menu_disconnect),
        false,
        Icons.Rounded.AutoFixOff,
        Runnable { game.disconnect() }
    ) {

    @Composable
    override fun MenuUI() {
        CompositionLocalProvider(
            LocalContentColor provides Color.White
        ) {
            MenuOptionTile(icon = icon, label = label, backgroundColor = Color(0xAA03A9F4))
        }
    }
}