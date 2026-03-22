package dev.ujhhgtg.wekit.hooks.items.contacts

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import dev.ujhhgtg.wekit.hooks.api.core.WeDatabaseApi
import dev.ujhhgtg.wekit.hooks.api.ui.WeContactPrefsScreenApi
import dev.ujhhgtg.wekit.hooks.api.ui.WeContactPrefsScreenApi.ContactInfoItem
import dev.ujhhgtg.wekit.hooks.api.ui.WeContactPrefsScreenApi.IContactInfoProvider
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import dev.ujhhgtg.wekit.utils.ToastUtils

@HookItem(
    path = "联系人与群组/显示微信 ID",
    desc = "在联系人与群组详情页面显示微信 ID"
)
object ShowWxIdInContactDetails : SwitchHookItem(), IContactInfoProvider {

    private const val PREF_KEY = "wxid_display"
    private const val SEPARATOR = ";"

    override fun getContactInfoItem(activity: Activity): ContactInfoItem {
        var wxId: String?
        val intentField = activity.intent.getStringExtra("Contact_User")
        wxId = if (intentField != null) {
            if (!intentField.startsWith("wxid_")) {
                val friends = WeDatabaseApi.getFriends()
                friends.firstOrNull { it.customWxid == intentField }?.wxId ?: intentField
            } else {
                intentField
            }
        } else {
            activity.intent.getStringExtra("RoomInfo_Id")
        }

        return ContactInfoItem(
            key = "$PREF_KEY$SEPARATOR$wxId",
            title = "微信 ID: ${wxId ?: "获取失败"}",
            position = 1
        )
    }

    override fun onItemClick(activity: Activity, key: String): Boolean {
        if (!key.startsWith(PREF_KEY)) return false
        val wxId = key.substringAfter(SEPARATOR)
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("WxId", wxId))
        ToastUtils.showToast(activity, "已复制")
        return true
    }

    override fun onEnable() {
        WeContactPrefsScreenApi.addProvider(this)
    }

    override fun onDisable() {
        WeContactPrefsScreenApi.removeProvider(this)
    }
}
