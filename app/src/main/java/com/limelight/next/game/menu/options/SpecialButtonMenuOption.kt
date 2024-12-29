package com.limelight.next.game.menu.options

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.next.defaultTileBackgroundColor

class SpecialButtonMenuOption(
    private val label: String,
    private val indexLabel: Int = index++,
    runnable: Runnable
) :
    MenuPanelOption(runnable) {

    companion object {
        private const val START_INDEX = 1
        private var index = START_INDEX
        fun init() {
            index = START_INDEX
        }
    }

    @Composable
    override fun MenuUI() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1F)
                .clip(RoundedCornerShape(2.dp))
                .background(defaultTileBackgroundColor)
                .padding(4.dp)
        ) {
            Text(
                modifier = Modifier
                    .align(Alignment.TopStart),
                text = "$indexLabel", fontSize = 32.sp
            )
            Text(modifier = Modifier.align(Alignment.BottomStart), text = label, fontSize = 12.sp)
        }
    }
}