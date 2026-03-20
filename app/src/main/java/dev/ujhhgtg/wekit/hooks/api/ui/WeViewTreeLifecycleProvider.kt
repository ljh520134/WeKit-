package dev.ujhhgtg.wekit.hooks.api.ui

import android.app.Activity
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import dev.ujhhgtg.wekit.core.model.ApiHookItem
import dev.ujhhgtg.wekit.hooks.utils.annotation.HookItem
import dev.ujhhgtg.wekit.ui.utils.LifecycleOwnerProvider
import dev.ujhhgtg.wekit.ui.utils.rootView
import dev.ujhhgtg.wekit.ui.utils.setLifecycleOwner

@HookItem(path = "API/宿主 ComposeView 生命周期提供方")
object WeViewTreeLifecycleProvider : ApiHookItem() {

    override fun onEnable() {
        WeMainActivityBeautifyApi.methodDoOnCreate.hookAfter(100) { param ->
            val activity = param.thisObject.asResolver()
                .firstField {
                    type = "com.tencent.mm.ui.MMFragmentActivity"
                }
                .get()!! as Activity

            val lifecycleOwner = LifecycleOwnerProvider.lifecycleOwner

            val decorView = activity.window.decorView
            decorView.setLifecycleOwner(lifecycleOwner)
            activity.rootView.setLifecycleOwner(lifecycleOwner)
        }
    }
}
