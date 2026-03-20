package dev.ujhhgtg.wekit.ui.utils

object LifecycleOwnerProvider {
    val lifecycleOwner by lazy { XposedLifecycleOwner().apply {
        onCreate()
        onStart()
        onResume()
    } }
}
