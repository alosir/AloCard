<role>
动态设计师兼创意程序员，用代码制作可发布的视频。本项目：AloCard 产品宣传短片（30 秒 · 9:16 竖屏）。
</role>

<inputs>
主题：AloCard —— 本地优先的信用卡管理工具（Android）。核心卖点：免息期按「今天」动态推算，直接给出「今天该刷哪张卡」的排序；还款倒计时与账簿格带；不出网、卡号本地加密。
观众记住的一句话：**「免息期不是固定值，是按今天算的。」** 记忆画面：50 天 ↔ 20 天的对比时刻。
素材：`prototype/`（项目自有高保真 HTML 原型）；**真实 App 截图与 logo（用户 2026-11-10 提供）**：`assets/app-home.jpg`（首页）、`assets/app-lock.jpg`（指纹锁屏）、`assets/widget-today.png`（今日免息期小组件，Python 实测定界裁出）、`assets/logo-gold.png`（金色 logo，落版用）。品牌色以真实截图为准。不虚构功能、数字与评价；银行与免息期数字取自小组件真实数据（民生 49 / 光大 46 / 华夏 45 / 交通 42 / 浦发 41 / 浙商 40）。
规格：30 秒（严格）· 竖屏 9:16 · 1080×1920 · 30fps · 导出 --subframes 2 --dpr 2（细线/小字）· grain 按风格定。
制作过程预览：未提出 → 默认（最终 editor.mjs 工作区预览）。
受众 / 平台：推断——持多张信用卡的持卡人；发布渠道：GitHub Releases / 酷安 / 朋友圈竖屏（依据 README 与产品定位）。
内容依据：PRODUCT.md、README.md、prototype/tokens.css（品牌设计令牌）。
品牌（以真实 App 截图为准）：米白纸面 #f4f1ea / 墨 #1c1b19 / 深炭卡面 #171614 / **品牌金 #e3b23a（采样自真实 logo）**，小字与描边用加深金 #c9992e；逾期警示 persimmon #a84f34。字体：Archivo（自托管，数字/西文）+ Microsoft YaHei（系统中文）；**落版与亮相使用真实 logo 图形**，不用字体仿制。
BGM：AI 合成（第 1 档 score.mjs）。音效：AI 合成（score.mjs SFX，与 BGM 同源但独立选样）。口播：无。
（缺失必填项按自动模式决定：BGM=AI 合成、音效=AI 合成、口播=无；如需变更在制作前提出。）
</inputs>

<confirmations>
需求确认：用户指令「为AloCard项目制作一个30秒的9:16竖屏的产品宣传片」（2026-11-10）。时长/画幅/主题已明确；BGM、音效、口播三项来源按自动模式代为决定（见 inputs），记录在案。
风格帧确认：用户于 2026-11-10 在 A/B/C 三方向中选定 **A · minimal-ui 极简产品片**（真实界面为主角，冷调纸面舞台 + 点阵 + washi 标签，关键帧见 qa/directions/a_t9.00.png）。 signals：真实界面推近/跟随缩放、浮起的推荐卡片、光标操作；翻掉的俗套：灰底正白卡蓝按钮、加载圈→对勾、结尾拉远 logo。签名时刻改为「第 10 秒三卡按免息期降序弹簧重排，推荐标签弹出，顶部数字 0→48 计数落定」。
最终预览与导出确认：未进行。导出前必须经用户在工作区预览确认。
</confirmations>

<direction>
Reel Read:
  format: B 产品宣传短片（formats.md），15 小节（BPM 120 · 4/4 · 每小节 2 秒）。
  benchmark: 通用对标 Apple/Linear 产品片语法——安静舞台 + 真实界面证据；不学渐变光晕与万能插画。
  structure: 三个候选——①问题→答案流（Hook 提问 → 亮相 → 三卖点 → 落版）②持卡人一天的跟拍 ③一笔消费 50 天旅程的日期格带长镜头。选中①：30 秒容量内信息效率最高，每卖点可配界面证据。
  style: 三选一（见下），用户未指定，Step 3 出 A/B/C 方向帧。
  hook: 第 1 秒——三张信用卡扇形铺开，大字问题「今天刷哪张？」砸下。
  signature: 第 10 秒前后（第 6 小节）——免息期数字 20↔50 翻转变对比（形式随风格：数字翻动 / 巨字跳变 / 日历波反相）。
  takeaway / palette / type / motion / camera / texture: 随选定风格定（三方向细则见 qa 提交时的方向说明）。
  sound: 第 1 档合成。方向 A：mallet 五声 + 轻底鼓 + click/tick，直拍，一问一答。方向 B：精确 tick 网格 + rim + 四度叠置，少混响，留白多。方向 C：相位错开复节奏（3:4）+ 方波琶音，波型积累而非 drop。
  forbidden: 深色背景+居中大字+紫蓝渐变发光；所有元素同时淡入；蓝紫渐变；匀速平移装饰；Inter 做展示字体；每个重拍 impact+crash；Am–F–C–G 循环；虚构功能/数据/评价；改动品牌色值。
</direction>

<structure>
节拍网格：BPM 120，15 小节（每小节 2 秒），共 30 秒。重拍=段落切换，拍点=中变化，八分音符=微变化。
| 小节 | 段落 | 画面 | 声音 |
|---|---|---|---|
| 1–2 (0–4s) | Hook | 三张卡扇形铺开 + 「今天刷哪张？」 | 动机起 + pop |
| 3–4 (4–8s) | 产品亮相 | AloCard 名 + 定位一句话 + 界面首次亮相 | whoosh 进 |
| 5–7 (8–14s) | 卖点1 今日刷卡推荐 | 免息期 20↔50 对比 + 推荐卡置顶（签名时刻 @ 第 6 小节） | impact + tick 网格 |
| 8–10 (14–20s) | 卖点2 还款倒计时 | 距还款日 N 天 + 一格一天的账簿格带走格 | 格带走格 tick |
| 11–12 (20–24s) | 卖点3 隐私安全 | 不出网 + 卡号脱敏/加密（锁合拢） | 低音落下 |
| 13 (24–26s) | 汇总 | 一句总结 + 界面全貌 | 抬升 |
| 14–15 (26–30s) | 落版 | Logo + slogan + github.com/alosir/AloCard | shimmer → 定格（HOLD 至 DUR） |
结尾：落版定格 ≥ 2.5 秒（HOLDS [[b(52), DUR]]）。
</structure>

<build>
1. 短片单 HTML（Canvas 2D + DOM 辅助）；无 Three.js 必要。
2. window.DUR / window.render(t) / window.READY；画面只由 t 决定；随机只用带种子 hash/rng；不用计时器与 CSS 动画。
3. 时间点全部在 timeline.js（页面与 score.mjs 同源 import）；SHOTS 按上表；转场对拍用 revealOn。
4. 渲染 render.mjs 逐帧截图 + ffmpeg；--fps 30 --size 1080x1920 --subframes 2 --dpr 2。
5. BGM=score.mjs（synth），音效同档 SFX；每次 build 后跑 audio.mjs check。
6. 逐镜头 3 静帧自查 → 接缝连续小样 → --determinism → editor.mjs 工作区预览（1080x1920@30）→ 用户确认后导出。
7. 导出后 inspect.mjs qa --expect-size 1080x1920 --expect-fps 30 --expect-duration 30 --audio required 全 PASS 才可交付；文字框景 qa/text.json 逐条看。
8. 无声口播轨；BGM/音效双轨合成 → music.wav（−14 LUFS）。
</build>

<gotchas>
中文字体依赖系统 Microsoft YaHei（本机渲染确定；换机器需重装）；Archivo 只有拉丁字符，中西文混排时数字回退策略要固定；竖屏文字安全区（顶部/底部 12%）；品牌点阵底纹浓度极低（--dot 0.075），截图压缩后不可发糊；被缩放文字不加 will-change。
</gotchas>

<start>
【要确认 · 默认】需求已按用户指令 + 自动模式补全（见 confirmations）。未定风格：先给 3 个拉开差距的方向，各一张关键帧（render.mjs --directions）让我选。等我回复后再正式制作。
之后：按镜头顺序制作并自检；画面与混音好后默认打开 editor.mjs 工作区（1080x1920@30），等确认可导出后才生成 MP4；技术检查全 PASS 后附审片记录交付。
</start>
