package com.su.moonlight.next.base

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import com.su.moonlight.next.LocalComposeDialogController


open class ComposeDialog(private val content: @Composable () -> Unit) :
    DialogFragment(), ComposeDialogController {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                CompositionLocalProvider(LocalComposeDialogController provides this@ComposeDialog) {
                    content()
                }
            }
        }
    }

    override fun dismissDialog() {
        dismiss()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            attributes = attributes.apply {
                gravity = Gravity.TOP
            }
        }
    }
}