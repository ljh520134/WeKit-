package dev.ujhhgtg.wekit.hooks.core

import dev.ujhhgtg.wekit.utils.TargetProcesses

abstract class ApiHookItem : BaseHookItem() {

    override fun startup(process: Int) {
        if (process != TargetProcesses.PROC_MAIN) return
        enable()
    }
}
