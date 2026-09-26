// Node Resolver（纯 ArkTS，零 @ohos；主机单测可执行）。
//
// 移植自 core/src/resolver/resolver.ts 的冻结基准。解析顺序（AGENTS §15，不得跳步）：
//   1. builtin alias exact → 2. normalized exact → 3. conservative fuzzy（唯一候选且 ≥0.9）
//   → 4. 用户确认（unresolved / ambiguous → **不生成 Proposal**）。
// 禁止 embedding / LLM / vector DB。
//
// ArkTS 约束：无正则（手工规范化）、无解构、BUILTIN_ALIASES 用数组（Record 无法迭代）。

/** 可解析实体：已有节点（含别名）。 */
export class ResolvableEntity {
  nodeId: string = '';
  name: string = '';
  aliases: string[] = [];
}

/** 解析结果：resolved / ambiguous / unresolved（MVP 只关心 resolved 与其余两类）。 */
export class ResolutionResult {
  status: string = 'unresolved';
  nodeId: string = '';
  matchType: string = '';
}

/** 内置 alias 表条目（merchantRaw 命中 → canonical 名，再与节点名/别名精确匹配）。 */
export class BuiltinAlias {
  canonical: string;
  aliases: string[];

  constructor(canonical: string, aliases: string[]) {
    this.canonical = canonical;
    this.aliases = aliases;
  }
}

/** 与 resolver.ts BUILTIN_ALIASES 逐条一致（顺序无关紧要，数组便于 ArkTS 迭代）。 */
export const BUILTIN_ALIASES: BuiltinAlias[] = [
  new BuiltinAlias('腾讯视频', ['腾讯视频VIP', '腾讯视频VIP会员', '腾讯视频VIP连续包月', '腾讯科技(深圳)有限公司']),
  new BuiltinAlias('网易云音乐', ['网易云音乐VIP', '网易云音乐黑胶VIP', '杭州网易云音乐科技有限公司']),
  new BuiltinAlias('中国移动', ['中国移动通信', '中国移动通信集团', '中国移动话费充值']),
  new BuiltinAlias('爱奇艺', ['爱奇艺VIP', '北京爱奇艺科技有限公司']),
  new BuiltinAlias('美团', ['美团平台商户', '北京三快在线科技有限公司', '美团外卖']),
  new BuiltinAlias('饿了么', ['饿了么平台商户', '拉扎斯网络科技(上海)有限公司']),
  new BuiltinAlias('滴滴出行', ['滴滴出行科技有限公司']),
  new BuiltinAlias('京东', ['京东平台商户', '北京京东世纪贸易有限公司']),
  new BuiltinAlias('淘宝', ['淘宝平台商户', '支付宝(中国)网络技术有限公司']),
  new BuiltinAlias('Apple', ['Apple Distribut', 'APPLE.COM/BILL']),
];

/**
 * normalizeMerchantName（resolver.ts 同语义，无正则实现）：
 * trim → lowercase → 删除全部空白 → 删除 '·'/'・' → （/）→ (/)。返回空串仅当输入全空白。
 */
export function normalizeMerchantName(name: string): string {
  let out = '';
  const s = name.trim().toLowerCase();
  for (const ch of s) {
    if (ch === ' ' || ch === '\t' || ch === '\n' || ch === '\r' || ch === '·' || ch === '・') {
      continue;
    }
    if (ch === '（') {
      out += '(';
    } else if (ch === '）') {
      out += ')';
    } else {
      out += ch;
    }
  }
  return out;
}

/** 编辑距离相似度（0..1，Levenshtein）。 */
export function similarity(a: string, b: string): number {
  if (a === b) {
    return 1;
  }
  const m = a.length;
  const n = b.length;
  if (m === 0 || n === 0) {
    return 0;
  }
  const prev: number[] = new Array<number>(n + 1);
  const cur: number[] = new Array<number>(n + 1);
  for (let j = 0; j <= n; j++) {
    prev[j] = j;
  }
  for (let i = 1; i <= m; i++) {
    cur[0] = i;
    for (let j = 1; j <= n; j++) {
      const cost = a.charAt(i - 1) === b.charAt(j - 1) ? 0 : 1;
      const up = prev[j];
      const left = cur[j - 1];
      const diag = prev[j - 1];
      cur[j] = Math.min(up + 1, left + 1, diag + cost);
    }
    // swap prev/cur（无解构）
    for (let j = 0; j <= n; j++) {
      prev[j] = cur[j];
    }
  }
  const dist = prev[n];
  return 1 - dist / Math.max(m, n);
}

const FUZZY_THRESHOLD = 0.9;

/**
 * 解析商户原始名到已有节点（顺序：alias exact → normalized exact → conservative fuzzy）。
 * unresolved / ambiguous 一律返回非 resolved（调用方不得生成 Proposal）。
 */
export function resolveMerchant(merchantRaw: string, entities: ResolvableEntity[]): ResolutionResult {
  const raw = merchantRaw.trim();
  const result = new ResolutionResult();
  if (raw === '') {
    return result;
  }
  const normalized = normalizeMerchantName(raw);

  // 1. builtin alias exact：merchantRaw 命中 alias 表 → canonical 名集合
  const canonicalNames = new Set<string>();
  for (const alias of BUILTIN_ALIASES) {
    let hit = alias.canonical === raw;
    for (const a of alias.aliases) {
      if (a === raw) {
        hit = true;
      }
    }
    if (normalizeMerchantName(alias.canonical) === normalized) {
      hit = true;
    }
    for (const a of alias.aliases) {
      if (normalizeMerchantName(a) === normalized) {
        hit = true;
      }
    }
    if (hit) {
      canonicalNames.add(alias.canonical);
    }
  }

  // 2. normalized exact（含 alias exact 命中）
  const exactMatches: Array<ResolvableEntity> = [];
  for (const e of entities) {
    let matched = false;
    if (canonicalNames.has(e.name)) {
      matched = true;
    }
    if (!matched) {
      const eNorm = normalizeMerchantName(e.name);
      if (eNorm === normalized) {
        matched = true;
      }
    }
    if (!matched) {
      for (const a of e.aliases) {
        if (normalizeMerchantName(a) === normalized) {
          matched = true;
          break;
        }
      }
    }
    if (matched) {
      exactMatches.push(e);
    }
  }
  if (exactMatches.length === 1) {
    const only = exactMatches[0];
    if (only !== undefined) {
      result.status = 'resolved';
      result.nodeId = only.nodeId;
      result.matchType = 'alias_exact';
      return result;
    }
  }
  if (exactMatches.length > 1) {
    result.status = 'ambiguous';
    return result;
  }

  // 3. conservative fuzzy：唯一候选且相似度 ≥ 0.9
  const fuzzy: Array<ResolvableEntity> = [];
  const fuzzyScores: Array<number> = [];
  for (const e of entities) {
    const names: string[] = [normalizeMerchantName(e.name)];
    for (const a of e.aliases) {
      names.push(normalizeMerchantName(a));
    }
    let best = 0;
    for (const n of names) {
      const sim = similarity(normalized, n);
      if (sim > best) {
        best = sim;
      }
    }
    if (best >= FUZZY_THRESHOLD) {
      fuzzy.push(e);
      fuzzyScores.push(Math.round(best * 1000) / 1000);
    }
  }
  if (fuzzy.length === 1) {
    const only = fuzzy[0];
    if (only !== undefined) {
      result.status = 'resolved';
      result.nodeId = only.nodeId;
      result.matchType = 'fuzzy';
      return result;
    }
  }
  if (fuzzy.length > 1) {
    result.status = 'ambiguous';
    return result;
  }

  // 4. 用户确认
  return result;
}