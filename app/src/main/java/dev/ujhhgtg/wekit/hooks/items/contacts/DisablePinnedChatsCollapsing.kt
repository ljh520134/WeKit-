package dev.ujhhgtg.wekit.hooks.items.contacts

import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import dev.ujhhgtg.wekit.dexkit.abc.IResolvesDex
import dev.ujhhgtg.wekit.hooks.api.core.WeDatabaseApi
import dev.ujhhgtg.wekit.hooks.core.HookItem
import org.luckypray.dexkit.DexKitBridge

@HookItem(
    path = "联系人与群组/禁用置顶聊天折叠",
    desc = "隐藏「折叠置顶聊天」选项\n启用本功能后, 需重启应用 2 次以使更改完全生效"
)
object DisablePinnedChatsCollapsing : SwitchHookItem(), IResolvesDex {

    private val methodAddCollapseChatItem by dexMethod()
    private val methodIfShouldAddCollapseChatItem by dexMethod()

    override fun onEnable() {
        methodAddCollapseChatItem.hookBefore { param ->
            WeDatabaseApi.execStatement("DELETE FROM rconversation WHERE username = 'message_fold'")
            param.result = null
        }
        methodIfShouldAddCollapseChatItem.hookBefore { param ->
            WeDatabaseApi.execStatement("DELETE FROM rconversation WHERE username = 'message_fold'")
            param.result = false
        }
    }

    override fun resolveDex(dexKit: DexKitBridge): Map<String, String> {
        val descriptors = mutableMapOf<String, String>()

        methodAddCollapseChatItem.find(dexKit, descriptors) {
            searchPackages("com.tencent.mm.ui.conversation")
            matcher {
                usingEqStrings("MicroMsg.FolderHelper", "fold item exist")
            }
        }

        methodIfShouldAddCollapseChatItem.find(dexKit, descriptors) {
            searchPackages("com.tencent.mm.ui.conversation")
            matcher {
                usingEqStrings("MicroMsg.FolderHelper", "checkIfShowFoldItem, ifShow:")
                returnType(Boolean::class.java)
            }
        }

        return descriptors
    }
}
