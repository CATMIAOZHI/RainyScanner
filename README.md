# 🌧️ 雨晴扫描 (RainyScanner)

[![Build & Release](https://github.com/CATMIAOZHI/RainyScanner/actions/workflows/release.yml/badge.svg)](https://github.com/CATMIAOZHI/RainyScanner/actions)
[![Release](https://img.shields.io/github/v/release/CATMIAOZHI/RainyScanner)](https://github.com/CATMIAOZHI/RainyScanner/releases/latest)

基于 **Jetpack Compose + CameraX + ZXing** 的 Android 条码/二维码扫描应用，采用 Material Design 3 樱粉主题。

## ✨ 特性

- **Jetpack Compose** — 现代化声明式 UI
- **CameraX + ZXing** — 纯软件解码，支持多种条码格式
- **ML Kit Barcode** — 备选扫描方案（依赖已集成）
- **扫描历史** — SharedPreferences 本地持久化
- **Material Design 3** — 元气樱粉配色
- **Gradle Version Catalog** — 统一依赖管理

## 📦 下载

前往 [Releases](https://github.com/CATMIAOZHI/RainyScanner/releases/latest) 下载最新 APK，直接安装即可。

## 🛠️ 构建

### 环境要求

- JDK 17+
- Android SDK (compileSdk 35, minSdk 24)

### 命令

```bash
./gradlew assembleDebug      # Debug APK
./gradlew assembleRelease    # Release APK（需签名）
./gradlew installDebug       # 安装到设备
./gradlew clean              # 清理
```

生成的 APK 位于 `app/build/outputs/apk/`。

## 📁 项目结构

```
RainyScanner/
├── app/
│   ├── src/main/java/com/rainyscanner/app/
│   │   ├── MainActivity.kt              # 主入口 + 双屏导航
│   │   ├── data/ScanHistory.kt          # 扫描历史持久化
│   │   └── ui/
│   │       ├── screen/
│   │       │   ├── ScannerScreen.kt     # CameraX + ZXing 实时扫描
│   │       │   └── HistoryScreen.kt     # 历史记录
│   │       └── theme/
│   │           ├── Color.kt / Theme.kt / Type.kt
│   └── build.gradle.kts
├── gradle/libs.versions.toml            # 统一依赖版本
├── build.gradle.kts
├── settings.gradle.kts
└── .github/workflows/release.yml        # CI/CD 自动构建发布
```

## 🔧 技术栈

| 类别 | 技术 |
|------|------|
| UI | Jetpack Compose + Material3 |
| 相机 | CameraX |
| 解码 | ZXing + ML Kit Barcode |
| 语言 | Kotlin |
| 构建 | Gradle 8.x + AGP 8.7 |
| CI/CD | GitHub Actions |

## 📝 自定义

### 修改包名

1. 修改 `app/build.gradle.kts` 中的 `namespace` 和 `applicationId`
2. 重命名源码目录 `java/com/rainyscanner/app` 为你的包名结构
3. 更新 `AndroidManifest.xml` 中的引用

### 修改主题颜色

编辑 `app/.../ui/theme/Color.kt`，替换颜色值即可。

## 📄 许可

MIT License
