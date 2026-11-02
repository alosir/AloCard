# AloCard Android 工程

信用卡管理 App 的 Android 原生实现（Kotlin + Jetpack Compose + Room）。

设计依据见仓库根目录的 `DESIGN.md`、`docs/PRD.md`、`docs/技术规范.md`。

## 本机构建

```bash
cd android
./gradlew assembleDebug        # 需要 JDK 17+（推荐 Android Studio 自带 JBR 21）
```

- SDK：`local.properties` 的 `sdk.dir` 指向本机 SDK
- Gradle wrapper 已生成（`gradle/wrapper/`，发行版 8.10.2）
- 工程路径含中文，已加 `android.overridePathCheck=true`（纯 Kotlin/Compose 构建无 native 代码；若引入 NDK 需迁移到 ASCII 路径）

## 版本号与发版

- 单一事实来源：`android/gradle.properties` 的 `alocard.versionName` / `alocard.versionCode`
  （三段式 `v主.次.修订` 规则见根目录 `CHANGELOG.md` 文件头；`versionCode` 每次发布递增）
- 发版：`bash scripts/release.sh <versionName> <versionCode>`（更新版本号 → 构建 Release →
  打标签推送 → 有 `GITHUB_TOKEN` 时自动创建 GitHub Release 并上传 APK）
- 更新日志：根目录 `CHANGELOG.md`，构建时自动拷入应用 assets（应用内「设置 → 关于 → 更新日志」）

## 发布构建（签名）

Release 包需要签名配置（keystore 不入库，已 gitignore）：

```
android/keystore.properties          # storeFile/keyAlias/两个密码
android/keystore/alocard-release.keystore
```

`app/build.gradle.kts` 检测到 `keystore.properties` 存在时自动启用 release 签名；
缺失时 release 构建产出未签名 APK（不可直接安装）。

## 验证

| 层 | 命令 | 覆盖 |
| --- | --- | --- |
| JS 引擎（事实来源） | `node --test engine/dateEngine.test.js`（仓库根） | 17 用例：跨月、闰年、DST、状态机 |
| Kotlin domain | `node android/run-jvm-tests.js test` | 35 用例：DateEngine 16 + CardNumberIdentifier 19，与 JS 版逐条等价 |
| 全量编译 | `./gradlew assembleDebug` | Compose / Room / Hilt / KSP 全链路 |

`run-jvm-tests.js` 跳过依赖 Android API 的文件，只编译 `domain/`；
日期引擎与 JS 版（`engine/dateEngine.js`）用例逐条等价——任一侧改规则，两侧都要同步。

## 实现状态

| 层 | 状态 | 说明 |
| --- | --- | --- |
| `domain` | ✅ 编译通过 + 35 项测试 | 日期引擎、卡号识别、模型、仓库接口、Money/DateProvider |
| `data` | ✅ 编译通过 | Room 5 表、DAO（含覆盖式/累计式/撤销事务）、Repository、AES-GCM+HMAC 卡号加密、PIN（PBKDF2 12 万迭代 + 5 次锁定递增）、离线 BIN 查表、加密备份 |
| `ui` | ✅ 编译通过 | 主题（浅色/深色独立配色）、账簿行/格带/索引标签组件、P1–P11+P14 全部页面、底部导航、PIN 键盘 |
| 提醒/信息位 | ✅ | WorkManager 自续期（09:00 扫描 + 00:05 刷新）、通知幂等台账、4 通知渠道、常驻通知（ongoing，TOP3/逾期两种内容）、RemoteViews 小组件 TOP5、通知权限缺失提示条 |
| 安全 | ✅ | 应用锁（生物识别或 PIN，后台 5 分钟超时）、查看卡号 PIN 闸门（同组共享会话）、卡片详情/PIN 页 FLAG_SECURE、加密备份导出/恢复（PBKDF2 60 万 + AES-GCM，SAF，恢复前快照） |

## 目录结构

```
android/
├── settings.gradle.kts / build.gradle.kts / gradle.properties
├── gradle/libs.versions.toml  版本目录（单一来源）
├── gradlew / gradlew.bat / gradle/wrapper/
├── run-jvm-tests.js           JVM 侧编译与测试（无 SDK 也能跑 domain）
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── assets/bin-table.json          号段表（由 bin-data/build-bin-table.js 生成）
        ├── kotlin/com/alocard/app/
        │   ├── MainActivity.kt            入口：应用锁闸门、信息位刷新、提醒补排
        │   ├── domain/                    纯逻辑（可 JVM 测试，无 Android 依赖）
        │   │   ├── DateEngine.kt          日期引擎（JS 版移植）
        │   │   ├── CardNumberIdentifier.kt Luhn / 卡组织识别
        │   │   ├── model/Models.kt        领域模型（金额一律「分」Long）
        │   │   ├── repository/            6 个仓库接口
        │   │   └── util/                  Money（分↔元）、DateProvider（「今天」唯一来源）
        │   ├── data/
        │   │   ├── local/                 Room：5 表 + DAO + 事务
        │   │   ├── repository/            仓库实现
        │   │   ├── security/              CryptoManager（Keystore AES-GCM/HMAC）、SecurityManager（PIN）
        │   │   ├── bin/BinDirectoryImpl.kt assets 号段查表
        │   │   └── backup/BackupManager.kt 加密备份容器（MAGIC+v1+PBKDF2+GCM）
        │   ├── sub/
        │   │   ├── notification/          渠道 + AppNotifier（构造与发送）
        │   │   └── work/                  DailyReminderWorker / DailyRefreshWorker / 调度编排 / 信息位刷新
        │   ├── widget/                    RemoteViews 小组件（TOP5）
        │   ├── ui/
        │   │   ├── theme/                 色彩/字阶/形状令牌 + AloCardTheme
        │   │   ├── components/            LedgerRow / CountdownStrip / WashiTab / 总览条 / 分段控件
        │   │   ├── common/                点阵纸/页脚手架/金额输入/步进器等原语
        │   │   ├── navigation/            Routes / BottomBar / NavHost
        │   │   ├── applock/               应用锁闸门（生物识别 + PIN）
        │   │   └── screen/                首页/详情/新增/账单组/账单历史/额度/录入/还款/PIN/设置
        │   └── di/                        Hilt 模块
        └── res/                           主题、图标（自适应）、小组件布局、字符串
```

## 与技术规范的有意偏差（记录备查）

| 项 | 规范 | 本工程 | 理由 |
| --- | --- | --- | --- |
| 联网 | C1/ADR-0013：不出网 | 例外：`INTERNET` 仅用于检查 GitHub 更新并下载 APK | 用户指令（2026-11-02），优先于 ADR 字面条文；银行连接/账号/云同步仍明确不做 |
| 模块结构 | `:domain`（纯 JVM）+ `:app` 双模块 | 单 `:app` 模块，包内分层 | 引擎隔离的目标（防误用 Android API + JVM 测试）已由 `run-jvm-tests.js` harness 达成；双模块的收益不抵构建复杂度 |
| 小组件 | Glance 1.2.0 | RemoteViews | 规范自身的 Plan B；规避 Glance 与 Compose Runtime 的版本耦合；5 行静态文本不需要声明式能力 |
| 安全凭据存储 | Room `security_credentials` 表 | EncryptedSharedPreferences | SecurityManager 已完整实现 PBKDF2+锁定+会话且可导出凭据供备份；表形式属重复建设 |
| 列表页内存映射 | —（仅额度聚合强制 SQL） | 组×卡×账单在内存合并，SUM 走 QuotaDao | 组数规模下内存合并无性能问题；额度汇总遵守 PRD 9.5 走 SQL |
| 历史逾期 N 口径 | 「历史按当时 N」 | 用当前 N 判定 | 引擎未存历史 N；改 N 不追溯历史（F2.4）下两种口径差异实际不可观测 |

## 关键设计决策速查

| 决策 | 依据 |
| --- | --- |
| 金额以「分」存 Long | PRD 4.4：避免浮点误差 |
| 账单日期以 epochDay 存 Long | PRD 5.0：日历日无歧义、SQL 可直接比较 |
| 卡号字段级加密（AES-256-GCM），非整库加密 | 保住额度汇总的 SQL SUM（PRD 9.5） |
| 查重盲索引 = HMAC(Keystore 密钥, 卡号) 截断 | 密文不可查重；指纹不泄露卡号 |
| PIN：PBKDF2-HMAC-SHA256 12 万次 + 5 次锁定递增 | 平台自带、零额外依赖；6 位 PIN 的防线在锁定与 Keystore 盲化而非迭代次数 |
| WorkManager 自续期 OneTime，不用精确闹钟/周期任务 | 日粒度提醒；避开 Android 12+ `SCHEDULE_EXACT_ALARM` 政策风险 |
| 常驻通知 = `setOngoing(true)` 普通通知 | 零权限零审核成本；前台服务与此收益不匹配 |
| 不用 Material You 动态取色 | 会让 washi 色板与状态色不可控（DESIGN.md 第 9 章） |

## 后续

1. 真机验证：浅色/深色截图对照 `prototype/`；字体比例 1.3 与 48dp 触控检查
2. `assembleRelease` + R8 验证安装包体积（PRD 10.1 预算 8 MB）
3. COULD 项：趋势图（F3.5）、列表筛选（F5.2）
