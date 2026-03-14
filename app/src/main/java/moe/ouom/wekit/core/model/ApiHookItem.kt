package moe.ouom.wekit.core.model

import moe.ouom.wekit.utils.common.SyncUtils

abstract class ApiHookItem : BaseHookItem() {

    val targetProcess: Int = targetProcess()

    open fun targetProcess(): Int = SyncUtils.PROC_MAIN
}
