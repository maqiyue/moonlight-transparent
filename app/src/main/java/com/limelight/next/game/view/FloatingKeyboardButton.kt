package com.limelight.next.game.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageButton
import com.limelight.Game
import com.limelight.R
import com.limelight.nvstream.NvConnection
import androidx.core.graphics.ColorUtils
import android.app.Activity

class FloatingKeyboardButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val conn: NvConnection
) : AppCompatImageButton(context, attrs, defStyleAttr) {
    
    private var lastX: Int = 0
    private var lastY: Int = 0
    private var isDragging = false
    private var initialX = 0f
    private var initialY = 0f
    private var game: Game? = null
    private val handler = Handler()
    private var isLongPress = false
    private var isPressed = false
    private val longPressRunnable = Runnable {
        isLongPress = true
        showCircularMenu()
    }
    
    init {
        setImageResource(R.drawable.ic_baseline_keyboard_24)
        alpha = 0.7f
        setColorFilter(Color.parseColor("#FFA500"), PorterDuff.Mode.SRC_IN)
        
        setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    isLongPress = false
                    isPressed = true
                    lastX = event.getRawX().toInt()
                    lastY = event.getRawY().toInt()
                    handler.postDelayed(longPressRunnable, 500)
                    updatePressedState()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.getRawX().toInt() - lastX
                    val dy = event.getRawY().toInt() - lastY
                    
                    if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                        isDragging = true
                        isPressed = false
                        handler.removeCallbacks(longPressRunnable)
                        updatePressedState()
                    }
                    
                    if (isDragging) {
                        val layoutParams = layoutParams as FrameLayout.LayoutParams
                        
                        var newX = layoutParams.leftMargin + dx
                        var newY = layoutParams.topMargin + dy
                        
                        val parent = parent as View
                        newX = newX.coerceIn(0, parent.width - width)
                        newY = newY.coerceIn(0, parent.height - height)
                        
                        layoutParams.leftMargin = newX
                        layoutParams.topMargin = newY
                        layoutParams.rightMargin = 0
                        layoutParams.bottomMargin = 0
                        
                        requestLayout()
                        
                        lastX = event.getRawX().toInt()
                        lastY = event.getRawY().toInt()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isPressed = false
                    handler.removeCallbacks(longPressRunnable)
                    if (!isDragging && !isLongPress) {
                        performClick()
                    }
                    updatePressedState()
                    true
                }
                else -> false
            }
        }
    }

    private fun updatePressedState() {
        alpha = if (isPressed) 0.8f else 0.5f
        imageAlpha = if (isPressed) 200 else 255
    }

    fun setGame(game: Game) {
        this.game = game
    }

    override fun performClick(): Boolean {
        super.performClick()
        game?.showHidekeyBoardLayoutController()
        return true
    }

    private fun showCircularMenu() {
        val menu = CircularMenuView(context, conn, this)
        val params = FrameLayout.LayoutParams(600, 600)
        
        val location = IntArray(2)
        getLocationOnScreen(location)
        
        val centerX = location[0] + width / 2
        val centerY = location[1] + height / 2
        
        params.leftMargin = centerX - 300
        params.topMargin = centerY - 300
        
        val parent = parent as FrameLayout
        params.leftMargin = params.leftMargin.coerceIn(0, parent.width - 600)
        params.topMargin = params.topMargin.coerceIn(0, parent.height - 600)
        
        parent.addView(menu, params)
    }
} 