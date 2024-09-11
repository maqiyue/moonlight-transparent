package com.su.moonlight.next.game.menu.options

import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.su.moonlight.next.App
import com.su.moonlight.next.R

class CancelMenuOption :
    MenuOption(
        App.ins.resources.getString(R.string.game_menu_cancel),
        false,
        Icons.Rounded.ArrowUpward,
        null
    ) {

    @Composable
    override fun MenuUI() {
        CompositionLocalProvider(
            LocalContentColor provides Color.White
        ) {
            MenuOptionTile(icon = icon, label = label, backgroundColor = Color(0xAAB0BEC5))
        }
    }
}