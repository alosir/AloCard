const { spawnSync } = require('child_process');
const path = require('path');
const fs = require('fs');

const CHROME = 'C:/Users/白泽/AppData/Local/ms-playwright/chromium-1247/chrome-win64/chrome.exe';
const BASE = 'http://localhost:8899/prototype/index.html';
const OUT = 'C:/Users/白泽/shots';
fs.mkdirSync(OUT, { recursive: true });

const shots = [
  { name: 'A-home-light', view: 'home', theme: 'light' },
  { name: 'B-home-dark', view: 'home', theme: 'dark' },
  { name: 'C-detail-light', view: 'detail', theme: 'light' },
  { name: 'D-detail-dark', view: 'detail', theme: 'dark' },
  { name: 'E-add-light', view: 'add', theme: 'light' },
  { name: 'F-add-dark', view: 'add', theme: 'dark' },
  { name: 'G-pin-light', view: 'pin', theme: 'light' },
  { name: 'H-pin-dark', view: 'pin', theme: 'dark' },
  { name: 'I-empty-light', view: 'empty', theme: 'light' },
  { name: 'J-empty-dark', view: 'empty', theme: 'dark' },
  { name: 'K-all-light', view: null, theme: 'light', w: 1320, h: 1600 },
  { name: 'L-all-dark', view: null, theme: 'dark', w: 1320, h: 1600 },
];

for (const s of shots) {
  const file = path.join(OUT, s.name + '.png');
  const url = `${BASE}?theme=${s.theme}${s.view ? '&view=' + s.view : ''}`;
  const r = spawnSync(CHROME, [
    '--headless', '--disable-gpu', '--no-sandbox', '--hide-scrollbars',
    '--force-device-scale-factor=2',
    `--window-size=${s.w || 900},${s.h || 1450}`,
    `--screenshot=${file}`,
    '--virtual-time-budget=5000',
    url,
  ], { encoding: 'utf8' });
  const ok = fs.existsSync(file);
  console.log(ok ? 'OK  ' : 'FAIL', s.name, ok ? fs.statSync(file).size + 'b' : '');
}
console.log('done ->', OUT);
