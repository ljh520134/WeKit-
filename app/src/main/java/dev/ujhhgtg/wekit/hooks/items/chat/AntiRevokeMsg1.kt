package dev.ujhhgtg.wekit.hooks.items.chat

import dev.ujhhgtg.wekit.dexkit.abc.IResolvesDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import org.luckypray.dexkit.DexKitBridge

@HookItem(path = "聊天/阻止消息撤回 1", desc = "无撤回提示")
object AntiRevokeMsg1 : SwitchHookItem(), IResolvesDex {
    private val methodRevokeMsg by dexMethod()

    override fun resolveDex(dexKit: DexKitBridge) {
        methodRevokeMsg.find(dexKit) {
            matcher {
                usingEqStrings("doRevokeMsg xmlSrvMsgId=%d talker=%s isGet=%s")
            }
        }
    }

    override fun onEnable() {
        methodRevokeMsg.hookBefore { param ->
            param.result = null
        }
    }
}
