package moe.ouom.wekit.loader.core

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.highcapable.kavaref.extension.toClass
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import dev.ujhhgtg.nameof.nameof
import moe.ouom.wekit.config.RuntimeConfig
import moe.ouom.wekit.constants.PackageConstants
import moe.ouom.wekit.dexkit.cache.DexCacheManager
import moe.ouom.wekit.hooks.core.HookItemsLoader
import moe.ouom.wekit.utils.common.ModuleRes
import moe.ouom.wekit.utils.common.SyncUtils
import moe.ouom.wekit.utils.log.WeLogger

object WeLauncher {

    fun init(cl: ClassLoader, context: Context) {
        val processType = SyncUtils.getProcessType()
        val currentProcessName = SyncUtils.getProcessName()
        WeLogger.i(TAG, "launching in processName=$currentProcessName, type=$processType)")

        runCatching {
            ParcelableFixer.init(cl, WeLauncher::class.java.classLoader!!)
            WeLogger.i(TAG, "ParcelableFixer installed.")
        }.onFailure { WeLogger.e(TAG, "Failed to install ParcelableFixer", it) }

        runCatching {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            DexCacheManager.init(requireNotNull(pInfo.versionName))
        }.onFailure { WeLogger.e(TAG, "failed to load version info", it) }

        if (processType == SyncUtils.PROC_MAIN) {
            runCatching {
                val appContext = context.applicationContext ?: context
                ActivityProxy.initForStubActivity(appContext)
                WeLogger.i(TAG, "Activity Proxy Hooks installed successfully (Main Process).")
            }.onFailure { WeLogger.e(TAG, "Failed to install Activity Proxy Hooks", it) }

            initMainProcessHooks(cl)
        } else {
            WeLogger.i(TAG, "Skipping UI hooks for non-main process: $currentProcessName")
        }

        runCatching {
            HookItemsLoader.loadHookItems(processType)
        }.onFailure { WeLogger.e(TAG, "failed to load hooks", it) }
    }

    private fun initMainProcessHooks(cl: ClassLoader) {
        WeLogger.i(TAG, "Initializing Main Process Hooks...")

        val launcherUiClass = LAUNCHER_UI_CLASS_NAME.toClass()

        runCatching {
            XposedHelpers.findAndHookMethod(launcherUiClass, "onResume", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as Activity
                    ModuleRes.init(activity, PackageConstants.PACKAGE_NAME_SELF)
                }
            })
        }.onFailure { WeLogger.e(TAG, "failed to hook LauncherUI.onResume", it) }

        runCatching {
            XposedHelpers.findAndHookMethod(launcherUiClass, "onCreate", Bundle::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as Activity
                    RuntimeConfig.setLauncherUiActivity(activity)
                    val sharedPreferences = activity.getSharedPreferences("com.tencent.mm_preferences", 0)
                    RuntimeConfig.setMmPrefs(sharedPreferences)
                }
            })
        }.onFailure { WeLogger.e(TAG, "failed to hook LauncherUI.onCreate", it) }
    }

    private const val LAUNCHER_UI_CLASS_NAME = "com.tencent.mm.ui.LauncherUI"
    private val TAG = nameof(WeLauncher)
}