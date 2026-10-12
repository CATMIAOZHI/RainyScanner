# 🌧️ 雨晴扫描 (RainyScanner)

[![Build & Release](https://github.com/CATMIAOZHI/RainyScanner/actions/workflows/release.yml/badge.svg)](https://github.com/CATMIAOZHI/RainyScanner/actions)
[![Release](https://img.shields.io/github/v/release/CATMIAOZHI/RainyScanner)](https://github.com/CATMIAOZHI/RainyScanner/releases/latest)

## 💡 为什么用雨晴扫描？

受够了平台之间的二维码封锁？**微信扫抖音码直接被拦截**、淘宝链接在微信里打不开、各种 App 扫码后强行跳自家内置浏览器——你连二维码里到底是什么都不知道。

这些 App 不是在做扫码，是在做**流量围墙**：扫到竞品链接 → 屏蔽，扫到自己平台的链接 → 劫持进自家生态。

**雨晴扫描**打破这堵墙。它不隶属于任何平台，只是一个纯粹的扫码工具：

- 🚫 **不拦截任何链接** — 微信、抖音、淘宝…所有链接一视同仁，扫出来原样展示
- 🧠 **智能识别类型** — 自动标注 URL / WIFI / 电话 / 邮箱等，你自己决定要不要打开
- 📋 **原始内容可见** — 先看清再操作，一键复制，不会被篡改或屏蔽
- 🔐 **通行密钥（Passkey / FIDO）直接放行** — 扫到 `FIDO:/…` 登录二维码，一键交给系统（Google Play 服务的 passkey 提供方）走官方混合登录流程；本 App 不解析、不代理，凭据始终留在系统里
- 📜 **本地历史** — 所有记录保存在本机，随时回顾
- 🌸 **清爽樱粉 UI** — Material Design 3 主题，干净不打扰

## ✨ 技术特性

- **Jetpack Compose** — 现代化声明式 UI
- **CameraX + ZXing** — 纯软件解码，支持 QR / Code128 / EAN / Data Matrix / PDF417 / Aztec
- **ML Kit Barcode** — 备选扫描引擎（依赖已集成，可切换）
- **扫描历史** — SharedPreferences 持久化，不上传任何数据
- **FIDO / Passkey** — 识别 `FIDO:/` 混合登录链接并交给系统 passkey 提供方（Google Play 服务）处理
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
│   │   ├── util/SystemOpener.kt         # 交给系统打开（URL / FIDO passkey）
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
