plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

import java.util.Properties
import java.io.File

// 工作区在华为家庭存储同步盘上：同步进程会在构建期间读写构建产物导致随机失败。
// 构建输出重定向到项目所在盘的根目录（非 NAS 同步目录，且与 KSP 生成文件同盘）。
layout.buildDirectory.set(File(rootDir.toPath().root.toFile(), "AloCardBuild/app"))

android {
    namespace = "com.alosir.card"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.alosir.card"
        minSdk = 26
        targetSdk = 35
        versionCode = (project.findProperty("alocard.versionCode") as String).toInt()
        versionName = project.findProperty("alocard.versionName") as String
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 发布签名（keystore 不入库；见 android/README.md「发布构建」）
    val keystorePropsFile = rootProject.file("keystore.properties")
    if (keystorePropsFile.exists()) {
        val keystoreProps = Properties().apply { keystorePropsFile.inputStream().use { load(it) } }
        signingConfigs {
            create("release") {
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
            }
        }
    }

    buildTypes {
        release {
            // R8 在内存耗尽环境下会 JVM 崩溃；可用 -Palocard.minify=false 临时关闭
            isMinifyEnabled = (project.findProperty("alocard.minify") as String?) != "false"
            isShrinkResources = isMinifyEnabled
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // 用 compilerOptions DSL（kotlinOptions 已在 Kotlin 2.2 弃用）
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Room schema 导出目录（exportSchema = true 的配套；schema JSON 纳入版本控制）
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

// 更新日志单一事实来源：仓库根 CHANGELOG.md 构建时拷入 assets，
// 应用内「设置 → 关于 → 更新日志」展示同一份内容（AGENTS.md 约定）。
// 注意：settings.gradle 在 android/ 下，仓库根是它的上一级。
// 用自定义任务暴露 DirectoryProperty，经 androidComponents 注册生成源目录——
// merge/lint/test 等所有消费者的任务依赖一次到位（Gradle 8 校验隐式输入）。
abstract class CopyChangelogTask : DefaultTask() {
    @get:InputFile
    abstract val changelog: org.gradle.api.file.RegularFileProperty

    @get:OutputDirectory
    abstract val output: org.gradle.api.file.DirectoryProperty

    @org.gradle.api.tasks.TaskAction
    fun copy() {
        val dir = output.get().asFile
        dir.mkdirs()
        changelog.get().asFile.copyTo(File(dir, "CHANGELOG.md"), overwrite = true)
    }
}

val copyChangelog = tasks.register<CopyChangelogTask>("copyChangelog") {
    changelog.set(rootProject.layout.projectDirectory.file("../CHANGELOG.md"))
    output.set(layout.buildDirectory.dir("generated/assets"))
}
androidComponents {
    onVariants(selector().all()) { variant ->
        variant.sources.assets!!.addGeneratedSourceDirectory(copyChangelog, CopyChangelogTask::output)
    }
}

dependencies {
    // --- Compose BOM：统一管理所有 Compose 库版本 ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // --- 数据层 ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // --- 安全 ---
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // --- 依赖注入 ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // --- 后台任务 ---
    implementation(libs.androidx.work.runtime.ktx)

    // 小组件用 RemoteViews 实现（widget/GracePeriodWidgetReceiver），
    // 不引入 Glance——规避其与 Compose Runtime 的版本耦合（技术规范 §1.9 的 Plan B）。

    // --- 调试 ---
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // --- 测试 ---
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
