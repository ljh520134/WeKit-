package dev.ujhhgtg.wekit.hooks.items.system

import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import dev.ujhhgtg.wekit.dexkit.abc.IResolvesDex
import dev.ujhhgtg.wekit.hooks.core.HookItem
import org.luckypray.dexkit.DexKitBridge

@HookItem(path = "系统与隐私/禁用 WebView 安全警告", desc = "禁用 WebView 相关的安全警告提示")
object DisableWebViewSafetyWarnings : SwitchHookItem(), IResolvesDex {
    private val methodGetIsInterceptEnabled by dexMethod()
    private val methodGetIsUrlSafe by dexMethod()

    override fun onEnable() {
        methodGetIsInterceptEnabled.hookBefore { param ->
            param.result = false
        }

        methodGetIsUrlSafe.hookBefore { param ->
            param.result = true
        }
    }

    override fun resolveDex(dexKit: DexKitBridge): Map<String, String> {
        val descriptors = mutableMapOf<String, String>()

        methodGetIsInterceptEnabled.find(dexKit, descriptors) {
            matcher {
                usingEqStrings(
                    "MicroMsg.WebViewHighRiskAdH5Interceptor",
                    "isInterceptEnabled, expt="
                )
            }
        }

        methodGetIsUrlSafe.find(dexKit, descriptors) {
            matcher {
                declaredClass(methodGetIsInterceptEnabled.method.declaringClass)
                usingEqStrings("http", "https")
            }
        }

        return descriptors
    }
}
