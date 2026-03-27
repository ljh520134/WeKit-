package dev.ujhhgtg.wekit.hooks.items.payment

import android.app.Activity
import com.highcapable.kavaref.extension.toClass
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem

@HookItem(path = "红包与支付/允许领取私聊红包", desc = "允许打开私聊中自己发出的红包")
object AllowPrivateChatReceiveOutgoingRedPackets : SwitchHookItem() {

    override fun onEnable() {
         listOf(
             "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyPrepareUI",
             "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyNewPrepareUI"
         ).forEach {
             it.toClass().hookBeforeOnCreate { param ->
                 val activity = param.thisObject as Activity
                 activity.intent.putExtra("key_type", 1)
             }
         }
    }
}
