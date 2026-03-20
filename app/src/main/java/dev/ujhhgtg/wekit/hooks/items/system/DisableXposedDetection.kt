package dev.ujhhgtg.wekit.hooks.items.system

import dev.ujhhgtg.nameof.nameof
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import dev.ujhhgtg.wekit.dexkit.abc.IResolvesDex
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.utils.HostInfo
import org.luckypray.dexkit.DexKitBridge

@HookItem(path = "系统与隐私/禁止应用检测 Xposed", desc = "防止应用检测 Xposed 框架是否存在")
object DisableXposedDetection : SwitchHookItem(), IResolvesDex {

    private val methodCheckStackTraceElements by dexMethod()

    override fun onEnable() {
        // google play version doesn't have tinker
        if (HostInfo.isHostGooglePlay) return

        methodCheckStackTraceElements.hookBefore { param ->
            param.result = false
        }
    }

    override fun resolveDex(dexKit: DexKitBridge): Map<String, String> {
        // placeholder method
        if (HostInfo.isHostGooglePlay) return mapOf(
            "${nameof(DisableXposedDetection)}:${nameof(methodCheckStackTraceElements)}" to
            "Lcom/tencent/mm/ui/LauncherUI;->()Lcom/tencent/mm/ui/LauncherUI;"
        )

        val descriptors = mutableMapOf<String, String>()

        methodCheckStackTraceElements.find(dexKit, descriptors) {
            searchPackages("com.tencent.mm.app")
            matcher {
                usingEqStrings(
                    "de.robv.android.xposed.XposedBridge",
                    "com.zte.heartyservice.SCC.FrameworkBridge"
                )
            }
        }

        return descriptors
    }
}
