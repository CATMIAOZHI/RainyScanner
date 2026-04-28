# 雨晴扫描 (RainyScanner)

一个基于 **Jetpack Compose** 的现代化 Android 条码/二维码扫描应用，采用 CameraX + ZXing 纯软件解码方案。

## 🚀 项目特性

✅ **Jetpack Compose** - 现代化声明式 UI 框架  
✅ **Material Design 3** - 元气樱粉主题配色  
✅ **CameraX + ZXing** - 高性能条码/二维码扫描  
✅ **Kotlin** - 100% Kotlin 编写  
✅ **Gradle Version Catalog** - 统一依赖管理  
✅ **扫描历史** - SharedPreferences 本地持久化  

## 📁 项目结构

```
RainyScanner/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/rainyscanner/app/
│   │   │   │   ├── MainActivity.kt              # 主Activity
│   │   │   │   ├── data/
│   │   │   │   │   └── ScanHistory.kt           # 扫描历史持久化
│   │   │   │   └── ui/
│   │   │   │       ├── screen/
│   │   │   │       │   ├── ScannerScreen.kt     # 扫描界面
│   │   │   │       │   └── HistoryScreen.kt     # 历史记录界面
│   │   │   │       └── theme/
│   │   │   │           ├── Color.kt             # 樱粉配色定义
│   │   │   │           ├── Theme.kt             # 主题配置
│   │   │   │           └── Type.kt              # 字体配置
│   │   │   ├── res/                             # 资源文件
│   │   │   └── AndroidManifest.xml              # 应用清单
│   │   ├── androidTest/                         # Android测试
│   │   │   └── java/com/rainyscanner/app/
│   │   │       └── ExampleInstrumentedTest.kt
│   │   └── test/                                # 单元测试
│   │       └── java/com/rainyscanner/app/
│   │           └── ExampleUnitTest.kt
│   ├── build.gradle.kts                         # App模块配置
│   └── proguard-rules.pro                       # 混淆规则
├── gradle/
│   ├── libs.versions.toml                       # 依赖版本管理
│   └── wrapper/                                 # Gradle Wrapper
├── tools/
│   └── aapt2/
│       └── aapt2-arm64-v8a                      # ARM64 AAPT2 二进制
├── build.gradle.kts                             # 项目级配置
├── settings.gradle.kts                          # 项目设置
├── gradle.properties                            # Gradle属性
├── gradlew / gradlew.bat                        # Gradle命令
├── setup_android_env.sh                         # 环境初始化脚本
└── .gitignore                                   # Git忽略
```

## 🛠️ 快速开始

### 1. 环境要求
- ✅ **JDK 17+**（必需）
- ✅ **Gradle** (已包含 Wrapper)
- ✅ **Android SDK** (可选，用于完整编译)

### 2. 构建项目

#### 使用 Operit 内置命令按钮
- 🔧 **初始化 Gradle Wrapper** - 首次使用
- 🔨 **构建项目** - 编译整个项目
- 🧹 **清理构建** - 清理构建缓存
- 📋 **查看所有任务** - 列出可用任务

#### 命令行方式
```bash
# Linux/Mac
./gradlew build              # 构建项目
./gradlew assembleDebug      # 打包Debug APK
./gradlew installDebug       # 安装到设备
./gradlew clean              # 清理构建

# Windows
gradlew.bat build
gradlew.bat assembleDebug
```

### 3. 生成的APK位置
```
app/build/outputs/apk/debug/app-debug.apk
```

## 📦 依赖管理

项目使用 **Gradle Version Catalog** 统一管理依赖版本。

### 查看当前依赖
在 `gradle/libs.versions.toml` 中定义：

```toml
[versions]
agp = "8.7.3"
kotlin = "2.3.10"
composeBom = "2026.01.01"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
```

### 添加新依赖
1. 在 `gradle/libs.versions.toml` 中添加版本和库定义
2. 在 `app/build.gradle.kts` 中引用：
   ```kotlin
   dependencies {
       implementation(libs.your.library.name)
   }
   ```

## 🎨 自定义应用

### 修改应用名称
编辑 `app/src/main/res/values/strings.xml`：
```xml
<string name="app_name">你的应用名</string>
```

### 修改包名
1. 更新 `app/build.gradle.kts` 中的 `namespace` 和 `applicationId`
2. 重命名 `java/com/rainyscanner/app` 目录结构
3. 更新 `AndroidManifest.xml` 中的包名引用

### 修改主题颜色
编辑 `app/src/main/java/.../ui/theme/Color.kt`：
```kotlin
val Purple80 = Color(0xFFD0BCFF)  // 修改为你的颜色
```

## 📱 Compose 示例

当前应用包含完整的扫描功能：

```kotlin
// MainActivity.kt — 双屏导航
@Composable
fun App(scanHistory: ScanHistory) {
    when (currentScreen) {
        Screen.Scanner -> ScannerScreen(...)  // CameraX + ZXing 实时扫码
        Screen.History -> HistoryScreen(...)  // 扫码历史管理
    }
}
```

你可以扩展：
- 添加更多条码格式支持（在 `ScannerScreen.kt` 的 `DecodeHintType` 中配置）
- 集成 ML Kit Barcode 作为备选扫描方案（依赖已引入）
- 添加 WIFI 二维码自动连接功能
- 实现导航（推荐使用 Navigation Compose）
- 集成 ViewModel、Repository 等架构组件

## 🔧 常用 Gradle 任务

```bash
./gradlew tasks              # 查看所有可用任务
./gradlew clean              # 清理构建
./gradlew build              # 完整构建
./gradlew assembleDebug      # 构建Debug APK
./gradlew assembleRelease    # 构建Release APK
./gradlew installDebug       # 安装Debug到设备
./gradlew test               # 运行单元测试
./gradlew connectedAndroidTest # 运行Android测试
```

## 📝 注意事项

⚠️ **关于 Android SDK**  
- 此模板可以在 Operit 的 Ubuntu 环境中构建
- 完整编译需要安装 Android SDK
- 推荐使用 Android Studio 进行完整开发

### ⚠️ ARM64 环境 AAPT2 替换（模板已内置）

Gradle 会自动从 Google Maven 下载 AAPT2，但官方分发在 ARM64 Linux 环境下不可直接使用。
此模板已经内置 ARM64 `aapt2`，`setup_android_env.sh` 会自动把它替换到 SDK build-tools 和 Gradle 缓存里。

**模板内置来源**：
- Release: https://github.com/ReVanced/aapt2/releases/tag/v1.0.0
- ARM64 aapt2: https://github.com/ReVanced/aapt2/releases/download/v1.0.0/aapt2-arm64-v8a
- SHA-256: `e5b5ff7f0d4f6ecd7fa5d05d77fed3f09f6f1bf80f078b8aada82bc578848561`

**你只需要执行**
```bash
chmod +x ./setup_android_env.sh
./setup_android_env.sh
```

脚本会自动完成：
- 替换 `$ANDROID_SDK/build-tools/35.0.0/aapt2`
- 替换 `~/.gradle/caches/modules-2/files-2.1/com.android.tools.build/aapt2` 下的 jar 内二进制
- 替换 `~/.gradle/caches/transforms-*` 中已经解压出来的 `aapt2`

⚠️ **关于包名**  
- 默认包名为 `com.java.myapplication`
- 发布前请修改为你的唯一包名

⚠️ **关于签名**  
- Debug 版本自动使用调试签名
- Release 版本需要配置签名密钥

## 🌐 相关资源

- [Jetpack Compose 官方文档](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [Android 开发者指南](https://developer.android.com/)
- [Kotlin 官方文档](https://kotlinlang.org/)

## 💡 提示

- 使用 `./gradlew --scan` 可以查看详细的构建分析
- 使用 `./gradlew build --info` 查看详细构建日志
- 修改 `gradle.properties` 可以调整构建性能

Happy Coding! 🤖✨
