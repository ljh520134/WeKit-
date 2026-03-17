# App
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
-keep class moe.ouom.wekit.loader.entry.lsp10x.Lsp10xUnifiedHookEntry { *; }
-keep class moe.ouom.wekit.loader.entry.xp51.Xp51HookEntry { *; }
-keep class moe.ouom.wekit.loader.entry.frida.FridaInjectEntry { *; }

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

# Suppress warnings
-dontwarn java.lang.reflect.AnnotatedType
-dontwarn java.beans.**
-dontwarn io.netty.internal.tcnative.**
-dontwarn jdk.jfr.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.log4j.**
-dontwarn java.lang.management.**
-dontwarn reactor.blockhound.**
-dontwarn com.aayushatharva.brotli4j.**
-dontwarn com.github.luben.zstd.**
-dontwarn com.jcraft.jzlib.**
-dontwarn com.ning.compress.**
-dontwarn lzma.sdk.**
-dontwarn net.jpountz.**
-dontwarn com.oracle.svm.core.annotate.**
-dontwarn io.netty.pkitesting.**
-dontwarn com.sun.nio.file.SensitivityWatchEventModifier
-dontwarn org.osgi.annotation.**
