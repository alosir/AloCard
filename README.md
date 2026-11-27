<p align="center">
  <img src="android/app/src/main/res/drawable-nodpi/alocard_logo.png" alt="AloCard" width="96" />
</p>

# AloCard

本地优先的信用卡管理 App：管理账单日、还款日、免息期、账单与额度，并在消费前给出「今天该刷哪张卡」的参考。

[![License](https://img.shields.io/badge/license-MIT-gold)](LICENSE)
[![Release](https://img.shields.io/github/v/release/alosir/AloCard)](https://github.com/alosir/AloCard/releases/latest)
[![Android](https://img.shields.io/badge/platform-Android-3DDC84)](android/)

## 下载

- 官网：<https://card.alosir.com>
- GitHub Release：<https://github.com/alosir/AloCard/releases/latest>
- 阿里云镜像：<https://card.alosir.com/download/AloCard-latest.apk>

## 功能

- **免息期推荐**：按当天视角计算每张卡的最长免息期，并给出今日刷卡顺序。
- **账单管理**：手工录入账单金额，支持 0 元账单、部分还款、累计还款与撤销上次还款。
- **灵活账期**：账单日支持 1–31 号，无此日期自动取月末；还款日支持“账单日推后 N 天”或“每月固定日期”。
- **同银行合并**：同银行多张卡共享账期与额度；额度页按银行聚合，多卡只取最高额度。
- **账单历史**：按期保存账单记录，支持剩余待还、已结清、逾期等状态。
- **桌面组件**：今日免息期与近期待还组件，随尺寸自动调整展示数量，并在数据变化后自动刷新。
- **拍照识别**：手动拍照识别卡号与有效期；识别结果本地处理，不上传。
- **图标库**：内置主流银行 Logo，也支持导入本地图片并圆形裁剪。
- **安全保护**：4 位数字密码、指纹解锁、卡号 AES-256-GCM 加密存储、加密备份与恢复。
- **离线识别**：内置银行卡号段表，支持号段预览与搜索；识别不到时可手动修改。
- **应用内更新**：从 GitHub Release 检查并下载新版本。

## 隐私原则

AloCard 不连接银行、不需要账号、不做云同步。账单、卡号、还款记录和自定义图标均保存在本机；卡号使用 AES-256-GCM 加密存储。唯一联网能力是检查应用更新。

## 技术栈

- Kotlin + Jetpack Compose
- Room + Hilt + WorkManager
- CameraX + ML Kit 文字识别
- RemoteViews 桌面小组件
- 本地日期引擎：JS 事实来源 + Kotlin 等价移植

## 构建

需要 JDK 17+（推荐 Android Studio 自带 JBR 21）与 Android SDK（compileSdk 35）。

```bash
cd android
./gradlew assembleDebug
./gradlew assembleRelease   # 需要本地签名配置
```

## 测试

```bash
node --test engine/dateEngine.test.js
node android/run-jvm-tests.js test
```

日期引擎以 `engine/dateEngine.js` 为事实来源，Kotlin 版本与它使用同一组核心用例。

## 仓库结构

| 目录 | 内容 |
| --- | --- |
| `android/` | Android 应用源码、资源、Room schema 与 JVM 测试 |
| `engine/` | 日期引擎事实来源、测试与内置号段表 |
| `bin-data/` | 号段表源数据、官方来源补充与生成脚本 |
| `scripts/` | GitHub 同步与发版脚本 |

## 数据与素材来源

- 号段表基础数据来自开源项目 [hexindai/bcbc](https://github.com/hexindai/bcbc)（MIT），并补充了银行官网公告中可核验的号段。
- 银行 Logo 来自开源图标素材与用户提供的素材；商标归各银行所有，仅用于发卡行识别展示。

## 贡献

欢迎提交 Issue 和 Pull Request。请确保：

- 日期规则同时更新 JS 与 Kotlin 两侧；
- 金额以“分”为单位存储；
- 不提交个人账单数据、签名文件或构建产物。

## License

[MIT](LICENSE)

## 版本记录

见 [CHANGELOG.md](CHANGELOG.md)。