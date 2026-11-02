# AloCard 工作区指令

## GitHub 自动同步（每次更新后必做）

任何代码/文档变更完成后，运行：

```bash
bash scripts/sync-github.sh "提交说明"
```

脚本自动 `git add -A` + commit + push 到 `git@github.com:alosir/AloCard.git`（main 分支，SSH 密钥已配置）。
不带参数时使用带时间戳的默认提交信息。

## 发布新版本

```bash
bash scripts/release.sh <versionName> <versionCode>   # 如：release.sh 1.1.0 2
```

脚本会：更新 `android/gradle.properties` 版本号 → 构建 Release APK → 复制到 `releases/` →
打 `v<versionName>` 标签并推送 → 若环境变量 `GITHUB_TOKEN` 有效则自动创建 GitHub Release 并上传 APK，
否则输出网页端手动发布指引。

发版前记得在 `CHANGELOG.md` 顶部补充该版本的更新条目（格式见文件头注释）。

## 仓库边界（开源准备，勿越界）

- **不上传**：产品设计与技术实施文档（`PRODUCT.md` `DESIGN.md` `CONTEXT.md` `docs/`）、`.impeccable/`、`.workbuddy/`、
  签名 keystore（`android/keystore/`）、构建产物与 APK（`releases/`）。
- 根目录 `README.md`、`CHANGELOG.md`、各子目录 README 正常维护并入库。
- 更新日志为单一事实来源：根 `CHANGELOG.md` 在构建时拷入应用 assets，应用内「更新日志」展示同一份内容。

## 工程要点

- 日期计算唯一事实来源是 `engine/dateEngine.js`；Kotlin 移植版必须与它行为一致（两侧同一组测试用例）。
- 金额一律「分」存 Long；日历日一律 epochDay 存 Long；任何日期推导只允许出现在 DateEngine。
- 构建：`cd android && ./gradlew assembleDebug`（JDK 用 Android Studio 自带 JBR 21）。
