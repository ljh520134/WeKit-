
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.aboutlibraries.android)
}

fun getCommitCount(): Int {
    return providers.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
    }.standardOutput.asText.get().trim().toInt()
}

fun getGitHash(): String {
    return providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
    }.standardOutput.asText.get().trim()
}

fun findNdkClang(androidHome: String, minSdk: Int, minNdk: Int = 29): String? {
    val ndkRoot = File("$androidHome/ndk")
    if (!ndkRoot.exists()) return null
    val isWindows = System.getProperty("os.name").orEmpty().contains("Windows", ignoreCase = true)
    val ext = if (isWindows) ".cmd" else ""

    return ndkRoot.listFiles()
        ?.filter { it.isDirectory }
        ?.mapNotNull { dir ->
            val parts = dir.name.split(".").mapNotNull { it.toIntOrNull() }
            if (parts.isNotEmpty() && parts[0] >= minNdk) dir else null
        }
        ?.sortedWith(compareBy(*Array(3) { i -> { d: File -> d.name.split(".").getOrNull(i)?.toIntOrNull() ?: 0 } }))
        ?.lastOrNull()
        ?.let { ndkDir ->
            fileTree(ndkDir).matching { include("**/*-linux-android${minSdk}-clang$ext") }
                .firstOrNull()
                ?.absolutePath
        }
}

fun setCargoClang(androidHome: String) {
    val minSdk = libs.versions.minSdk.get().toInt()
    val isWindows = System.getProperty("os.name").orEmpty().contains("Windows", ignoreCase = true)
    val ext = if (isWindows) ".cmd" else ""

    val clangPath = findNdkClang(androidHome, minSdk) ?: error("No NDK >= $minSdk found in $androidHome/ndk")
    logger.lifecycle("Found NDK clang: $clangPath")

    val ndkBinDir = File(clangPath).parent.replace('\\', '/')
    val configToml = rootProject.file("app/src/main/rust/wekit-native/.cargo/config.toml")

    configToml.parentFile.mkdirs()
    configToml.writeText("""
        [target.aarch64-linux-android]
        ar = "$ndkBinDir/llvm-ar"
        linker = "$ndkBinDir/aarch64-linux-android${minSdk}-clang$ext"

        [target.x86_64-linux-android]
        ar = "$ndkBinDir/llvm-ar"
        linker = "$ndkBinDir/x86_64-linux-android${minSdk}-clang$ext"

        [target.armv7-linux-androideabi]
        ar = "$ndkBinDir/llvm-ar"
        linker = "$ndkBinDir/armv7a-linux-androideabi${minSdk}-clang$ext"

        [target.i686-linux-android]
        ar = "$ndkBinDir/llvm-ar"
        linker = "$ndkBinDir/i686-linux-android${minSdk}-clang$ext"

        [env]
        CC_aarch64_linux_android = "$ndkBinDir/aarch64-linux-android${minSdk}-clang$ext"
        CXX_aarch64_linux_android = "$ndkBinDir/aarch64-linux-android${minSdk}-clang++$ext"
        AR_aarch64_linux_android = "$ndkBinDir/llvm-ar"

        CC_x86_64_linux_android = "$ndkBinDir/x86_64-linux-android${minSdk}-clang$ext"
        CXX_x86_64_linux_android = "$ndkBinDir/x86_64-linux-android${minSdk}-clang++$ext"
        AR_x86_64_linux_android = "$ndkBinDir/llvm-ar"

        CC_armv7-linux-androideabi = "$ndkBinDir/armv7a-linux-androideabi${minSdk}-clang$ext"
        CXX_armv7-linux-androideabi = "$ndkBinDir/armv7a-linux-androideabi${minSdk}-clang++$ext"
        AR_armv7-linux-androideabi = "$ndkBinDir/llvm-ar"

        CC_i686-linux-android = "$ndkBinDir/i686-linux-android${minSdk}-clang$ext"
        CXX_i686-linux-android = "$ndkBinDir/i686-linux-android${minSdk}-clang++$ext"
        AR_i686-linux-android = "$ndkBinDir/llvm-ar"
    """.trimIndent()
    )
    logger.lifecycle("Written .cargo/config.toml to ${configToml.absolutePath}")
}

configure<ApplicationExtension> {
    val androidHome = gradleLocalProperties(rootDir, providers).getProperty("sdk.dir")
        ?: System.getenv("ANDROID_HOME")
        ?: error("ANDROID_HOME / sdk.dir not set")
    val ndkVer = libs.versions.ndk.get()

    if (!File("$androidHome/ndk/$ndkVer").exists()) {
        logger.lifecycle("NDK $ndkVer not found, installing via sdkmanager...")
        val isWindows = System.getProperty("os.name").orEmpty().contains("Windows", ignoreCase = true)
        val sdkmanager = "$androidHome/cmdline-tools/latest/bin/sdkmanager" + if (isWindows) ".bat" else ""
        providers.exec { commandLine(sdkmanager, "--install", "ndk;$ndkVer") }.result.get()
        logger.lifecycle("NDK $ndkVer installed")
    }

    if (!rootProject.file("app/src/main/rust/wekit-native/.cargo/config.toml").exists()) {
        logger.lifecycle("Cargo config not found, configuring it...")
        setCargoClang(androidHome)
    }

    namespace = libs.versions.namespace.get()
    ndkVersion = ndkVer

    val commitCount = getCommitCount()
    val gitHash = getGitHash()

    logger.lifecycle(
        """
             _       __     __ __ _ __
            | |     / /__  / //_/(_) /_
            | | /| / / _ \/ ,<  / / __/
            | |/ |/ /  __/ /| |/ / /_
            |__/|__/\___/_/ |_/_/\__/

       [WeKit] WeChat, now with superpowers
        """
    )

    defaultConfig {
        applicationId = libs.versions.namespace.get()
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        compileSdk = libs.versions.targetSdk.get().toInt()
        versionCode = commitCount
        versionName = "git+$gitHash"

        buildConfigField("String", "GIT_HASH", "\"${gitHash}\"")
        buildConfigField("String", "TAG", "\"WeKit\"")
        buildConfigField("long", "BUILD_TIMESTAMP", "${System.currentTimeMillis()}L")
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")
            isUniversalApk = false
        }
    }

    sourceSets["main"].jniLibs.directories += "src/main/jniLibs"

    signingConfigs {
        create("release") {
            val keystoreFile = System.getenv("WEKIT_KEYSTORE_FILE")?.takeIf { it.isNotBlank() }

            if (keystoreFile != null) {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("WEKIT_KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() } ?: ""
                keyAlias = System.getenv("WEKIT_KEY_ALIAS")?.takeIf { it.isNotBlank() } ?: ""
                keyPassword = System.getenv("WEKIT_KEY_PASSWORD")?.takeIf { it.isNotBlank() } ?: ""

                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
                logger.lifecycle("Using release keystore: $keystoreFile")
            } else {
                logger.lifecycle("No keystore configured, using debug signing")
                val debugKeystore = File(System.getProperty("user.home"), ".android/debug.keystore")
                storeFile = debugKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        // 修复：debug 也使用签名配置，体积更小
        debug {
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jdk.get().toInt())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jdk.get().toInt())
    }

    packaging {
        resources.excludes += listOf(
            "kotlin/**",
            "**.bin",
            "kotlin-tooling-metadata.json",
            "META-INF/INDEX.LIST"
        )
        resources.merges += listOf(
            "META-INF/io.netty.versions.properties",
            "META-INF/xposed/*",
            "org/mozilla/javascript/**"
        )

        jniLibs {
            useLegacyPackaging = false
        }
    }

    @Suppress("UnstableApiUsage")
    androidResources {
        localeFilters += setOf("zh")
        additionalParameters += listOf("--allow-reserved-package-id", "--package-id", "0x69")
    }

    buildFeatures {
        resValues = true
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jdk.get()))
    }
    jvmToolchain(libs.versions.jdk.get().toInt())
}

val adbProvider = androidComponents.sdkComponents.adb
androidComponents {
    onVariants { variant ->
        val kotlinSources = variant.sources.kotlin ?: return@onVariants

        kotlinSources.addGeneratedSourceDirectory(
            generateMethodHashes,
            GenerateMethodHashesTask::outputDir
        )
    }
}

fun isHooksDirPresent(task: Task): Boolean {
    return task.outputs.files.any { outputDir ->
        File(outputDir, "${libs.versions.namespace.get().replace(".", "/")}/hooks").exists()
    }
}

tasks.withType<KotlinCompile>().configureEach {
    if (name.contains("Release")) {
        outputs.upToDateWhen { task ->
            isHooksDirPresent(task)
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    if (name.contains("Release")) {
        outputs.upToDateWhen { task ->
            isHooksDirPresent(task)
        }
    }
}

abstract class GenerateMethodHashesTask : DefaultTask() {
    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val namespace: Property<String>

    @TaskAction
    fun generate() {
        val srcDir = sourceDir.get().asFile
        val outDir = outputDir.get().asFile
        val outputFile = outDir.resolve("${namespace.get().replace(".", "/")}/dexkit/cache/GeneratedMethodHashes.kt")

        val hashMap = mutableMapOf<String, String>()
        srcDir.walk().filter { it.isFile && it.extension == "kt" && it.readText().contains("IResolvesDex") }.forEach { file ->
            val content = file.readText()
            val packageName = Regex("""package\s+([\w.]+)""").find(content)?.groupValues?.get(1)
            val className = Regex("""(?:class|object)\s+(\w+)""").find(content)?.groupValues?.get(1) ?: return@forEach
            val fullClassName = if (packageName != null) "$packageName.$className" else className

            val resolveDexMatch = Regex("""override\s+fun\s+resolveDex\s*\(""").find(content)
            if (resolveDexMatch != null) {
                val start = content.indexOf('{', resolveDexMatch.range.last)
                if (start != -1) {
                    var count = 0
                    for (i in start until content.length) {
                        if (content[i] == '{') count++ else if (content[i] == '}') count--
                        if (count == 0) {
                            val body = content.substring(start, i + 1)
                            val hash = MessageDigest.getInstance("MD5").digest(body.toByteArray()).joinToString("") { "%02x".format(it) }
                            hashMap[fullClassName] = hash
                            break
                        }
                    }
                }
            }
        }

        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            package ${namespace.get()}.dexkit.cache
            object GeneratedMethodHashes {
                private val hashes = mapOf(${hashMap.entries.sortedBy { it.key }.joinToString(",") { "\"${it.key}\" to \"${it.value}\"" }})
                fun getHash(className: String) = hashes[className] ?: ""
            }
        """.trimIndent()
        )
    }
}

val generateMethodHashes = tasks.register<GenerateMethodHashesTask>("generateMethodHashes") {
    group = "wekit"
    sourceDir.set(file("src/main/java"))
    outputDir.set(layout.buildDirectory.dir("generated/source/methodhashes"))
    namespace.set(libs.versions.namespace.get())
}

val rustProjectDir = file("src/main/rust/wekit-native")
val rustLibName = "libwekit_native.so"

val abiToTarget = mapOf(
    "arm64-v8a" to "aarch64-linux-android",
    "x86_64" to "x86_64-linux-android",
    "armeabi-v7a" to "armv7-linux-androideabi",
    "x86" to "i686-linux-android"
)

// 全局变量存储 NDK 路径，供 cargo 任务使用
val androidHome = gradleLocalProperties(rootDir, providers).getProperty("sdk.dir")
    ?: System.getenv("ANDROID_HOME")
    ?: error("ANDROID_HOME / sdk.dir not set")
val ndkVer = libs.versions.ndk.get()

val cargoTasks = listOf("arm64-v8a").map { abi ->
    val target = abiToTarget[abi]!!
    tasks.register<Exec>("cargoBuild_${abi.replace('-', '_')}") {
        group = "rust"
        description = "Compile Rust for $abi"
        workingDir = rustProjectDir
        commandLine = listOf(
            "cargo", "build",
            "--release",
            "--target", target,
        )
        doLast {
            val soSrc = rustProjectDir
                .resolve("target/$target/release/$rustLibName")
            val soDir = layout.projectDirectory
                .dir("src/main/jniLibs/$abi").asFile
            soDir.mkdirs()

            // 使用 NDK 中的 llvm-strip 完整路径
            val llvmStrip = "$androidHome/ndk/$ndkVer/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip"

            logger.lifecycle("Stripping $abi library with: $llvmStrip")
            ProcessBuilder(llvmStrip, "--strip-all", soSrc.absolutePath)
                .inheritIO()
                .start()
                .waitFor()

            soSrc.copyTo(soDir.resolve(rustLibName), overwrite = true)
            logger.lifecycle("Copied stripped library to: ${soDir.resolve(rustLibName).absolutePath}")
        }
    }
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("JniLibFolders") }
    .configureEach { cargoTasks.forEach { t -> dependsOn(t) } }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.android.material)
    implementation(libs.androidx.activity)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.browser)
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose.m3)
    implementation(libs.kyant0.backdrop)
    implementation(libs.kyant0.shapes)

    implementation(libs.composablehorizons.material.symbols.filled)
    implementation(libs.composablehorizons.material.symbols.outlined)

    implementation(libs.google.guava)
    implementation(libs.google.protobuf.javalite)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.mmkv)

    compileOnly(libs.legacyxposed.api)
    compileOnly(project(":libs:common:libxposed-api"))
    implementation(libs.libxposed.service)
    implementation(libs.dexlib2)
    implementation(libs.dexkit)
    implementation(libs.hiddenapibypass)
    implementation(libs.kavaref.core)
    implementation(libs.kavaref.extension)
    implementation(libs.libsu.core)
    implementation(libs.dexmaker)
    implementation(project(":libs:common:annotation-scanner"))
    ksp(project(":libs:common:annotation-scanner"))

    implementation(libs.dalvik.dx)
    implementation(libs.okhttp3.okhttp)

    implementation(libs.rhino.android)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    implementation(libs.markwon.core)
    implementation(libs.markwon.ext.strikethrough)
    implementation(libs.markwon.ext.tables)
    implementation(libs.markwon.ext.tasklist)
    implementation(libs.markwon.html)
    implementation(libs.markwon.image)
    implementation(libs.markwon.svg)
    implementation(libs.markwon.gif)
    implementation(libs.mcp.server)
    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.sse)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.osmdroid.android)

    implementation(project(":libs:external:nameof-kt:api"))
    compileOnly(project(":libs:common:stubs"))
}

configurations.all {
    exclude(group = "org.jetbrains", module = "annotations-java5")
}

evaluationDependsOn(":libs:external:nameof-kt:plugin")
tasks.withType<KotlinJvmCompile>().configureEach {
    val pluginJarTask = project(":libs:external:nameof-kt:plugin").tasks.named<org.gradle.jvm.tasks.Jar>("jar")
    dependsOn(pluginJarTask)

    compilerOptions {
        val pluginJarPath = pluginJarTask.get().archiveFile.get().asFile.absolutePath
        freeCompilerArgs.add("-Xplugin=$pluginJarPath")
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
    }
}