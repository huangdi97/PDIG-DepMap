/**
 * Node Resolver — Proposal 之前必须先解决实体 (GOAL §14 / CANONICAL §5.6/§6.7)
 *
 * 顺序（不得跳步）：
 *   1. builtin alias exact
 *   2. normalized exact
 *   3. conservative fuzzy（唯一候选且相似度 ≥0.9）
 *   4. 用户确认（unresolved / ambiguous → 不生成 Proposal）
 *
 * 禁止 embedding / LLM / vector DB。
 */

import type { DepNode } from '../domain/types.ts'

export type ResolutionMatchType = 'alias_exact' | 'normalized_exact' | 'fuzzy'

export type ResolutionResult =
  | { status: 'resolved'; nodeId: string; matchType: ResolutionMatchType }
  | { status: 'ambiguous'; candidates: Array<{ nodeId: string; matchType: ResolutionMatchType; score: number }> }
  | { status: 'unresolved' }

/** 内置 alias 表：商户原始名 → 规范名（只做保守映射，可扩展）。 */
export const BUILTIN_ALIASES: Record<string, string[]> = {
  腾讯视频: ['腾讯视频VIP', '腾讯视频VIP会员', '腾讯视频VIP连续包月', '腾讯科技(深圳)有限公司'],
  网易云音乐: ['网易云音乐VIP', '网易云音乐黑胶VIP', '杭州网易云音乐科技有限公司'],
  中国移动: ['中国移动通信', '中国移动通信集团', '中国移动话费充值'],
  爱奇艺: ['爱奇艺VIP', '北京爱奇艺科技有限公司'],
  美团: ['美团平台商户', '北京三快在线科技有限公司', '美团外卖'],
  饿了么: ['饿了么平台商户', '拉扎斯网络科技(上海)有限公司'],
  滴滴出行: ['滴滴出行科技有限公司'],
  京东: ['京东平台商户', '北京京东世纪贸易有限公司'],
  淘宝: ['淘宝平台商户', '支付宝(中国)网络技术有限公司'],
  Apple: ['Apple Distribut', 'APPLE.COM/BILL']
}

export interface ResolvableEntity {
  nodeId: string
  name: string
  /** 用户/内置为该节点登记的别名 */
  aliases?: string[]
}

export function normalizeMerchantName(name: string): string {
  return name
    .trim()
    .toLowerCase()
    .replace(/\s+/g, '')
    .replace(/[·・]/g, '')
    .replace(/[（]/g, '(')
    .replace(/[）]/g, ')')
}

/** 编辑距离相似度（0..1）。 */
export function similarity(a: string, b: string): number {
  if (a === b) return 1
  const m = a.length
  const n = b.length
  if (m === 0 || n === 0) return 0
  let prev = new Array<number>(n + 1)
  let cur = new Array<number>(n + 1)
  for (let j = 0; j <= n; j++) prev[j] = j
  for (let i = 1; i <= m; i++) {
    cur[0] = i
    for (let j = 1; j <= n; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1
      cur[j] = Math.min(prev[j]! + 1, cur[j - 1]! + 1, prev[j - 1]! + cost)
    }
    ;[prev, cur] = [cur, prev]
  }
  return 1 - prev[n]! / Math.max(m, n)
}

const FUZZY_THRESHOLD = 0.9

/**
 * 解析商户原始名到已有节点。
 * 结果为 ambiguous / unresolved 时不得生成 Proposal（等待用户确认）。
 */
export function resolveMerchant(merchantRaw: string, entities: ResolvableEntity[]): ResolutionResult {
  const raw = merchantRaw.trim()
  if (raw === '') return { status: 'unresolved' }
  const normalized = normalizeMerchantName(raw)

  // 1. builtin alias exact：merchantRaw 命中 alias 表 → 规范名，再与节点名/别名精确匹配
  const canonicalNames = new Set<string>()
  for (const [canonical, aliases] of Object.entries(BUILTIN_ALIASES)) {
    if (canonical === raw || aliases.includes(raw) || normalizeMerchantName(canonical) === normalized || aliases.some(a => normalizeMerchantName(a) === normalized)) {
      canonicalNames.add(canonical)
    }
  }

  // 2. normalized exact（含 alias exact 命中）
  const exactMatches: Array<{ nodeId: string; matchType: ResolutionMatchType }> = []
  for (const e of entities) {
    const eNames = [e.name, ...(e.aliases ?? [])]
    const eNorm = eNames.map(normalizeMerchantName)
    if (canonicalNames.has(e.name) || canonicalNames.has(e.name.trim())) {
      exactMatches.push({ nodeId: e.nodeId, matchType: 'alias_exact' })
    } else if (eNorm.includes(normalized)) {
      exactMatches.push({ nodeId: e.nodeId, matchType: 'alias_exact' })
    }
  }
  if (exactMatches.length === 1) {
    return { status: 'resolved', nodeId: exactMatches[0]!.nodeId, matchType: exactMatches[0]!.matchType }
  }
  if (exactMatches.length > 1) {
    return {
      status: 'ambiguous',
      candidates: exactMatches.map(m => ({ ...m, score: 1 }))
    }
  }

  // 3. conservative fuzzy：唯一候选且相似度 ≥ 0.9
  const fuzzy: Array<{ nodeId: string; score: number }> = []
  for (const e of entities) {
    const names = [e.name, ...(e.aliases ?? [])].map(normalizeMerchantName)
    const best = Math.max(...names.map(n => similarity(normalized, n)))
    if (best >= FUZZY_THRESHOLD) fuzzy.push({ nodeId: e.nodeId, score: Math.round(best * 1000) / 1000 })
  }
  fuzzy.sort((a, b) => b.score - a.score || (a.nodeId < b.nodeId ? -1 : 1))
  if (fuzzy.length === 1 && fuzzy[0]!.score >= FUZZY_THRESHOLD) {
    return { status: 'resolved', nodeId: fuzzy[0]!.nodeId, matchType: 'fuzzy' }
  }
  if (fuzzy.length > 1) {
    return { status: 'ambiguous', candidates: fuzzy.map(f => ({ ...f, matchType: 'fuzzy' as const })) }
  }

  // 4. 用户确认
  return { status: 'unresolved' }
}

// ---------------------------------------------------------------------------
// 支付方式解析：'招商银行信用卡(4417)' → 卡节点线索；'零钱' → 微信账户自身
// ---------------------------------------------------------------------------

export interface PaymentMethodInfo {
  kind: 'bank_card' | 'wechat_balance' | 'wechat_change_pocket' | 'other'
  bankName: string | null
  cardType: 'credit' | 'debit' | null
  last4: string | null
  raw: string
}

export function parsePaymentMethod(raw: string): PaymentMethodInfo {
  const s = raw.trim()
  // 形如 '招商银行信用卡(4417)' / '工商银行储蓄卡(1234)'
  const m = /^(.+?)(信用卡|储蓄卡)[（(](\d{4})[）)]$/.exec(s)
  if (m) {
    return {
      kind: 'bank_card',
      bankName: m[1]!,
      cardType: m[2] === '信用卡' ? 'credit' : 'debit',
      last4: m[3]!,
      raw: s
    }
  }
  if (s === '零钱') return { kind: 'wechat_balance', bankName: null, cardType: null, last4: null, raw: s }
  if (s === '零钱通') return { kind: 'wechat_change_pocket', bankName: null, cardType: null, last4: null, raw: s }
  return { kind: 'other', bankName: null, cardType: null, last4: null, raw: s }
}

/** 在已有节点中按 bank + last4 精确找银行卡节点（ Resolver 顺序的 normalized exact 应用）。 */
export function findBankCardNode(info: PaymentMethodInfo, nodes: DepNode[]): DepNode | null {
  if (info.kind !== 'bank_card' || info.last4 === null) return null
  const matches = nodes.filter(n => {
    if (n.kind !== 'payment_instrument') return false
    if (n.last4 !== info.last4) return false
    // 银行名放宽：节点 issuer 或 name 包含银行名（招行/招商银行）
    const bank = info.bankName ?? ''
    const haystack = `${n.issuer ?? ''}${n.name}`
    return bank === '' || haystack.includes(bank) || bank.includes(haystack) || aliasBankMatch(bank, haystack)
  })
  return matches.length === 1 ? matches[0]! : null
}

function aliasBankMatch(bank: string, haystack: string): boolean {
  const table: Record<string, string[]> = {
    招行: ['招商银行', '招商'],
    工行: ['工商银行', '工商'],
    建行: ['建设银行', '建设'],
    中行: ['中国银行'],
    农行: ['农业银行']
  }
  for (const [alias, fulls] of Object.entries(table)) {
    if ((bank.startsWith(alias) || fulls.some(f => bank.startsWith(f))) && fulls.some(f => haystack.includes(f) || haystack.includes(alias))) {
      return true
    }
  }
  return false
}
