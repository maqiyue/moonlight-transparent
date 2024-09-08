package com.su.moonlight.next.game.pref

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.base.BaseComposeView
import com.limelight.preferences.PreferenceConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@Preview
@Composable
private fun Preview() {
    Column(modifier = Modifier.background(Color.White)) {
        Text(text = "Lite")
        PerformanceOverlay("测试", true)
        Text(text = "Fill")
        PerformanceOverlay("测试", false)
    }
}

@Composable
private fun PerformanceOverlay(info: String = "测试", isLiteMode: Boolean = true) {
    val showInfo = info.ifBlank { "no data" }
    if (isLiteMode) {
        LitePerformanceOverlay(info = showInfo)
    } else {
        FillPerformanceOverlay(info = showInfo)
    }
}

@Composable
private fun LitePerformanceOverlay(info: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x35000000))

        ) {
            Text(
                modifier = Modifier.padding(2.dp),
                text = info,
                color = Color.White,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun FillPerformanceOverlay(info: String) {
    Box(
        modifier = Modifier
            .padding(9.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x35000000))
    ) {
        Row(horizontalArrangement = Arrangement.Start) {
            Text(
                modifier = Modifier.padding(2.dp),
                text = info,
                color = Color.White,
                fontSize = 10.sp
            )
        }
    }
}

class PerformanceOverlayView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defaultStyle: Int = 0
) : BaseComposeView(context, attributeSet, defaultStyle) {

    var isLiteMode: Boolean = true

    private val flow = MutableStateFlow<PerformanceInfo>(PerformanceInfo.EMPTY)
    private val safeFlow =
        flow
            .buffer(50)
            .map { if (isLiteMode) it.toLiteDesc() else it.toFillDesc() }
            .flowOn(Dispatchers.Default)

    fun initConfig(config: PreferenceConfiguration) {
        isLiteMode = config.enablePerfOverlayLite
    }

    fun submitInfo(info: PerformanceInfo) {
        flow.tryEmit(info)
    }

    @Composable
    override fun UiContent() {
        val state by safeFlow.collectAsState(initial = "")
        PerformanceOverlay(state, isLiteMode)
    }

}