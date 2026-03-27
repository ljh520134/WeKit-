package dev.ujhhgtg.wekit.constants

import dev.ujhhgtg.wekit.BuildConfig

object PackageNames {

    const val WECHAT = "com.tencent.mm"
    const val THIS = BuildConfig.APPLICATION_ID

    @JvmStatic
    fun isWeChat(packageName: String) = packageName.startsWith(WECHAT)
}
