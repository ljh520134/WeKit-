package dev.ujhhgtg.wekit.hooks.items.moments

import android.content.ContentValues
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.highcapable.kavaref.extension.toClass
import dev.ujhhgtg.nameof.nameof
import dev.ujhhgtg.wekit.R
import dev.ujhhgtg.wekit.hooks.api.core.WeDatabaseApi
import dev.ujhhgtg.wekit.hooks.api.core.WeDatabaseListenerApi
import dev.ujhhgtg.wekit.hooks.api.ui.WeMomentsContextMenuApi
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import dev.ujhhgtg.wekit.ui.content.AlertDialogContent
import dev.ujhhgtg.wekit.ui.content.Button
import dev.ujhhgtg.wekit.ui.content.TextButton
import dev.ujhhgtg.wekit.ui.utils.showComposeDialog
import dev.ujhhgtg.wekit.utils.ModuleRes
import dev.ujhhgtg.wekit.utils.showToast
import dev.ujhhgtg.wekit.utils.WeLogger
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.LinkedList

@HookItem(
    path = "朋友圈/朋友圈伪集赞",
    desc = "自定义朋友圈点赞用户列表"
)
object FakeMomentsLikes : SwitchHookItem(), WeMomentsContextMenuApi.IMenuItemsProvider,
    WeDatabaseListenerApi.IUpdateListener {

    private val TAG = nameof(FakeMomentsLikes)
    private const val TBL_SNS_INFO = "SnsInfo"

    // 存储每个朋友圈动态的伪点赞用户配置 (snsId -> Set<微信id>)
    private val fakeLikeWxIds = mutableMapOf<Long, Set<String>>()

    private var snsObjectClass: Class<*>? = null
    private var parseFromMethod: Method? = null
    private var toByteArrayMethod: Method? = null
    private var likeUserListField: Field? = null
    private var likeUserListCountField: Field? = null
    private var likeCountField: Field? = null
    private var likeFlagField: Field? = null
    private var snsUserProtobufClass: Class<*>? = null

    override fun onEnable() {
        initReflection()
        WeMomentsContextMenuApi.addProvider(this)
        WeDatabaseListenerApi.addListener(this)
    }

    override fun onDisable() {
        WeMomentsContextMenuApi.removeProvider(this)
        WeDatabaseListenerApi.removeListener(this)
    }

    override fun getMenuItems(): List<WeMomentsContextMenuApi.MenuItem> {
        return listOf(
            WeMomentsContextMenuApi.MenuItem(
                777006,
                "伪点赞",
                { ModuleRes.getDrawable(R.drawable.star_24px)!! },
                { _, _ -> true }) { moments ->
                val allFriends = WeDatabaseApi.getContacts()

                val displayItems = allFriends.map { contact ->
                    buildString {
                        if (contact.remarkName.isNotBlank()) {
                            append(contact.remarkName)
                            if (contact.nickname.isNotBlank()) append(" (${contact.nickname})")
                        } else if (contact.nickname.isNotBlank()) {
                            append(contact.nickname)
                        } else {
                            append(contact.wxId)
                        }
                    }
                }

                val snsInfo = moments.snsInfo
                val snsId = moments.snsInfo!!.javaClass.superclass!!
                    .getDeclaredField("field_snsId")
                    .apply { isAccessible = true }
                    .get(snsInfo) as Long

                val currentSelected = fakeLikeWxIds[snsId] ?: emptySet()
                val initialSelection = allFriends.mapIndexedNotNull { index, contact ->
                    if (contact.wxId in currentSelected) index else null
                }.toSet()

                showComposeDialog(moments.activity) {
                    var selectedIndices by remember { mutableStateOf(initialSelection) }

                    AlertDialogContent(
                        title = { Text("选择伪点赞用户") },
                        text = {
                            LazyColumn {
                                itemsIndexed(displayItems) { index, label ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedIndices = if (index in selectedIndices)
                                                    selectedIndices - index
                                                else
                                                    selectedIndices + index
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = index in selectedIndices,
                                            onCheckedChange = { checked ->
                                                selectedIndices = if (checked)
                                                    selectedIndices + index
                                                else
                                                    selectedIndices - index
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = label)
                                    }
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { onDismiss() }) {
                                Text("取消")
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                val selectedWxids = selectedIndices.map { allFriends[it].wxId }.toSet()
                                if (selectedWxids.isEmpty()) {
                                    fakeLikeWxIds.remove(snsId)
                                    showToast("已清除朋友圈的伪点赞配置")
                                } else {
                                    fakeLikeWxIds[snsId] = selectedWxids
                                    showToast("已设置朋友圈的 ${selectedWxids.size} 个伪点赞")
                                }
                                onDismiss()
                            }) {
                                Text("确定")
                            }
                        }
                    )
                }
            }
        )
    }

    override fun onUpdate(table: String, values: ContentValues): Boolean {
        try {
            injectFakeLikes(table, values)
        } catch (e: Throwable) {
            WeLogger.e(TAG, "处理数据库更新异常", e)
        }

        return false // 返回 false 表示继续原有流程
    }

    private fun initReflection() {
        try {
            snsObjectClass = "com.tencent.mm.protocal.protobuf.SnsObject".toClass()

            snsObjectClass?.let { clazz ->
                parseFromMethod = clazz.getMethod("parseFrom", ByteArray::class.java)
                toByteArrayMethod = clazz.getMethod("toByteArray")

                listOf(
                    "LikeUserList",
                    "LikeUserListCount",
                    "LikeCount",
                    "LikeFlag"
                ).forEach { name ->
                    clazz.getDeclaredField(name).also { field ->
                        field.isAccessible = true
                        when (name) {
                            "LikeUserList" -> likeUserListField = field
                            "LikeUserListCount" -> likeUserListCountField = field
                            "LikeCount" -> likeCountField = field
                            "LikeFlag" -> likeFlagField = field
                        }
                    }
                }
            }

            snsUserProtobufClass = "com.tencent.mm.plugin.sns.ui.SnsCommentFooter".toClass()
                .getMethod("getCommentInfo").returnType
        } catch (e: Exception) {
            WeLogger.e(TAG, "反射初始化失败", e)
        }
    }

    private fun injectFakeLikes(tableName: String, values: ContentValues) = runCatching {
        if (tableName != TBL_SNS_INFO) return@runCatching
        val snsId = values.get("snsId") as? Long ?: return@runCatching
        val fakeWxIds = fakeLikeWxIds[snsId] ?: emptySet()
        if (fakeWxIds.isEmpty() || snsObjectClass == null || snsUserProtobufClass == null) return@runCatching

        val snsObj = snsObjectClass!!.getDeclaredConstructor().newInstance()
        parseFromMethod?.invoke(snsObj, values.get("attrBuf") as? ByteArray ?: return@runCatching)

        val fakeList = LinkedList<Any>().apply {
            fakeWxIds.forEach { wxid ->
                snsUserProtobufClass!!.getDeclaredConstructor().newInstance().apply {
                    javaClass.getDeclaredField("d").apply { isAccessible = true }.set(this, wxid)
                    add(this)
                }
            }
        }

        likeUserListField?.set(snsObj, fakeList)
        likeUserListCountField?.set(snsObj, fakeList.size)
        likeCountField?.set(snsObj, fakeList.size)
        likeFlagField?.set(snsObj, 1)

        values.put("attrBuf", toByteArrayMethod?.invoke(snsObj) as? ByteArray ?: return@runCatching)
        WeLogger.i(TAG, "成功为朋友圈 $snsId 注入 ${fakeList.size} 个伪点赞")
    }.onFailure { WeLogger.e(TAG, "注入伪点赞失败", it) }
}
