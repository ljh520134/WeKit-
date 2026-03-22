package dev.ujhhgtg.wekit.hooks.items.chat

import android.view.View
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import de.robv.android.xposed.XC_MethodHook
import dev.ujhhgtg.wekit.hooks.api.core.WeMessageApi
import dev.ujhhgtg.wekit.hooks.api.core.WeServiceApi
import dev.ujhhgtg.wekit.hooks.api.core.model.MessageType
import dev.ujhhgtg.wekit.hooks.api.ui.WeChatMessageViewApi
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import dev.ujhhgtg.wekit.utils.LruCache
import java.lang.reflect.InvocationTargetException

@HookItem(path = "聊天/自动语音转文字", desc = "自动将语音消息转为文字")
object AutoSpeechToText : SwitchHookItem(),
    WeChatMessageViewApi.ICreateViewListener {

    private val processedMessages = LruCache<Long, Boolean>()

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
        if (!msgInfo.isType(MessageType.VOICE)) return

        val id = msgInfo.id
        if (processedMessages[id] == true) {
            return
        }

        val chattingContext = WeChatMessageViewApi.getChattingContextFromParam(param)
        val apiMan = chattingContext.asResolver()
            .firstField {
                type = WeServiceApi.methodApiManagerGetApi.method.declaringClass
            }
            .get()!!
        val api = WeServiceApi.getApiByClass(apiMan, WeMessageApi.classTransformChattingComponent.clazz)
        val chatViewItem = api.asResolver()
            .firstMethod {
                parameters(Long::class)
                returnType { clazz ->
                    clazz.name.startsWith("com.tencent.mm.ui.chatting.viewitems")
                }
            }
            .invoke(id)

        if (chatViewItem.toString() == "NoTransform") {
            processedMessages[id] = true
            try {
                api.asResolver()
                    .firstMethod {
                        parameters(
                            WeMessageApi.classMsgInfo.clazz,
                            Boolean::class.java,
                            Int::class.java,
                            Int::class.java
                        )
                        returnType = Void::class.javaPrimitiveType
                    }
                    .invoke(msgInfo.instance, false, -1, 0)
            } catch (_: InvocationTargetException) {
                // WeChat throws `java.lang.NullPointerException: getImgPath(...) must not be null`,
                // but that's not what we should care about and doesn't affect functionality
            }
        }
    }
}
