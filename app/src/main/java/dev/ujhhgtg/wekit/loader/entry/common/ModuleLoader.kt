package dev.ujhhgtg.wekit.loader.entry.common

import dev.ujhhgtg.nameof.nameof
import dev.ujhhgtg.wekit.loader.abc.IHookBridge
import dev.ujhhgtg.wekit.loader.abc.ILoaderService
import dev.ujhhgtg.wekit.loader.startup.UnifiedEntryPoint
import dev.ujhhgtg.wekit.utils.WeLogger

object ModuleLoader {

    private val TAG = nameof(ModuleLoader)
    private var isInitialized = false

    @JvmStatic
    fun init(
        hostDataDir: String,
        hostClassLoader: ClassLoader,
        loaderService: ILoaderService,
        hookBridge: IHookBridge?,
        modulePath: String,
        allowDynamicLoad: Boolean
    ) {
        if (isInitialized) return
        isInitialized = true

        WeLogger.i(TAG, "initializing from entry point ${loaderService.entryPointName}")
        UnifiedEntryPoint.entry(loaderService, hostClassLoader, modulePath)
    }
}
