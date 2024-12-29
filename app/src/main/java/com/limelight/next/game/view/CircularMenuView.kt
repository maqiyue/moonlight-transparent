package com.limelight.next.game.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.OvershootInterpolator
import android.view.animation.ScaleAnimation
import android.widget.FrameLayout
import androidx.core.graphics.ColorUtils
import com.limelight.binding.input.KeyboardTranslator
import com.limelight.nvstream.NvConnection
import com.limelight.nvstream.input.KeyboardPacket
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class CircularMenuView @JvmOverloads constructor(
    context: Context,
    private val conn: NvConnection,
    private val anchorView: View
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val radius = 180f
    private val centerRadius = 40f
    private val itemRadius = 45f
    private var centerX = 0f
    private var centerY = 0f
    private var currentScale = 0f
    
    private val menuItems = listOf(
        MenuItem("全选", shortArrayOf(KeyboardTranslator.VK_LCONTROL.toShort(), KeyboardTranslator.VK_A.toShort()), Color.parseColor("#7C4DFF")), // 深紫
        MenuItem("撤回", shortArrayOf(KeyboardTranslator.VK_LCONTROL.toShort(), KeyboardTranslator.VK_Z.toShort()), Color.parseColor("#536DFE")), // 靛蓝
        MenuItem("剪切", shortArrayOf(KeyboardTranslator.VK_LCONTROL.toShort(), KeyboardTranslator.VK_X.toShort()), Color.parseColor("#448AFF")), // 蓝色
        MenuItem("复制", shortArrayOf(KeyboardTranslator.VK_LCONTROL.toShort(), KeyboardTranslator.VK_C.toShort()), Color.parseColor("#FF4081")), // 粉红
        MenuItem("粘贴", shortArrayOf(KeyboardTranslator.VK_LCONTROL.toShort(), KeyboardTranslator.VK_V.toShort()), Color.parseColor("#FF5252"))  // 红色
    )
    
    private var selectedIndex = -1
    private var pressedIndex = -1
    private val buttonPositions = mutableListOf<ButtonPosition>()

    // 添加成员变量来存储角度信息
    private var visibleStartAngle: Float = 0f
    private var visibleEndAngle: Float = 0f

    init {
        paint.style = Paint.Style.FILL
        textPaint.apply {
            color = Color.WHITE
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        alpha = 0.9f
        startShowAnimation()
    }

    private fun startShowAnimation() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200
            interpolator = OvershootInterpolator()
            addUpdateListener { 
                currentScale = it.animatedValue as Float
                invalidate()
            }
        }.start()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 更新中心点坐标
        centerX = w / 2f
        centerY = h / 2f
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.scale(currentScale, currentScale, centerX, centerY)
        
        // 更新中心点为小键盘位置
        val anchorLocation = IntArray(2)
        anchorView.getLocationOnScreen(anchorLocation)
        val anchorCenterX = anchorLocation[0] + anchorView.width / 2f
        val anchorCenterY = anchorLocation[1] + anchorView.height / 2f
        
        val viewLocation = IntArray(2)
        getLocationOnScreen(viewLocation)
        centerX = anchorCenterX - viewLocation[0]
        centerY = anchorCenterY - viewLocation[1]
        
        buttonPositions.clear()
        
        // 获取视图在屏幕中的实际边界
        val viewLeft = viewLocation[0].toFloat()
        val viewTop = viewLocation[1].toFloat()
        val viewRight = viewLeft + width
        val viewBottom = viewTop + height
        
        // 计算圆弧在屏幕内的可见部分
        val visibleAngles = mutableListOf<Float>()
        for (deg in 0..360 step 1) {
            val rad = Math.toRadians(deg.toDouble())
            val x = centerX + cos(rad).toFloat() * radius
            val y = centerY + sin(rad).toFloat() * radius
            
            // 转换为屏幕坐标
            val screenX = x + viewLocation[0]
            val screenY = y + viewLocation[1]
            
            // 检查点是否在视图边界内
            if (screenX >= viewLeft && screenX <= viewRight && 
                screenY >= viewTop && screenY <= viewBottom) {
                visibleAngles.add(deg.toFloat())
            }
        }
        
        // 找出连续的可见角度范围
        if (visibleAngles.isNotEmpty()) {
            // 处理跨越360度的情况
            val ranges = mutableListOf<Pair<Float, Float>>()
            var currentStart = visibleAngles[0]
            var prevAngle = visibleAngles[0]
            
            for (i in 1 until visibleAngles.size) {
                val currentAngle = visibleAngles[i]
                if (currentAngle - prevAngle > 1) {
                    // 找到一个间隔，保存当前范围
                    ranges.add(currentStart to prevAngle)
                    currentStart = currentAngle
                }
                prevAngle = currentAngle
            }
            // 添加最后一个范围
            ranges.add(currentStart to visibleAngles.last())
            
            // 检查是否需要连接首尾
            if (ranges.size > 1 && 
                visibleAngles.last() >= 359f && 
                visibleAngles[0] <= 1f) {
                // 首尾相连，合并第一个和最后一个范围
                val firstRange = ranges.first()
                val lastRange = ranges.last()
                ranges.removeAt(ranges.lastIndex)
                ranges.removeAt(0)
                ranges.add(lastRange.first to (firstRange.second + 360f))
            }
            
            // 找出最大的范围
            val (maxStart, maxEnd) = ranges.maxByOrNull { 
                it.second - it.first 
            } ?: (0f to 360f)
            
            // 保存计算出的角度到成员变量
            visibleStartAngle = maxStart
            visibleEndAngle = maxEnd
            
            // 移除圆弧绘制代码，直接绘制按钮
            val arcLength = maxEnd - maxStart
            val segmentAngle = arcLength / menuItems.size
            
            // 只绘制按钮
            menuItems.forEachIndexed { index, item ->
                val segmentStart = maxStart + index * segmentAngle
                val middleAngle = segmentStart + segmentAngle / 2
                
                val angle = Math.toRadians(middleAngle.toDouble())
                val x = centerX + cos(angle).toFloat() * radius
                val y = centerY + sin(angle).toFloat() * radius
                
                paint.color = when {
                    index == selectedIndex && index == pressedIndex -> {
                        ColorUtils.blendARGB(item.color, Color.BLACK, 0.3f)
                    }
                    index == selectedIndex -> item.color
                    else -> item.color
                }
                
                canvas.drawCircle(x, y, itemRadius, paint)
                
                textPaint.apply {
                    color = Color.WHITE
                    textSize = 24f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(item.name, x, y + 8f, textPaint)
                
                buttonPositions.add(ButtonPosition(x, y, index))
            }
        }
        
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 检查是否点击到按钮
                selectedIndex = findTouchedButton(event.x, event.y)
                pressedIndex = selectedIndex
                invalidate()
                // 如果没有点击到按钮，收起菜单
                if (selectedIndex == -1) {
                    startHideAnimation {
                        (parent as? ViewGroup)?.removeView(this)
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val touchedIndex = findTouchedButton(event.x, event.y)
                if (selectedIndex != touchedIndex) {
                    selectedIndex = touchedIndex
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (selectedIndex != -1) {
                    sendKeys(menuItems[selectedIndex].keyCodes)
                    pressedIndex = -1
                    invalidate()
                }
            }
        }
        return true
    }
    
    private fun findTouchedButton(touchX: Float, touchY: Float): Int {
        // 检查每个按钮
        for (pos in buttonPositions) {
            val dx = touchX - pos.x
            val dy = touchY - pos.y
            // 如果触摸点在按钮半径内
            if (dx * dx + dy * dy <= itemRadius * itemRadius) {
                return pos.index
            }
        }
        return -1
    }
    
    private data class ButtonPosition(
        val x: Float,
        val y: Float,
        val index: Int
    )

    private data class MenuItem(
        val name: String,
        val keyCodes: ShortArray,
        val color: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MenuItem

            if (name != other.name) return false
            if (!keyCodes.contentEquals(other.keyCodes)) return false
            if (color != other.color) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + keyCodes.contentHashCode()
            result = 31 * result + color
            return result
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 移除时播放消失动画
        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 150
            interpolator = AccelerateInterpolator()
            addUpdateListener { 
                currentScale = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    (parent as? ViewGroup)?.removeView(this@CircularMenuView)
                }
            })
        }.start()
    }

    companion object {
        private const val TEST_GAME_FOCUS_DELAY: Long = 10
        private const val KEY_UP_DELAY: Long = 25

        const val PREF_NAME: String = "specialPrefs" // SharedPreferences的名称

        const val KEY_NAME: String = "special_key" // 要保存的键名称

        private fun getModifier(key: Short): Byte {
            return when (key.toInt()) {
                KeyboardTranslator.VK_LSHIFT -> KeyboardPacket.MODIFIER_SHIFT
                KeyboardTranslator.VK_LCONTROL -> KeyboardPacket.MODIFIER_CTRL
                KeyboardTranslator.VK_LWIN -> KeyboardPacket.MODIFIER_META
                KeyboardTranslator.VK_LMENU -> KeyboardPacket.MODIFIER_ALT
                else -> 0
            }
        }
    }

    private fun sendKeys(keys: ShortArray) {
        val modifier = byteArrayOf(0.toByte())

        for (key in keys) {
            conn.sendKeyboardInput(key, KeyboardPacket.KEY_DOWN, modifier[0], 0.toByte())

            // Apply the modifier of the pressed key, e.g. CTRL first issues a CTRL event (without
            // modifier) and then sends the following keys with the CTRL modifier applied
            modifier[0] = (modifier[0].toInt() or getModifier(key).toInt()).toByte()
        }

        Handler(Looper.getMainLooper()).postDelayed((Runnable {
            for (pos in keys.indices.reversed()) {
                val key = keys[pos]

                // Remove the keys modifier before releasing the key
                modifier[0] = (modifier[0].toInt() and getModifier(key).toInt().inv()).toByte()

                conn.sendKeyboardInput(key, KeyboardPacket.KEY_UP, modifier[0], 0.toByte())
            }
        }), KEY_UP_DELAY)
    }

    private fun startHideAnimation(onAnimationEnd: () -> Unit) {
        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 150
            interpolator = AccelerateInterpolator()
            addUpdateListener { 
                currentScale = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onAnimationEnd.invoke()
                }
            })
        }.start()
    }
} 