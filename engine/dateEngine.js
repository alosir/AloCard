'use strict';

/**
 * AloCard 账期日期计算引擎
 *
 * 纯函数实现，无第三方依赖。所有日期运算都在「本地时区的日历日」层面完成：
 *  - 内部统一使用本地零点的 Date（new Date(y, m-1, d) 语义）；
 *  - 日数差通过 Date.UTC(y, m-1, d) 的整数差计算，避免夏令时导致的毫秒偏差；
 *  - 禁止使用 (date1 - date2) / 86400000 这种受夏令时影响的算法。
 *
 * 规则来源：CONTEXT.md（领域语言）、docs/adr/0001（免息期为今日视角的动态值）、
 * 0002（账单日 1–28、账单日当天消费计入本期）、0003（还款日 = 账单日 + N）、
 * 0008（到期 ≠ 逾期）。
 */

const MS_PER_DAY = 86400000;

/** 账单日取值范围：银行实际只开 1–28 号，不做月末兜底（ADR 0002） */
const MIN_BILLING_DAY = 1;
const MAX_BILLING_DAY = 28;

/** 时效标记（规则 5） */
const STATUS_NORMAL = 'normal';
const STATUS_DUE = 'due';
const STATUS_OVERDUE = 'overdue';

/** 账单状态（规则 7） */
const BILL_PENDING_ENTRY = 'pending_entry';
const BILL_DUE_UNPAID = 'due_unpaid';
const BILL_PARTIALLY_PAID = 'partially_paid';
const BILL_SETTLED = 'settled';

/**
 * 构造本地零点的日历日。
 * 用 setFullYear 而非 Date 构造器，避免 0–99 年被映射到 1900–1999。
 */
function makeDate(year, month1, day) {
  const dt = new Date(0);
  dt.setFullYear(year, month1 - 1, day);
  dt.setHours(0, 0, 0, 0);
  return dt;
}

/** 某年某月（month1 为 1–12）的天数 */
function daysInMonth(year, month1) {
  const dt = new Date(0);
  dt.setFullYear(year, month1, 0);
  dt.setHours(0, 0, 0, 0);
  return dt.getDate();
}

/** 日历日序号（整数天），跨夏令时也精确 */
function dayNumber(date) {
  const t = new Date(0);
  t.setUTCFullYear(date.getFullYear(), date.getMonth(), date.getDate());
  t.setUTCHours(0, 0, 0, 0);
  return t.getTime() / MS_PER_DAY;
}

/** 两个日历日之间的天数（b - a，整数） */
function daysBetween(a, b) {
  return dayNumber(b) - dayNumber(a);
}

/** 日历日加减天数（自动跨月/跨年） */
function addDays(date, days) {
  return makeDate(date.getFullYear(), date.getMonth() + 1, date.getDate() + days);
}

/** 月份平移，返回 { year, month }（month 为 1–12） */
function shiftMonth(year, month1, delta) {
  const zeroBased = year * 12 + (month1 - 1) + delta;
  return {
    year: Math.floor(zeroBased / 12),
    month: (((zeroBased % 12) + 12) % 12) + 1,
  };
}

/** 校验并归一化账单日：必须是 1–28 的整数，否则抛错（规则 6） */
function normalizeBillingDay(billingDay) {
  if (typeof billingDay !== 'number' || !Number.isFinite(billingDay)) {
    throw new TypeError(`billingDay 必须是数字，收到 ${String(billingDay)}`);
  }
  if (!Number.isInteger(billingDay)) {
    throw new RangeError(`billingDay 必须是整数，收到 ${billingDay}`);
  }
  if (billingDay < MIN_BILLING_DAY || billingDay > MAX_BILLING_DAY) {
    throw new RangeError(
      `billingDay 必须落在 ${MIN_BILLING_DAY}–${MAX_BILLING_DAY}（银行账单日实际取值范围，不做月末兜底），收到 ${billingDay}`
    );
  }
  return billingDay;
}

/** 校验并归一化偏移天数 N：必须是正整数 */
function normalizeOffset(dueOffsetDays) {
  if (typeof dueOffsetDays !== 'number' || !Number.isFinite(dueOffsetDays)) {
    throw new TypeError(`dueOffsetDays 必须是数字，收到 ${String(dueOffsetDays)}`);
  }
  if (!Number.isInteger(dueOffsetDays)) {
    throw new RangeError(`dueOffsetDays 必须是整数，收到 ${dueOffsetDays}`);
  }
  if (dueOffsetDays < 1) {
    throw new RangeError(`dueOffsetDays 必须 ≥ 1（还款日必须晚于账单日），收到 ${dueOffsetDays}`);
  }
  return dueOffsetDays;
}

/** 缺失值判定（用于「待完善」状态） */
function isMissing(value) {
  return value === null || value === undefined || value === '';
}

/**
 * 解析参考日期：接受 Date 对象或 'YYYY-MM-DD' 字符串。
 * 字符串按本地日历日解析（绝不使用 new Date('2026-03-06') 这种 UTC 解析）。
 */
function parseRefDate(value, label = 'refDate') {
  if (value instanceof Date) {
    if (Number.isNaN(value.getTime())) {
      throw new TypeError(`${label} 不是合法的 Date`);
    }
    return makeDate(value.getFullYear(), value.getMonth() + 1, value.getDate());
  }
  if (typeof value === 'string') {
    const m = /^(\d{4})-(\d{1,2})-(\d{1,2})$/.exec(value.trim());
    if (!m) {
      throw new TypeError(`${label} 必须是 'YYYY-MM-DD' 字符串或 Date 对象，收到 ${value}`);
    }
    const year = Number(m[1]);
    const month = Number(m[2]);
    const day = Number(m[3]);
    if (month < 1 || month > 12) {
      throw new RangeError(`${label} 的月份非法：${value}`);
    }
    if (day < 1 || day > daysInMonth(year, month)) {
      throw new RangeError(`${label} 的日期非法：${value}`);
    }
    return makeDate(year, month, day);
  }
  throw new TypeError(`${label} 必须是 'YYYY-MM-DD' 字符串或 Date 对象，收到 ${String(value)}`);
}

/** 某年某月的账单日（按目标月天数 clamp，通用做法，不假设月份长度固定） */
function billingDateOf(billingDay, year, month1) {
  const day = Math.min(billingDay, daysInMonth(year, month1));
  return makeDate(year, month1, day);
}

/**
 * 规则 1：消费所属账单日（最近下一次账单日）
 * = 满足 billDate >= D 的最小账单日；账单日当天（相等）计入以该账单日出账的本期账单。
 */
function nextBillingDate(billingDay, refDate) {
  const day = normalizeBillingDay(billingDay);
  const ref = parseRefDate(refDate);
  const sameMonth = billingDateOf(day, ref.getFullYear(), ref.getMonth() + 1);
  if (dayNumber(sameMonth) >= dayNumber(ref)) {
    return sameMonth;
  }
  const next = shiftMonth(ref.getFullYear(), ref.getMonth() + 1, 1);
  return billingDateOf(day, next.year, next.month);
}

/**
 * 规则 2：本期账单日（最近一次已出账账单）
 * = 满足 billDate <= D 的最大账单日；D 恰为账单日时与规则 1 结果相同（都是 D 本身）。
 */
function currentBillingDate(billingDay, refDate) {
  const day = normalizeBillingDay(billingDay);
  const ref = parseRefDate(refDate);
  const sameMonth = billingDateOf(day, ref.getFullYear(), ref.getMonth() + 1);
  if (dayNumber(sameMonth) <= dayNumber(ref)) {
    return sameMonth;
  }
  const prev = shiftMonth(ref.getFullYear(), ref.getMonth() + 1, -1);
  return billingDateOf(day, prev.year, prev.month);
}

/**
 * 规则 6：当前账单周期长度（天）
 * = 本期账单日（最近一次已出账账单日）到下一个账单日的天数，随月份在 28–31 天之间变动。
 */
function cycleLength(billingDay, refDate) {
  const day = normalizeBillingDay(billingDay);
  const ref = parseRefDate(refDate);
  const start = currentBillingDate(day, ref);
  const next = shiftMonth(start.getFullYear(), start.getMonth() + 1, 1);
  const end = billingDateOf(day, next.year, next.month);
  return daysBetween(start, end);
}

/**
 * 规则 6：校验偏移天数 N
 * 1 ≤ N ≤ 当前参考日期所处账单周期长度；越界即非法配置，抛错。
 */
function validateOffset(dueOffsetDays, billingDay, refDate) {
  const offset = normalizeOffset(dueOffsetDays);
  const len = cycleLength(billingDay, refDate);
  if (offset > len) {
    throw new RangeError(
      `dueOffsetDays (${offset}) 超过当前账单周期长度 (${len} 天)：会导致还款日早于下一个账单日、或免息期超过一个完整周期，属于非法配置`
    );
  }
  return undefined;
}

/**
 * 规则 3：免息期（今日视角的动态值）
 * = (消费所属账单日 + N) − D
 * 数据不完整（缺 billingDay 或 dueOffsetDays）时返回 null（不可用标记）。
 */
function gracePeriod(billingDay, dueOffsetDays, refDate) {
  if (isMissing(billingDay) || isMissing(dueOffsetDays)) {
    return null;
  }
  const day = normalizeBillingDay(billingDay);
  const offset = normalizeOffset(dueOffsetDays);
  const ref = parseRefDate(refDate);
  validateOffset(offset, day, ref);
  const bill = nextBillingDate(day, ref);
  const due = addDays(bill, offset);
  return daysBetween(ref, due);
}

/**
 * 规则 4：距还款日
 * = (本期账单日 + N) − D
 * 数据不完整时返回 null（不可用标记）。
 */
function daysUntilDue(billingDay, dueOffsetDays, refDate) {
  if (isMissing(billingDay) || isMissing(dueOffsetDays)) {
    return null;
  }
  const day = normalizeBillingDay(billingDay);
  const offset = normalizeOffset(dueOffsetDays);
  const ref = parseRefDate(refDate);
  validateOffset(offset, day, ref);
  const bill = currentBillingDate(day, ref);
  const due = addDays(bill, offset);
  return daysBetween(ref, due);
}

/**
 * 规则 5：时效标记
 * daysUntilDue > 0 → 'normal'；= 0 → 'due'（当天还款，不算逾期）；< 0 → 'overdue'
 */
function dueStatus(days) {
  if (typeof days !== 'number' || !Number.isFinite(days)) {
    throw new TypeError(`days 必须是数字，收到 ${String(days)}`);
  }
  if (days > 0) return STATUS_NORMAL;
  if (days === 0) return STATUS_DUE;
  return STATUS_OVERDUE;
}

/**
 * 规则 7：账单状态机（与时效标记正交）
 * 判定顺序：待录入 → 待还款 → 部分还款 → 已结清。
 * 金额为 0 且已还 0 归为「待还款」（billAmount !== null && paidAmount === 0 先命中）。
 */
function billStatus(billAmount, paidAmount) {
  if (billAmount === null || billAmount === undefined) {
    return BILL_PENDING_ENTRY;
  }
  if (typeof billAmount !== 'number' || !Number.isFinite(billAmount)) {
    throw new TypeError(`billAmount 必须是数字或 null，收到 ${String(billAmount)}`);
  }
  if (billAmount < 0) {
    throw new RangeError(`billAmount 不能为负数，收到 ${billAmount}`);
  }
  const paid = paidAmount === null || paidAmount === undefined ? 0 : paidAmount;
  if (typeof paid !== 'number' || !Number.isFinite(paid)) {
    throw new TypeError(`paidAmount 必须是数字，收到 ${String(paidAmount)}`);
  }
  if (paid < 0) {
    throw new RangeError(`paidAmount 不能为负数，收到 ${paid}`);
  }
  if (paid === 0) return BILL_DUE_UNPAID;
  if (paid < billAmount) return BILL_PARTIALLY_PAID;
  return BILL_SETTLED;
}

/**
 * 规则 8：账单组完整度
 * 缺少 billingDay 或 dueOffsetDays → 'incomplete'。
 */
function groupCompleteness(group) {
  if (!group || typeof group !== 'object') {
    return 'incomplete';
  }
  if (isMissing(group.billingDay) || isMissing(group.dueOffsetDays)) {
    return 'incomplete';
  }
  return 'complete';
}

/**
 * 导出方式同时兼容 Node（CommonJS）与浏览器（全局）。
 * 目的是让 HTML 原型复用同一份日期逻辑，避免原型与单元测试各写一套而逐渐漂移。
 */
const API = {
  nextBillingDate,
  currentBillingDate,
  gracePeriod,
  daysUntilDue,
  dueStatus,
  billStatus,
  cycleLength,
  validateOffset,
  groupCompleteness,
};

if (typeof module !== 'undefined' && typeof module.exports !== 'undefined') {
  module.exports = API;
}

if (typeof globalThis !== 'undefined') {
  globalThis.AloCardDateEngine = API;
}
