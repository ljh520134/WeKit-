package dev.ujhhgtg.wekit.hooks.api.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.extension.toClass
import de.robv.android.xposed.XposedHelpers
import dev.ujhhgtg.nameof.nameof
import dev.ujhhgtg.wekit.hooks.core.ApiHookItem
import dev.ujhhgtg.wekit.hooks.api.core.WeMessageApi
import dev.ujhhgtg.wekit.hooks.items.chat.SendCustomAppMessage
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.utils.ToastUtils
import dev.ujhhgtg.wekit.utils.logging.WeLogger

@HookItem(path = "API/聊天界面底栏扩展", desc = "为聊天界面底栏的发送按钮提供长按事件监听功能")
object WeChatFooterApi : ApiHookItem() {

    private val TAG = nameof(WeChatFooterApi)
    private const val KEY_FIELD_TO_USER = "cache_toUser"

    override fun onEnable() {
        "com.tencent.mm.pluginsdk.ui.chat.ChatFooter".toClass().asResolver().apply {
            firstConstructor {
                parameters(Context::class, AttributeSet::class, Int::class)
            }.hookAfter { param ->
                val chatFooterInstance = param.thisObject
                findAndBindSendButton(chatFooterInstance)
            }

            firstMethod {
                name = "setUserName"
            }.hookAfter { param ->
                val toUser = param.args[0] as? String
                if (!toUser.isNullOrEmpty()) {
                    XposedHelpers.setAdditionalInstanceField(
                        param.thisObject,
                        KEY_FIELD_TO_USER,
                        toUser
                    )
                }
            }
        }
    }

    private fun findAndBindSendButton(chatFooter: Any) {
        try {
            val fields = chatFooter.javaClass.declaredFields
            for (field in fields) {
                field.isAccessible = true
                if (!Button::class.java.isAssignableFrom(field.type)) continue

                val button = field.get(chatFooter) as? Button ?: continue
                val text = button.text?.toString()?.trim() ?: ""

                if (text == "发送" || text.equals("Send", ignoreCase = true)) {
                    button.setOnLongClickListener { view ->
                        try {
                            handleLongClickLogic(chatFooter, view)
                        } catch (e: Throwable) {
                            WeLogger.e(TAG, "业务逻辑执行出错", e)
                        }
                        true
                    }
                    break
                }
            }
        } catch (e: Throwable) {
            WeLogger.e(TAG, "查找按钮过程出错", e)
        }
    }

    private fun handleLongClickLogic(chatFooter: Any, buttonView: View) {
        val content = try {
            XposedHelpers.callMethod(chatFooter, "getLastText") as? String
        } catch (_: Throwable) {
            WeLogger.w(TAG, "getLastText 调用失败")
            null
        }

        val toUser =
            XposedHelpers.getAdditionalInstanceField(chatFooter, KEY_FIELD_TO_USER) as? String

        WeLogger.d(TAG, "content: $content, toUser: $toUser")

        if (toUser != null) {
            if (!content.isNullOrEmpty() && toUser.isNotEmpty()) {
                if (SendCustomAppMessage.isEnabled) {
                    val isSuccess = WeMessageApi.sendXmlAppMsg(toUser, content)
                    if (!isSuccess) {
                        WeLogger.e(TAG, "发送 XML 消息失败")
                        ToastUtils.showToast("发送 XML 消息失败，请检查格式")
                    } else {
                        findInputEditText(chatFooter as? View, content)?.setText("")
                    }
                }
            } else {
                WeLogger.e(TAG, "信息不完整，无法发送！User: $toUser, Content: $content")
                ToastUtils.showToast("未获取到当前聊天对象或内容为空")
            }
        }
    }

    private fun findInputEditText(view: View?, content: String): android.widget.EditText? {
        if (view is android.widget.EditText) {
            return view
        }

        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                val result = findInputEditText(child, content)
                if (content == (result?.text?.toString() ?: "")) {
                    return result
                }
            }
        }

        return null
    }
}
