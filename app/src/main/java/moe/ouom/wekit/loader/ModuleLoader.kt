package moe.ouom.wekit.loader;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import moe.ouom.wekit.loader.hookapi.ILoaderService;
import moe.ouom.wekit.loader.startup.UnifiedEntryPoint;

public class ModuleLoader {

    private static final ArrayList<Throwable> sInitErrors = new ArrayList<>(1);
    private static boolean sLoaded = false;

    public static void initialize(
            @NonNull ClassLoader hostClassLoader,
            @NonNull ILoaderService loaderService,
            @NonNull String modulePath
    ) throws ReflectiveOperationException {
        if (sLoaded) {
            return;
        }
        sLoaded = true;
        UnifiedEntryPoint.entry(modulePath, loaderService, hostClassLoader);
    }

    public static List<Throwable> getInitErrors() {
        return sInitErrors;
    }
}
