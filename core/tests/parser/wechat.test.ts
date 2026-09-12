import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import {
  parseWechatBill,
  decodeBill,
  detect,
  parseCsvLine,
} from '../../src/parser/wechat/parser.ts'
import { detectRecurrence } from '../../src/parser/wechat/recurring.ts'
import {
  assignFingerprints,
  computeFingerprintWithTxnId,
} from '../../src/fingerprint/fingerprint.ts'

const FX = (name: string) =>
  new Uint8Array(readFileSync(join(import.meta.dirname, '..', 'fixtures', name)))

describe('WeChat Parser — fixtures (MVP_ACCEPTANCE D)', () => {
  it('normal fixture：11 列、金额、方向、卡尾号解析', () => {
    const r = parseWechatBill(FX('normal-wechat.csv'))
    expect(r.errors).toHaveLength(0)
    expect(r.observations).toHaveLength(6)
    const first = r.observations[0]!
    expect(first.merchantRaw).toBe('腾讯视频')
    expect(first.amount).toBe(25)
    expect(first.direction).toBe('out')
    expect(first.paymentMethodRaw).toBe('招商银行信用卡(4417)')
    expect(first.sourceTxnId).toMatch(/^4200001978/)
    expect(first.occurredAt).toBe('2026-06-03T10:23:00+08:00')
    const redPacket = r.observations.find((o) => o.merchantRaw === '张三')!
    expect(redPacket.direction).toBe('in')
    expect(redPacket.amount).toBe(200)
  })

  it('BOM fixture：UTF-8 BOM 正确剥离', () => {
    const raw = FX('utf8-bom.csv')
    expect(raw[0]).toBe(0xef)
    const { encoding } = decodeBill(raw)
    expect(encoding).toBe('utf-8')
    const r = parseWechatBill(raw)
    expect(r.errors).toHaveLength(0)
    expect(r.observations).toHaveLength(2)
    expect(r.observations[0]!.merchantRaw).toBe('腾讯视频')
  })

  it('GBK fixture：GB18030 解码', () => {
    const { encoding } = decodeBill(FX('gbk.csv'))
    expect(encoding).toBe('gb18030')
    const r = parseWechatBill(FX('gbk.csv'))
    expect(r.errors).toHaveLength(0)
    expect(r.observations).toHaveLength(2)
    expect(r.observations[0]!.merchantRaw).toBe('腾讯视频')
    expect(r.observations[1]!.merchantRaw).toBe('肯德基')
  })

  it('header-offset fixture：说明行跳过，正确定位列头', () => {
    const r = parseWechatBill(FX('header-offset.csv'))
    expect(r.errors).toHaveLength(0)
    expect(r.observations).toHaveLength(1)
    expect(r.observations[0]!.merchantRaw).toBe('腾讯视频')
  })

  it('refund fixture：退款行保留为观测事实并带状态', () => {
    const r = parseWechatBill(FX('refund.csv'))
    expect(r.errors).toHaveLength(0)
    expect(r.observations).toHaveLength(3)
    const refunded = r.observations.find((o) => o.status === '已全额退款')!
    expect(refunded.amount).toBe(90)
    const refundBack = r.observations.find((o) => o.status === '退款成功')!
    expect(refundBack.direction).toBe('in')
  })

  it('malformed fixture：坏行计入 errors，不抛出、不影响好行', () => {
    const r = parseWechatBill(FX('malformed.csv'))
    expect(r.observations).toHaveLength(2)
    expect(r.observations.map((o) => o.merchantRaw).sort()).toEqual(['正常行', '腾讯视频'])
    expect(r.errors.length).toBe(4)
    expect(r.errors.every((e) => e.line > 0)).toBe(true)
  })

  it('same-amount-twice fixture：同金额两行都保留', () => {
    const r = parseWechatBill(FX('same-amount-twice.csv'))
    expect(r.observations).toHaveLength(2)
    expect(r.observations.every((o) => o.amount === 25)).toBe(true)
  })

  it('duplicate-import fixture：文件内完全重复行（无单号）产生 ordinal 指纹，不互相吞并', () => {
    const r = parseWechatBill(FX('duplicate-import.csv'))
    expect(r.observations).toHaveLength(2)
    const fps = assignFingerprints('secret', r.observations, {
      adapterId: 'wechat_statement',
      sourceInstanceId: 'inst-wechat-test',
    })
    expect(fps[0]!.fingerprint).not.toBe(fps[1]!.fingerprint)
    expect(fps.every((f) => !f.stable)).toBe(true)
  })

  it('detect：微信账单特征识别', () => {
    expect(detect(new TextDecoder().decode(FX('normal-wechat.csv')))).toBeGreaterThan(0.5)
    expect(detect('name,amount\nfoo,1')).toBe(0)
  })

  it('parseCsvLine：引号内逗号与转义', () => {
    expect(parseCsvLine('a,"b,c","d""e"')).toEqual(['a', 'b,c', 'd"e'])
  })
})

describe('Fingerprint (GOAL §12 / MVP_ACCEPTANCE A)', () => {
  it('优先稳定交易号：HMAC-SHA256(fpSecret, adapterId:sourceInstanceId:sourceTxnId)', () => {
    const fp = computeFingerprintWithTxnId('secret', 'wechat_statement', 'inst-wechat-test', 'TXN1')
    expect(fp).toMatch(/^[0-9a-f]{64}$/)
    expect(
      computeFingerprintWithTxnId('secret', 'wechat_statement', 'inst-wechat-test', 'TXN1'),
    ).toBe(fp)
    expect(
      computeFingerprintWithTxnId('other-secret', 'wechat_statement', 'inst-wechat-test', 'TXN1'),
    ).not.toBe(fp)
    // v2 隔离：同 txn id 跨 SourceInstance 不冲突
    expect(
      computeFingerprintWithTxnId('secret', 'wechat_statement', 'inst-other', 'TXN1'),
    ).not.toBe(fp)
  })

  it('重复导入测试：1–6 月导入后导入 1–8 月，只累计 7–8 月新记录', () => {
    const janJun = parseWechatBill(FX('dup-jan-jun.csv')).observations
    const janAug = parseWechatBill(FX('dup-jan-aug.csv')).observations
    expect(janJun).toHaveLength(6)
    expect(janAug).toHaveLength(8)

    const fpSecret = 'test-fp-secret'
    const firstSession = assignFingerprints(fpSecret, janJun, {
      adapterId: 'wechat_statement',
      sourceInstanceId: 'inst-wechat-test',
    })
    expect(firstSession.every((f) => f.stable)).toBe(true)

    // 模拟第二次导入：逐条检查指纹是否已存在
    const known = new Set(firstSession.map((f) => f.fingerprint))
    const secondSession = assignFingerprints(fpSecret, janAug, {
      adapterId: 'wechat_statement',
      sourceInstanceId: 'inst-wechat-test',
    })
    const fresh = secondSession.filter((f) => !known.has(f.fingerprint))
    const duplicates = secondSession.filter((f) => known.has(f.fingerprint))
    expect(duplicates).toHaveLength(6)
    expect(fresh).toHaveLength(2)
  })

  it('无稳定交易号：canonical row 回退，不同金额/时间产生不同指纹', () => {
    const obs = parseWechatBill(FX('duplicate-import.csv')).observations
    const other = parseWechatBill(FX('same-amount-twice.csv')).observations
    const fps1 = assignFingerprints('secret', obs, {
      adapterId: 'wechat_statement',
      sourceInstanceId: 'inst-wechat-test',
    })
    const fps2 = assignFingerprints('secret', other, {
      adapterId: 'wechat_statement',
      sourceInstanceId: 'inst-wechat-test',
    })
    // duplicate-import 两行（同 canonical）→ #1/#2 不同
    expect(fps1[0]!.fingerprint).not.toBe(fps1[1]!.fingerprint)
    // same-amount-twice 两行时间不同 → 指纹不同
    expect(fps2[0]!.fingerprint).not.toBe(fps2[1]!.fingerprint)
  })

  it('不泄漏：指纹输出与原始交易号/商户/金额字符串无直接包含关系', () => {
    const obs = parseWechatBill(FX('normal-wechat.csv')).observations
    const fps = assignFingerprints('secret', obs, {
      adapterId: 'wechat_statement',
      sourceInstanceId: 'inst-wechat-test',
    })
    for (let i = 0; i < obs.length; i++) {
      const o = obs[i]!
      const fp = fps[i]!.fingerprint
      expect(fp.includes(o.merchantRaw)).toBe(false)
      expect(fp.includes(o.description)).toBe(false)
      if (o.sourceTxnId) expect(fp.includes(o.sourceTxnId)).toBe(false)
    }
  })
})

describe('Recurrence detection (GOAL §15)', () => {
  it('recurring-monthly：腾讯视频 6 次月付 → monthly + 高置信度', () => {
    const r = parseWechatBill(FX('recurring-monthly.csv'))
    const tencent = r.observations.filter((o) => o.merchantRaw === '腾讯视频')
    const rec = detectRecurrence('腾讯视频', tencent)
    expect(rec).not.toBeNull()
    expect(rec!.period).toBe('monthly')
    expect(rec!.occurrences).toBe(6)
    expect(rec!.confidence).toBeGreaterThan(0.7)
    expect(rec!.typicalAmount).toBe(25)
    expect(rec!.paymentMethods).toEqual(['招商银行信用卡(4417)'])
  })

  it('non-recurring：不规则间隔/金额 → 不识别为周期', () => {
    const r = parseWechatBill(FX('non-recurring.csv'))
    const rec = detectRecurrence('测试商户', r.observations)
    expect(rec).toBeNull()
  })

  it('退款观测不参与周期识别', () => {
    const r = parseWechatBill(FX('refund.csv'))
    const rec = detectRecurrence('某电商平台', r.observations)
    expect(rec).toBeNull() // 只有 1 次有效支出（90 已退款被排除）
  })

  it('少于 2 次有效观测 → null', () => {
    expect(detectRecurrence('x', [])).toBeNull()
  })
})
