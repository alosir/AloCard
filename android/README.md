# AloCard Android

AloCard 的 Android 原生实现，使用 Kotlin、Jetpack Compose、Room 与 Hilt。

## 构建

需要 JDK 17+（推荐 Android Studio 自带 JBR 21）与 Android SDK（compileSdk 35）。

```bash
cd android
./gradlew assembleDebug
./gradlew assembleRelease
```

- `local.properties` 中的 `sdk.dir` 指向本机 Android SDK。
- Release 签名需要 `keystore.properties` 与 `keystore/alocard-release.keystore`，二者均不入库。

## 测试

```bash
node --test engine/dateEngine.test.js
node android/run-jvm-tests.js test
```

`run-jvm-tests.js` 只编译不依赖 Android API 的 `domain/` 代码，可在没有 Android SDK 的环境运行核心规则测试。

## 目录结构

```text
app/src/main/
├── assets/bin-table.json          # 离线银行卡号段表
├── kotlin/com/alosir/card/
│   ├── MainActivity.kt            # 入口、应用锁、信息位刷新
│   ├── domain/                    # 日期引擎、卡号识别、OCR 解析、聚合规则
│   ├── data/                      # Room、加密、备份、号段、图标存储
│   ├── sub/                       # 通知渠道、WorkManager、每日刷新
│   ├── widget/                    # RemoteViews 桌面组件
│   ├── ui/                        # Compose 页面、组件、主题、导航
│   └── di/                        # Hilt 模块
└── res/                           # 主题、银行图标、桌面组件布局
```

## 数据与日期规则

- 金额以“分”为单位存储为 `Long`。
- 日历日以 `epochDay` 存储为 `Long`。
- 日期推导只允许出现在日期引擎；JS 版与 Kotlin 版必须保持行为一致。
- 账单日支持 1–31 号；无此日期自动取月末。
- 还款日支持账单日推后 N 天或每月固定日期。
- 0 元账单自动视为已还清。

## 发版

版本号单一事实来源是 `android/gradle.properties`：

```bash
bash scripts/release.sh <versionName> <versionCode>
```

发版脚本会更新版本号、构建 Release APK、推送标签，并在 `GITHUB_TOKEN` 有效时创建 GitHub Release 与上传 APK。更新日志以根目录 `CHANGELOG.md` 为准，构建时同步进应用。