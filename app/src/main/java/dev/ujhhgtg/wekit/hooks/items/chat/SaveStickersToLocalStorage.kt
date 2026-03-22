package dev.ujhhgtg.wekit.hooks.items.chat

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.condition.type.Modifiers
import com.highcapable.kavaref.extension.toClass
import dev.ujhhgtg.nameof.nameof
import dev.ujhhgtg.wekit.dexkit.abc.IResolvesDex
import dev.ujhhgtg.wekit.dexkit.dsl.dexClass
import dev.ujhhgtg.wekit.hooks.api.core.model.MessageType
import dev.ujhhgtg.wekit.hooks.api.ui.WeChatMessageContextMenuApi
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import dev.ujhhgtg.wekit.utils.KnownPaths
import dev.ujhhgtg.wekit.utils.ModuleRes
import dev.ujhhgtg.wekit.utils.ToastUtils
import dev.ujhhgtg.wekit.utils.logging.WeLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.luckypray.dexkit.DexKitBridge
import kotlin.io.path.div
import kotlin.io.path.outputStream

@HookItem(path = "聊天/贴纸保存到本地", desc = "在贴纸消息菜单添加保存按钮, 允许将图片保存到本地")
object SaveStickersToLocalStorage : SwitchHookItem(), IResolvesDex,
    WeChatMessageContextMenuApi.IMenuItemsProvider {

    private val TAG = nameof(SaveStickersToLocalStorage)

    private val classEmojiFileEncryptMgr by dexClass()

    override fun onEnable() {
        WeChatMessageContextMenuApi.addProvider(this)
    }

    override fun onDisable() {
        WeChatMessageContextMenuApi.removeProvider(this)
    }

    override fun resolveDex(dexKit: DexKitBridge) {
        classEmojiFileEncryptMgr.find(dexKit) {
            matcher {
                methods {
                    add {
                        usingEqStrings(
                            "MicroMsg.emoji.EmojiFileEncryptMgr",
                            "decode emoji file failed. path is no exist :%s "
                        )
                    }
                }
            }
        }
    }

    override fun getMenuItems(): List<WeChatMessageContextMenuApi.MenuItem> {
        return listOf(
            @Suppress("UNCHECKED_CAST")
            WeChatMessageContextMenuApi.MenuItem(
                777001,
                "存本地",
                { ModuleRes.getDrawable("download_24px")!! },
                { msgInfo -> msgInfo.isType(MessageType.STICKER) }
            ) { _, _, msgInfo ->
                val md5 = msgInfo.imagePath
                val emojiInfo = StickersSync.getEmojiInfoByMd5(md5)
                val emojiFileEncryptMgr = classEmojiFileEncryptMgr.asResolver()
                    .firstMethod {
                        modifiers(Modifiers.STATIC)
                        parameterCount = 0
                    }
                    .invoke()!!
                var bytes = emojiFileEncryptMgr.asResolver()
                    .firstMethod {
                        parameters("com.tencent.mm.api.IEmojiInfo")
                        returnType = ByteArray::class
                    }
                    .invoke(emojiInfo) as ByteArray
                bytes = "com.tencent.mm.plugin.gif.MMWXGFJNI".toClass().asResolver()
                    .firstMethod {
                        name = "nativeWxamToGif"
                    }
                    .invoke(bytes) as ByteArray

                val fileName = "sticker_${System.currentTimeMillis()}.gif"
                CoroutineScope(Dispatchers.IO).launch {
                    runCatching {
                        (KnownPaths.downloads / fileName).outputStream().use { out ->
                            out.write(bytes)
                        }
                    }.onSuccess {
                        withContext(Dispatchers.Main) {
                            ToastUtils.showToast("已将贴纸保存到 /sdcard/Download/WeKit/$fileName")
                        }
                    }.onFailure { e ->
                        WeLogger.e(TAG, "failed to save sticker $fileName", e)
                        withContext(Dispatchers.Main) {
                            ToastUtils.showToast("贴纸保存失败! 查看日志以了解错误详情")
                        }
                    }
                }
            }
        )
    }
}
