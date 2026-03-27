package dev.ujhhgtg.wekit.hooks.items.system

import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.extension.toClass
import dev.ujhhgtg.wekit.constants.PackageNames
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem

// https://github.com/Ujhhgtg/PandorasBox
@HookItem(path = "系统与隐私/预见性返回动画", desc = "为宿主的活动强制启用预见性返回动画")
object PredictiveBackGestures : SwitchHookItem() {

    private const val PRIVATE_FLAG_ENABLE_ON_BACK_INVOKED_CALLBACK = 1 shl 2
    private const val PRIVATE_FLAG_DISABLE_ON_BACK_INVOKED_CALLBACK = 1 shl 3
    private const val PRIVATE_FLAG_EXT_ENABLE_ON_BACK_INVOKED_CALLBACK = 1 shl 3

    override fun onEnable() {
        ApplicationInfo::class.asResolver()
            .firstConstructor {
                parameters(ApplicationInfo::class.java)
            }.hookAfter { param ->
                val info = param.args[0] as ApplicationInfo
                val field =
                    info.asResolver().firstField { name = "privateFlagsExt" }
                var flags = field.get() as Int
                flags = flags or PRIVATE_FLAG_EXT_ENABLE_ON_BACK_INVOKED_CALLBACK
                field.set(flags)
            }

        ActivityInfo::class.asResolver()
            .firstConstructor()
            .hookAfter { param ->
                val info = param.thisObject as ActivityInfo
                if (PackageNames.isWeChat(info.packageName)) return@hookAfter
                // FIXME: workaround for conversation page being part of main activity
                if (info.name == "com.tencent.mm.ui.LauncherUI") return@hookAfter
                applyPbgFlag(info)
            }

        "android.app.ActivityThread".toClass().asResolver()
            .firstMethod { name = "handleLaunchActivity" }
            .hookBefore { param ->
                val record = param.args[0]
                val infoField =
                    record.asResolver().firstField { name = "activityInfo" }
                val info = infoField.get() as ActivityInfo
                if (PackageNames.isWeChat(info.packageName)) return@hookBefore
                // FIXME: workaround for conversation page being part of main activity
                if (info.name == "com.tencent.mm.ui.LauncherUI") return@hookBefore
                applyPbgFlag(info)
            }
    }

    private fun applyPbgFlag(info: ActivityInfo) {
        val field = info.asResolver().firstField { name = "privateFlags" }
        var flags = field.get() as Int
        flags = flags or (PRIVATE_FLAG_ENABLE_ON_BACK_INVOKED_CALLBACK)
        flags = flags and (PRIVATE_FLAG_DISABLE_ON_BACK_INVOKED_CALLBACK).inv()
        field.set(flags)
    }
}
