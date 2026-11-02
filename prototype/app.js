/* ============================================================
   AloCard 高保真交互原型
   日期计算复用 engine/dateEngine.js，与单元测试同一份逻辑。
   ============================================================ */

const E = window.AloCardDateEngine;

/* ---------- 演示数据（合成，示意用） ---------- */
const TODAY = '2026-10-24';

const GROUPS = [
  {
    id: 'g1',
    bank: '招商银行',
    glyph: '招',
    color: 'var(--tab-indigo)',
    billingDay: 5,
    offset: 20,
    limit: 50000,
    bill: 8320,
    paid: 3000,
    cards: [
      { id: 'c1', tail: '6688', name: '日常消费卡', scheme: '中国银联' },
      { id: 'c2', tail: '1032', name: '差旅备用卡', scheme: 'VISA' },
    ],
  },
  {
    id: 'g2',
    bank: '工商银行',
    glyph: '工',
    color: 'var(--tab-teal)',
    billingDay: 12,
    offset: 20,
    limit: 30000,
    bill: null,
    paid: 0,
    cards: [{ id: 'c3', tail: '2251', name: '通勤卡', scheme: '中国银联' }],
  },
  {
    id: 'g3',
    bank: '建设银行',
    glyph: '建',
    color: 'var(--tab-mustard)',
    billingDay: 28,
    offset: 15,
    limit: 40000,
    bill: 5600,
    paid: 0,
    cards: [{ id: 'c4', tail: '8820', name: '旅行卡', scheme: '中国银联' }],
  },
  {
    id: 'g4',
    bank: '中信银行',
    glyph: '中',
    color: 'var(--tab-plum)',
    billingDay: 20,
    offset: 25,
    limit: 20000,
    bill: 1240,
    paid: 1240,
    cards: [{ id: 'c5', tail: '4409', name: '购物卡', scheme: 'Mastercard' }],
  },
];

/* ---------- 图标（统一 1.6 描边） ---------- */
const ICONS = {
  cards: `<svg class="icon" viewBox="0 0 24 24"><rect x="2.5" y="6" width="19" height="13" rx="2"/><path d="M2.5 10h19"/><path d="M6 6V4.5A1.5 1.5 0 0 1 7.5 3h11"/></svg>`,
  gauge: `<svg class="icon" viewBox="0 0 24 24"><path d="M20.5 16a9 9 0 1 0-17 0"/><path d="M12 13l4-4"/><circle cx="12" cy="13" r="1.4"/></svg>`,
  clock: `<svg class="icon" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3.2 2"/></svg>`,
  gear: `<svg class="icon" viewBox="0 0 24 24"><circle cx="12" cy="12" r="3"/><path d="M19.4 14.5a1.6 1.6 0 0 0 .33 1.77l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.6 1.6 0 0 0-1.77-.33 1.6 1.6 0 0 0-1 1.47V21a2 2 0 1 1-4 0v-.1a1.6 1.6 0 0 0-1.05-1.47 1.6 1.6 0 0 0-1.77.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.6 1.6 0 0 0 .33-1.77 1.6 1.6 0 0 0-1.47-1H3a2 2 0 1 1 0-4h.1a1.6 1.6 0 0 0 1.47-1.05 1.6 1.6 0 0 0-.33-1.77l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.6 1.6 0 0 0 1.77.33H9a1.6 1.6 0 0 0 1-1.47V3a2 2 0 1 1 4 0v.1a1.6 1.6 0 0 0 1 1.47 1.6 1.6 0 0 0 1.77-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.6 1.6 0 0 0-.33 1.77V9a1.6 1.6 0 0 0 1.47 1H21a2 2 0 1 1 0 4h-.1a1.6 1.6 0 0 0-1.4 1.5z"/></svg>`,
  plus: `<svg class="icon" viewBox="0 0 24 24"><path d="M12 5v14M5 12h14"/></svg>`,
  chevron: `<svg class="icon icon--sm" viewBox="0 0 24 24"><path d="M9 5l7 7-7 7"/></svg>`,
  eye: `<svg class="icon icon--sm" viewBox="0 0 24 24"><path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7z"/><circle cx="12" cy="12" r="3"/></svg>`,
  lock: `<svg class="icon icon--sm" viewBox="0 0 24 24"><rect x="4.5" y="10.5" width="15" height="10" rx="2"/><path d="M8 10.5V7a4 4 0 0 1 8 0v3.5"/></svg>`,
  copy: `<svg class="icon icon--sm" viewBox="0 0 24 24"><rect x="8.5" y="8.5" width="12" height="12" rx="2"/><path d="M15.5 5.5v-1a1 1 0 0 0-1-1h-10a1 1 0 0 0-1 1v10a1 1 0 0 0 1 1h1"/></svg>`,
  bell: `<svg class="icon" viewBox="0 0 24 24"><path d="M18 8.5a6 6 0 1 0-12 0c0 6-2.5 7.5-2.5 7.5h17S18 14.5 18 8.5"/><path d="M13.7 19.5a2 2 0 0 1-3.4 0"/></svg>`,
  sheet: `<svg class="icon" viewBox="0 0 24 24"><path d="M5 3.5h14a1.5 1.5 0 0 1 1.5 1.5v14a1.5 1.5 0 0 1-1.5 1.5H5A1.5 1.5 0 0 1 3.5 19V5A1.5 1.5 0 0 1 5 3.5z"/><path d="M7.5 8h9M7.5 12h9M7.5 16h5"/></svg>`,
  check: `<svg class="icon icon--sm" viewBox="0 0 24 24"><path d="M4.5 12.5l5 5 10-11"/></svg>`,
};

/* ---------- 工具 ---------- */
const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];
const money = (n) => '¥' + n.toLocaleString('zh-CN');
const clamp = (n, lo, hi) => Math.max(lo, Math.min(hi, n));

/* ---------- 计算：全部走引擎 ---------- */
function computeGroup(g) {
  const grace = E.gracePeriod(g.billingDay, g.offset, TODAY);
  const due = E.daysUntilDue(g.billingDay, g.offset, TODAY);
  const status = E.dueStatus(due);
  const billStatus = E.billStatus(g.bill, g.paid);
  const billDate = E.currentBillingDate(g.billingDay, TODAY);
  const nextBill = E.nextBillingDate(g.billingDay, TODAY);
  const cycle = E.cycleLength(g.billingDay, TODAY);
  return { grace, due, status, billStatus, billDate, nextBill, cycle };
}

function fmtDate(d) {
  return `${d.getMonth() + 1} 月 ${d.getDate()} 日`;
}

function dueMonthDay(m, g) {
  const d = new Date(
    m.billDate.getFullYear(),
    m.billDate.getMonth(),
    m.billDate.getDate() + g.offset
  );
  return `${d.getMonth() + 1}/${d.getDate()}`;
}

function dueText(due, billStatus) {
  // 已结清的账单不再催还款——倒计时对它没有意义，改为显示已还清。
  if (billStatus === 'settled') return '本期已结清';
  if (due > 0) return `${due} 天后还款`;
  if (due === 0) return '今天还款';
  return `已逾期 ${Math.abs(due)} 天`;
}

function statusChip(g, m) {
  if (m.status === 'overdue') return { text: '逾期', cls: 'row__status--overdue' };
  if (m.status === 'due') return { text: '到期', cls: 'row__status--due' };
  if (m.billStatus === 'settled') return { text: '已结清', cls: 'row__status--done' };
  if (m.billStatus === 'pending_entry') return { text: '待录入', cls: '' };
  if (m.billStatus === 'partially_paid') return { text: '部分还款', cls: 'row__status--due' };
  return { text: '待还款', cls: '' };
}

/* ---------- 倒计时格带：本期账单日 → 还款日 之间的每一天 ----------
   逾期时今天已越过还款日，此时格带需包含「还款日之后到今天」的逾期段，
   否则整条都是空的，看不出已逾期多久。 */
function countdownCells(g, m) {
  const start = m.billDate;
  const end = new Date(start.getFullYear(), start.getMonth(), start.getDate() + g.offset);
  const today = new Date(2026, 9, 24);
  const span = Math.round((end - start) / 86400000);
  const todayIdx = Math.round((today - start) / 86400000);
  let html = '';

  if (todayIdx > span) {
    // 已逾期：整条格带填实，末格标记为逾期端
    for (let i = 0; i < span; i++) {
      html += '<span class="countdown__cell countdown__cell--past"></span>';
    }
    html += '<span class="countdown__cell countdown__cell--due"></span>';
    return html;
  }

  const idx = clamp(todayIdx, 0, span);
  for (let i = 0; i < span; i++) {
    let cls = 'countdown__cell';
    if (i < idx) cls += ' countdown__cell--past';
    else if (i === idx) cls += ' countdown__cell--today';
    html += `<span class="${cls}"></span>`;
  }
  return html;
}

/* ---------- 汇总 ---------- */
function totals() {
  let limit = 0;
  let used = 0;
  for (const g of GROUPS) {
    limit += g.limit;
    used += Math.max(0, (g.bill ?? 0) - g.paid);
  }
  return { limit, used, remain: limit - used };
}

/* ---------- 详情页格带：与首页同一套逻辑 ---------- */
function renderDetail() {
  const g = GROUPS[0];
  const m = computeGroup(g);
  const root = $('[data-panel="detail"]')?.closest('.device') || document;
  const el = $('#detail-countdown', root);
  if (!el) return;
  el.innerHTML = countdownCells(g, m);
  const cap = $('#detail-countdown-caption', root);
  if (cap) {
    cap.textContent = `${m.billDate.getMonth() + 1}/${m.billDate.getDate()} 出账 → ${dueMonthDay(
      m,
      g
    )} 还款`;
  }
}

/* ============================================================
   渲染：卡片列表首页
   ============================================================ */
let sortMode = 'grace';

/* ---------- 作用域：所有渲染限定在首页设备内，避免多屏并存时取错元素 ---------- */
function homeRoot() {
  return $('[data-panel="home"]')?.closest('.device') || document;
}

function renderLedger() {
  const list = $('#ledger', homeRoot());
  if (!list) return;
  const rows = GROUPS.map((g) => ({ g, m: computeGroup(g) }));
  rows.sort((a, b) =>
    sortMode === 'grace' ? b.m.grace - a.m.grace : a.m.due - b.m.due
  );

  list.innerHTML = rows
    .map(({ g, m }) => {
      const chip = statusChip(g, m);
      const cards = g.cards
        .map(
          (c) => `
        <li class="row">
          <div class="row__head">
            <h3 class="row__name">${c.name}</h3>
            <span class="row__tail num">尾号 ${c.tail}</span>
            ${
              g.cards.length > 1
                ? '<span class="row__shared">共享账单</span>'
                : ''
            }
            <span class="row__status ${chip.cls}">${chip.text}</span>
          </div>
          <div class="row__body">
            <span class="row__grace">
              <span class="row__grace-value num">${m.grace}</span>
              <span class="row__grace-unit">天免息</span>
            </span>
            <span class="row__due">
              <span class="row__due-text ${
                m.status === 'overdue' && m.billStatus !== 'settled'
                  ? 'row__due-text--overdue'
                  : ''
              } ${
                m.status === 'due' && m.billStatus !== 'settled'
                  ? 'row__due-text--due'
                  : ''
              }">${dueText(m.due, m.billStatus)}</span>
              <span class="row__due-sub num">还款 ${fmtDate(
                new Date(
                  m.billDate.getFullYear(),
                  m.billDate.getMonth(),
                  m.billDate.getDate() + g.offset
                )
              )}</span>
            </span>
          </div>
          <div class="countdown">${countdownCells(g, m)}</div>
          <div class="countdown__caption num">
            ${m.billDate.getMonth() + 1}/${m.billDate.getDate()} 出账 → ${dueMonthDay(
              m,
              g
            )} 还款
          </div>
        </li>`
        )
        .join('');

      return `
      <div class="ledger__group" style="--tab-color:${g.color}">
        <div class="tab"><span class="tab__glyph">${g.glyph}</span></div>
        <ul style="list-style:none;margin:0;padding:0">${cards}</ul>
      </div>`;
    })
    .join('');
}

function renderOverview() {
  const t = totals();
  const root = homeRoot();
  const used = $('#ov-used', root);
  const limit = $('#ov-limit', root);
  const remain = $('#ov-remain', root);
  const meter = $('#ov-meter', root);
  if (!used) return;
  used.textContent = money(t.used);
  if (limit) limit.textContent = money(t.limit);
  if (remain) remain.textContent = money(t.remain);
  if (meter) meter.style.width = clamp((t.used / t.limit) * 100, 0, 100) + '%';
}

/* ============================================================
   弹层：录入账单金额
   ============================================================ */
function openBillSheet(groupId) {
  const g = GROUPS.find((x) => x.id === groupId);
  const m = computeGroup(g);
  const root = $('[data-panel="home"]')?.closest('.device') || document;
  const sheet = $('#bill-sheet', root);
  if (!sheet) return;
  $('#bill-sheet-title', root).textContent = `${g.bank} · 录入账单金额`;
  $('#bill-sheet-sub', root).innerHTML =
    m.status === 'overdue'
      ? `<span style="color:var(--state-overdue)">${dueText(m.due, m.billStatus)}</span> · 账单日 ${fmtDate(m.billDate)}`
      : `账单日 ${fmtDate(m.billDate)} · 还款日 ${fmtDate(
          new Date(
            m.billDate.getFullYear(),
            m.billDate.getMonth(),
            m.billDate.getDate() + g.offset
          )
        )}`;
  $('#bill-amount', root).value = g.bill ?? '';
  sheet.dataset.open = 'true';
}

function openPaySheet(groupId) {
  const g = GROUPS.find((x) => x.id === groupId);
  const m = computeGroup(g);
  const root = $('[data-panel="home"]')?.closest('.device') || document;
  const sheet = $('#pay-sheet', root);
  if (!sheet) return;
  const remain = Math.max(0, (g.bill ?? 0) - g.paid);
  $('#pay-sheet-title', root).textContent = `${g.bank} · 标记还款`;
  $('#pay-sheet-sub', root).innerHTML = `账单 <b class="num">${money(
    g.bill ?? 0
  )}</b> · 已还 <b class="num">${money(g.paid)}</b> · 待还 <b class="num">${money(remain)}</b>`;
  $('#pay-amount', root).value = remain;
  $('#pay-amount', root).placeholder = String(remain);
  sheet.dataset.open = 'true';
}

/* ============================================================
   小组件 + 通知预览
   ============================================================ */
function renderWidget() {
  const root = $('[data-panel="widget"]')?.closest('.device') || document;
  const rows = GROUPS.map((g) => ({ g, m: computeGroup(g) }))
    .sort((a, b) => b.m.grace - a.m.grace)
    .slice(0, 5);
  const box = $('#widget-rows', root);
  if (!box) return;
  box.innerHTML = rows
    .map(
      ({ g, m }, i) => `
    <div class="widget__row ${i === 0 ? 'widget__first' : ''}">
      <span class="widget__rank num">${i + 1}</span>
      <span class="widget__bank">${g.bank}</span>
      <span class="widget__days num">${m.grace}</span>
    </div>`
    )
    .join('');

  const urgent = GROUPS.map((g) => ({ g, m: computeGroup(g) }))
    .filter((x) => x.m.due <= 3 && x.m.billStatus !== 'settled')
    .sort((a, b) => a.m.due - b.m.due);
  const body = $('#notif-body', root);
  if (!body) return;

  if (urgent.length > 0) {
    const first = urgent[0];
    const rest = urgent.slice(0, 3);
    body.innerHTML = `
      <div class="notification__body">
        ${first.g.bank} ${dueText(first.m.due, first.m.billStatus)}
        ${first.m.billStatus !== 'pending_entry'
          ? `，待还 ${money(Math.max(0, (first.g.bill ?? 0) - first.g.paid))}`
          : '，账单金额待录入'}
      </div>
      <ul class="notification__list">${rest
        .map(
          ({ g, m }) =>
            `<li><span>${g.bank}</span><b>${m.due <= 0 ? '已逾期' : m.due + ' 天'}</b></li>`
        )
        .join('')}</ul>`;
  } else {
    // 无临近到期账单时，常驻通知转为「今日刷卡建议」
    const top = GROUPS.map((g) => ({ g, m: computeGroup(g) }))
      .sort((a, b) => b.m.grace - a.m.grace)
      .slice(0, 3);
    body.innerHTML = `
      <div class="notification__body">今天刷这张免息期最长</div>
      <ul class="notification__list">${top
        .map(
          ({ g, m }) =>
            `<li><span>${g.bank}</span><b>${m.grace} 天</b></li>`
        )
        .join('')}</ul>`;
  }
}

/* ============================================================
   绑定
   ============================================================ */
function init() {
  // URL 参数驱动视图与主题（用于批量截图与分享特定状态）
  const params = new URLSearchParams(location.search);
  const view = params.get('view');
  const theme = params.get('theme');

  const saved = theme || localStorage.getItem('alocard-theme') || 'light';
  document.documentElement.setAttribute('data-theme', saved);
  localStorage.setItem('alocard-theme', saved);
  $$('[data-theme-btn]').forEach((b) => {
    b.setAttribute(
      'aria-pressed',
      String(b.dataset.themeBtn === saved)
    );
    b.addEventListener('click', () => {
      const t = b.dataset.themeBtn;
      document.documentElement.setAttribute('data-theme', t);
      localStorage.setItem('alocard-theme', t);
      $$('[data-theme-btn]').forEach((x) =>
        x.setAttribute('aria-pressed', String(x.dataset.themeBtn === t))
      );
    });
  });

  // 排序
  $$('[data-sort]').forEach((b) => {
    b.addEventListener('click', () => {
      sortMode = b.dataset.sort;
      $$('[data-sort]').forEach((x) =>
        x.setAttribute('aria-selected', String(x.dataset.sort === sortMode))
      );
      renderLedger();
    });
  });

  // 所有屏幕并列展示（同一台手机的不同画面），不做单屏切换。
  // URL 参数 view=xxx 时只显示该屏，便于单独截图。
  const only = params.get('view');
  if (only) {
    $$('.device').forEach((d) => {
      const p = $('[data-panel]', d);
      d.style.display = p && p.dataset.panel === only ? '' : 'none';
    });
    // 独立表面不属于任何一屏，单屏模式下收起，避免挤占视口
    const surfaces = $('.surfaces');
    if (surfaces) surfaces.style.display = 'none';
  }

  // 弹层关闭
  $$('[data-close]').forEach((b) => {
    b.addEventListener('click', () => {
      document.getElementById(b.dataset.close).dataset.open = 'false';
    });
  });
  $$('.sheet').forEach((s) => {
    s.addEventListener('click', (ev) => {
      if (ev.target === s) s.dataset.open = 'false';
    });
  });

  // 免息期步进器（按设备作用域）
  document.querySelectorAll('.device').forEach((dev) => {
    const stepper = $('#offset-stepper', dev);
    if (!stepper) return;
    $$('[data-step]', stepper).forEach((b) => {
      b.addEventListener('click', () => {
        const d = Number(b.dataset.step);
        const cur = Number($('#offset-value', dev).textContent);
        const next = clamp(cur + d, 1, 28);
        $('#offset-value', dev).textContent = next;
        $('#offset-preview', dev).textContent = `账单日 5 号 → 还款日 ${5 + next} 号`;
      });
    });
  });

  // 安全密码（按设备作用域）
  document.querySelectorAll('.device').forEach((dev) => {
    const pinCells = $$('.pin__cell', dev);
    if (!pinCells.length) return;
    let pinFilled = 0;
    $$('.pin__key', dev).forEach((k) => {
      k.addEventListener('click', () => {
        const v = k.dataset.key;
        if (v === 'del') {
          pinFilled = Math.max(0, pinFilled - 1);
        } else {
          pinFilled = Math.min(6, pinFilled + 1);
        }
        pinCells.forEach((c, i) => {
          c.textContent = i < pinFilled ? '•' : '';
          c.classList.toggle('pin__cell--filled', i < pinFilled);
          c.classList.toggle('pin__cell--active', i === pinFilled);
        });
        const hint = $('#pin-hint', dev);
        if (hint && pinFilled === 6) {
          hint.textContent = '已设置 6 位安全密码';
          hint.style.color = 'var(--state-done)';
        }
      });
    });
  });

  renderLedger();
  renderOverview();
  renderWidget();
  renderDetail();
}

document.addEventListener('DOMContentLoaded', init);
