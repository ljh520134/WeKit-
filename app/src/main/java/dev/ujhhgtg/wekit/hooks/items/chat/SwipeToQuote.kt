package dev.ujhhgtg.wekit.hooks.items.chat

import android.annotation.SuppressLint
import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import android.widget.FrameLayout
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import de.robv.android.xposed.XC_MethodHook
import dev.ujhhgtg.wekit.dexkit.dsl.dexClass
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import dev.ujhhgtg.wekit.dexkit.abc.IResolvesDex
import dev.ujhhgtg.wekit.hooks.api.core.WeMessageApi
import dev.ujhhgtg.wekit.hooks.api.core.WeServiceApi
import dev.ujhhgtg.wekit.hooks.api.core.model.MessageInfo
import dev.ujhhgtg.wekit.hooks.api.ui.WeChatMessageViewApi
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.utils.LruCache
import org.luckypray.dexkit.DexKitBridge
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp

@SuppressLint("StaticFieldLeak")
@HookItem(path = "聊天/左划引用消息", desc = "在消息上左划以引用")
object SwipeToQuote : SwitchHookItem(), IResolvesDex,
    WeChatMessageViewApi.ICreateViewListener {

    private val cache = LruCache<Pair<String, Long>, Boolean>()

    override fun onEnable() {
        WeChatMessageViewApi.addListener(this)
    }

    override fun onDisable() {
        WeChatMessageViewApi.removeListener(this)
    }

    override fun onCreateView(
        param: XC_MethodHook.MethodHookParam,
        view: View
    ) {
        val msgInfo = WeChatMessageViewApi.getMsgInfoFromParam(param)
        if (cache[msgInfo.talker to msgInfo.id] == true) return
        val chattingContext = WeChatMessageViewApi.getChattingContextFromParam(param)
        attachSwipeGesture(view, chattingContext, msgInfo)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachSwipeGesture(
        originalView: View,
        chattingContext: Any,
        msgInfo: MessageInfo
    ) {
        var startX = 0f
        var startY = 0f
        var isDragging = false
        val triggerThreshold = dpToPx(originalView.context, 60).toFloat()
        var triggered = false

        ViewGroup::class.asResolver()
            .firstMethod {
                name = "onInterceptTouchEvent"
            }
            .hookAfter { param ->
                val v = param.thisObject as ViewGroup
                if (v !== originalView) return@hookAfter

                val event = param.args[0] as MotionEvent

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.x
                        startY = event.y
                        isDragging = false
                        triggered = false
                        // Never intercept DOWN — children need it to arm their listeners
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.x - startX
                        val dy = event.y - startY

                        if (!isDragging && abs(dx) > abs(dy) && dx < 0) {
                            isDragging = true
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        }

                        if (isDragging) {
                            // Returning true here cancels children and routes
                            // subsequent events to our onTouchEvent
                            param.result = true
                        }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isDragging = false
                    }
                }
            }

        View::class.asResolver().firstMethod {
            name = "onTouchEvent"
            superclass()
        }
            .hookAfter { param ->
                val v = param.thisObject as View
                if (v !== originalView) return@hookAfter

                val event = param.args[0] as MotionEvent

                when (event.action) {
                    MotionEvent.ACTION_MOVE -> {
                        if (isDragging) {
                            val rawDx = event.x - startX
                            val dx = rawDx.coerceIn(-triggerThreshold, 0f)
                            v.translationX = dx

                            if (!triggered && rawDx < -triggerThreshold) {
                                triggered = true
                                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            }

                            param.result = true
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        if (isDragging) {
                            v.animate()
                                .translationX(0f)
                                .setDuration(250)
                                .setInterpolator(SpringInterpolator())
                                .start()

                            if (triggered) onSwipeLeft(originalView, chattingContext)

                            v.parent?.requestDisallowInterceptTouchEvent(false)
                            isDragging = false

                            param.result = true
                        }
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        if (isDragging) {
                            v.animate()
                                .translationX(0f)
                                .setDuration(150)
                                .start()

                            v.parent?.requestDisallowInterceptTouchEvent(false)

                            isDragging = false
                        }
                    }
                }
            }

        cache[msgInfo.talker to msgInfo.id] = true
    }

    // Poor man's spring: decelerates then slightly overshoots back to 0
    private class SpringInterpolator : Interpolator {
        override fun getInterpolation(t: Float): Float {
            // Damped sine approximation
            return (1f - (cos(t * PI * 2.5) * exp(-t * 5f))).toFloat()
        }
    }

    private fun onSwipeLeft(
        originalView: View,
        chattingContext: Any
    ) {
        val apiMan = chattingContext.asResolver()
            .firstField { type = WeServiceApi.methodApiManagerGetApi.method.declaringClass }
            .get()!!
        val api = WeServiceApi.getApiByClass(apiMan, classChattingUiFootComponent.clazz)
        val chatFooter = api.asResolver()
            .firstField { type = "com.tencent.mm.pluginsdk.ui.chat.ChatFooter" }
            .get()!! as FrameLayout
        val quoteMethod = chatFooter.asResolver()
            .firstMethod {
                parameters { params ->
                    params[0] == WeMessageApi.classMsgInfo.clazz
                }
                returnType = Boolean::class
            }.self
        val chatHolder = originalView.tag.asResolver()
            .firstField {
                name = "chatHolder"
                superclass()
            }.get()!!
        val msgInfo = methodGetMsgInfo.method.invoke(null, chatHolder, chattingContext)
        if (quoteMethod.parameterCount == 1) {
            quoteMethod.invoke(chatFooter, msgInfo)
        } else {
            quoteMethod.invoke(chatFooter, msgInfo, null)
        }
    }

    private fun dpToPx(context: Context, dp: Int) =
        (dp * context.resources.displayMetrics.density).toInt()

    private val classChattingUiFootComponent by dexClass()
    private val methodGetMsgInfo by dexMethod()

    override fun resolveDex(dexKit: DexKitBridge): Map<String, String> {
        val descriptors = mutableMapOf<String, String>()

        classChattingUiFootComponent.find(dexKit, descriptors) {
            searchPackages("com.tencent.mm.ui.chatting.component")
            matcher {
                usingEqStrings(
                    "MicroMsg.ChattingUI.FootComponent",
                    "onNotifyChange event %s talker %s"
                )
            }
        }

        methodGetMsgInfo.find(dexKit, descriptors) {
            searchPackages("com.tencent.mm.ui.chatting.viewitems")
            matcher {
                usingEqStrings("ItemDataTag", "getCurrentMsg2 err")
            }
        }

        return descriptors
    }
}
