package dev.ujhhgtg.wekit.hooks.core

import com.highcapable.kavaref.resolver.ConstructorResolver
import com.highcapable.kavaref.resolver.MethodResolver
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import dev.ujhhgtg.wekit.constants.PreferenceKeys
import dev.ujhhgtg.wekit.dexkit.dsl.DexConstructorDelegate
import dev.ujhhgtg.wekit.dexkit.dsl.DexMethodDelegate
import dev.ujhhgtg.wekit.preferences.WePrefs
import dev.ujhhgtg.wekit.utils.HookAction
import dev.ujhhgtg.wekit.utils.TargetProcesses
import dev.ujhhgtg.wekit.utils.logging.WeLogger
import java.lang.reflect.Executable

abstract class BaseHookItem {

    var path: String = ""

    var description: String = ""

    open val targetProcesses = TargetProcesses.PROC_MAIN

    var hasEnabled: Boolean = false
        private set

    fun enable(process: Int = 1) {
        if (hasEnabled) return
        if (process and targetProcesses == 0) return
        runCatching {
            hasEnabled = true
            onEnable()
        }.onFailure { e -> WeLogger.e("failed to enable item", e) }
    }

    fun disable(process: Int = 1) {
        if (!hasEnabled) return
        if (process and targetProcesses == 0) return
        runCatching {
            hasEnabled = false
            onDisable()
        }.onFailure { e -> WeLogger.e("failed to disable item", e) }
    }

    open fun onEnable() {}

    open fun onDisable() {}

    // --- hookBefore ---

    inline fun Executable.hookBefore(
        priority: Int = WePrefs.getIntOrDef(PreferenceKeys.HOOK_PRIORITY, 50),
        crossinline action: HookAction
    ) {
        XposedBridge.hookMethod(
            this,
            object :
                XC_MethodHook(priority) {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    executeHookAction(param, action)
                }
            }
        )
    }

    @JvmName("hookBefore2")
    inline fun MethodResolver<*>.hookBefore(
        priority: Int = WePrefs.getIntOrDef(PreferenceKeys.HOOK_PRIORITY, 50),
        crossinline action: HookAction
    ) {
        return this.self.hookBefore(priority, action)
    }

    @JvmName("hookBefore3")
    inline fun ConstructorResolver<*>.hookBefore(
        priority: Int = WePrefs.getIntOrDef(PreferenceKeys.HOOK_PRIORITY, 50),
        crossinline action: HookAction
    ) {
        return this.self.hookBefore(priority, action)
    }

    // --- end hookBefore ---

    // --- hookAfter ---

    inline fun Executable.hookAfter(
        priority: Int = WePrefs.getIntOrDef(PreferenceKeys.HOOK_PRIORITY, 50),
        crossinline action: HookAction
    ) {
        XposedBridge.hookMethod(
            this,
            object :
                XC_MethodHook(priority) {
                override fun afterHookedMethod(param: MethodHookParam) {
                    executeHookAction(param, action)
                }
            }
        )
    }

    @JvmName("hookAfter2")
    inline fun MethodResolver<*>.hookAfter(
        priority: Int = WePrefs.getIntOrDef(PreferenceKeys.HOOK_PRIORITY, 50),
        crossinline action: HookAction
    ) {
        return this.self.hookAfter(priority, action)
    }

    @JvmName("hookAfter3")
    inline fun ConstructorResolver<*>.hookAfter(
        priority: Int = WePrefs.getIntOrDef(PreferenceKeys.HOOK_PRIORITY, 50),
        crossinline action: HookAction
    ) {
        return this.self.hookAfter(priority, action)
    }

    // --- end hookAfter ---

    // --- dex delegate ---

    inline fun DexMethodDelegate.hookBefore(
        priority: Int = WePrefs.getIntOrDef(PreferenceKeys.HOOK_PRIORITY, 50),
        crossinline action: HookAction
    ) {
        return this.method.hookBefore(priority, action)
    }

    inline fun DexMethodDelegate.hookAfter(
        priority: Int = WePrefs.getIntOrDef(PreferenceKeys.HOOK_PRIORITY, 50),
        crossinline action: HookAction
    ) {
        return this.method.hookAfter(priority, action)
    }

    inline fun DexConstructorDelegate.hookBefore(
        priority: Int = WePrefs.getIntOrDef(PreferenceKeys.HOOK_PRIORITY, 50),
        crossinline action: HookAction
    ) {
        return this.constructor.hookBefore(priority, action)
    }

    inline fun DexConstructorDelegate.hookAfter(
        priority: Int = WePrefs.getIntOrDef(PreferenceKeys.HOOK_PRIORITY, 50),
        crossinline action: HookAction
    ) {
        return this.constructor.hookAfter(priority, action)
    }

    // --- end dex delegate ---

    inline fun executeHookAction(param: XC_MethodHook.MethodHookParam, action: HookAction) {
        if (this is SwitchHookItem && !this.isEnabled) return
        if (!hasEnabled) return
        runCatching {
            action(param)
        }.onFailure { e -> WeLogger.e("executeHookAction", "failed to execute hook of $path", e) }
    }
}
