import fs from 'node:fs';

const html = fs.readFileSync('website/app/index.html', 'utf8');
const css = fs.readFileSync('website/app/app.css', 'utf8');
const js = fs.readFileSync('website/app/app.js', 'utf8');

const mustContain = [
  '现在',
  '基础设施',
  '变更',
  '记录',
  '我',
  'unknown ≠ safe',
  'done ≠ verified',
  'After = Plan Projection',
  '重新分析当前影响',
  '发生于',
  '记录于',
  '验证于',
  '此对象暂不能直接创建变更计划',
  'Make-Before-Break',
];

const missing = mustContain.filter((value) => !js.includes(value));
if (missing.length) {
  throw new Error('Missing Web VNext contract text: ' + missing.join(', '));
}

if (!html.includes('./app.css') || !html.includes('./app.js')) {
  throw new Error('Web app shell is not wired to local CSS/JS assets');
}

if (!css.includes('.app-shell') || !css.includes('.mobile-bar') || !css.includes('@media')) {
  throw new Error('Responsive Web VNext shell styles are incomplete');
}

const forbiddenNetwork = ['fetch(', 'XMLHttpRequest', 'WebSocket(', 'EventSource('];
const networkHits = forbiddenNetwork.filter((value) => js.includes(value));
if (networkHits.length) {
  throw new Error('Preview must stay browser-local; network API found: ' + networkHits.join(', '));
}

console.log('WEB_VNEXT_SMOKE=PASS');
console.log('truth_contracts=' + mustContain.length);
console.log('network_writes=0');
