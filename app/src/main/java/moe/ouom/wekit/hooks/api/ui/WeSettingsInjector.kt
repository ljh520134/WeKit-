package moe.ouom.wekit.hooks.api.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.MenuItem
import com.android.dx.stock.ProxyBuilder
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.extension.ClassLoaderProvider
import com.highcapable.kavaref.extension.createInstance
import com.highcapable.kavaref.extension.toClass
import com.highcapable.kavaref.extension.toClassOrNull
import de.robv.android.xposed.XposedHelpers
import dev.ujhhgtg.nameof.nameof
import moe.ouom.wekit.BuildConfig
import moe.ouom.wekit.core.dsl.dexClass
import moe.ouom.wekit.core.dsl.dexMethod
import moe.ouom.wekit.core.model.ApiHookItem
import moe.ouom.wekit.dexkit.DexMethodDescriptor
import moe.ouom.wekit.dexkit.abc.IResolvesDex
import moe.ouom.wekit.hooks.utils.annotation.HookItem
import moe.ouom.wekit.ui.content.MainSettingsDialog
import moe.ouom.wekit.utils.ModulePaths
import moe.ouom.wekit.utils.logging.WeLogger
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Modifier
import java.nio.file.Path

@SuppressLint("DiscouragedApi", "StaticFieldLeak")
@HookItem(path = "API/设置模块入口")
object WeSettingsInjector : ApiHookItem(), IResolvesDex {

    private val methodSetKey by dexMethod()
    private val methodSetTitle by dexMethod()
    private val methodGetKey by dexMethod()
    private val methodAddPref by dexMethod()

    // method 3
    private val methodSettingGroupPluginOnClick by dexMethod()
    private val classSettingItemClassesProvider by dexClass()
    private val classBaseSettingItem by dexClass()
    private val classSettingLocation by dexClass()

    private val TAG = nameof(WeSettingsInjector)

    private const val KEY_WEKIT_ENTRY = "wekit_settings_entry"
    private const val TITLE_WEKIT_ENTRY = "WeKit 设置"
    private const val PREFERENCE_CLASS_NAME = "com.tencent.mm.ui.base.preference.Preference"

    @SuppressLint("NonUniqueDexKitData")
    override fun resolveDex(dexKit: DexKitBridge): Map<String, String> {
        val descriptors = mutableMapOf<String, String>()

        // 查找 Preference 类
        val prefClass = dexKit.findClass {
            matcher { className = PREFERENCE_CLASS_NAME }
        }.singleOrNull() ?: run {
            WeLogger.e(TAG, "Preference 类未找到")
            return descriptors
        }

        // 查找 setKey 方法
        methodSetKey.find(dexKit, allowMultiple = true, descriptors = descriptors) {
            searchPackages("com.tencent.mm.ui.base.preference")
            matcher {
                declaredClass = PREFERENCE_CLASS_NAME
                returnType = "void"
                paramTypes("java.lang.String")
                usingStrings("Preference")
            }
        }

        // 查找 setTitle 方法
        val setTitleCandidates = prefClass.findMethod {
            matcher {
                returnType = "void"
                paramTypes("java.lang.CharSequence")
            }
        }
        if (setTitleCandidates.isNotEmpty()) {
            val target = setTitleCandidates.last()
            methodSetTitle.setDescriptor(
                DexMethodDescriptor(
                    target.className,
                    target.methodName,
                    target.methodSign
                )
            )
            methodSetTitle.getDescriptorString()?.let {
                descriptors[methodSetTitle.key] = it
            }
        }

        // 查找 getKey 方法
        val getKeyCandidates = prefClass.findMethod {
            matcher {
                paramCount = 0
                returnType = "java.lang.String"
            }
        }

        val targetGetKey = getKeyCandidates.firstOrNull { it.name != "toString" }

        if (targetGetKey != null) {
            methodGetKey.setDescriptor(
                DexMethodDescriptor(
                    targetGetKey.className,
                    targetGetKey.methodName,
                    targetGetKey.methodSign
                )
            )
            methodGetKey.getDescriptorString()?.let {
                descriptors[methodGetKey.key] = it
            }
        }

        // 查找 Adapter 类和 addPreference 方法
        val adapterClass = dexKit.findClass {
            searchPackages("com.tencent.mm.ui.base.preference")
            matcher {
                superClass = "android.widget.BaseAdapter"
                methods {
                    add {
                        modifiers = Modifier.PUBLIC
                        name = "getView"
                        paramCount = 3
                    }
                    add {
                        name = "<init>"
                        paramCount = 3
                    }
                }
            }
        }.singleOrNull()

        if (adapterClass != null) {
            methodAddPref.find(dexKit, allowMultiple = true, descriptors = descriptors) {
                searchPackages("com.tencent.mm.ui.base.preference")
                matcher {
                    declaredClass = adapterClass.name
                    paramTypes(PREFERENCE_CLASS_NAME, "int")
                    returnType = "void"
                }
            }
        }

        methodSettingGroupPluginOnClick.find(dexKit, descriptors) {
            matcher {
                declaredClass = "com.tencent.mm.plugin.setting.ui.setting_new.settings.other.SettingGroupPlugin"
                usingEqStrings("MicroMsg.SettingGroupPlugin", "openLiteApp query:%s")
            }
        }

        classSettingItemClassesProvider.find(dexKit, descriptors) {
            matcher {
                usingEqStrings("Repairer_Setting")

                superClass {
                    usingEqStrings("type")
                }
            }
        }

        classBaseSettingItem.find(dexKit, descriptors) {
            matcher {
                usingEqStrings("", "activity", "context", "intent")

                addMethod {
                    name = "<init>"
                    paramTypes("androidx.appcompat.app.AppCompatActivity")
                }

                addInterface {
                    className("com.tencent.mm.plugin.newtips.model", StringMatchType.StartsWith)
                }
            }
        }

        classSettingLocation.find(dexKit, descriptors) {
            matcher {
                usingEqStrings("SettingLocation(parentGroup=", ", frontItem=")
            }
        }

        return descriptors
    }

    override fun onEnable() {
        // 尝试 Hook 旧版 UI
        tryHookLegacySettings()

        // 尝试 Hook 新版 UI (8.0.67+)
        tryHookNewSettingsMethod1()
        tryHookNewSettingsMethod2()
        // tryHookNewSettingsMethod3()
    }

    /**
     * 适配旧版 SettingsUI (基于 PreferenceScreen)
     */
    private fun tryHookLegacySettings() {
        // 检查类是否存在
        val clsSettingsUi = "com.tencent.mm.plugin.setting.ui.setting.SettingsUI"
            .toClassOrNull() ?: return

        val setKeyMethod = methodSetKey.method
        val setTitleMethod = methodSetTitle.method
        val getKeyMethod = methodGetKey.method
        val addPrefMethod = methodAddPref.method

        clsSettingsUi.asResolver().firstMethod {
            name = "initView"
            parameterCount = 0
        }.hookAfter { param ->
            val activity = param.thisObject as Activity
            val context = activity as Context

            try {
                val clsIconPref =
                    "com.tencent.mm.ui.base.preference.IconPreference".toClass()
                val prefInstance = clsIconPref.createInstance(context)

                setKeyMethod.invoke(prefInstance, KEY_WEKIT_ENTRY)
                setTitleMethod.invoke(prefInstance, TITLE_WEKIT_ENTRY)

                val prefScreen = XposedHelpers.callMethod(activity, "getPreferenceScreen")

                addPrefMethod.invoke(prefScreen, prefInstance, 0)

            } catch (e: Throwable) {
                WeLogger.e(TAG, "插入选项失败", e)
            }
        }

        WeLogger.i(TAG, "injected settings")

        clsSettingsUi.asResolver().firstMethod { name = "onPreferenceTreeClick" } .hookBefore { param ->
            if (param.args.size < 2) return@hookBefore
            val preference = param.args[1] ?: return@hookBefore

            val key = getKeyMethod.invoke(preference) as? String

            if (KEY_WEKIT_ENTRY == key) {
                val activity = param.thisObject as Activity

                openSettingsDialog(activity)

                param.result = true
            }
        }
    }

    private fun tryHookNewSettingsMethod1() {
        val newSettingsCls =
            "com.tencent.mm.plugin.setting.ui.setting_new.base.BaseSettingPrefUI"
                .toClassOrNull() ?: return

        newSettingsCls.asResolver().firstMethod { name = "onCreate" }.hookAfter { param ->
            if (param.thisObject.javaClass.name
                != "com.tencent.mm.plugin.setting.ui.setting_new.MainSettingsUI"
            ) return@hookAfter

            val activity = param.thisObject as Activity
            activity.asResolver()
                .firstMethod {
                    name = "addTextOptionMenu"
                    parameters(
                        Int::class,
                        String::class,
                        MenuItem.OnMenuItemClickListener::class
                    )
                    superclass()
                }
                .invoke(0, BuildConfig.TAG, SettingsMenuItemClickListener(activity))
        }
    }

    private fun createSettingItemClass(
        cacheDir: Path
    ): Class<*> {
        val baseClass = classBaseSettingItem.clazz
        val classSettingGroupMain = "com.tencent.mm.plugin.setting.ui.setting_new.settings.SettingGroupMain".toClass()

        val handler = InvocationHandler { proxy, method, args ->
            when (method.name) {
                "C6" -> return@InvocationHandler classSettingGroupMain
                "K6" -> return@InvocationHandler 1
                "Q6" -> { openSettingsDialog(args[0] as Context) }
                // header name string resource
//                "u6" -> return@InvocationHandler 0x7f1067e8
                "w6" -> return@InvocationHandler "SettingGroup_Main_Other_WeKit"
                "x6" -> return@InvocationHandler classSettingLocation.clazz.createInstance(
                    classSettingGroupMain,
                    "com.tencent.mm.plugin.setting.ui.setting_new.settings.SettingAdditionHeaderSearch".toClass()
                )
                "z6" -> return@InvocationHandler 0x7f1067e8
                else -> return@InvocationHandler ProxyBuilder.callSuper(proxy, method, *args)
            }
        }

        return ProxyBuilder.forClass(baseClass)
            .dexCache(cacheDir.toFile())
            .parentClassLoader(ClassLoaderProvider.classLoader!!)
            .constructorArgTypes("androidx.appcompat.app.AppCompatActivity".toClass())
            .handler(handler)
            .buildProxyClass()
            .also {
                // if generating a proxy class with buildProxyClass(), instances do not automatically have a handler set
                it.asResolver().firstConstructor().hookAfter { param ->
                    ProxyBuilder.setInvocationHandler(param.thisObject, handler)
                }
            }
    }

    private fun tryHookNewSettingsMethod2() {
        val customSettingItemClass = createSettingItemClass(ModulePaths.cache)

        // create dependency chain
        "com.tencent.mm.plugin.setting.ui.setting_new.settings.SettingGroupPersonalInfo".toClass().asResolver()
            .firstMethod {
                returnType = classSettingLocation.clazz
            }
            .hookBefore { param ->
                param.result = classSettingLocation.clazz.createInstance(
                    "com.tencent.mm.plugin.setting.ui.setting_new.settings.SettingGroupMain".toClass(),
                    customSettingItemClass
                )
            }

        classSettingItemClassesProvider.clazz.asResolver().firstMethod()
            .hookAfter { param ->
                val map = param.result as? Map<*, *>? ?: return@hookAfter
                val originalSet = map.values.first() as LinkedHashSet<*>
                val newSet = originalSet + customSettingItemClass
                WeLogger.i(TAG, "injected setting item class into setting classes map; originalSet.size=${originalSet.size}, newSet.size=${newSet.size}")
                param.result = mapOf(map.keys.first() to newSet)
            }

        val newSettingsCls =
            "com.tencent.mm.plugin.setting.ui.setting_new.base.BaseSettingPrefUI"
                .toClassOrNull() ?: return

        newSettingsCls.asResolver().firstMethod { name = "superImportUIComponents" }.hookAfter { param ->
            if (param.thisObject.javaClass.name
                != "com.tencent.mm.plugin.setting.ui.setting_new.MainSettingsUI"
            ) return@hookAfter

            @Suppress("UNCHECKED_CAST")
            val settingItemClasses = param.args[0] as HashSet<Class<*>>
            settingItemClasses.add(customSettingItemClass)
        }
    }

//    private fun tryHookNewSettingsMethod3() {
//        methodSettingGroupPluginOnClick.hookBefore { param ->
//            val context = param.args[0] as Context
//            openSettingsDialog(context)
//            param.result = null
//        }
//    }

    private fun openSettingsDialog(context: Context) {
        MainSettingsDialog(context).show()
    }

    private class SettingsMenuItemClickListener(val context: Context) :
        MenuItem.OnMenuItemClickListener {
        override fun onMenuItemClick(p0: MenuItem): Boolean {
            openSettingsDialog(context)
            return true
        }
    }
}