package dev.ujhhgtg.wekit.hooks.core

import dev.ujhhgtg.wekit.utils.TargetProcesses

abstract class ApiHookItem : BaseHookItem() {

    override fun startup() {
        if (!TargetProcesses.isInMain) return
        enable()
    }
}
