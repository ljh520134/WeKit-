package dev.ujhhgtg.wekit.hooks.items.chat

import dev.ujhhgtg.wekit.dexkit.dsl.dexClass
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import dev.ujhhgtg.wekit.dexkit.abc.IResolvesDex
import dev.ujhhgtg.wekit.hooks.core.HookItem
import org.luckypray.dexkit.DexKitBridge

@HookItem(path = "聊天/禁止上传正在输入状态", desc = "禁止应用上传「对方正在输入」状态")
object DisableTypingStatusUploading : SwitchHookItem(), IResolvesDex {

    private val classMmTypingSendReq by dexClass()

    override fun onEnable() {
        classMmTypingSendReq.asResolver().firstMethod { name = "doScene" }
            .hookBefore { param ->
                param.result = -1
            }
    }

    override fun resolveDex(dexKit: DexKitBridge): Map<String, String> {
        val descriptors = mutableMapOf<String, String>()

        classMmTypingSendReq.find(dexKit, descriptors) {
            searchPackages("com.tencent.mm.modelsimple")
            matcher {
                usingEqStrings(
                    "null cannot be cast to non-null type com.tencent.mm.protocal.MMTypingSend.Req",
                    "autoAuth"
                )
            }
        }

        return descriptors
    }
}
