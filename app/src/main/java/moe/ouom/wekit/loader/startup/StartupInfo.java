package moe.ouom.wekit.loader.startup;

import android.app.Application;

import androidx.annotation.NonNull;

import java.util.Objects;

import moe.ouom.wekit.loader.hookapi.ILoaderService;


public class StartupInfo {

    public static Application hostApp;
    private static String modulePath;
    private static ILoaderService loaderService;

    public static String getModulePath() {
        if (modulePath == null) {
            throw new IllegalStateException("Module path is null");
        }
        return modulePath;
    }

    public static void setModulePath(@NonNull String modulePath) {
        Objects.requireNonNull(modulePath);
        StartupInfo.modulePath = modulePath;
    }

    public static void setHostApp(Application hostApp) {
        Objects.requireNonNull(hostApp);
        StartupInfo.hostApp = hostApp;
    }

    @NonNull
    public static ILoaderService getLoaderService() {
        return loaderService;
    }

    public static void setLoaderService(@NonNull ILoaderService loaderService) {
        Objects.requireNonNull(loaderService);
        StartupInfo.loaderService = loaderService;
    }
}
