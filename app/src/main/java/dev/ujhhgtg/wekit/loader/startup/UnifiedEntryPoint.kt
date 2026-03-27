package dev.ujhhgtg.wekit.loader.startup

import android.app.Instrumentation
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.extension.ClassLoaderProvider
import com.highcapable.kavaref.extension.toClass
import dev.ujhhgtg.nameof.nameof
import dev.ujhhgtg.wekit.loader.abc.ILoaderService
import dev.ujhhgtg.wekit.utils.WeLogger
import dev.ujhhgtg.wekit.utils.hookAfterDirectly

object UnifiedEntryPoint {

    private val TAG = nameof(UnifiedEntryPoint)

    fun entry(
        loaderService: ILoaderService,
        hostClassLoader: ClassLoader,
        modulePath: String
    ) {
        ClassLoaderProvider.classLoader = hostClassLoader

        "com.tencent.mm.app.Application".toClass().asResolver()
            .firstMethod { name = "attachBaseContext" }
            .hookAfterDirectly {
                // Hook Instrumentation.callApplicationOnCreate 以确保在 Tinker 热更新完成后再进行延迟初始化
                // 这可以解决某些模块在热更新环境下找不到入口的问题
                Instrumentation::class.asResolver()
                    .firstMethod {
                        name = "callApplicationOnCreate"
                    }
                    .hookAfterDirectly { _ ->
                        runCatching {
                            StartupAgent.startup(
                                modulePath,
                                loaderService
                            )
                        }.onFailure { WeLogger.e(TAG, "StartupAgent failed", it) }
                    }
            }
    }
}
