package dev.ujhhgtg.wekit.hooks.core

// 声明某个类是 hook 项目 等待被扫描自动编译添加
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class HookItem(
    val path: String,  // 功能路径
    val desc: String = "" // 功能描述
)
