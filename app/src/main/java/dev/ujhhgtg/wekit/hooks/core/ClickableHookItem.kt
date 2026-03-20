package dev.ujhhgtg.wekit.hooks.core

import android.content.Context

abstract class ClickableHookItem : SwitchHookItem() {

    open val alwaysRun: Boolean = false

    open val noSwitchWidget = false

    abstract fun onClick(context: Context)
}
