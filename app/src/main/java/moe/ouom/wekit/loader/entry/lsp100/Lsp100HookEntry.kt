package moe.ouom.wekit.loader.entry.lsp100

import android.content.pm.ApplicationInfo
import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.annotations.XposedApiExact
import moe.ouom.wekit.BuildConfig
import moe.ouom.wekit.constants.PackageNames
import moe.ouom.wekit.loader.entry.common.ModuleLoader
import moe.ouom.wekit.loader.entry.lsp100.Lsp100HookImpl.Companion.init
import moe.ouom.wekit.loader.entry.lsp10x.Lsp10xHookEntryHandler

@XposedApiExact(100)
class Lsp100HookEntry(private val self: XposedModule) :
    Lsp10xHookEntryHandler {

    init {
        init(self)
    }

    @XposedApiExact(100)
    override fun onPackageLoaded(param: PackageLoadedParam) {
        val packageName = param.getPackageName()
        if (packageName.startsWith(PackageNames.WECHAT)) {
            if (param.isFirstPackage()) {
                val modulePath = self.applicationInfo.sourceDir
                handleLoadHostPackage(
                    param.getClassLoader(),
                    param.getApplicationInfo(),
                    modulePath
                )
            }
        }
    }

    private fun handleLoadHostPackage(cl: ClassLoader, ai: ApplicationInfo, modulePath: String) {
        val dataDir = ai.dataDir
        Log.d(
            BuildConfig.TAG,
            "Lsp100HookEntry.handleLoadHostPackage: dataDir=$dataDir, modulePath=$modulePath"
        )
        try {
            ModuleLoader.initialize(
                dataDir,
                cl,
                Lsp100HookImpl.INSTANCE,
                Lsp100HookImpl.INSTANCE,
                modulePath,
                true
            )
        } catch (e: ReflectiveOperationException) {
            throw RuntimeException(e)
        }
    }
}
