package dev.ujhhgtg.wekit.hooks.items.contacts

import android.widget.BaseAdapter
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.extension.toClass
import dev.ujhhgtg.wekit.constants.PackageNames
import dev.ujhhgtg.wekit.hooks.core.HookItem
import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import dev.ujhhgtg.wekit.utils.cast

@HookItem(path = "联系人与群组/显示隐藏朋友设置项", desc = "阻止微信隐藏朋友设置; 部分设置项可能名称可能不显示, 但不影响功能")
object DisplayHiddenContactSettings : SwitchHookItem() {

    override fun onEnable() {
        "${PackageNames.WECHAT}.plugin.profile.ui.ProfileSettingUI".toClass().asResolver()
            .firstMethod {
                name = "initView"
            }.hookAfter { param ->
                val prefScreen = param.thisObject.asResolver()
                    .firstMethod {
                        name = "getPreferenceScreen"
                        superclass()
                    }.invoke()!!
                val hiddenSet = prefScreen.asResolver()
                    .firstField {
                        type = HashSet::class
                    }.get()!! as HashSet<*>
                hiddenSet.clear()
                prefScreen.cast<BaseAdapter>().notifyDataSetChanged()
            }
    }
}
