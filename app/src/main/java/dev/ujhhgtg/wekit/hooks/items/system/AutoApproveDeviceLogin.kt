package dev.ujhhgtg.wekit.hooks.items.system

import android.app.Activity
import android.widget.Button
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.extension.toClass
import dev.ujhhgtg.nameof.nameof
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.utils.logging.WeLogger

@HookItem(path = "系统与隐私/自动批准设备登录", desc = "其他设备请求登录时自动勾选选项并点击按钮")
object AutoApproveDeviceLogin : SwitchHookItem() {
    private const val AUTO_SYNC_MESSAGES = 0x1
    private const val SHOW_LOGIN_DEVICE = 0x2
    private const val AUTO_LOGIN_DEVICE = 0x4

    private val TAG = nameof(AutoApproveDeviceLogin)

    override fun onEnable() {
        val targetClass = "com.tencent.mm.plugin.webwx.ui.ExtDeviceWXLoginUI".toClass()

        targetClass.asResolver().firstMethod { name = "onCreate" }.hookBefore { param ->
            val activity = param.thisObject as Activity
            var functionControl = 0
            functionControl = functionControl or AUTO_SYNC_MESSAGES
            functionControl = functionControl or SHOW_LOGIN_DEVICE
            functionControl = functionControl or AUTO_LOGIN_DEVICE
            activity.intent.putExtra("intent.key.function.control", functionControl)
            activity.intent.putExtra("intent.key.need.show.privacy.agreement", false)
        }

        targetClass.asResolver().firstMethod { name = "initView" }.hookAfter { param ->
            val fields = param.thisObject.javaClass.declaredFields
            val buttonField = fields.firstOrNull { it.type == Button::class.java }
                ?: run {
                    WeLogger.w(TAG, "Button field not found in initView")
                    return@hookAfter
                }
            buttonField.isAccessible = true
            val button = buttonField.get(param.thisObject) as? Button
                ?: run {
                    WeLogger.w(TAG, "Button field value is null")
                    return@hookAfter
                }
            button.callOnClick()
        }
    }
}
