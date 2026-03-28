package dev.ujhhgtg.wekit.hooks.items.system

import dev.ujhhgtg.wekit.dexkit.abc.IResolvesDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import org.luckypray.dexkit.DexKitBridge

@HookItem(path = "系统与隐私/禁用存储空间不足检测", desc = "「隐藏应用列表」等隐藏 Root 模块有时会使应用获取到的可用空间不正确, 而微信在可用空间不足时会强制要求清理空间才可继续使用, 本功能移除了该限制")
object DisableLowAvailableStorageDetection : SwitchHookItem(), IResolvesDex {

    private val methodCheckSpaceAndDisplayDialog by dexMethod()

    override fun onEnable() {
        methodCheckSpaceAndDisplayDialog.hookBefore { param ->
            param.result = null
        }
    }

    override fun resolveDex(dexKit: DexKitBridge) {
        methodCheckSpaceAndDisplayDialog.find(dexKit) {
            matcher {
                usingEqStrings("MicroMsg.StorageDisableAlertUI", "getWechatTotalSize timeout, read from mmkv")
            }
        }
    }
}
