# AloCard

本地优先的信用卡管理工具：管住院账单日、最后还款日与免息期，并在每次消费前给出「今天该刷哪张卡」的免息期排序建议。

**不连接银行、没有账号、不出网**——账单金额与还款记录全部由持卡人手工录入；卡号本地加密存储。唯一的网络用途是检查应用更新（GitHub Release），可自行断开使用。

## 功能

- **今日刷卡推荐**：免息期是「今天视角」的动态值（账单日当天最短、次日最长），按免息期降序即推荐顺序
- **还款倒计时**：距还款日天数 + 到期/逾期文字标记 + 一格一天的账簿格带
- **账单组模型**：同银行多卡共享账期与额度，不重复计数
- **账单历史**：按期滚动存档，覆盖式录入、累计式还款、可撤销上次标记
- **额度总览**：总授信 / 已用 / 剩余可用（诚实标注估算口径）
- **卡号安全**：AES-256-GCM 加密存储，默认脱敏，查看需 6 位安全密码，支持生物识别应用锁
- **提醒**：还款前 3/1/0 天、账单日次日录入引导、卡片到期前 30 天；小组件与常驻通知展示今日免息期 TOP5
- **加密备份**：PBKDF2 + AES-256-GCM 容器，可跨设备恢复
- **在线更新**：检查 GitHub 最新 Release 并下载安装（应用内完成）

## 下载

最新 Release APK（含更新日志）：<https://github.com/alosir/AloCard/releases/latest>

## 构建

需要 JDK 17+（推荐 Android Studio 自带 JBR 21）与 Android SDK（compileSdk 35）。

```bash
cd android
./gradlew assembleDebug      # 调试包
./gradlew assembleRelease    # 发布包（需签名配置，见 android/README.md）
```

## 验证

```bash
node --test engine/dateEngine.test.js   # 日期引擎（事实来源）17 用例
node android/run-jvm-tests.js test      # Kotlin 域层 35 用例，与 JS 版逐条等价
```

## 仓库结构

| 目录 | 内容 |
| --- | --- |
| `android/` | Android 应用本体（Kotlin + Jetpack Compose + Room），详见 [android/README.md](android/README.md) |
| `engine/` | 日期引擎 JS 事实来源 + 测试 + 离线号段表 |
| `prototype/` | 高保真 HTML 交互原型（双击 `index.html` 即可打开） |
| `bin-data/` | 号段表生成脚本与上游数据（bcbc 数据集，MIT） |

## 版本与更新日志

- 版本号规则：三段式 `v主版本.次版本.修订号`；`versionCode` 每次发布递增
- 更新日志：[CHANGELOG.md](CHANGELOG.md)（应用内「设置 → 更新日志」同步展示）

## 许可证

源码以 MIT 协议开源（号段数据来自 [bcbc](https://github.com/)。详细许可见 `engine/bin-table.json` 的 meta 与 `bin-data/`）。
