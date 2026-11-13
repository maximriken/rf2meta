package net.l2meta.scanner

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import kotlin.math.abs

/**
 * Плаваюча кнопка поверх гри. Її можна перетягувати пальцем.
 * Використовує тип вікна TYPE_ACCESSIBILITY_OVERLAY, тому окремий дозвіл
 * «Показ поверх інших застосунків» не потрібен.
 */
class OverlayButton(
    private val ctx: Context,
    private val onClick: () -> Unit,
) {
    private val wm = ctx.getSystemService(WindowManager::class.java)
    private var shown = false

    private val view = TextView(ctx).apply {
        text = "Скан"
        setTextColor(Color.WHITE)
        textSize = 14f
        gravity = Gravity.CENTER
        setPadding(dp(18), dp(10), dp(18), dp(10))
        background = GradientDrawable().apply {
            cornerRadius = dp(22).toFloat()
            setColor(0xCC1E6FD9.toInt())
            setStroke(dp(1), Color.WHITE)
        }
    }

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = dp(16)
        y = dp(260)
    }

    init {
        setupDrag()
    }

    fun show() {
        if (!shown) {
            wm.addView(view, params)
            shown = true
        }
    }

    fun hide() {
        if (shown) {
            runCatching { wm.removeView(view) }
            shown = false
        }
    }

    fun setText(text: String) {
        view.text = text
    }

    fun setVisible(visible: Boolean) {
        view.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDrag() {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false
        val slop = dp(8)

        view.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = e.rawX
                    touchY = e.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - touchX).toInt()
                    val dy = (e.rawY - touchY).toInt()
                    if (abs(dx) > slop || abs(dy) > slop) moved = true
                    if (moved) {
                        params.x = startX + dx
                        params.y = startY + dy
                        wm.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) onClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun dp(v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()
}
