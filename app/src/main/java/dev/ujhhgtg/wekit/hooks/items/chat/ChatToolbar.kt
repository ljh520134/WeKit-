package dev.ujhhgtg.wekit.hooks.items.chat

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.outlined.VideoChat
import androidx.compose.material.icons.outlined.VoiceChat
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.view.children
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.extension.createInstance
import com.highcapable.kavaref.extension.toClass
import dev.ujhhgtg.nameof.nameof
import dev.ujhhgtg.wekit.core.dsl.dexMethod
import dev.ujhhgtg.wekit.core.model.SwitchHookItem
import dev.ujhhgtg.wekit.dexkit.abc.IResolvesDex
import dev.ujhhgtg.wekit.hooks.utils.annotation.HookItem
import dev.ujhhgtg.wekit.ui.utils.AppTheme
import dev.ujhhgtg.wekit.ui.utils.LifecycleOwnerProvider
import dev.ujhhgtg.wekit.ui.utils.findViewByChildIndexes
import dev.ujhhgtg.wekit.ui.utils.findViewWhich
import dev.ujhhgtg.wekit.ui.utils.iterable
import dev.ujhhgtg.wekit.ui.utils.setLifecycleOwner
import dev.ujhhgtg.wekit.utils.logging.WeLogger
import dev.ujhhgtg.wekit.utils.now
import kotlinx.coroutines.flow.MutableStateFlow
import org.luckypray.dexkit.DexKitBridge
import kotlin.time.Duration.Companion.seconds

@SuppressLint("StaticFieldLeak")
@HookItem(path = "聊天/聊天工具栏", desc = "在输入框上方添加工具栏")
object ChatToolbar : SwitchHookItem(), IResolvesDex {

    private val TAG = nameof(ChatToolbar)

    private val methodAppPanelInitAppGrid by dexMethod()
    private val methodAppPanelOnMeasure by dexMethod()

    private var lastToolListUpdateTime = now()

    private lateinit var appPanel: LinearLayout

    data class MenuItem(
        val name: String,
        val onClickListener: AdapterView.OnItemClickListener,
        val onLongClickListener: AdapterView.OnItemLongClickListener,
        val gridView: GridView,
        val itemView: View,
        val indexInGrid: Int
    )

    private val toolsState = MutableStateFlow<List<Pair<String, MenuItem>>>(emptyList())

    override fun onEnable() {
        methodAppPanelInitAppGrid.apply {
            hookBefore { param ->
                appPanel = param.args[0] as LinearLayout

                val measurer = methodAppPanelOnMeasure.method.declaringClass
                    .createInstance(appPanel)
                // onMeasure() would be called again with actual width & height anyways,
                // so as long as those numbers aren't too small, it's probably fine
                // currently no unwanted side effects are observed, except that the pager's
                // indicator disappears
                methodAppPanelOnMeasure.method.invoke(measurer, 1440, 1200)
            }

            hookAfter { param ->
                val now = now()
                if (now - lastToolListUpdateTime < 2.seconds) return@hookAfter

                val tools = mutableListOf<Pair<String, MenuItem>>()

                val appPanel = param.args[0] as LinearLayout
                val grids = appPanel.findViewByChildIndexes<ViewGroup>(0, 0, 0)!!
                    .children.map { view -> view as GridView }
                grids.forEach { grid ->
                    val onClickListener = grid.asResolver()
                        .firstField {
                            type = AdapterView.OnItemClickListener::class
                        }.get()!! as AdapterView.OnItemClickListener
                    val onLongClickListener = grid.asResolver()
                        .firstField {
                            type = AdapterView.OnItemLongClickListener::class
                        }.get()!! as AdapterView.OnItemLongClickListener
                    val listAdapter = grid.adapter
                    listAdapter.iterable(grid).forEachIndexed { index, itemView ->
                        val name = (itemView.tag.asResolver()
                            .firstField { type = TextView::class }
                            .get()!! as TextView).text.toString()
                        tools.add(
                            name to MenuItem(
                                name,
                                onClickListener,
                                onLongClickListener,
                                grid,
                                itemView,
                                index
                            )
                        )
                    }
                }

                WeLogger.d(TAG, "populated tool list with ${tools.size} items")
                toolsState.value = tools

                // rate limit this since this method is called REALLY frequently
                lastToolListUpdateTime = now()
            }
        }

        "com.tencent.mm.pluginsdk.ui.chat.ChatFooter".toClass().asResolver()
            .firstConstructor {
                parameters(Context::class, AttributeSet::class, Int::class)
            }
            .self
            .hookAfter { param ->
                val lifecycleOwner = LifecycleOwnerProvider.lifecycleOwner

                val chatFooter = param.thisObject as FrameLayout
                chatFooter.setLifecycleOwner(lifecycleOwner)
                val linearLayout = chatFooter.findViewByChildIndexes<LinearLayout>(0, 1)!!
                linearLayout.setLifecycleOwner(lifecycleOwner)
                if (linearLayout.findViewWhich<View> { it is ComposeView } != null) return@hookAfter

                val activity = chatFooter.context as Activity
                activity.window.decorView.setLifecycleOwner(lifecycleOwner)

                linearLayout.addView(ComposeView(activity).apply {
                    setLifecycleOwner(lifecycleOwner)

                    setContent {
                        AppTheme {
                            val tools by toolsState.collectAsStateWithLifecycle()

                            val visibleItems = remember(tools) {
                                tools.filter { NAME_TO_ICON_MAP.containsKey(it.first) }
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                            ) {
                                // workaroung for weird WeChat bug where 'Album' doesn't display, so I have to render it manually

                                item {
                                    FeatureChip("相册", Icons.Default.PhotoLibrary) {
                                        tools[0].second.let {
                                            it.onClickListener.onItemClick(
                                                it.gridView,
                                                it.itemView,
                                                0,
                                                0
                                            )
                                        }
                                    }
                                }

                                item {
                                    FeatureChip("系统拍摄", Icons.Default.Camera) {
                                        tools[0].second.onLongClickListener.onItemLongClick(
                                            null,
                                            null,
                                            0,
                                            0
                                        )
                                    }
                                }

                                items(visibleItems, key = { it.first }) {
                                    val icon = NAME_TO_ICON_MAP[it.first]!!
                                    FeatureChip(it.first, icon) {
                                        it.second.onClickListener.onItemClick(
                                            it.second.gridView,
                                            it.second.itemView,
                                            it.second.indexInGrid + 1,
                                            0
                                        )
                                    }
                                }
                            }
                        }
                    }
                }, 0)
            }
    }

    private val NAME_TO_ICON_MAP = mapOf(
        "拍摄" to Icons.Default.Camera,
        "视频通话" to Icons.Outlined.VideoChat, // because Icons.Default.VoiceChat is outlined
        "语音通话" to Icons.Outlined.VoiceChat,
        "位置" to Icons.Default.LocationOn,
        "红包" to Icons.Default.Mail,
        "礼物" to Icons.Default.Redeem,
        "转账" to Icons.Default.AttachMoney,
        "语音输入" to Icons.Default.Mic,
        "收藏" to Icons.Default.Favorite,
        "接龙" to Icons.Default.FormatListNumbered,
        "文件" to Icons.Default.AttachFile,
        "名片" to Icons.Default.AccountBox,
        "音乐" to Icons.Default.MusicNote
    )

    override fun resolveDex(dexKit: DexKitBridge): Map<String, String> {
        val descriptors = mutableMapOf<String, String>()

        methodAppPanelInitAppGrid.find(dexKit, descriptors) {
            matcher {
                declaredClass = "com.tencent.mm.pluginsdk.ui.chat.AppPanel"
                usingEqStrings("MicroMsg.AppPanel", "initAppGrid()")
            }
        }

        methodAppPanelOnMeasure.find(dexKit, descriptors) {
            searchPackages("com.tencent.mm.pluginsdk.ui.chat")
            matcher {
                usingEqStrings(
                    "MicroMsg.AppPanel",
                    "onMeasure width: %d, heigth:%d, isMeasured:%b, gridWidth:%d, gridHeight:%d"
                )
            }
        }

        return descriptors
    }
}

@Composable
private fun FeatureChip(text: String, icon: ImageVector, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                Modifier.size(AssistChipDefaults.IconSize)
            )
        }
    )
}
