const STORAGE_KEY = 'pdig.web-vnext.state.v1';

const iconPaths = {
  now: '<path d="M4 13.5 12 6l8 7.5"/><path d="M6.5 11.5V20h11v-8.5"/><path d="M10 20v-5h4v5"/>',
  infra: '<rect x="4" y="4" width="6" height="6" rx="2"/><rect x="14" y="4" width="6" height="6" rx="2"/><rect x="4" y="14" width="6" height="6" rx="2"/><rect x="14" y="14" width="6" height="6" rx="2"/>',
  change: '<path d="M4 7h11"/><path d="m12 4 3 3-3 3"/><path d="M20 17H9"/><path d="m12 14-3 3 3 3"/>',
  records: '<path d="M7 3h10v4H7z"/><path d="M5 5v16h14V5"/><path d="M8 11h8M8 15h8"/>',
  me: '<circle cx="12" cy="8" r="4"/><path d="M4.5 21c.7-4 3.2-6 7.5-6s6.8 2 7.5 6"/>',
  search: '<circle cx="11" cy="11" r="6"/><path d="m16 16 4 4"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  eye: '<path d="M2.5 12s3.5-6 9.5-6 9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6Z"/><circle cx="12" cy="12" r="2.5"/>',
  eyeoff: '<path d="m4 4 16 16"/><path d="M10.5 6.2c.5-.1 1-.2 1.5-.2 6 0 9.5 6 9.5 6a15 15 0 0 1-3.1 3.7"/><path d="M6.2 6.2C3.8 8 2.5 12 2.5 12s3.5 6 9.5 6c1 0 2-.2 2.8-.4"/>',
  lock: '<rect x="5" y="10" width="14" height="11" rx="3"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/>',
  phone: '<rect x="7" y="2.5" width="10" height="19" rx="2.5"/><path d="M10 5h4M11 18.5h2"/>',
  email: '<rect x="3" y="5" width="18" height="14" rx="2.5"/><path d="m4.5 7 7.5 6 7.5-6"/>',
  card: '<rect x="3" y="5" width="18" height="14" rx="2.5"/><path d="M3 9h18M7 15h4"/>',
  account: '<path d="M4 9 12 4l8 5"/><path d="M5 10h14v9H5z"/><path d="M9 10v9M15 10v9"/>',
  service: '<circle cx="12" cy="12" r="3"/><path d="M12 2v4M12 18v4M2 12h4M18 12h4M5 5l3 3M16 16l3 3M19 5l-3 3M8 16l-3 3"/>',
  device: '<rect x="4" y="4" width="16" height="12" rx="2"/><path d="M9 20h6M12 16v4"/>',
  check: '<path d="m5 12 4 4L19 6"/>',
  warn: '<path d="M12 3 2.8 20h18.4L12 3Z"/><path d="M12 9v5M12 17.5v.2"/>',
  chevron: '<path d="m9 5 7 7-7 7"/>',
  arrow: '<path d="M5 12h14"/><path d="m14 7 5 5-5 5"/>',
  history: '<path d="M4 7v5h5"/><path d="M5.2 16a8 8 0 1 0 .2-8.2L4 12"/><path d="M12 8v4l3 2"/>',
  shield: '<path d="M12 3 5 6v5c0 5 3 8 7 10 4-2 7-5 7-10V6l-7-3Z"/><path d="m9 12 2 2 4-4"/>',
  link: '<path d="M10 13a4 4 0 0 0 5.7 0l2.3-2.3A4 4 0 0 0 12.3 5L11 6.3"/><path d="M14 11a4 4 0 0 0-5.7 0L6 13.3A4 4 0 0 0 11.7 19l1.3-1.3"/>',
  x: '<path d="m6 6 12 12M18 6 6 18"/>',
  refresh: '<path d="M20 7v5h-5"/><path d="M4 17v-5h5"/><path d="M6 9a7 7 0 0 1 12-2l2 5M18 15a7 7 0 0 1-12 2l-2-5"/>',
  more: '<circle cx="5" cy="12" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/>',
};

function icon(name) {
  return '<span class="icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
    (iconPaths[name] || iconPaths.more) + '</svg></span>';
}

const assetKinds = {
  phone: { label: '手机号', icon: 'phone', hero: '通信身份', canCreate: true },
  email: { label: '邮箱', icon: 'email', hero: '通信身份', canCreate: true },
  card: { label: '银行卡', icon: 'card', hero: '支付工具', canCreate: true },
  account: { label: '账户', icon: 'account', hero: '账户身份', canCreate: true },
  service: { label: '服务', icon: 'service', hero: '服务节点', canCreate: true },
  device: { label: '设备', icon: 'device', hero: '可信设备', canCreate: false },
};

const initialData = {
  graphRevision: 8,
  assets: [
    { id:'phone-main', kind:'phone', name:'主手机号', value:'+86 138 6821 9472', status:'使用中', lifecycle:'主要身份入口', unknown:'独立恢复路径尚未确认', dependencies:['account-main','service-bank','service-cloud'] },
    { id:'email-main', kind:'email', name:'主邮箱', value:'hao@example.com', status:'使用中', lifecycle:'主要通信身份', unknown:'恢复能力未记录', dependencies:['account-main','service-cloud'] },
    { id:'card-main', kind:'card', name:'日常支付卡', value:'•••• 2866', status:'使用中', lifecycle:'2029-08 到期', unknown:'部分自动扣款仍待确认', dependencies:['service-cloud'] },
    { id:'account-main', kind:'account', name:'主账户', value:'个人核心账户', status:'使用中', lifecycle:'身份与恢复中心', unknown:'第二恢复路径未知', dependencies:['phone-main','email-main'] },
    { id:'service-bank', kind:'service', name:'银行服务', value:'金融服务', status:'使用中', lifecycle:'需要持续可访问', unknown:'备用认证方式未记录', dependencies:['phone-main','card-main'] },
    { id:'service-cloud', kind:'service', name:'云账户', value:'数字服务', status:'使用中', lifecycle:'资料与订阅', unknown:'恢复方式不完整', dependencies:['phone-main','email-main'] },
  ],
  plan: {
    id:'plan-phone', title:'更换手机号', targetId:'phone-main', stale:true,
    lastAnalyzedRevision:6, currentRevision:8, reanalysis:null,
    actions:[
      { id:'prepare', title:'检查旧手机号承担的恢复与认证能力', detail:'确认哪些账户依赖此手机号验证身份或找回账号。', phase:'prepare', done:true, doneAt:'2026-10-28T09:30:00Z', verification:null },
      { id:'migrate', title:'添加新手机号并迁移关键账户', detail:'在关键账户中把验证与恢复方式切换到新手机号。', phase:'change', done:true, doneAt:'2026-10-28T10:16:00Z', verification:{status:'pending',verifiedAt:null} },
      { id:'verify', title:'验证新手机号恢复路径', detail:'用新手机号实际完成一次验证或恢复，确认关键路径真实可用。', phase:'verify', done:false, doneAt:null, verification:{status:'pending',verifiedAt:null} },
      { id:'retire', title:'停用旧手机号（新路径全部验证后）', detail:'只有新路径真实完成并验证后，旧手机号才能停用。', phase:'change', done:false, doneAt:null, verification:null },
    ],
  },
  records:[
    { id:'r-1', title:'检查旧手机号承担的恢复与认证能力', status:'recorded', occurredAt:null, recordedAt:'2026-10-28T09:30:00Z', verifiedAt:null, source:'正式变更计划', evidence:null },
    { id:'r-2', title:'添加新手机号并迁移关键账户', status:'recorded', occurredAt:null, recordedAt:'2026-10-28T10:16:00Z', verifiedAt:null, source:'正式变更计划', evidence:null },
  ],
};

const state = {
  view:'now', infraKind:'all', selectedAssetId:null, privacy:false,
  searchOpen:false, searchQuery:'', addOpen:false, toast:'',
  data: structuredClone(initialData),
};

function loadState() {
  try {
    const stored = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null');
    if (stored && typeof stored === 'object') {
      if (typeof stored.privacy === 'boolean') state.privacy = stored.privacy;
      if (stored.data?.assets && stored.data?.plan && stored.data?.records) state.data = stored.data;
    }
  } catch (_) {}

  const params = new URLSearchParams(location.search);
  const requestedView = params.get('view');
  if (['now','infra','change','records','me'].includes(requestedView)) state.view = requestedView;
  if (params.get('privacy') === '1') state.privacy = true;
}
function persist() { localStorage.setItem(STORAGE_KEY, JSON.stringify({privacy:state.privacy,data:state.data})); }
function esc(v) { return String(v ?? '').replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;').replaceAll("'",'&#039;'); }
function masked(v) { return state.privacy ? '<span class="masked" aria-label="身份信息已遮蔽">••••••••</span>' : esc(v); }
function fmtDate(v) {
  if (!v) return '未记录';
  const d = new Date(v);
  if (Number.isNaN(d.getTime())) return '未记录';
  return new Intl.DateTimeFormat('zh-CN',{year:'numeric',month:'2-digit',day:'2-digit'}).format(d);
}
function nowIso() { return new Date().toISOString(); }
function asset(id) { return state.data.assets.find(x=>x.id===id) || null; }
function kindAssets(kind) { return state.data.assets.filter(x=>kind==='all'||x.kind===kind); }
function countKind(kind) { return state.data.assets.filter(x=>x.kind===kind).length; }
function activeViewLabel() { return ({now:'现在',infra:'基础设施',change:'变更',records:'记录',me:'我'})[state.view]; }
function actionById(id) { return state.data.plan.actions.find(a=>a.id===id); }

function actionBlocked(id) {
  const p=state.data.plan;
  if (p.stale) return true;
  if (id==='prepare') return false;
  if (id==='migrate') return !actionById('prepare')?.done;
  if (id==='verify') {
    const a=actionById('migrate');
    return !a?.done || a.verification?.status!=='verified';
  }
  if (id==='retire') {
    const a=actionById('verify');
    return !a?.done || a.verification?.status!=='verified';
  }
  return false;
}
function actionState(a) {
  if (a.done && a.verification?.status==='verified') return 'verified';
  if (a.done && a.verification && a.verification.status!=='verified') return 'pending-verification';
  if (a.done) return 'done';
  if (actionBlocked(a.id)) return 'blocked';
  return 'current';
}
function planStatusText(p) {
  const verified=p.actions.filter(a=>a.verification?.status==='verified').length;
  const done=p.actions.filter(a=>a.done).length;
  return done+'/'+p.actions.length+' 已记录 · '+verified+' 已验证';
}

function renderSidebar() {
  const items=[['now','now','现在'],['infra','infra','基础设施'],['change','change','变更'],['records','records','记录'],['me','me','我']];
  return '<aside class="sidebar"><div class="brand"><div class="brand-mark">P</div><div class="brand-copy"><strong>PDIG</strong><span>Digital continuity</span></div></div>' +
    '<div class="nav-label">Workspace</div><nav class="nav" aria-label="主导航">' +
    items.map(([id,ic,label])=>'<button class="nav-button '+(state.view===id?'active':'')+'" data-nav="'+id+'"><span class="nav-icon">'+icon(ic)+'</span><span class="nav-text">'+label+'</span></button>').join('') +
    '</nav><div class="sidebar-foot"><div class="local-badge"><strong>'+icon('shield')+' 浏览器本地预览</strong><p>当前为 Web Preview 示例 Reality。不会把示例数据声明为你的正式 Personal Reality。</p></div>' +
    '<button class="sidebar-action" data-open-search><span class="nav-icon">'+icon('search')+'</span><span class="nav-text">全局搜索</span></button></div></aside>';
}
function renderTopbar() {
  return '<header class="topbar"><div class="breadcrumb"><span>PDIG</span><span>›</span><strong>'+activeViewLabel()+'</strong></div><div class="topbar-spacer"></div>' +
    '<button class="search-trigger" data-open-search>'+icon('search')+'<span>搜索对象、关系或变更</span><kbd>⌘ K</kbd></button>' +
    '<button class="top-action privacy-toggle" data-privacy title="'+(state.privacy?'关闭隐私遮蔽':'开启隐私遮蔽')+'">'+icon(state.privacy?'eyeoff':'eye')+'</button>' +
    '<button class="top-action primary" data-open-add>'+icon('plus')+'<span class="top-action-label">记录对象</span></button></header>';
}
function pageHead(eyebrow,title,subtitle,actions='') {
  return '<div class="page-head"><div class="page-head-copy"><div class="eyebrow">'+esc(eyebrow)+'</div><h1>'+esc(title)+'</h1><p>'+esc(subtitle)+'</p></div>' +
    (actions?'<div class="page-head-actions">'+actions+'</div>':'')+'</div>';
}
function renderAssetStrip() {
  return '<div class="asset-strip">'+['phone','email','card','account','service','device'].map(kind=>{
    const first=kindAssets(kind)[0], meta=assetKinds[kind], count=countKind(kind);
    const attrs=first?'data-select-asset="'+first.id+'"':'data-infra-kind="'+kind+'" data-nav="infra"';
    return '<button class="asset-card '+(state.selectedAssetId===first?.id?'selected':'')+'" '+attrs+'><span class="asset-icon '+kind+'">'+icon(meta.icon)+'</span><strong>'+
      esc(first?.name||meta.label)+'</strong><div class="asset-value">'+(first?masked(first.value):'当前没有已确认对象')+'</div><div class="asset-foot"><span class="dot '+(count?'good':'')+'"></span>'+
      (count?count+' 个已记录':'空列表 ≠ 外部不存在')+'</div></button>';
  }).join('')+'</div>';
}
function renderNow() {
  const p=state.data.plan, target=asset(p.targetId);
  const pending=p.actions.filter(a=>a.done&&a.verification&&a.verification.status!=='verified').length;
  const unknown=state.data.assets.filter(a=>a.unknown).length;
  const heroTitle=p.stale?'有一项变更需要重新分析当前影响':pending?'有操作已经完成，但还没有真实验证':'当前没有阻断性的连续性动作';
  const heroText=p.stale
    ? '“'+p.title+'”的分析基准是数据版本 '+p.lastAnalyzedRevision+'，当前 Reality 已经是版本 '+p.currentRevision+'。在重新分析前，执行与验证入口保持关闭。'
    : pending ? '“'+p.title+'”中仍有 '+pending+' 项已记录完成但未验证。旧路径在新路径真实验证前不能退休。'
    : '这里展示的是已确认事实和仍然未知的边界，而不是健康分数。';
  return pageHead('Current reality','你的数字生活，现在是什么状态','先看已经确认的对象和正在发生的变化。PDIG 不把未知解释成安全，也不把“做了”解释成“验证通过”。') +
    '<div class="hero"><div><div class="hero-label">CURRENT CONTINUITY</div><h2>'+esc(heroTitle)+'</h2><p>'+esc(heroText)+'</p><div class="hero-meta">' +
    '<span>'+icon('infra')+' '+state.data.assets.length+' 个已记录对象</span><span>'+icon('link')+' '+state.data.assets.reduce((n,a)=>n+a.dependencies.length,0)+' 条已确认对象关系</span><span>'+icon('warn')+' '+unknown+' 个未知边界</span></div></div>' +
    '<div class="hero-side"><div class="continuity-orbit" aria-label="连续性关系摘要"><div class="orbit-ring"></div><div class="orbit-ring second"></div><div class="orbit-core"><div><strong>'+state.data.assets.length+
    '</strong><span>已确认对象</span></div></div><span class="orbit-node n1">手机号</span><span class="orbit-node n2">账户</span><span class="orbit-node n3">服务</span><span class="orbit-node n4">支付</span></div></div></div>' +
    '<div class="section"><div class="section-head"><h2>基础设施</h2><p>先认对象，再谈依赖</p><span class="spacer"></span><button class="text-link" data-nav="infra">查看全部 →</button></div>'+renderAssetStrip()+'</div>' +
    '<div class="section two-col"><div class="panel"><div class="panel-title"><div><h3>现在最值得处理</h3><p>基于已确认事实和明确未知项，不做风险分数。</p></div></div><div class="attention-list">' +
    '<div class="attention-item" data-nav="change"><div class="attention-state">'+icon(p.stale?'refresh':'warn')+'</div><div class="attention-copy"><strong>'+(p.stale?'重新分析“更换手机号”':'验证新手机号路径')+'</strong><span>'+(p.stale?'Reality 已从版本 6 变为 8，旧影响快照不能继续写入。':'动作已记录完成，但验证证据还没有形成。')+'</span></div><span class="chevron">'+icon('chevron')+'</span></div>' +
    '<div class="attention-item" data-select-asset="account-main"><div class="attention-state">'+icon('link')+'</div><div class="attention-copy"><strong>主账户的第二恢复路径未知</strong><span>未知不等于不存在，也不等于安全；需要继续补充事实。</span></div><span class="chevron">'+icon('chevron')+'</span></div>' +
    '<div class="attention-item" data-select-asset="service-bank"><div class="attention-state">'+icon('shield')+'</div><div class="attention-copy"><strong>银行服务的备用认证方式未记录</strong><span>这里只表达缺失的事实，不猜 Passkey、Authenticator 或受信设备。</span></div><span class="chevron">'+icon('chevron')+'</span></div></div></div>' +
    '<div class="panel"><div class="panel-title"><div><h3>进行中的变更</h3><p>Current → Transition → After</p></div></div><div class="plan-list"><div class="plan-item" data-nav="change"><div class="attention-state">'+icon('change')+'</div><div class="plan-copy"><strong>'+esc(p.title)+'</strong><span>'+masked(target?.value||'')+' · '+(p.stale?'需要重新分析':planStatusText(p))+'</span><div class="plan-progress">'+
    p.actions.map(a=>'<i class="'+(a.done?'done':actionBlocked(a.id)?'':'current')+'"></i>').join('')+'</div></div><span class="chip '+(p.stale?'warn':'unknown')+'">'+(p.stale?'stale':'进行中')+'</span></div></div></div></div>';
}
function renderCollectionRow(a) {
  const meta=assetKinds[a.kind];
  return '<div class="collection-row" data-select-asset="'+a.id+'"><span class="mini-icon">'+icon(meta.icon)+'</span><span class="row-identity"><strong>'+masked(a.name)+'</strong><span>'+masked(a.value)+'</span></span>' +
    '<span class="row-cell">'+esc(a.lifecycle)+'</span><span class="row-cell hide-mid">'+esc(a.unknown)+'</span><span><span class="chip good">'+esc(a.status)+'</span></span><span class="chevron">'+icon('chevron')+'</span></div>';
}
function renderInfrastructure() {
  const filters=[['all','全部'],...Object.entries(assetKinds).map(([k,v])=>[k,v.label])], items=kindAssets(state.infraKind);
  return pageHead('Inventory → typed dependency','基础设施','这里首先是你的数字对象清单。关系只显示已经确认的事实；缺失关系保持未知，不从名称、数量或类别推断。') +
    '<div class="collection-toolbar"><div class="segment">'+filters.map(([id,label])=>'<button class="'+(state.infraKind===id?'active':'')+'" data-infra-kind="'+id+'">'+esc(label)+' '+(id==='all'?state.data.assets.length:countKind(id))+'</button>').join('')+'</div></div>' +
    (items.length?'<div class="collection"><div class="collection-row header"><span></span><span>对象</span><span>生命周期</span><span class="hide-mid">连续性边界</span><span>状态</span><span></span></div>'+items.map(renderCollectionRow).join('')+'</div>'
    :'<div class="empty"><div><strong>当前没有该类型的已确认对象</strong><p>空列表不代表外部不存在，只表示这里尚未记录。Web Preview 不会为了让页面“看起来完整”而制造对象。</p></div></div>');
}
function actionButton(a) {
  const s=actionState(a);
  if (state.data.plan.stale) return '';
  if (s==='pending-verification') return '<button class="button soft" data-action-verify="'+a.id+'">确认验证</button>';
  if (s==='current') return '<button class="button primary" data-action-done="'+a.id+'">记录完成</button>';
  if (s==='blocked') return '<button class="button" disabled>等待前置步骤</button>';
  return '';
}
function renderAction(a) {
  const s=actionState(a), map={verified:['已验证','good'],'pending-verification':['已记录完成 · 待验证','warn'],done:['已记录完成','good'],blocked:['等待前置步骤','unknown'],current:['下一步','warn']}, label=map[s];
  const mc=(s==='verified'||s==='done')?'done':(s==='current'||s==='pending-verification')?'current':'blocked';
  return '<div class="action-row"><div class="action-marker '+mc+'">'+icon((s==='verified'||s==='done')?'check':s==='blocked'?'lock':'arrow')+'</div><div class="action-copy"><strong>'+esc(a.title)+'</strong><p>'+esc(a.detail)+'</p><div class="action-meta"><span class="chip '+label[1]+'">'+label[0]+'</span><span class="chip">'+(a.phase==='prepare'?'准备':a.phase==='verify'?'验证':'变更')+'</span>'+(a.doneAt?'<span class="chip">记录于 '+fmtDate(a.doneAt)+'</span>':'')+'</div></div><div>'+actionButton(a)+'</div></div>';
}
function renderChange() {
  const p=state.data.plan, target=asset(p.targetId);
  let freshness='';
  if (p.stale) freshness='<div class="stale-banner"><div class="icon">'+icon('refresh')+'</div><div><strong>基础设施已发生变化</strong><span>分析基准 '+p.lastAnalyzedRevision+' → 当前数据版本 '+p.currentRevision+'。旧快照不能继续完成或验证。</span></div><button class="button warn" data-reanalyze>重新分析当前影响</button></div>';
  else if (p.reanalysis) freshness='<div class="stale-banner" style="background:var(--success-soft);border-color:#d5e9e3"><div class="icon" style="color:var(--success)">'+icon('check')+'</div><div><strong>重新分析完成</strong><span>这里只比较正式影响投影；不会自动把步骤标记为完成或已验证。</span></div></div><div class="delta-grid"><div class="delta-card"><span>新增影响</span><strong>'+p.reanalysis.added+'</strong></div><div class="delta-card"><span>已不在当前影响</span><strong>'+p.reanalysis.removed+'</strong></div><div class="delta-card"><span>状态变化</span><strong>'+p.reanalysis.changed+'</strong></div></div>';
  return pageHead('Change orchestration','变更','把“我想换掉一个东西”变成可验证的迁移过程。Break-Before-Make 被禁止；完成与验证是两种不同状态。','<button class="button" data-reset-plan>'+icon('refresh')+'重置演示</button>') +
    '<div class="panel"><div class="panel-title"><div><h3>'+esc(p.title)+'</h3><p>目标：'+masked(target?.name||'')+' · '+masked(target?.value||'')+'</p></div><span class="spacer"></span><span class="chip '+(p.stale?'warn':'good')+'">'+(p.stale?'需要重新分析':'正式影响已更新')+'</span></div>' +
    '<div class="change-stage"><section><div class="stage-name">Current</div><div class="stage-value">'+masked(target?.value||'')+'<span>当前 Reality · 旧号码仍然有效</span></div></section><section><div class="stage-name">Transition</div><div class="stage-value">新旧路径并存<span>先迁移并验证，再考虑停用旧路径</span></div></section><section><div class="stage-name">After · Plan projection</div><div class="stage-value">新号码承担关键能力<span>计划预期 · 未发生 · 不代表现实</span></div></section></div>'+freshness+'</div>' +
    '<div class="section panel"><div class="panel-title"><div><h3>执行路径</h3><p>每一步都基于当前计划状态；不会通过点击列表本身自动完成。</p></div></div><div class="timeline">'+p.actions.map(renderAction).join('')+'</div></div>';
}
function renderRecord(r) {
  return '<div class="record-item"><div class="record-kind">'+icon(r.status==='verified'?'shield':'history')+'</div><div class="record-copy"><strong>'+esc(r.title)+'</strong><span>'+(r.status==='verified'?'已验证':'已记录完成')+' · 来源：'+esc(r.source)+'</span><div class="record-clocks"><span><b>发生于</b> '+(r.occurredAt?fmtDate(r.occurredAt):'时间未记录')+'</span><span><b>记录于</b> '+fmtDate(r.recordedAt)+'</span><span><b>验证于</b> '+(r.verifiedAt?fmtDate(r.verifiedAt):'未验证')+'</span></div></div><div class="record-source">验证证据：'+(r.evidence?esc(r.evidence):'未记录')+'<br>记录不等于现实发生时间</div></div>';
}
function renderRecords() {
  const records=[...state.data.records].reverse();
  return pageHead('Evidence trace','记录','发生于、记录于、验证于严格分开。权威模型不知道实际发生时间时，就显示“发生时间未记录”。')+
    '<div class="panel"><div class="panel-title"><div><h3>正式变更计划记录</h3><p>“已记录完成”和“已验证”是两个独立状态；没有 evidence 时不会补写结论。</p></div><span class="spacer"></span><span class="chip unknown">'+records.length+' 条</span></div><div class="record-list">'+
    (records.length?records.map(renderRecord).join(''):'<div class="empty"><div><strong>还没有正式记录</strong><p>完成或验证动作后，记录会出现在这里。</p></div></div>')+'</div></div>';
}
function renderMe() {
  return pageHead('Personal reality','我','管理浏览器端的展示、隐私和示例 Reality。这个 Web Preview 不把浏览器示例数据冒充为 Production Authority。')+
    '<div class="settings-grid"><div class="setting-card"><h3>全局隐私遮蔽</h3><p>遮蔽对象名称、标识值和搜索回显。Truth 状态本身不会被修改：unknown 仍然是 unknown。</p><div class="setting-foot"><button class="switch '+(state.privacy?'on':'')+'" data-privacy aria-pressed="'+state.privacy+'"><i></i></button><span class="chip '+(state.privacy?'good':'unknown')+'">'+(state.privacy?'已开启':'已关闭')+'</span></div></div>' +
    '<div class="setting-card"><h3>数据模式</h3><p>当前为浏览器本地 Web Preview。示例 Reality 只用于评审产品体验，不是你的正式 Personal Reality，也不向远端发送。</p><div class="setting-foot"><span class="chip good">LOCAL PREVIEW</span><span class="chip">无网络写入</span></div></div>' +
    '<div class="setting-card"><h3>当前数据版本</h3><p>Graph revision 用于判断 ChangePlan 是否 stale。这里只展示已有版本，不把版本数字解释成安全程度。</p><div class="setting-foot"><strong style="font-size:24px">'+state.data.graphRevision+'</strong><span class="chip">graphRevision</span></div></div>' +
    '<div class="setting-card"><h3>重置 Web Preview</h3><p>清除当前浏览器保存的示例变更进度和手动新增对象，恢复内置 demo。不会影响仓库、Android 或任何真实数据。</p><div class="setting-foot"><button class="button danger" data-reset-all>恢复示例 Reality</button></div></div></div>';
}
function renderContinuityContext() {
  const unknown=state.data.assets.filter(a=>a.unknown).length;
  return '<div class="context-card"><div class="context-kicker">Impact lens</div><div class="context-title">连续性摘要</div><div class="context-sub">不是安全评分。这里只汇总已确认事实和明确未知。</div><div class="context-metric"><div class="context-metric-row"><span>已记录对象</span><strong>'+state.data.assets.length+'</strong></div><div class="context-metric-row"><span>已确认关系</span><strong>'+state.data.assets.reduce((n,a)=>n+a.dependencies.length,0)+'</strong></div><div class="context-metric-row"><span>未知边界</span><strong>'+unknown+'</strong></div></div><div class="truth-box"><strong>Truth boundary</strong><br>关系数量 ≠ 独立恢复路径。没有确认过的 recovery / authentication 事实，Web 不会通过名称或常识补齐。</div></div>' +
    '<div class="context-card"><div class="context-kicker">Next action</div><div class="context-title">先处理 stale plan</div><div class="context-sub">“更换手机号”的旧影响快照已经落后于当前 Reality。</div><div style="margin-top:14px"><button class="button soft" data-nav="change">进入变更 →</button></div></div>';
}
function renderAssetContext(a) {
  if (!a) { state.selectedAssetId=null; return renderContinuityContext(); }
  const meta=assetKinds[a.kind], deps=a.dependencies.map(asset).filter(Boolean), unsupported=['account','email','service'].includes(a.kind);
  const boundary=a.kind==='email'?'邮箱更换尚未开放正式可执行模板；恢复与认证影响不会由界面自行猜测。':a.kind==='service'?'服务迁移尚未开放正式可执行模板；不会把关系数量当作迁移计划。':'账户变更尚未开放正式可执行模板；不会用通用步骤替代真实变更语义。';
  return '<div class="context-card"><div style="display:flex;align-items:center;gap:10px"><span class="asset-icon '+a.kind+'" style="margin:0">'+icon(meta.icon)+'</span><div style="min-width:0;flex:1"><div class="context-kicker">'+esc(meta.hero)+'</div><div class="context-title">'+masked(a.name)+'</div><div class="context-sub">'+masked(a.value)+'</div></div><button class="button icon-button" data-clear-selection aria-label="关闭详情">'+icon('x')+'</button></div><div class="context-metric"><div class="context-metric-row"><span>状态</span><strong>'+esc(a.status)+'</strong></div><div class="context-metric-row"><span>生命周期</span><strong>'+esc(a.lifecycle)+'</strong></div><div class="context-metric-row"><span>已确认关系</span><strong>'+deps.length+'</strong></div></div><div class="truth-box"><strong>未知状态</strong><br>'+esc(a.unknown)+'</div></div>' +
    '<div class="context-card"><div class="context-kicker">Confirmed dependencies</div><div class="context-title">已经确认的关系</div><div class="dependency-list">'+(deps.length?deps.map(d=>'<div class="dependency-row"><span class="mini-icon">'+icon(assetKinds[d.kind].icon)+'</span><div class="dependency-copy"><strong>'+masked(d.name)+'</strong><span>'+masked(d.value)+' · 已确认对象关系</span></div></div>').join(''):'<div class="context-sub">尚无已确认关系。没有关系不代表不存在依赖。</div>')+'</div></div>' +
    (unsupported?'<div class="context-card"><div class="context-kicker">Change boundary</div><div class="context-title">此对象暂不能直接创建变更计划</div><div class="context-sub">'+boundary+'</div><div style="margin-top:14px"><button class="button soft" data-nav="change">查看变更中心 →</button></div></div>':'');
}
function renderPlanContext() {
  const p=state.data.plan, v=actionById('verify');
  return '<div class="context-card"><div class="context-kicker">Plan truth</div><div class="context-title">'+esc(p.title)+'</div><div class="context-sub">After 是计划投影，不是已经发生的 Reality。</div><div class="context-metric"><div class="context-metric-row"><span>分析基准</span><strong>'+p.lastAnalyzedRevision+'</strong></div><div class="context-metric-row"><span>当前数据版本</span><strong>'+p.currentRevision+'</strong></div><div class="context-metric-row"><span>新路径验证</span><strong>'+(v.verification?.status==='verified'?'已验证':'尚未验证')+'</strong></div></div><div class="truth-box"><strong>Make-Before-Break</strong><br>'+(actionBlocked('retire')?'旧手机号仍受保护：新路径没有全部真实验证。':'新路径已经完成并验证；旧路径现在才具备退休资格。')+'</div></div>';
}
function renderRecordsContext() {
  const verified=state.data.records.filter(r=>r.status==='verified').length;
  return '<div class="context-card"><div class="context-kicker">Three clocks</div><div class="context-title">发生 ≠ 记录 ≠ 验证</div><div class="context-sub">没有 occurredAt 时必须显示“发生时间未记录”，不能拿 doneAt 或当前时间替代。</div><div class="context-metric"><div class="context-metric-row"><span>已记录操作</span><strong>'+state.data.records.length+'</strong></div><div class="context-metric-row"><span>已验证</span><strong>'+verified+'</strong></div><div class="context-metric-row"><span>缺少验证证据</span><strong>'+state.data.records.filter(r=>!r.evidence).length+'</strong></div></div></div>';
}
function renderContext() {
  if (state.selectedAssetId) return renderAssetContext(asset(state.selectedAssetId));
  if (state.view==='change') return renderPlanContext();
  if (state.view==='records') return renderRecordsContext();
  return renderContinuityContext();
}
function renderWorkspace() {
  const body=state.view==='now'?renderNow():state.view==='infra'?renderInfrastructure():state.view==='change'?renderChange():state.view==='records'?renderRecords():renderMe();
  const context=state.view!=='me';
  return '<div class="workspace '+(context?'with-context':'')+'"><section class="page-main"><div class="demo-ribbon"><strong>Web VNext Preview</strong><span>示例数据 · Truth semantics：unknown ≠ safe，done ≠ verified，After = Plan Projection。</span></div>'+body+'</section>'+(context?'<aside class="page-context">'+renderContext()+'</aside>':'')+'</div>';
}
function renderSearchModal() {
  const results=state.searchQuery.trim()?search(state.searchQuery):state.data.assets.slice(0,5);
  const rows=results.length?results.map(a=>'<div class="search-result" data-search-result="'+a.id+'"><span class="mini-icon">'+icon(assetKinds[a.kind].icon)+'</span><div><strong>'+(state.privacy?'<span class="masked">••••••</span>':esc(a.name))+'</strong><span>'+(state.privacy?'身份信息已遮蔽':esc(a.value))+' · '+esc(assetKinds[a.kind].label)+'</span></div></div>').join(''):'<div class="empty" style="min-height:160px"><div><strong>没有找到已记录对象</strong><p>未找到不代表不存在，只表示当前 Reality 中没有匹配记录。</p></div></div>';
  return '<div class="modal-backdrop" data-modal-backdrop><div class="modal" role="dialog" aria-modal="true" aria-label="全局搜索"><div class="modal-head"><h2>搜索</h2><span class="spacer"></span><button class="button icon-button" data-close-search>'+icon('x')+'</button></div><div class="modal-body"><input class="search-input" id="global-search" autocomplete="off" placeholder="对象名称、标识、状态…" value="'+esc(state.searchQuery)+'"><div class="search-results">'+rows+'</div></div></div></div>';
}
function search(q) {
  const n=q.trim().toLowerCase();
  return state.data.assets.filter(a=>[a.name,a.value,a.status,a.lifecycle,assetKinds[a.kind].label].some(v=>String(v).toLowerCase().includes(n)));
}
function renderAddModal() {
  const opts=Object.entries(assetKinds).filter(([,v])=>v.canCreate).map(([k,v])=>'<option value="'+k+'">'+esc(v.label)+'</option>').join('');
  return '<div class="modal-backdrop" data-modal-backdrop><form class="modal" id="add-asset-form"><div class="modal-head"><h2>记录一个对象</h2><span class="spacer"></span><button type="button" class="button icon-button" data-close-add>'+icon('x')+'</button></div><div class="modal-body"><div class="truth-box" style="margin-top:0"><strong>Web Preview boundary</strong><br>这里只创建浏览器本地示例对象，不会自动生成依赖关系，也不会把名称推断成 recovery / authentication / provider 事实。</div><div class="form-grid" style="margin-top:14px"><div class="field"><label>对象类型</label><select name="kind">'+opts+'</select></div><div class="field"><label>状态</label><select name="status"><option>使用中</option><option>备用</option><option>准备停用</option></select></div><div class="field full"><label>名称</label><input name="name" required maxlength="48" placeholder="例如：工作邮箱"></div><div class="field full"><label>标识或备注</label><input name="value" required maxlength="80" placeholder="例如：name@example.com"></div><div class="field full"><label>生命周期事实（可选）</label><input name="lifecycle" maxlength="80" placeholder="例如：主要通信身份"></div></div><div class="modal-actions"><button type="button" class="button" data-close-add>取消</button><button class="button primary" type="submit">记录对象</button></div></div></form></div>';
}
function renderMobileBar() {
  return '<nav class="mobile-bar" aria-label="移动端主导航">'+[['now','now'],['infra','infra'],['change','change'],['records','records'],['me','me']].map(([id,ic])=>'<button class="'+(state.view===id?'active':'')+'" data-nav="'+id+'" aria-label="'+id+'">'+icon(ic)+'</button>').join('')+'</nav>';
}
function renderApp() {
  document.querySelector('#app').innerHTML='<div class="app-shell">'+renderSidebar()+'<main class="main-shell">'+renderTopbar()+renderWorkspace()+'</main>'+renderMobileBar()+'</div>'+(state.searchOpen?renderSearchModal():'')+(state.addOpen?renderAddModal():'')+(state.toast?'<div class="toast" role="status">'+esc(state.toast)+'</div>':'');
}
function addRecord(title,status,recordedAt,verifiedAt=null) {
  const r=state.data.records.find(x=>x.title===title);
  if (r) { r.status=status; if(recordedAt)r.recordedAt=recordedAt; if(verifiedAt)r.verifiedAt=verifiedAt; return; }
  state.data.records.push({id:'record-'+crypto.randomUUID(),title,status,occurredAt:null,recordedAt,verifiedAt,source:'正式变更计划',evidence:null});
}
function showToast(msg) {
  state.toast=msg; renderApp(); clearTimeout(showToast.timer);
  showToast.timer=setTimeout(()=>{state.toast='';renderApp();},2400);
}
function handleClick(e) {
  const nav=e.target.closest('[data-nav]');
  if (nav) { state.view=nav.dataset.nav; state.selectedAssetId=null; renderApp(); return; }
  const sel=e.target.closest('[data-select-asset]');
  if (sel) { state.selectedAssetId=sel.dataset.selectAsset; renderApp(); return; }
  const kind=e.target.closest('[data-infra-kind]');
  if (kind) { state.infraKind=kind.dataset.infraKind; if(kind.dataset.nav)state.view=kind.dataset.nav; renderApp(); return; }
  if (e.target.closest('[data-clear-selection]')) { state.selectedAssetId=null; renderApp(); return; }
  if (e.target.closest('[data-privacy]')) { state.privacy=!state.privacy; persist(); renderApp(); return; }
  if (e.target.closest('[data-open-search]')) { state.searchOpen=true; state.searchQuery=''; renderApp(); queueMicrotask(()=>document.querySelector('#global-search')?.focus()); return; }
  if (e.target.closest('[data-close-search]')) { state.searchOpen=false; renderApp(); return; }
  if (e.target.closest('[data-open-add]')) { state.addOpen=true; renderApp(); return; }
  if (e.target.closest('[data-close-add]')) { state.addOpen=false; renderApp(); return; }
  const result=e.target.closest('[data-search-result]');
  if (result) { state.searchOpen=false; state.view='infra'; state.infraKind='all'; state.selectedAssetId=result.dataset.searchResult; renderApp(); return; }
  if (e.target.closest('[data-reanalyze]')) {
    const p=state.data.plan; p.stale=false; p.lastAnalyzedRevision=p.currentRevision; p.reanalysis={added:1,removed:0,changed:0}; persist();
    showToast('重新分析完成：使用当前正式投影；没有自动完成或验证任何步骤。'); return;
  }
  const done=e.target.closest('[data-action-done]');
  if (done) {
    const a=actionById(done.dataset.actionDone); if(!a||actionBlocked(a.id)||state.data.plan.stale)return;
    const t=nowIso(); a.done=true; a.doneAt=t; addRecord(a.title,'recorded',t); persist(); showToast('已记录完成。完成 ≠ 已验证。'); return;
  }
  const verify=e.target.closest('[data-action-verify]');
  if (verify) {
    const a=actionById(verify.dataset.actionVerify); if(!a?.done||!a.verification||state.data.plan.stale)return;
    const t=nowIso(); a.verification.status='verified'; a.verification.verifiedAt=t; addRecord(a.title,'verified',a.doneAt||t,t); persist(); showToast('验证已记录。旧路径门槛会根据所有前置验证重新判断。'); return;
  }
  if (e.target.closest('[data-reset-plan]')) { state.data.plan=structuredClone(initialData.plan); state.data.records=structuredClone(initialData.records); persist(); showToast('变更演示已重置。'); return; }
  if (e.target.closest('[data-reset-all]')) { state.data=structuredClone(initialData); state.privacy=false; state.selectedAssetId=null; persist(); showToast('Web Preview 已恢复到内置示例 Reality。'); return; }
  if (e.target.matches('[data-modal-backdrop]')) { state.searchOpen=false; state.addOpen=false; renderApp(); }
}
function handleInput(e) {
  if (e.target.id!=='global-search') return;
  state.searchQuery=e.target.value;
  const results=search(state.searchQuery), box=document.querySelector('.search-results');
  if (!box) return;
  box.innerHTML=results.length?results.map(a=>'<div class="search-result" data-search-result="'+a.id+'"><span class="mini-icon">'+icon(assetKinds[a.kind].icon)+'</span><div><strong>'+(state.privacy?'<span class="masked">••••••</span>':esc(a.name))+'</strong><span>'+(state.privacy?'身份信息已遮蔽':esc(a.value))+' · '+esc(assetKinds[a.kind].label)+'</span></div></div>').join(''):'<div class="empty" style="min-height:160px"><div><strong>没有找到已记录对象</strong><p>未找到不代表不存在，只表示当前 Reality 中没有匹配记录。</p></div></div>';
}
function handleSubmit(e) {
  if (e.target.id!=='add-asset-form') return;
  e.preventDefault();
  const f=new FormData(e.target), kind=String(f.get('kind')||''), name=String(f.get('name')||'').trim(), value=String(f.get('value')||'').trim();
  if (!assetKinds[kind]?.canCreate || !name || !value) return;
  const id=kind+'-'+crypto.randomUUID();
  state.data.assets.push({id,kind,name,value,status:String(f.get('status')||'使用中'),lifecycle:String(f.get('lifecycle')||'').trim()||'生命周期事实未记录',unknown:'连续性关系尚未确认',dependencies:[]});
  state.data.graphRevision+=1; state.data.plan.currentRevision=state.data.graphRevision;
  if (state.data.plan.lastAnalyzedRevision<state.data.graphRevision) state.data.plan.stale=true;
  state.addOpen=false; state.view='infra'; state.infraKind=kind; state.selectedAssetId=id; persist();
  showToast('对象已记录到浏览器本地示例 Reality；没有自动生成任何依赖关系。');
}
function handleKeydown(e) {
  if ((e.metaKey||e.ctrlKey)&&e.key.toLowerCase()==='k') { e.preventDefault(); state.searchOpen=true; state.searchQuery=''; renderApp(); queueMicrotask(()=>document.querySelector('#global-search')?.focus()); }
  if (e.key==='Escape'&&(state.searchOpen||state.addOpen)) { state.searchOpen=false; state.addOpen=false; renderApp(); }
}
loadState();
document.addEventListener('click',handleClick);
document.addEventListener('input',handleInput);
document.addEventListener('submit',handleSubmit);
document.addEventListener('keydown',handleKeydown);
renderApp();
