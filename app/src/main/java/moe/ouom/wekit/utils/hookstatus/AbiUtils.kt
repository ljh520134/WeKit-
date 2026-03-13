package moe.ouom.wekit.utils.hookstatus

import android.content.Context
import android.content.pm.PackageManager
import moe.ouom.wekit.host.HostInfo
import moe.ouom.wekit.loader.startup.StartupInfo
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * This is intended to be used in module process only.
 */
object AbiUtils {

    const val ABI_ARM32: Int = 1
    const val ABI_ARM64: Int = 1 shl 1
    const val ABI_X86: Int = 1 shl 2
    const val ABI_X86_64: Int = 1 shl 3

    private var cachedModuleAbiFlavor: String? = null

    fun getApplicationActiveAbi(packageName: String): String? {
        val ctx: Context = HostInfo.application
        val pm = ctx.packageManager
        try {
            // find apk path
            val libDir = pm.getApplicationInfo(packageName, 0).nativeLibraryDir ?: return null
            // find abi
            val abiList = HashSet<String>(4)
            for (abi in arrayOf("arm", "arm64", "x86", "x86_64")) {
                if (File(libDir, abi).exists()) {
                    abiList.add(abi)
                } else if (libDir.endsWith(abi)) {
                    abiList.add(abi)
                }
            }
            if (abiList.isEmpty()) {
                return null
            }
            // TODO: 2022-03-14 handle multi arch
            return abiList.iterator().next()
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }
    }

    val moduleFlavorName: String
        get() {
            if (cachedModuleAbiFlavor != null) {
                return cachedModuleAbiFlavor!!
            }
            val apkPath: String
            if (HostInfo.isHost) {
                apkPath = StartupInfo.getModulePath()
            } else {
                // self process
                apkPath = HostInfo.application.packageCodePath
            }
            check(
                File(apkPath).exists()
            ) { "getModuleFlavorName, apk not found: $apkPath" }
            val abis: HashSet<String>
            try {
                abis = getApkAbiList(apkPath)
            } catch (e: Exception) {
                throw RuntimeException(
                    "getModuleFlavorName, getApkAbiList failed: " + e.message,
                    e
                )
            }
            val abiFlags = getAbiFlags(abis)
            cachedModuleAbiFlavor =
                if ((abiFlags and (ABI_ARM32 or ABI_ARM64 or ABI_X86 or ABI_X86_64)) == (ABI_ARM32 or ABI_ARM64 or ABI_X86 or ABI_X86_64)) {
                    "universal"
                } else if ((abiFlags and (ABI_ARM32 or ABI_ARM64)) == (ABI_ARM32 or ABI_ARM64)) {
                    "armAll"
                } else if (abiFlags == ABI_ARM32) {
                    "arm32"
                } else if (abiFlags == ABI_ARM64) {
                    "arm64"
                } else {
                    "unknown"
                }
            return cachedModuleAbiFlavor!!
        }

    private fun getAbiFlags(abis: HashSet<String>): Int {
        var abiFlags = 0
        for (abi in abis) {
            abiFlags = when (abi) {
                "armeabi-v7a" -> abiFlags or ABI_ARM32
                "arm64-v8a" -> abiFlags or ABI_ARM64
                "x86" -> abiFlags or ABI_X86
                "x86_64" -> abiFlags or ABI_X86_64
                else -> throw IllegalStateException("getModuleFlavorName, unknown abi: $abi")
            }
        }
        return abiFlags
    }

    fun queryModuleAbiList(): Set<String> {
        when (moduleFlavorName) {
            "arm32" -> {
                return setOf("arm")
            }

            "arm64" -> {
                return setOf("arm64")
            }

            "armAll" -> {
                return setOf("arm", "arm64")
            }

            "universal" -> {
                return setOf("arm", "arm64", "x86", "x86_64")
            }

            else -> {
                return setOf()
            }
        }
    }

    fun getApkAbiList(apkPath: String): HashSet<String> {
        val zipFile = ZipFile(apkPath)
        val abiList = HashSet<String>(4)
        val it = zipFile.entries()
        while (it.hasMoreElements()) {
            val entry: ZipEntry = it.nextElement()
            if (entry.name.startsWith("lib/")) {
                val abi = entry.name.substring(4, entry.name.indexOf('/', 4))
                abiList.add(abi)
            }
        }
        zipFile.close()
        return abiList
    }

    fun archStringToLibDirName(arch: String): String {
        return when (arch) {
            "x86", "i386", "i486", "i586", "i686" -> "x86"
            "x86_64", "amd64" -> "x86_64"
            "arm", "armhf", "armv7l", "armeabi", "armeabi-v7a" -> "arm"
            "aarch64", "arm64", "arm64-v8a", "armv8l" -> "arm64"
            else -> throw IllegalArgumentException("unsupported arch: $arch")
        }
    }

    fun archStringToArchInt(arch: String): Int {
        return when (arch) {
            "arm", "arm32", "armeabi", "armeabi-v7a", "armv7l" ->                 // actually, armv7l is ARMv8 CPU in 32-bit compatibility mode,
                // I don't know if we should throw armv7l into ABI_ARM64
                ABI_ARM32

            "arm64", "arm64-v8a", "aarch64" -> ABI_ARM64
            "x86", "i386", "i486", "i586", "i686" -> ABI_X86
            "x86_64", "amd64" -> ABI_X86_64
            else -> 0
        }
    }

    fun getSuggestedAbiVariant(requestedAbi: Int): String {
        if (requestedAbi == ABI_ARM32) {
            return "arm32"
        }
        if (requestedAbi == ABI_ARM64) {
            return "arm64"
        }
        if ((requestedAbi or ABI_ARM32 or ABI_ARM64) == (ABI_ARM32 or ABI_ARM64)) {
            return "armAll"
        }
        return "universal"
    }
}
