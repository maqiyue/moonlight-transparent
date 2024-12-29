package com.limelight.next.game.menu.options

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.next.defaultTileBackgroundColor

@Preview
@Composable
private fun Preview() {
    Box(modifier = Modifier.size(96.dp)) {
        MenuOptionTile(icon = Icons.Default.AcUnit, label = "测试")
    }
}

@Composable
fun MenuOptionTile(
    icon: ImageVector,
    label: String,
    backgroundColor: Color = defaultTileBackgroundColor
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1F)
            .clip(RoundedCornerShape(2.dp))
            .background(backgroundColor)
            .padding(4.dp)
    ) {
        Icon(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 8.dp)
                .size(48.dp),
            imageVector = icon,
            contentDescription = ""
        )
        Text(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            text = label,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

open class MenuOption @JvmOverloads constructor(
    @JvmField val label: String,
    @JvmField val withGameFocus: Boolean,
    protected val icon: ImageVector = Icons.Default.AutoAwesome,
    runnable: Runnable?
) : MenuPanelOption(runnable) {

    constructor(label: String, runnable: Runnable?) : this(label, false, runnable = runnable)

    @Composable
    override fun MenuUI() {
        MenuOptionTile(icon = icon, label = label)
    }
}

abstract class MenuPanelOption(
    @JvmField val runnable: Runnable?
) {

    @Composable
    abstract fun MenuUI()

    open val isAutoCloseMenu: Boolean = true
}