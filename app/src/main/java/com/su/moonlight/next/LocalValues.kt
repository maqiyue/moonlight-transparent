package com.su.moonlight.next

import androidx.compose.runtime.compositionLocalOf
import com.su.moonlight.next.base.ComposeDialogController

val LocalComposeDialogController = compositionLocalOf<ComposeDialogController> { object :ComposeDialogController{
    override fun dismissDialog() {
        TODO("Not yet implemented")
    }
} }