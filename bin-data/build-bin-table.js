'use strict';

/**
 * 从 bin-data/ 下的两个 CSV 生成 engine/bin-table.json（内置 BIN 表数据文件）。
 *
 * 输入：
 *   bcbc-bin-cn.csv      bin,bank,type,length
 *   bcbc-bank-names-cn.csv bank,name
 *
 * 输出：
 *   engine/bin-table.json
 *
 * 规则（取舍见 bin-data/README.md）：
 *   - 只保留 6 位 BIN 与 8 位 IIN 两类可查表键；3/4/5/7/9/10 位条目当前查表顺序用不到。
 *   - 保留全部卡类型（CC/DC/SCC/PC），由 detectBank 原样返回 type，交给调用方决定是否展示。
 *   - 同一前缀的多条记录全部保留（例如 622162 同时是光大 CC/16 与北京银行 DC/19），
 *     detectBank 再按卡号长度消歧。
 *
 * 运行：node bin-data/build-bin-table.js
 */

const fs = require('node:fs');
const path = require('node:path');

const DATA_DIR = __dirname;
const BIN_CSV = path.join(DATA_DIR, 'bcbc-bin-cn.csv');
const NAME_CSV = path.join(DATA_DIR, 'bcbc-bank-names-cn.csv');
const OUT_FILE = path.join(DATA_DIR, '..', 'engine', 'bin-table.json');

/** 读 CSV，返回去掉表头与空行后的二维数组 */
function readCsv(file) {
  const text = fs.readFileSync(file, 'utf8');
  const lines = text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line !== '');
  const header = lines[0].split(',');
  return lines.slice(1).map((line) => {
    const cells = line.split(',');
    const row = {};
    header.forEach((key, i) => {
      row[key] = cells[i] === undefined ? '' : cells[i].trim();
    });
    return row;
  });
}

/** 银行代码 -> 中文名 */
function buildBankNames(rows) {
  const names = {};
  for (const row of rows) {
    if (!row.bank) continue;
    names[row.bank] = row.name;
  }
  return names;
}

/** 前缀 -> [[银行代码, 卡类型, 卡号长度], ...] */
function buildBins(rows) {
  const bins = {};
  let skipped = 0;
  for (const row of rows) {
    const bin = row.bin;
    if (!/^\d+$/.test(bin)) {
      skipped += 1;
      continue;
    }
    if (bin.length !== 6 && bin.length !== 8) {
      skipped += 1;
      continue;
    }
    const length = Number(row.length);
    if (!Number.isInteger(length) || length < 8 || length > 19) {
      skipped += 1;
      continue;
    }
    if (!bins[bin]) bins[bin] = [];
    bins[bin].push([row.bank, row.type, length]);
  }
  return { bins, skipped };
}

/** 手写 JSON 序列化：一个条目一行，便于 diff 与人工审阅 */
function stringifyTable(table) {
  const out = [];
  out.push('{');
  out.push('  "meta": ' + JSON.stringify(table.meta, null, 2).split('\n').join('\n  ') + ',');
  out.push('  "banks": {');
  const bankKeys = Object.keys(table.banks);
  bankKeys.forEach((key, i) => {
    const comma = i === bankKeys.length - 1 ? '' : ',';
    out.push(`    ${JSON.stringify(key)}: ${JSON.stringify(table.banks[key])}${comma}`);
  });
  out.push('  },');
  out.push('  "bins": {');
  const binKeys = Object.keys(table.bins);
  binKeys.forEach((key, i) => {
    const comma = i === binKeys.length - 1 ? '' : ',';
    out.push(`    ${JSON.stringify(key)}: ${JSON.stringify(table.bins[key])}${comma}`);
  });
  out.push('  }');
  out.push('}');
  return out.join('\n') + '\n';
}

function main() {
  const binRows = readCsv(BIN_CSV);
  const nameRows = readCsv(NAME_CSV);
  const banks = buildBankNames(nameRows);
  const { bins, skipped } = buildBins(binRows);

  // 统计：是否有 BIN 引用了没有中文名的银行代码
  const missingNames = new Set();
  for (const rows of Object.values(bins)) {
    for (const row of rows) {
      if (!banks[row[0]]) missingNames.add(row[0]);
    }
  }

  const six = Object.keys(bins).filter((k) => k.length === 6).length;
  const eight = Object.keys(bins).filter((k) => k.length === 8).length;
  const rowsIn = Object.values(bins).reduce((sum, rows) => sum + rows.length, 0);

  const table = {
    meta: {
      source: 'bcbc-bin-cn.csv + bcbc-bank-names-cn.csv',
      sourceProject: 'bcbc (中国银行卡 BIN 码数据集)',
      license: 'MIT',
      snapshot: '2026-10-24',
      snapshotNote: '快照时间为 bin-data/ 下 CSV 文件的落盘日期；上游仓库的提交时间可能更早。',
      generatedBy: 'bin-data/build-bin-table.js',
      binCount: six,
      iinCount: eight,
      rowCount: rowsIn,
      bankNameCount: Object.keys(banks).length,
      trimmedOut: skipped,
      schema: 'bins: 前缀 -> [[银行代码, 卡类型, 卡号长度], ...]',
      disclaimer: '识别结果仅供参考，请以卡片实物为准。',
    },
    banks,
    bins,
  };

  fs.writeFileSync(OUT_FILE, stringifyTable(table), 'utf8');

  const sizeKb = (fs.statSync(OUT_FILE).size / 1024).toFixed(1);
  console.log(`写入 ${OUT_FILE}`);
  console.log(`  6 位 BIN ${six} 个 / 8 位 IIN ${eight} 个 / 记录 ${rowsIn} 条`);
  console.log(`  银行中文名 ${Object.keys(banks).length} 条`);
  console.log(`  跳过非 6/8 位或非法条目 ${skipped} 条`);
  console.log(`  文件大小 ${sizeKb} KB`);
  if (missingNames.size > 0) {
    console.log(`  ! 缺少中文名的银行代码（将保留原始代码）: ${[...missingNames].join(', ')}`);
  } else {
    console.log('  所有银行代码均有中文名');
  }
}

main();
