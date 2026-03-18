package moe.ouom.wekit.loader.entry.lsp101

import android.content.pm.ApplicationInfo
import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.annotations.XposedApiMin
import moe.ouom.wekit.BuildConfig
import moe.ouom.wekit.constants.PackageNames
import moe.ouom.wekit.loader.entry.common.ModuleLoader
import moe.ouom.wekit.loader.entry.lsp10x.Lsp10xHookEntryHandler

@XposedApiMin(101)
class Lsp101HookEntry(private val self: XposedModule) : Lsp10xHookEntryHandler {

    fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        Lsp101HookImpl.init(self)
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
    }

    fun onPackageReady(param: PackageReadyParam) {
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
            "Lsp101HookEntry: handleLoadHostPackage: dataDir=$dataDir, modulePath=$modulePath"
        )
        ModuleLoader.initialize(
            dataDir,
            cl,
            Lsp101HookImpl.INSTANCE,
            Lsp101HookImpl.INSTANCE,
            modulePath,
            true
        )
    }
}
