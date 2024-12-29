package com.limelight.next

import androidx.compose.runtime.compositionLocalOf
import com.limelight.next.base.ComposeDialogController

val LocalComposeDialogController = compositionLocalOf<ComposeDialogController> { object :ComposeDialogController{
    override fun dismissDialog() {
        TODO("Not yet implemented")
    }
} }