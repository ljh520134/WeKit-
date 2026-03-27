package dev.ujhhgtg.wekit.hooks.items.chat

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Home
import com.composables.icons.materialsymbols.outlined.Send_time_extension
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.extension.toClass
import com.mpatric.mp3agic.Mp3File
import dev.ujhhgtg.wekit.activity.StubFragmentActivity
import dev.ujhhgtg.wekit.hooks.api.core.WeMessageApi
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import dev.ujhhgtg.wekit.ui.content.AlertDialogContent
import dev.ujhhgtg.wekit.ui.content.Button
import dev.ujhhgtg.wekit.ui.content.FilledIconButton
import dev.ujhhgtg.wekit.ui.content.TextButton
import dev.ujhhgtg.wekit.ui.utils.AppTheme
import dev.ujhhgtg.wekit.ui.utils.XposedLifecycleOwner
import dev.ujhhgtg.wekit.ui.utils.findViewByChildIndexes
import dev.ujhhgtg.wekit.ui.utils.findViewWhich
import dev.ujhhgtg.wekit.ui.utils.findViewsWhich
import dev.ujhhgtg.wekit.ui.utils.setLifecycleOwner
import dev.ujhhgtg.wekit.ui.utils.showComposeDialog
import dev.ujhhgtg.wekit.utils.KnownPaths
import dev.ujhhgtg.wekit.utils.SilkCodec
import dev.ujhhgtg.wekit.utils.coerceToInt
import dev.ujhhgtg.wekit.utils.showToast
import dev.ujhhgtg.wekit.utils.showToastSuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.reflect.Method
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.outputStream
import android.widget.Button as AndroidButton

@HookItem(
    path = "聊天/聊天输入栏增强",
    desc = "为聊天输入栏添加更多功能\n1. 在聊天界面长按「发送」或「加号菜单」按钮打开菜单\n2. 长按「语音」按钮发送自定义语音文件 (SILK/AMR 或 MP3)"
)
object ChatInputBarEnhancements : SwitchHookItem() {

    var currentConv: String? = null
    private lateinit var methodGetLastText: Method

    override fun onEnable() {
        "com.tencent.mm.pluginsdk.ui.chat.ChatFooter".toClass().asResolver().apply {
            methodGetLastText = firstMethod { name = "getLastText" }.self

            firstConstructor {
                parameters(Context::class, AttributeSet::class, Int::class)
            }
                .hookAfter { param ->
                    val chatFooter = param.thisObject as FrameLayout
                    val searchedView = chatFooter.findViewByChildIndexes<View>(0)!!
                    val imgButtons = searchedView.findViewsWhich<ImageButton> { view ->
                        view.javaClass.simpleName == "WeImageButton"
                    }
                    val voiceButton = imgButtons.first()
                    val menuButton = imgButtons.last()
                    val sendButton = searchedView.findViewWhich<AndroidButton> { view ->
                        view.javaClass.name == "android.widget.Button" && run {
                            val text = (view as AndroidButton).text?.toString()?.trim() ?: ""
                            text == "发送" || text.equals("send", ignoreCase = true)
                        }
                    }!!

                    voiceButton.setOnLongClickListener { view ->
                        val context = view.context
                        val currentConv = currentConv
                        if (currentConv.isNullOrBlank()) {
                            showToast("当前聊天对象获取失败!")
                            return@setOnLongClickListener true
                        }

                        StubFragmentActivity.launch(context) {
                            val importLauncher = registerForActivityResult(
                                ActivityResultContracts.OpenDocument()
                            ) { uri ->
                                if (uri == null) {
                                    finish()
                                    return@registerForActivityResult
                                }

                                lifecycleScope.launch(Dispatchers.IO) {
                                    val tempPath = KnownPaths.moduleCache / "voice.tmp"
                                    contentResolver.openInputStream(uri)!!.use { fis ->
                                        tempPath.outputStream().use { fos ->
                                            fis.copyTo(fos)
                                        }
                                    }
                                    val mimeType = contentResolver.getType(uri) ?: return@launch
                                    val isSilk = mimeType == "audio/amr"
                                    showToastSuspend("语音文件准备完成")
                                    val durationMs = if (!isSilk) {
                                        runCatching { mp3DurationMs(tempPath.absolutePathString()) }.getOrDefault(0L)
                                    } else 0L

                                    withContext(Dispatchers.Main) {
                                        finish()
                                        showComposeDialog(context) {
                                            var durationInput by remember { mutableStateOf(durationMs.toString()) }
                                            AlertDialogContent(
                                                title = { Text("发送语音文件") },
                                                text = {
                                                    TextField(
                                                        value = durationInput,
                                                        onValueChange = { durationInput = it.filter { c -> c.isDigit() } },
                                                        label = { Text("语音时长 (毫秒)") })
                                                },
                                                dismissButton = { TextButton(onDismiss) { Text("取消") } },
                                                confirmButton = {
                                                    Button(onClick = {
                                                        val durationMs = durationInput.toLongOrNull()
                                                        if (durationMs == null) {
                                                            showToast("时长格式不正确!")
                                                            return@Button
                                                        }

                                                        var success = false
                                                        if (isSilk) {
                                                            showToast("正在发送 SILK...")
                                                            success = WeMessageApi.sendVoice(
                                                                currentConv,
                                                                tempPath.absolutePathString(),
                                                                durationMs.coerceToInt()
                                                            )
                                                        } else {
                                                            showToast("正在将 MP3 转换为 SILK...")
                                                            val tempSilkPath = KnownPaths.moduleCache / "voice.2.tmp"
                                                            val convSuccess = SilkCodec.mp3ToSilk(
                                                                tempPath.absolutePathString(),
                                                                tempSilkPath.absolutePathString())
                                                            if (convSuccess) {
                                                                showToast("转换成功! 正在发送...")
                                                                success = WeMessageApi.sendVoice(
                                                                    currentConv,
                                                                    tempSilkPath.absolutePathString(),
                                                                    durationMs.coerceToInt()
                                                                )
                                                            }
                                                            else {
                                                                showToast("转换失败! 查看日志以了解错误详情")
                                                            }
                                                            tempSilkPath.deleteIfExists()
                                                            return@Button
                                                        }
                                                        showToast("语音发送${if (success) "成功" else "失败!"}")
                                                        tempPath.deleteIfExists()
                                                        onDismiss()
                                                    }) { Text("确定") }
                                                })
                                        }
                                    }
                                }
                            }
                            // android couldn't distinguish AMR-extension SILK files, so we just use amr here
                            importLauncher.launch(arrayOf("audio/amr", "audio/mpeg"))
                        }

                        return@setOnLongClickListener true
                    }

                    listOf(menuButton, sendButton).forEach {
                        it.setOnLongClickListener { view ->
                            val context = view.context
                            val lifecycleOwner = XposedLifecycleOwner.create()

                            chatFooter.addView(ComposeView(context).apply {
                                setLifecycleOwner(lifecycleOwner)

                                setContent {
                                    AppTheme {
                                        var shouldShow by remember { mutableStateOf(true) }
                                        if (shouldShow) {
                                            ModalBottomSheet(onDismissRequest = { shouldShow = false }) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 16.dp, vertical = 24.dp),
                                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                                    verticalAlignment = Alignment.Top,
                                                ) {
                                                    ActionItem(
                                                        icon = MaterialSymbols.Outlined.Send_time_extension,
                                                        label = "发送卡片消息",
                                                        onClick = {
                                                            val currentConv = currentConv
                                                            val content = methodGetLastText.invoke(chatFooter) as String
                                                            if (currentConv.isNullOrBlank()) {
                                                                showToast("当前聊天对象获取失败!")
                                                                return@ActionItem
                                                            }

                                                            if (content.isEmpty()) {
                                                                showToast("输入内容为空!")
                                                                return@ActionItem
                                                            }

                                                            val isSuccess = WeMessageApi.sendXmlAppMsg(currentConv, content)
                                                            if (!isSuccess) {
                                                                showToast("发送卡片消息失败, 请检查格式")
                                                                return@ActionItem
                                                            }

                                                            chatFooter.findViewWhich<EditText> { view is EditText }?.setText("")
                                                            shouldShow = false
                                                        },
                                                    )

                                                    ActionItem(
                                                        icon = MaterialSymbols.Outlined.Home,
                                                        label = "测试",
                                                        onClick = { },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            })
                            return@setOnLongClickListener true
                        }
                    }
                }

            firstMethod {
                name = "setUserName"
            }.hookAfter { param ->
                val conv = param.args[0] as? String
                if (!conv.isNullOrEmpty()) {
                    currentConv = conv
                }
            }
        }
    }
}

private fun mp3DurationMs(path: String): Long {
    val mp3 = Mp3File(path)
    return mp3.lengthInMilliseconds
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
        ) {
            Icon(icon, contentDescription = label)
        }
        Text(
            text = label,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
