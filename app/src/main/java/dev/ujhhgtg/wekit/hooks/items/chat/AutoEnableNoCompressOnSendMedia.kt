package dev.ujhhgtg.wekit.hooks.items.chat

import android.app.Activity
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.extension.toClass
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import dev.ujhhgtg.wekit.hooks.core.HookItem

@HookItem(path = "聊天/自动启用发送原图", desc = "发送媒体时自动勾选发送原图选项")
object AutoEnableNoCompressOnSendMedia : SwitchHookItem() {

    private val HOOKED_CLASS_NAMES = listOf(
        "com.tencent.mm.plugin.gallery.ui.AlbumPreviewUI",
        "com.tencent.mm.plugin.gallery.ui.ImagePreviewUI"
    )

    override fun onEnable() {
        for (clsName in HOOKED_CLASS_NAMES) {
            clsName.toClass().asResolver().firstMethod { name = "onCreate" }.hookBefore { param ->
                val activity = param.thisObject as Activity
                activity.intent.putExtra("send_raw_img", true)
            }
        }
    }
}
