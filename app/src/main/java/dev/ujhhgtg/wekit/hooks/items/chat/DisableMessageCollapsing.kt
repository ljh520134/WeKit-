package dev.ujhhgtg.wekit.hooks.items.chat

import dev.ujhhgtg.wekit.dexkit.abc.IResolvesDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import org.luckypray.dexkit.DexKitBridge

@HookItem(path = "聊天/禁用消息折叠", desc = "阻止聊天消息被折叠")
object DisableMessageCollapsing : SwitchHookItem(), IResolvesDex {

    private val methodFoldMsg by dexMethod()

    override fun resolveDex(dexKit: DexKitBridge) {
        methodFoldMsg.find(dexKit) {
            matcher {
                usingStrings(".msgsource.sec_msg_node.clip-len")
                paramTypes(
                    Int::class.java,
                    CharSequence::class.java,
                    null,
                    Boolean::class.javaPrimitiveType,
                    null,
                    null
                )
            }
        }
    }

    override fun onEnable() {
        methodFoldMsg.hookBefore { param ->
            param.result = null
        }
    }
}
