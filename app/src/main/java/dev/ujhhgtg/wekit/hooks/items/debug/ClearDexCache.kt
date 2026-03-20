package dev.ujhhgtg.wekit.hooks.items.debug

import android.content.Context
import androidx.compose.material3.Text
import dev.ujhhgtg.wekit.hooks.core.ClickableHookItem
import dev.ujhhgtg.wekit.dexkit.cache.DexCacheManager
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.ui.content.AlertDialogContent
import dev.ujhhgtg.wekit.ui.content.TextButton
import dev.ujhhgtg.wekit.ui.utils.showComposeDialog

@HookItem(path = "调试/清除适配信息", desc = "点击清除适配信息")
object ClearDexCache : ClickableHookItem() {
    override fun onClick(context: Context) {
        showComposeDialog(context) {
            AlertDialogContent(
                title = { Text("清除适配信息") },
                text = {
                    Text(
                        "这将删除所有的 DEX 适配信息，宿主重启后需要重新适配。\n" +
                                "确定清除吗？"
                    )
                },
                dismissButton = { TextButton(onClick = dismiss) { Text("取消") } },
                confirmButton = {
                    TextButton(onClick = {
                        DexCacheManager.clearAllCache()
                        dismiss()
                    }) { Text("确定") }
                })
        }
    }

    override val noSwitchWidget: Boolean
        get() = true
}
