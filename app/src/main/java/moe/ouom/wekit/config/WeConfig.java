package moe.ouom.wekit.config;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.Map;
import java.util.Set;

import moe.ouom.wekit.constants.Constants;

public abstract class WeConfig implements SharedPreferences, SharedPreferences.Editor {

    private static WeConfig sDefConfig;

    protected WeConfig() {
    }

    @NonNull
    public static synchronized WeConfig getDefaultConfig() {
        if (sDefConfig == null) {
            sDefConfig = new MmkvConfigManagerImpl("global_config");
        }
        return sDefConfig;
    }

    public static void dPutBoolean(@NonNull String key, Boolean b) {
        getDefaultConfig().edit().putBoolean(key, b).apply();
    }

    public static void dPutString(@NonNull String key, String s) {
        getDefaultConfig().edit().putString(key, s).apply();
    }

    public static void dPutInt(@NonNull String key, int i) {
        getDefaultConfig().edit().putInt(key, i).apply();
    }

    public static boolean dGetBoolean(@NonNull String key) {
        return getDefaultConfig().getBooleanOrFalse(key);
    }

    public static boolean dGetBooleanDefTrue(@NonNull String key) {
        return getDefaultConfig().getBooleanOrDefault(key, true);
    }

    public static String dGetString(@NonNull String key, String d) {
        return getDefaultConfig().getStringOrDefault(key, d);
    }

    public static int dGetInt(@NonNull String key, int d) {
        return getDefaultConfig().getIntOrDefault(key, d);
    }

    @Nullable
    public abstract File getFile();

    @Nullable
    public Object getOrDefault(@NonNull String key, @Nullable Object def) {
        if (!containsKey(key)) {
            return def;
        }
        return getObject(key);
    }

    public boolean getBooleanOrFalse(@NonNull String key) {
        return getBooleanOrDefault(key, false);
    }

    public boolean getBoolPref(@NonNull String key) {
        return getBooleanOrDefault(Constants.PREF_KEY_PREFIX + key, false);
    }

    public String getStringPref(@NonNull String key, @Nullable String def) {
        return getString(Constants.PREF_KEY_PREFIX + key, def);
    }

    public int getIntPrek(@NonNull String key, int def) {
        return getInt(Constants.PREF_KEY_PREFIX + key, def);
    }

    public long getLongPrek(@NonNull String key, long def) {
        return getLong(Constants.PREF_KEY_PREFIX + key, def);
    }

    public boolean getBooleanOrDefault(@NonNull String key, boolean def) {
        return getBoolean(key, def);
    }

    public int getIntOrDefault(@NonNull String key, int def) {
        return getInt(key, def);
    }


    @Nullable
    public abstract String getString(@NonNull String key);

    @NonNull
    public String getStringOrDefault(@NonNull String key, @NonNull String defVal) {
        return getString(key, defVal);
    }

    @NonNull
    public Set<String> getStringSetOrDefault(@NonNull String key, @NonNull Set<String> defVal) {
        return getStringSet(key, defVal);
    }

    @Nullable
    public abstract Object getObject(@NonNull String key);

    @Nullable
    public byte[] getBytes(@NonNull String key) {
        return getBytes(key, null);
    }

    @Nullable
    public abstract byte[] getBytes(@NonNull String key, @Nullable byte[] defValue);

    @NonNull
    public abstract byte[] getBytesOrDefault(@NonNull String key, @NonNull byte[] defValue);

    public abstract void putBytes(@NonNull String key, @NonNull byte[] value);

    /**
     * @return READ-ONLY all config
     * @deprecated Avoid use getAll(), MMKV only have limited support for this.
     */
    @Override
    @Deprecated
    @NonNull
    public abstract Map<String, ?> getAll();

    public abstract void save();

    public long getLongOrDefault(@Nullable String key, long i) {
        return getLong(key, i);
    }

    @NonNull
    public abstract WeConfig putObject(@NonNull String key, @NonNull Object v);

    public boolean containsKey(@NonNull String k) {
        return contains(k);
    }

    @NonNull
    @Override
    public Editor edit() {
        return this;
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(
            @NonNull OnSharedPreferenceChangeListener listener) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(
            @NonNull OnSharedPreferenceChangeListener listener) {
        throw new UnsupportedOperationException("not implemented");
    }

    public abstract boolean isReadOnly();

    public abstract boolean isPersistent();
}
