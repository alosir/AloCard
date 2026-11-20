// score.mjs — AloCard promo · minimal-ui 声音方向（brief §sound）：
// mallet 五声问答 + 轻底鼓 + click/tick，直拍，Lydian 一点亮。C 大调五声，maj7 铺底。
// hook 提问（高 mallet 动机）→ reveal 回答（低 pluck/kalimba）；sell-sort 进轻 groove，
// 签名 impact 一次；sell-count 收成 tick 网格 + 单音 ostinato；sell-priv 近乎裸 pad；
// recap 抬升 → endcard Cmaj7 定格到片尾。
import { DUR, BPM, CUES } from './timeline.js';
export { DUR };

const P5 = ['C', 'D', 'E', 'G', 'A'];

export default function score(S) {
  S.tempo(BPM);
  const drums = S.bus('drums', { gain: 0.55 });
  const bass = S.bus('bass', { gain: 0.6, duck: 0.5 });
  const music = S.bus('music', { gain: 0.42, reverb: 0.32, duck: 0.55, highpass: 160 });
  const sfx = S.bus('sfx', { gain: 0.85, reverb: 0.16 });

  const m = (n, dur = 1.2, o) => S.mallet(n, dur, o);
  const roots = [
    ['C2', 'maj7'], ['A1', 'min11'], ['F1', 'maj9'], ['G1', '6'],
    ['C2', 'maj7'], ['F1', 'maj9'], ['G1', 'sus4'], ['A1', 'min11'],
    ['F1', 'maj9'], ['G1', '6'], ['A1', 'min11'], ['F1', 'maj9'],
    ['G1', 'sus4'], ['C2', 'maj7'], ['C2', 'maj7'],
  ];
  const kickSoft = { decay: 0.22 };

  for (let bar = 0; bar < 15; bar++) {
    const t = S.bar(bar), [root, q] = roots[bar];
    const padT = bar === 14 ? DUR - t + 1.6 : S.bar(1) * 0.98;
    music.add(S.pad(S.chord(S.midi(root) + 12, q), padT, { cutoff: bar >= 10 && bar <= 11 ? 1100 : 2000 }), t);

    if (bar >= 1) bass.add(S.bass(root, S.bar(1) * 0.9, { cutoff: 460, sub: 0.35 }), t);

    for (let k = 0; k < 4; k++) {
      const bt = t + S.beat(k);
      const preHit = bar === 4 && k === 3;                       // 签名前的半拍留呼吸
      const backBeat = bar >= 2 && bar !== 10 && bar !== 11 && bar !== 14;
      if (backBeat && !preHit) drums.add(S.kick(kickSoft), bt, { gain: bar < 4 ? 0.4 : 0.55 });
      if (bar >= 2 && bar !== 10 && bar !== 11 && !preHit) drums.add(S.hat({ decay: 0.04 }), bt + S.beat(0.5), { gain: 0.32, pan: 0.18 });
      if (bar >= 4 && bar <= 6 && !preHit) {                     // sell-sort 轻 groove
        drums.add(S.hat({ decay: 0.03 }), bt + S.beat(0.25), { gain: 0.16, pan: -0.15 });
        if (k % 2 === 1) drums.add(S.clap(), bt, { gain: 0.28 });
      }
      if (bar >= 7 && bar <= 9 && (k === 1 || k === 3)) drums.add(S.rim(), bt, { gain: 0.4 });   // 格带网格
    }

    // ── mallet 声部：一问一答 ──
    if (bar === 0) { music.add(m('E6'), t, { gain: 0.7 }); music.add(m('D6'), t + S.beat(1.5), { gain: 0.55 }); }
    if (bar === 1) { music.add(m('C6'), t, { gain: 0.6 }); music.add(m('A5', 1.6, { material: 'kalimba' }), t + S.beat(2), { gain: 0.6 }); }
    if (bar === 2) ['G5', 'A5', 'C6'].forEach((n, i) => music.add(m(n, 1, { material: 'kalimba' }), t + S.beat(i), { gain: 0.55 }));
    if (bar === 3) music.add(m('E6', 1.4), t + S.beat(1), { gain: 0.5 });
    if (bar >= 4 && bar <= 6) {                                    // 五声 ostinato
      const seq = ['C6', 'E6', 'G6', 'A6', 'G6', 'E6', 'D6', 'E6'];
      seq.forEach((n, i) => music.add(m(n, 0.5), t + S.beat(i / 2), { gain: 0.3, pan: i % 2 ? 0.2 : -0.2 }));
    }
    if (bar >= 7 && bar <= 9) for (let i = 0; i < 8; i++) music.add(m('E6', 0.35, { material: 'xylo' }), t + S.beat(i / 2), { gain: 0.16, pan: 0.1 });
    if (bar === 12) ['C6', 'E6', 'G6', 'A6'].forEach((n, i) => music.add(m(n, 0.8), t + S.beat(i / 2), { gain: 0.4 }));
    if (bar === 13) { music.add(m('C6', 2), t, { gain: 0.5 }); music.add(m('E6', 2), t + S.beat(2), { gain: 0.45 }); }
    if (bar === 14) music.add(m('G6', 3, { material: 'vibes' }), t, { gain: 0.45 });
  }

  // ── 事件音 ──
  for (const c of CUES) {
    if (c.sfx === 'pop') sfx.add(S.pop('C6'), c.t, { gain: 0.55 });
    if (c.sfx === 'tick') sfx.add(S.tick(), c.t, { gain: 0.4, pan: Math.sin(c.t * 7) * 0.4 });
    if (c.sfx === 'whoosh') sfx.add(S.whoosh(0.6), c.t - 0.42, { gain: 0.6 });
    if (c.sfx === 'impact') { sfx.add(S.impact(), c.t, { gain: 0.85 }); drums.add(S.crash(), c.t, { gain: 0.22 }); S.duck(c.t - 0.5, 0.5); S.duck(c.t, 1); }
    if (c.sfx === 'thunk') { sfx.add(S.kick({ decay: 0.55 }), c.t, { gain: 0.9 }); S.duck(c.t, 0.5); }
    if (c.sfx === 'shimmer') sfx.add(S.shimmer(1.8), c.t, { gain: 0.45 });
  }
}
