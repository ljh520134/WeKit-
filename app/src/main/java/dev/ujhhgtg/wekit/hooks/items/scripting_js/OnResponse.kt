package dev.ujhhgtg.wekit.hooks.items.scripting_js

import dev.ujhhgtg.wekit.hooks.core.SwitchHookItem
import dev.ujhhgtg.wekit.hooks.core.HookItem

@HookItem(path = "脚本/触发器：收到响应", desc = "收到响应时是否执行 onResponse()")
object OnResponse : SwitchHookItem()
