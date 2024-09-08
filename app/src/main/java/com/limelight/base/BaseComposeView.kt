package com.limelight.base

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView

abstract class BaseComposeView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defaultStyle: Int = 0
) : FrameLayout(context, attributeSet, defaultStyle) {

    companion object {
        val DefaultContentLayoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    init {
        addView(ComposeView(context).apply {
            setContent { UiContent() }
        }, DefaultContentLayoutParams)
    }

    @Composable
    abstract fun UiContent()
}