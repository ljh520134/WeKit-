package moe.ouom.wekit.loader.legacy;

import static moe.ouom.wekit.constants.PackageNames.THIS;
import static moe.ouom.wekit.constants.PackageNames.WECHAT;

import androidx.annotation.Keep;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import moe.ouom.wekit.loader.ModuleLoader;

/**
 * Entry point for started Xposed API 51-99.
 * <p>
 * Xposed is used as ART hook implementation.
 */

@Keep
public class LegacyHookEntry implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    public static String sCurrentPackageName = null;
    private static XC_LoadPackage.LoadPackageParam sLoadPackageParam = null;
    private static StartupParam sInitZygoteStartupParam = null;
    private static String sModulePath = null;

    /**
     * Get the {@link XC_LoadPackage.LoadPackageParam} of the current module.
     *
     * @return the lpparam
     */
    public static XC_LoadPackage.LoadPackageParam getLoadPackageParam() {
        if (sLoadPackageParam == null) {
            throw new IllegalStateException("LoadPackageParam is null");
        }
        return sLoadPackageParam;
    }

    /**
     * Get the path of the current module.
     *
     * @return the module path
     */
    public static String getModulePath() {
        if (sModulePath == null) {
            throw new IllegalStateException("Module path is null");
        }
        return sModulePath;
    }

    /**
     * Get the {@link StartupParam} of the current module.
     *
     * @return the initZygote param
     */
    public static StartupParam getInitZygoteStartupParam() {
        if (sInitZygoteStartupParam == null) {
            throw new IllegalStateException("InitZygoteStartupParam is null");
        }
        return sInitZygoteStartupParam;
    }

    /**
     * *** No kotlin code should be invoked here.*** May cause a crash.
     */
    @Keep
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws ReflectiveOperationException {
        sLoadPackageParam = lpparam;
        // check LSPosed dex-obfuscation
        switch (lpparam.packageName) {
            case THIS: {
                Xp51HookStatusInit.init(lpparam.classLoader);
                break;
            }
            case WECHAT:
                if (sInitZygoteStartupParam == null) {
                    throw new IllegalStateException("handleLoadPackage: sInitZygoteStartupParam is null");
                }
                sCurrentPackageName = lpparam.packageName;
                ModuleLoader.initialize(lpparam.classLoader,
                        Xp51HookImpl.INSTANCE, getModulePath());
                break;
            default:
                break;
        }
    }

    /**
     * *** No kotlin code should be invoked here.*** May cause a crash.
     */
    @Override
    public void initZygote(StartupParam startupParam) {
        sInitZygoteStartupParam = startupParam;
        sModulePath = startupParam.modulePath;
    }

}
