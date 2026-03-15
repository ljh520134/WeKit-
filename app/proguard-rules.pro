# Module
-keep class **.R$* { *; }
-keep interface moe.ouom.wekit.dexkit.intf.IResolvesDex { *; }
-keep class * implements moe.ouom.wekit.dexkit.intf.IResolvesDex {
    public void resolveDex(...);
}
-keep class moe.ouom.wekit.core.model.SwitchHookItem { *; }
-keep class moe.ouom.wekit.core.model.ClickableHookItem { *; }
-keep class moe.ouom.wekit.core.model.ApiHookItem { *; }
-keep class moe.ouom.wekit.core.model.BaseHookItem { *; }
-keep class moe.ouom.wekit.hooks.items.scripting_kts.** { *; }
-adaptresourcefilecontents src/main/assets/xposed_init
-adaptresourcefilecontents src/main/resources/META-INF/xposed/java_init.list

# Attributes
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault,LineNumberTable,SourceFile,*Annotation*

# Natives
-keepclasseswithmembernames class * {
    native <methods>;
}

# Android
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# Gson
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# MMKV
-keep class com.tencent.mmkv.MMKV {
    public long decodeLong(java.lang.String, long);
    public static com.tencent.mmkv.MMKV defaultMMKV();
}

# Suppress warnings
-dontwarn java.lang.reflect.AnnotatedType
-dontwarn java.beans.**
