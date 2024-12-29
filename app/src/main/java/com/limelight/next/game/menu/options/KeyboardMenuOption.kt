package com.limelight.next.game.menu.options

import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixOff
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.limelight.Game
import com.limelight.R



class KeyboardMenuOption(game: Game) :
    MenuOption(game.resources.getString(R.string.game_menu_toggle_keyboard),
        true,
        Icons.Rounded.Keyboard,
        Runnable { game.toggleKeyboard() }
    ) {

    @Composable
    override fun MenuUI() {
        CompositionLocalProvider(
            LocalContentColor provides Color.White
        ) {
            MenuOptionTile(icon = icon, label = label, backgroundColor = Color(0xAA607D8B))
        }
    }
}