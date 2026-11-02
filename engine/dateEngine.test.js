'use strict';

/**
 * AloCard 账期日期计算引擎 —— 单元测试
 *
 * 运行：node --test engine/dateEngine.test.js
 *
 * 测试开始前把进程时区固定为 Asia/Shanghai：
 *  1) 让所有「日历日」断言与运行机器的时区无关（结果可复现）；
 *  2) 让用例 11 的「带时区偏移的 Date」在本机得到确定的本地日历日。
 * 跨时区/夏令时的额外回归用子进程（带 TZ 环境变量）验证，见「跨时区与夏令时回归」。
 */

process.env.TZ = 'Asia/Shanghai';

const test = require('node:test');
const assert = require('node:assert');
const { execFileSync } = require('node:child_process');
const path = require('node:path');

const {
  nextBillingDate,
  currentBillingDate,
  gracePeriod,
  daysUntilDue,
  dueStatus,
  billStatus,
  cycleLength,
  validateOffset,
  groupCompleteness,
} = require('./dateEngine');

/** 把 Date 格式化为本地日历日 'YYYY-MM-DD' */
function ymd(date) {
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${m}-${d}`;
}

const ENGINE_PATH = path.join(__dirname, 'dateEngine.js');

/** 在指定时区下跑一段脚本，返回其 stdout（用于跨时区/夏令时回归） */
function runInTimeZone(tz, script) {
  return execFileSync(process.execPath, ['-e', script], {
    env: { ...process.env, TZ: tz },
    encoding: 'utf8',
  }).trim();
}

// ---------------------------------------------------------------------------
// 用例 1：账单日 5 号、N=20、参考日 2026-03-06
//
// 推导：
//   · 消费所属账单日（规则 1）：满足 billDate >= 03-06 的最小账单日。
//     当月账单日 03-05 已过（严格早于 03-06），故计入下一期 2026-04-05。
//   · 免息期（规则 3）= (2026-04-05 + 20 天) − 2026-03-06
//                    = 2026-04-25 − 2026-03-06 = 50 天。
//   · 本期账单日（规则 2）：满足 billDate <= 03-06 的最大账单日 = 2026-03-05。
//   · 距还款日（规则 4）= (2026-03-05 + 20 天) − 2026-03-06
//                    = 2026-03-25 − 2026-03-06 = 19 天。
// ---------------------------------------------------------------------------
test('用例 1｜账单日 5 号、N=20、ref=2026-03-06', () => {
  const ref = '2026-03-06';
  assert.equal(ymd(nextBillingDate(5, ref)), '2026-04-05');
  assert.equal(gracePeriod(5, 20, ref), 50);
  assert.equal(ymd(currentBillingDate(5, ref)), '2026-03-05');
  assert.equal(daysUntilDue(5, 20, ref), 19);
});

// ---------------------------------------------------------------------------
// 用例 2：参考日恰好是账单日当天（账单日 5 号、ref=2026-03-05）
//
// 规则 1 与规则 2 在此重合，都等于 D 本身：
//   · next = current = 2026-03-05（账单日当天的消费计入以该账单日出账的本期账单）。
//   · 免息期 = (03-05 + 20) − 03-05 = 20 = N（最短点，见规则 3 自检）。
//   · 距还款日 = (03-05 + 20) − 03-05 = 20 = N。
// ---------------------------------------------------------------------------
test('用例 2｜参考日恰好是账单日当天：next = current = D，grace = daysUntilDue = N', () => {
  const ref = '2026-03-05';
  assert.equal(ymd(nextBillingDate(5, ref)), '2026-03-05');
  assert.equal(ymd(currentBillingDate(5, ref)), '2026-03-05');
  assert.equal(gracePeriod(5, 20, ref), 20);
  assert.equal(daysUntilDue(5, 20, ref), 20);
});

// ---------------------------------------------------------------------------
// 用例 3：账单日 5 号、N=20、ref=2026-03-04（账单日前一天）
//
//   · next = 2026-03-05（当月账单日尚未到，>= D）。
//   · 免息期 = (2026-03-05 + 20) − 2026-03-04 = 2026-03-25 − 03-04 = 21 天。
// ---------------------------------------------------------------------------
test('用例 3｜ref=2026-03-04：next=2026-03-05，grace=21', () => {
  assert.equal(ymd(nextBillingDate(5, '2026-03-04')), '2026-03-05');
  assert.equal(gracePeriod(5, 20, '2026-03-04'), 21);
});

// ---------------------------------------------------------------------------
// 用例 4：跨月边界（账单日 28 号、2 月只有 28 天）
//
// 按规则 1（billDate >= D 的最小账单日，等于也算）：
//   · ref=2026-01-29 → 当月账单日 01-28 已过 → next = 2026-02-28（2 月 28 天）。
//   · ref=2026-01-28（恰为账单日当天）→ 规则 1/2 均等于 D 本身 → next = 2026-01-28。
//     （任务清单在此处的字面期望是「账单日当天 → 次月」，与规则 1、规则 2 的显式
//       表述以及 ADR 0001「账单日当天免息期最短 ≈ N」冲突，按规则实现，详见 README。）
//   · cycleLength：本期账单日 2026-01-28 → 下一账单日 2026-02-28 = 31 天；
//     而 2026-02-28 → 2026-03-28 = 28 天（2 月短月）。
// ---------------------------------------------------------------------------
test('用例 4｜跨月边界：账单日 28 号', () => {
  assert.equal(ymd(nextBillingDate(28, '2026-01-29')), '2026-02-28');
  assert.equal(ymd(nextBillingDate(28, '2026-01-28')), '2026-01-28');
  assert.equal(ymd(currentBillingDate(28, '2026-01-29')), '2026-01-28');
  assert.equal(cycleLength(28, '2026-01-29'), 31);
  assert.equal(cycleLength(28, '2026-02-28'), 28);
  // 跨月后的免息期：(2026-02-28 + 20) − 2026-01-29 = 2026-03-20 − 01-29 = 50 天
  assert.equal(gracePeriod(28, 20, '2026-01-29'), 50);
  // 规则 1 与规则 2 在账单日当天重合
  assert.equal(ymd(nextBillingDate(28, '2026-01-28')), ymd(currentBillingDate(28, '2026-01-28')));
});

// ---------------------------------------------------------------------------
// 用例 5：闰年（2028 年 2 月有 29 天）
//
// 按规则 1：账单日固定为 28 号，且不做月末兜底（ADR 0002），
// 所以 2 月的账单日是 2028-02-28，而不是 2028-02-29（后者需要 29/30/31 号，
// 已超出 billingDay 合法区间）。闰年的真实影响体现在账单周期长度上：
//   2028-02-28 → 2028-03-28 = 29 天，而 2026-02-28 → 2026-03-28 = 28 天。
// ---------------------------------------------------------------------------
test('用例 5｜闰年：账单日 28 号，2 月账单日为 28 日，周期长度为 29 天', () => {
  assert.equal(ymd(nextBillingDate(28, '2028-01-29')), '2028-02-28');
  assert.equal(ymd(nextBillingDate(28, '2028-02-01')), '2028-02-28');
  assert.equal(ymd(currentBillingDate(28, '2028-03-01')), '2028-02-28');
  // 闰年效应：周期长度 29 天
  assert.equal(cycleLength(28, '2028-03-01'), 29);
  assert.equal(cycleLength(28, '2026-03-01'), 28);
  // 2 月 29 日是真实存在的日历日，且会被正确跳过（它不是账单日）
  assert.equal(ymd(nextBillingDate(28, '2028-02-29')), '2028-03-28');
  assert.equal(ymd(currentBillingDate(28, '2028-02-29')), '2028-02-28');
});

// ---------------------------------------------------------------------------
// 用例 6：账单周期长度随月份变动（28–31 天）
// ---------------------------------------------------------------------------
test('用例 6｜周期长度：2 月短月为 28 天，其余按每月天数变化', () => {
  // 本期账单日 → 下一账单日
  assert.equal(cycleLength(28, '2026-02-28'), 28); // 2026-02-28 → 2026-03-28
  assert.equal(cycleLength(28, '2026-01-29'), 31); // 2026-01-28 → 2026-02-28
  assert.equal(cycleLength(5, '2026-03-06'), 31); // 2026-03-05 → 2026-04-05
  assert.equal(cycleLength(5, '2026-02-06'), 28); // 2026-02-05 → 2026-03-05
  assert.equal(cycleLength(15, '2026-04-20'), 30); // 2026-04-15 → 2026-05-15
  assert.equal(cycleLength(1, '2026-12-01'), 31); // 2026-12-01 → 2027-01-01
  assert.equal(cycleLength(28, '2027-02-28'), 28); // 2027-02-28 → 2027-03-28
});

// ---------------------------------------------------------------------------
// 用例 7：时效标记三档（规则 5）—— 到期当天还款不算逾期
// ---------------------------------------------------------------------------
test('用例 7｜时效标记三档', () => {
  assert.equal(dueStatus(5), 'normal');
  assert.equal(dueStatus(1), 'normal');
  assert.equal(dueStatus(0), 'due');
  assert.equal(dueStatus(-1), 'overdue');
  assert.equal(dueStatus(-30), 'overdue');
  // 与 daysUntilDue 联动：距还款日 0 天 → 到期
  assert.equal(dueStatus(daysUntilDue(5, 20, '2026-03-25')), 'due');
  assert.equal(dueStatus(daysUntilDue(5, 20, '2026-03-26')), 'overdue');
  assert.equal(dueStatus(daysUntilDue(5, 20, '2026-03-24')), 'normal');
});

// ---------------------------------------------------------------------------
// 用例 8：账单状态机四种 + 金额为 0 的边界（规则 7）
// ---------------------------------------------------------------------------
test('用例 8｜账单状态机四种与 0 金额边界', () => {
  assert.equal(billStatus(null, 0), 'pending_entry');
  assert.equal(billStatus(null, 100), 'pending_entry');
  assert.equal(billStatus(1000, 0), 'due_unpaid');
  assert.equal(billStatus(1000, 400), 'partially_paid');
  assert.equal(billStatus(1000, 1000), 'settled');
  assert.equal(billStatus(1000, 1200), 'settled');
  // 边界：金额 0 且已还 0 → 待还款（不是已结清）
  assert.equal(billStatus(0, 0), 'due_unpaid');
  assert.equal(billStatus(0, 50), 'settled');
});

// ---------------------------------------------------------------------------
// 用例 9：非法输入
// ---------------------------------------------------------------------------
test('用例 9｜非法输入抛错', () => {
  // billingDay 必须是 1–28 的整数
  for (const badDay of [0, 29, 30, 31, -1, 2.5]) {
    assert.throws(() => nextBillingDate(badDay, '2026-03-06'), RangeError);
    assert.throws(() => currentBillingDate(badDay, '2026-03-06'), RangeError);
    assert.throws(() => cycleLength(badDay, '2026-03-06'), RangeError);
    assert.throws(() => gracePeriod(badDay, 20, '2026-03-06'), RangeError);
    assert.throws(() => daysUntilDue(badDay, 20, '2026-03-06'), RangeError);
    assert.throws(() => validateOffset(10, badDay, '2026-03-06'), RangeError);
  }
  // N 超过当前账单周期长度 → 非法配置
  assert.throws(() => validateOffset(29, 28, '2026-02-28'), RangeError); // 周期仅 28 天
  assert.throws(() => validateOffset(32, 5, '2026-03-06'), RangeError); // 周期仅 31 天
  // N 必须在 [1, 周期长度] 内
  assert.throws(() => validateOffset(0, 5, '2026-03-06'), RangeError);
  assert.throws(() => validateOffset(-1, 5, '2026-03-06'), RangeError);
  assert.throws(() => validateOffset(1.5, 5, '2026-03-06'), RangeError);
  // 合法边界不抛错
  assert.doesNotThrow(() => validateOffset(28, 28, '2026-02-28'));
  assert.doesNotThrow(() => validateOffset(31, 5, '2026-03-06'));
  assert.doesNotThrow(() => validateOffset(1, 5, '2026-03-06'));
  // 非法日期
  assert.throws(() => nextBillingDate(5, '2026-02-30'), RangeError);
  assert.throws(() => nextBillingDate(5, '2026-13-01'), RangeError);
  assert.throws(() => nextBillingDate(5, 'not-a-date'), TypeError);
  assert.throws(() => nextBillingDate(5, new Date('nonsense')), TypeError);
});

// ---------------------------------------------------------------------------
// 用例 10：待完善状态（规则 8）
// ---------------------------------------------------------------------------
test('用例 10｜账单组完整度', () => {
  assert.equal(groupCompleteness({ billingDay: 5 }), 'incomplete');
  assert.equal(groupCompleteness({ dueOffsetDays: 20 }), 'incomplete');
  assert.equal(groupCompleteness({}), 'incomplete');
  assert.equal(groupCompleteness({ billingDay: 5, dueOffsetDays: 20 }), 'complete');
  assert.equal(groupCompleteness(null), 'incomplete');
  // 数据不完整时不计算免息期与距还款日，统一返回 null（不可用标记）
  assert.equal(gracePeriod(5, null, '2026-03-06'), null);
  assert.equal(gracePeriod(null, 20, '2026-03-06'), null);
  assert.equal(daysUntilDue(5, undefined, '2026-03-06'), null);
  assert.equal(daysUntilDue(null, 20, '2026-03-06'), null);
});

// ---------------------------------------------------------------------------
// 用例 11：跨时区回归 —— 带时区偏移的 Date 按「本地日历日」处理
// ---------------------------------------------------------------------------
test('用例 11｜带时区偏移的 Date 与等效日历日字符串结果一致', () => {
  const withOffset = new Date('2026-03-06T23:30:00+08:00');
  // 本机时区为 Asia/Shanghai：该时刻的本地日历日就是 2026-03-06
  assert.equal(ymd(withOffset), '2026-03-06');
  assert.equal(ymd(nextBillingDate(5, withOffset)), ymd(nextBillingDate(5, '2026-03-06')));
  assert.equal(ymd(currentBillingDate(5, withOffset)), ymd(currentBillingDate(5, '2026-03-06')));
  assert.equal(gracePeriod(5, 20, withOffset), gracePeriod(5, 20, '2026-03-06'));
  assert.equal(daysUntilDue(5, 20, withOffset), daysUntilDue(5, 20, '2026-03-06'));
  assert.equal(cycleLength(5, withOffset), cycleLength(5, '2026-03-06'));

  // 纯日期字符串不能被当成 UTC 解析（new Date('2026-03-06') 会偏到前一天）
  const parsedAsUtc = new Date('2026-03-06');
  assert.equal(ymd(nextBillingDate(5, '2026-03-06')), '2026-04-05');
  assert.notEqual(ymd(parsedAsUtc), ymd(nextBillingDate(5, '2026-03-06')));
});

// ---------------------------------------------------------------------------
// 与任务清单字面期望的差异说明
//
// 清单用例 4 写「账单日 28 号、ref=2026-01-28 → next 必须是 2026-02-28」，
// 清单用例 5 写「ref=2028-01-28 → next 必须是 2028-02-29」。
// 这两个期望只有在同时满足以下两条时才成立：
//   (a) 账单日当天「严格晚于今天」才推进到次月（与规则 1「等于也算」相反，
//       也与清单自己的用例 2「next = current = 2026-03-05」直接矛盾）；
//   (b) 2 月的账单日取当月最后一天（月末兜底）——ADR 0002 明确不做兜底，
//       且账单日上限 28 时 2028-02-29 需要 billingDay ≥ 29 才可能。
// 故本引擎按已定稿规则实现（规则 1/2/3 + ADR 0001/0002），此处把该差异钉死，
// 避免后续被误改回去。
// ---------------------------------------------------------------------------
test('差异说明｜账单日当天按规则 1/2 停留当月，不做月末兜底', () => {
  // 清单用例 4 的字面期望值 2026-02-28 依然可达：ref 晚一天即可
  assert.equal(ymd(nextBillingDate(28, '2026-01-29')), '2026-02-28');
  // 清单用例 5 的字面期望值 2028-02-29 在 billingDay=28 下不可达（它不是账单日）
  assert.equal(ymd(nextBillingDate(28, '2028-01-29')), '2028-02-28');
  assert.notEqual(ymd(nextBillingDate(28, '2028-02-01')), '2028-02-29');
  // 2028-02-29 是真实存在的日历日，会被正确地「跳过」而不是被当成账单日
  assert.equal(ymd(currentBillingDate(28, '2028-02-29')), '2028-02-28');
  assert.equal(ymd(nextBillingDate(28, '2028-02-29')), '2028-03-28');
});

// ---------------------------------------------------------------------------
// 补充：规则 3 的取值范围自检（最短 ≈ N，最长 ≈ N + 一个完整账单周期）
// ---------------------------------------------------------------------------
test('规则 3｜免息期取值范围自检', () => {
  // 最短点：账单日当天 → N
  assert.equal(gracePeriod(5, 20, '2026-03-05'), 20);
  // 最长点：账单日次日 → N + 周期 - 1 天（周期 31 天：03-05 → 04-05）
  assert.equal(gracePeriod(5, 20, '2026-03-06'), 50);
  // 同理 2 月短周期
  assert.equal(gracePeriod(5, 20, '2026-02-06'), 47); // 周期 28 天
});

// ---------------------------------------------------------------------------
// 补充：规则 4 / 规则 5 在时间轴上的连续性
// ---------------------------------------------------------------------------
test('规则 4｜距还款日随日期推移递减', () => {
  assert.equal(daysUntilDue(5, 20, '2026-03-06'), 19);
  assert.equal(daysUntilDue(5, 20, '2026-03-24'), 1);
  assert.equal(daysUntilDue(5, 20, '2026-03-25'), 0);
  assert.equal(daysUntilDue(5, 20, '2026-03-26'), -1);
  // 跨月：本期账单日切到 3 月 5 日
  assert.equal(daysUntilDue(5, 20, '2026-04-05'), 20);
});

// ---------------------------------------------------------------------------
// 补充：输入类型与边界
// ---------------------------------------------------------------------------
test('输入｜Date 与字符串等价、非法输入抛错', () => {
  assert.equal(ymd(nextBillingDate(5, new Date(2026, 2, 6))), '2026-04-05');
  assert.equal(ymd(nextBillingDate(5, new Date('2026-03-06T00:00:00'))), '2026-04-05');
  assert.equal(gracePeriod(5, 20, new Date(2026, 2, 6)), 50);
  assert.equal(daysUntilDue(5, 20, new Date(2026, 2, 6)), 19);
  // 跨年
  assert.equal(ymd(nextBillingDate(20, '2026-12-25')), '2027-01-20');
  assert.equal(ymd(currentBillingDate(20, '2027-01-05')), '2026-12-20');
  assert.equal(cycleLength(20, '2026-12-21'), 31); // 2026-12-20 → 2027-01-20
});

// ---------------------------------------------------------------------------
// 补充：账单状态与时效标记正交（规则 7 × 规则 5）
// ---------------------------------------------------------------------------
test('正交性｜账单状态与时效标记互不影响', () => {
  assert.equal(billStatus(1000, 1000), 'settled');
  assert.equal(dueStatus(0), 'due'); // 已结清也可以是「到期」
  assert.equal(billStatus(1000, 0), 'due_unpaid');
  assert.equal(dueStatus(-5), 'overdue'); // 待还款也可以是「逾期」
});

// ---------------------------------------------------------------------------
// 跨时区与夏令时回归（子进程，避免污染当前进程的时区状态）
// ---------------------------------------------------------------------------
test('跨时区与夏令时回归｜天数差按日历日计算，不受夏令时影响', () => {
  const script = `
    const e = require(${JSON.stringify(ENGINE_PATH)});
    console.log(JSON.stringify({
      cycle: e.cycleLength(8, '2026-02-15'),        // 2026-02-08 → 2026-03-08，跨夏令时
      due: e.daysUntilDue(7, 2, '2026-03-07'),      // 2026-03-07 → 2026-03-09，跨夏令时
      localDay: (() => { const d = new Date('2026-03-06T23:30:00+08:00');
        return d.getFullYear() + '-' + (d.getMonth()+1) + '-' + d.getDate(); })(),
      nextFromOffset: (() => { const d = new Date('2026-03-06T23:30:00+08:00');
        const r = e.nextBillingDate(5, d);
        return r.getFullYear() + '-' + (r.getMonth()+1) + '-' + r.getDate(); })(),
    }));
  `;

  // America/New_York：2026-03-08 开始夏令时（本地零点跨 23 小时）
  const ny = JSON.parse(runInTimeZone('America/New_York', script));
  assert.equal(ny.cycle, 28);
  assert.equal(ny.due, 2);

  // Asia/Tokyo (+09:00)：23:30+08:00 的本地日历日是 2026-03-07
  const tokyo = JSON.parse(runInTimeZone('Asia/Tokyo', script));
  assert.equal(tokyo.localDay, '2026-3-7');
  assert.equal(tokyo.nextFromOffset, '2026-4-5');

  // Asia/Shanghai (+08:00)：本地日历日仍是 2026-03-06
  const sh = JSON.parse(runInTimeZone('Asia/Shanghai', script));
  assert.equal(sh.localDay, '2026-3-6');
  assert.equal(sh.nextFromOffset, '2026-4-5');
  assert.equal(sh.cycle, 28);
  assert.equal(sh.due, 2);
});
