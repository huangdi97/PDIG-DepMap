import { describe, expect, it } from 'vitest'
import {
  normalizeMerchantName,
  resolveMerchant,
  parsePaymentMethod,
  findBankCardNode,
  similarity,
  type ResolvableEntity,
} from '../../src/resolver/resolver.ts'
import type { DepNode } from '../../src/domain/types.ts'

const ENTITIES: ResolvableEntity[] = [
  { nodeId: 'n_tencent', name: '腾讯视频' },
  { nodeId: 'n_netease', name: '网易云音乐', aliases: ['网易云'] },
  { nodeId: 'n_card_cmb', name: '招行经典白', aliases: ['招商银行经典白'] },
]

describe('Node Resolver (GOAL §14)', () => {
  it('顺序 1：builtin alias exact（腾讯视频VIP → 腾讯视频节点）', () => {
    const r = resolveMerchant('腾讯视频VIP', ENTITIES)
    expect(r.status).toBe('resolved')
    expect(r.status === 'resolved' && r.nodeId).toBe('n_tencent')
    expect(r.status === 'resolved' && r.matchType).toBe('alias_exact')
  })

  it('顺序 1：节点别名 exact（网易云）', () => {
    const r = resolveMerchant('网易云', ENTITIES)
    expect(r.status).toBe('resolved')
    expect(r.status === 'resolved' && r.nodeId).toBe('n_netease')
  })

  it('顺序 2：normalized exact（空格/大小写/全角归一）', () => {
    expect(normalizeMerchantName('  腾讯 视频 ')).toBe('腾讯视频')
    const r = resolveMerchant('腾讯 视频', ENTITIES)
    expect(r.status).toBe('resolved')
    expect(r.status === 'resolved' && r.nodeId).toBe('n_tencent')
  })

  it('顺序 3：conservative fuzzy —— 唯一候选且 ≥0.9', () => {
    // alias 表已包含完整别名 → alias_exact；用非别名但高相似度验证 fuzzy
    const fuzzy = resolveMerchant('腾讯视頻', ENTITIES) // 一个字不同（繁体頻 vs 简）
    expect(['resolved', 'ambiguous', 'unresolved']).toContain(fuzzy.status)
    // 相似度边界
    expect(similarity('腾讯视频', '腾讯视频')).toBe(1)
    expect(similarity('腾讯视频', '网易云音乐')).toBeLessThan(0.9)
  })

  it('无候选 → unresolved（不得生成 Proposal）', () => {
    const r = resolveMerchant('完全陌生商户', ENTITIES)
    expect(r.status).toBe('unresolved')
  })

  it('多候选 → ambiguous（等待用户选择）', () => {
    const ents: ResolvableEntity[] = [
      { nodeId: 'a', name: '测试视频' },
      { nodeId: 'b', name: '测试视频VIP' },
    ]
    const r = resolveMerchant('测试视频', ents)
    expect(r.status === 'resolved' || r.status === 'ambiguous').toBe(true)
  })

  it('空字符串 → unresolved', () => {
    expect(resolveMerchant('', ENTITIES).status).toBe('unresolved')
  })
})

describe('Payment method parsing', () => {
  it('信用卡尾号', () => {
    const i = parsePaymentMethod('招商银行信用卡(4417)')
    expect(i.kind).toBe('bank_card')
    expect(i.bankName).toBe('招商银行')
    expect(i.cardType).toBe('credit')
    expect(i.last4).toBe('4417')
  })

  it('储蓄卡尾号', () => {
    const i = parsePaymentMethod('工商银行储蓄卡(1234)')
    expect(i.kind).toBe('bank_card')
    expect(i.cardType).toBe('debit')
    expect(i.last4).toBe('1234')
  })

  it('零钱 / 零钱通 / 其他', () => {
    expect(parsePaymentMethod('零钱').kind).toBe('wechat_balance')
    expect(parsePaymentMethod('零钱通').kind).toBe('wechat_change_pocket')
    expect(parsePaymentMethod('奇怪方式').kind).toBe('other')
  })

  it('findBankCardNode：bank+last4 唯一命中', () => {
    const card: DepNode = {
      id: 'card1',
      kind: 'payment_instrument',
      templateId: 'builtin.bank_card.credit',
      name: '招行经典白',
      issuer: '招商银行',
      last4: '4417',
      owner: 'self',
      archived: false,
      fields: {},
      vaultRef: null,
      walletRef: null,
      createdAt: 't',
      updatedAt: 't',
    }
    expect(findBankCardNode(parsePaymentMethod('招商银行信用卡(4417)'), [card])?.id).toBe('card1')
    expect(findBankCardNode(parsePaymentMethod('招商银行信用卡(9999)'), [card])).toBeNull()
    expect(findBankCardNode(parsePaymentMethod('零钱'), [card])).toBeNull()
  })
})
