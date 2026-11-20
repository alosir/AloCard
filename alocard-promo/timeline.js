// timeline.js — the single source of truth for time. Imported by the page (picture) AND score.mjs (sound),
// so every cut, hit and sound effect lands on the same number. Plain ES module: no DOM, no Node APIs.

export const BPM = 120;                 // 4/4, one bar = 2 s, 15 bars = 30 s
export const BEAT = 60 / BPM;
export const BAR = BEAT * 4;
/** beat number → seconds (fractions allowed: b(4.5) is the "and" of beat 5) */
export const b = n => n * BEAT;
/** When to START a transition so the new picture first shows ON the hit (a beat / bar time).
 *  reveal = how far into the transition the new picture first becomes visible (0 = at once; 0.3 = an iris that opens
 *  after a third). Started on the beat, a transition reveals late and reads as "the sound came first". */
export const revealOn = (hit, dur, reveal = 0) => hit - reveal * dur;

export const DUR = b(60); // 30 s = 15 bars @ BPM 120

// Shots / sections on the grid (see brief.md <structure>).
export const SHOTS = [
  { id: 'hook',       t0: b(0),  t1: b(8) },   // 0–4s   三张卡扇形铺开 + 「今天刷哪张？」
  { id: 'reveal',     t0: b(8),  t1: b(16) },  // 4–8s   产品亮相
  { id: 'sell-sort',  t0: b(16), t1: b(28) },  // 8–14s  卖点1 免息期 20↔50 对比 + 推荐置顶
  { id: 'sell-count', t0: b(28), t1: b(40) },  // 14–20s 卖点2 还款倒计时 · 账簿格带
  { id: 'sell-priv',  t0: b(40), t1: b(48) },  // 20–24s 卖点3 不出网 · 卡号加密
  { id: 'recap',      t0: b(48), t1: b(52) },  // 24–26s 汇总
  { id: 'endcard',    t0: b(52), t1: DUR },    // 26–30s 落版定格
];

// Picture events that need a sound. The page reads them for flashes / kicks, the score for SFX.
export const CUES = [
  { t: b(1),   sfx: 'pop' },      // hook: 问题砸下
  { t: b(8),   sfx: 'whoosh' },   // 手机升起（产品亮相）
  { t: b(20),  sfx: 'impact' },   // 签名：三卡按免息期重排落定
  ...[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11].map(i => ({ t: b(28.5) + i * BEAT, sfx: 'tick' })), // 账簿格带逐格走
  { t: b(44),  sfx: 'thunk' },    // 锁合拢
  { t: b(52),  sfx: 'shimmer' },  // 落版
];

// What the QA tools should know you meant:
// deliberate still moments — the end card holds to the end (intended, not dead air)
export const HOLDS = [[b(53), DUR]];
// the big hits that should breathe before they land（落版是平静品牌定格，非 drop，不入此列）
export const DROPS = [b(20)];
