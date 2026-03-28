package dev.ujhhgtg.wekit.hooks.items.debug

import android.content.Context
import android.content.Intent
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.ujhhgtg.wekit.constants.PackageNames
import dev.ujhhgtg.wekit.hooks.core.ClickableHookItem
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.ui.content.AlertDialogContent
import dev.ujhhgtg.wekit.ui.content.Button
import dev.ujhhgtg.wekit.ui.content.TextButton
import dev.ujhhgtg.wekit.ui.utils.showComposeDialog

@HookItem(path = "调试/跳转对话", desc = "打开指定微信 ID 的对话界面")
object OpenConversation : ClickableHookItem() {

    override val noSwitchWidget = true

    override fun onClick(context: Context) {
        showComposeDialog(context) {
            var wxId by remember { mutableStateOf("") }
            AlertDialogContent(title = { Text("跳转对话") },
                text = {
                    TextField(
                        value = wxId,
                        onValueChange = { wxId = it },
                        label = { Text("微信 ID") })
                },
                dismissButton = { TextButton(onDismiss) { Text("取消") } },
                confirmButton = {
                    Button(onClick = {
                        context.startActivity(Intent().apply {
                            setClassName(context.packageName, "${PackageNames.WECHAT}.plugin.profile.ui.ProfileSettingUI")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            putExtra("Contact_User", wxId)
                        })
                    }) { Text("朋友设置") }
                    Button(onClick = {
                        context.startActivity(Intent().apply {
                            setClassName(context.packageName, "${PackageNames.WECHAT}.ui.chatting.ChattingUI")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            putExtra("Chat_User", wxId)
                        })
                    }) { Text("对话") }
                })
        }
    }
}
