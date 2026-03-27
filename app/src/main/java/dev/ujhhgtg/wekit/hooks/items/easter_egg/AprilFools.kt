package dev.ujhhgtg.wekit.hooks.items.easter_egg

import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Shader
import android.text.TextPaint
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.extension.toClass
import dev.ujhhgtg.wekit.hooks.core.BaseHookItem
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.preferences.WePrefs
import dev.ujhhgtg.wekit.utils.TargetProcesses
import java.lang.reflect.Field
import java.time.LocalDate
import java.time.Month
import java.util.WeakHashMap


@HookItem(path = "愚人节菜单", desc = "不显示于模块界面, 愚人节自动启用")
object AprilFools : BaseHookItem() {

    const val KEY_SURRENDER = "april_fools_surrender"

    override fun startup(process: Int) {
        if (process != TargetProcesses.PROC_MAIN) return

        if (!LocalDate.now().isAprilFools) {
            WePrefs.putBool(KEY_SURRENDER, false)
        }
        else {
            if (WePrefs.getBoolOrFalse(KEY_SURRENDER)) return
            enable()
        }
    }

    private val viewStateMap = WeakHashMap<View, TextViewAnimationState>()
    private data class TextViewAnimationState(val matrix: Matrix, var offset: Float)

    private lateinit var noMeasuredTvTextProp: Field
    private lateinit var noMeasuredTvPaintProp: Field

    private const val PIXELS_PER_FRAME = 10.0f
    private val RAINBOW_COLORS = intArrayOf(
        Color.RED, Color.YELLOW, Color.GREEN,
        Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
    )

    override fun onEnable() {
        ImageView::class.asResolver()
            .firstConstructor { parameterCount = 4 }.hookAfter { param ->
            applyRotation(param.thisObject as View)
        }

        "com.tencent.mm.ui.widget.QImageView".toClass().asResolver()
            .firstConstructor().hookAfter { param ->
                applyRotation(param.thisObject as View)
            }

        TextView::class.asResolver().firstMethod { name = "onDraw" }.hookBefore { param ->
            val tv = param.thisObject as TextView
            applyRainbowEffect(tv, tv.text, tv.paint)
        }

        "com.tencent.mm.ui.base.NoMeasuredTextView".toClass().asResolver()
            .firstMethod { name = "onDraw" }.hookBefore { param ->
                val view = param.thisObject as View

                if (!::noMeasuredTvTextProp.isInitialized) {
                    noMeasuredTvTextProp = view.asResolver().firstField { name = "mText" }.self.apply { isAccessible = true }
                    noMeasuredTvPaintProp = view.asResolver().firstField { type = TextPaint::class }.self.apply { isAccessible = true }
                }

                applyRainbowEffect(
                    view,
                    noMeasuredTvTextProp.get(view) as CharSequence,
                    noMeasuredTvPaintProp.get(view) as TextPaint)
            }
    }

    private fun applyRotation(view: View) {
        view.post {
            ObjectAnimator.ofFloat(view, "rotation", 0f, 360f).apply {
                duration = 1000
                repeatCount = ObjectAnimator.INFINITE
                interpolator = LinearInterpolator()
                start()
            }
        }
    }

    private fun applyRainbowEffect(view: View, text: CharSequence, paint: TextPaint) {
        val width = view.measuredWidth.toFloat()
        if (width <= 0f || text.isEmpty()) return

        val state = viewStateMap.getOrPut(view) { TextViewAnimationState(Matrix(), 0f) }

        val rainbowWidth = width.coerceAtLeast(400f)
        val shader = LinearGradient(
            0f, 0f, rainbowWidth, 0f,
            RAINBOW_COLORS, null, Shader.TileMode.REPEAT
        )

        state.offset += PIXELS_PER_FRAME
        if (state.offset > rainbowWidth) state.offset -= rainbowWidth

        state.matrix.setTranslate(state.offset, 0f)
        shader.setLocalMatrix(state.matrix)

        paint.shader = shader

        view.postInvalidateDelayed(60)
    }
}

val LocalDate.isAprilFools get() = this.month == Month.APRIL && this.dayOfMonth == 1
