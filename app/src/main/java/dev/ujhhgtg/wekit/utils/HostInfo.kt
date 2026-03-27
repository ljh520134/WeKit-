package dev.ujhhgtg.wekit.utils

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.extension.toClass
import dev.ujhhgtg.wekit.constants.PackageNames

enum class HostSpecies { WeChat, WeKit, Unknown }

data class HostInfoImpl(
    val application: Application,
    val packageName: String,
    val hostName: String,
    val versionCode: Long,
    val versionCode32: Int,
    val versionName: String,
    val hostSpecies: HostSpecies
)

object HostInfo {

    private lateinit var _info: HostInfoImpl

    val info: HostInfoImpl get() = _info
    val application: Application get() = _info.application
    val appInfo: ApplicationInfo get() = application.applicationInfo
    val packageName: String get() = _info.packageName
    val versionCode32: Int get() = _info.versionCode32
    val versionCode: Int get() = versionCode32
    val isModule: Boolean get() = _info.hostSpecies == HostSpecies.WeKit
    val isHost: Boolean get() = !isModule

    val isHostGooglePlay: Boolean by lazy {
        runCatching {
            "com.tencent.mm.boot.BuildConfig".toClass().asResolver()
                .firstField { name = "BUILD_TAG" }
                .get() as? String?
        }.getOrNull()?.contains("GP", ignoreCase = true) ?: false
    }

    fun init(application: Application) {
        check(!::_info.isInitialized) { "HostInfo has already been initialized" }

        val pm = application.packageManager
        val packageName = application.packageName
        val packageInfo = try {
            pm.getPackageInfo(packageName, PackageManager.GET_META_DATA)
        } catch (e: PackageManager.NameNotFoundException) {
            WeLogger.e("failed to get package info", e)
            throw e
        }

        _info = HostInfoImpl(
            application = application,
            packageName = packageName,
            hostName = application.applicationInfo.loadLabel(pm).toString(),
            versionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
            versionCode32 = PackageInfoCompat.getLongVersionCode(packageInfo).toInt(),
            versionName = packageInfo.versionName.orEmpty(),
            hostSpecies = run {
                if (PackageNames.isWeChat(packageName)) return@run HostSpecies.WeChat
                return@run when (packageName) {
                    PackageNames.THIS -> HostSpecies.WeKit
                    else -> HostSpecies.Unknown
                }
            }
        )
    }
}
