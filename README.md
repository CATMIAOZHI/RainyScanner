# 🌧️ 雨晴扫描 (RainyScanner)

[![Build & Release](https://github.com/CATMIAOZHI/RainyScanner/actions/workflows/release.yml/badge.svg)](https://github.com/CATMIAOZHI/RainyScanner/actions)
[![Release](https://img.shields.io/github/v/release/CATMIAOZHI/RainyScanner)](https://github.com/CATMIAOZHI/RainyScanner/releases/latest)

## 💡 为什么用雨晴扫描？

受够了系统相机的扫码体验？扫到链接**自动跳转浏览器**、扫到 WIFI 码静默连网、扫到电话直接拨出——你连看清楚内容的机会都没有。更别提某些 App 还会**拦截扫码结果**塞进自己的推广页。

**雨晴扫描**做一件事：**把控制权还给你**。

- 🔒 **不自动跳转** — 扫到 URL 不会直接打开浏览器，先让你看清链接再决定
- 📋 **原始内容可见** — 扫描结果完整展示，一键复制，不会被拦截篡改
- 🧠 **智能识别类型** — 自动标注 URL / WIFI / 电话 / 邮箱 / 地理位置等
- 📜 **本地历史** — 所有扫码记录保存在本地，随时回顾
- 🌸 **清爽樱粉 UI** — Material Design 3 主题，舒服不刺眼

## ✨ 技术特性

- **Jetpack Compose** — 现代化声明式 UI
- **CameraX + ZXing** — 纯软件解码，支持 QR / Code128 / EAN / Data Matrix / PDF417 / Aztec
- **ML Kit Barcode** — 备选扫描引擎（依赖已集成，可切换）
- **扫描历史** — SharedPreferences 持久化，不上传任何数据
- **Gradle Version Catalog** — 统一依赖管理

## 📦 下载

前往 [Releases](https://github.com/CATMIAOZHI/RainyScanner/releases/latest) 下载 APK 直接安装。

## 🛠️ 构建

**环境**：JDK 17+、Android SDK (compileSdk 35)

```bash
./gradlew assembleDebug      # Debug APK
./gradlew assembleRelease    # Release APK（已配置签名）
./gradlew installDebug       # 安装到设备
```

APK 输出：`app/build/outputs/apk/`

## 📁 项目结构

```
RainyScanner/
├── app/
│   ├── src/main/java/com/rainyscanner/app/
│   │   ├── MainActivity.kt              # 主入口 + 扫描/历史双屏导航
│   │   ├── data/ScanHistory.kt          # 扫描历史持久化
│   │   └── ui/
│   │       ├── screen/
│   │       │   ├── ScannerScreen.kt     # CameraX + ZXing 实时扫描
│   │       │   └── HistoryScreen.kt     # 历史记录
│   │       └── theme/
│   │           ├── Color.kt / Theme.kt / Type.kt
│   └── build.gradle.kts
├── gradle/libs.versions.toml            # 统一依赖版本
└── .github/workflows/release.yml        # CI/CD 自动构建发布
```

## 🔧 技术栈

| 类别 | 技术 |
|------|------|
| UI | Jetpack Compose + Material3 |
| 相机 | CameraX |
| 解码 | ZXing + ML Kit Barcode |
| 语言 | 100% Kotlin |
| 构建 | Gradle + AGP 8.7 |
| CI/CD | GitHub Actions |

## 📄 许可

MIT License
