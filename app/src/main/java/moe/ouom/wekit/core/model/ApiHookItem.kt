package moe.ouom.wekit.core.model

import de.robv.android.xposed.XC_MethodHook
import moe.ouom.wekit.utils.common.SyncUtils

abstract class ApiHookItem : BaseHookItem() {

    val targetProcess: Int = targetProcess()

    open fun targetProcess(): Int = SyncUtils.PROC_MAIN

    final override fun tryExecute(param: XC_MethodHook.MethodHookParam, hookAction: HookAction) {
        super.tryExecute(param, hookAction)
    }
}
