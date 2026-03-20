package dev.ujhhgtg.wekit.hooks.items.chat

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.extension.toClass
import dev.ujhhgtg.nameof.nameof
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.ujhhgtg.wekit.dexkit.dsl.dexClass
import dev.ujhhgtg.wekit.dexkit.dsl.dexMethod
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import dev.ujhhgtg.wekit.dexkit.abc.IResolvesDex
import dev.ujhhgtg.wekit.hooks.api.core.WeServiceApi
import dev.ujhhgtg.wekit.hooks.api.core.model.MessageType
import dev.ujhhgtg.wekit.hooks.api.ui.WeChatMessageContextMenuApi
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.utils.KnownPaths
import dev.ujhhgtg.wekit.utils.ModuleRes
import dev.ujhhgtg.wekit.utils.ToastUtils
import dev.ujhhgtg.wekit.utils.logging.WeLogger
import org.luckypray.dexkit.DexKitBridge
import xyz.xxin.silkdecoder.SilkDecoder
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.nameWithoutExtension

@HookItem(
    path = "聊天/语音保存到本地",
    desc = "在语音消息菜单添加保存按钮, 允许将语音文件保存到本地"
)
object SaveVoicesToLocalStorage : SwitchHookItem(), IResolvesDex,
    WeChatMessageContextMenuApi.IMenuItemsProvider {

    private val TAG = nameof(SaveVoicesToLocalStorage)

    private val classVoiceLogic by dexClass()
    private val methodGetAmrFullPath by dexMethod()

    private lateinit var methodStreamSilkDecInit: Method
    private lateinit var methodStreamSilkDecUnInit: Method
    private lateinit var methodStreamSilkDoDec: Method

    override fun onEnable() {
        val clazz = "com.tencent.mm.modelvoice.MediaRecorder".toClass()
        methodStreamSilkDecInit = clazz.asResolver()
            .firstMethod { name = "StreamSilkDecInit" }
            .self
        methodStreamSilkDecUnInit = clazz.asResolver()
            .firstMethod { name = "StreamSilkDecUnInit" }
            .self
        methodStreamSilkDoDec = clazz.asResolver()
            .firstMethod { name = "StreamSilkDoDec" }
            .self

        WeChatMessageContextMenuApi.addProvider(this)
    }

    override fun onDisable() {
        WeChatMessageContextMenuApi.removeProvider(this)
    }

    override fun resolveDex(dexKit: DexKitBridge): Map<String, String> {
        val descriptors = mutableMapOf<String, String>()

        classVoiceLogic.find(dexKit, descriptors) {
            matcher {
                usingEqStrings("MicroMsg.VoiceLogic", "startRecord insert voicestg success")
            }
        }

        methodGetAmrFullPath.find(dexKit, descriptors) {
            matcher {
                usingEqStrings("getAmrFullPath cost: ")
            }
        }

        return descriptors
    }

    override fun getMenuItems(): List<WeChatMessageContextMenuApi.MenuItem> {
        return listOf(
            WeChatMessageContextMenuApi.MenuItem(
                777003,
                "存本地",
                { ModuleRes.getDrawable("download_24px")!! },
                { msgInfo -> msgInfo.isType(MessageType.VOICE) }
            ) { _, _, msgInfo ->
                CoroutineScope(Dispatchers.IO).launch {
                    val encPath = msgInfo.imagePath
                    var service: Any? = null
                    if (!Modifier.isStatic(methodGetAmrFullPath.method.modifiers)) {
                        service =
                            WeServiceApi.getServiceByClass(methodGetAmrFullPath.method.declaringClass)
                    }
                    val amrPath =
                        Path(methodGetAmrFullPath.method.invoke(service, null, encPath, true) as String)
                    val mp3Name = amrPath.nameWithoutExtension + ".mp3"
                    val mp3Path = KnownPaths.downloads / mp3Name

                    runCatching {
                        SilkDecoder.decodeToMp3(amrPath.toString(), mp3Path.toString())
                    }.onSuccess {
                        withContext(Dispatchers.Main) {
                            ToastUtils.showToast("已将语音保存到 /sdcard/Download/WeKit/$mp3Name")
                        }
                    }.onFailure { e ->
                        WeLogger.e(TAG, "failed to save voice $mp3Name", e)
                        withContext(Dispatchers.Main) {
                            ToastUtils.showToast("语音保存失败! 查看日志以了解错误详情")
                        }
                    }
                }
            }
        )
    }
}
