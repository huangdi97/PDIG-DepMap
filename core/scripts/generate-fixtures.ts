/**
 * 合成微信账单 fixtures 生成器（确定性）。
 * 运行：node --experimental-strip-types scripts/generate-fixtures.ts
 * 真实账单禁止提交 Git —— 本目录只允许合成数据 (AGENTS §16/§21)。
 */
import { mkdirSync, writeFileSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import iconv from 'iconv-lite'

const outDir = join(dirname(fileURLToPath(import.meta.url)), '..', 'tests', 'fixtures')
mkdirSync(outDir, { recursive: true })

const HEADER =
  '交易时间,交易类型,交易对方,商品,收/支,金额(元),支付方式,当前状态,交易单号,商户单号,备注'

function preamble(start: string, end: string, count: number, income: string, expense: string): string[] {
  return [
    '微信支付账单明细,,,,,,,',
    `微信支付账单明细,起始时间:[${start}]  终止时间:[${end}],,,,,,,`,
    '导出类型:[全部],,,,,,,,',
    '导出时间:[2026-09-01 09:00:00],,,,,,,,',
    `共${count}笔记录,,,,,,,`,
    `收入:${income},,,,,,,,`,
    `支出:${expense},,,,,,,,`,
    '中性交易:0笔 0.00元,,,,,,,,',
    '备注：以下是微信支付账单明细列表：,,,,,,,,',
    '----------------------微信支付账单明细列表--------------------,,,,,,,'
  ]
}

function row(time: string, type: string, merchant: string, product: string, dir: string, amount: string, method: string, status: string, txn: string, merchantTxn = '', note = '/') {
  return [time, type, merchant, product, dir, `¥${amount}`, method, status, txn, merchantTxn, note].join(',')
}

let txnSeq = 1000
function txn(date: string): string {
  txnSeq += 1
  return `4200001978${date.replace(/[- :]/g, '')}${String(txnSeq).padStart(6, '0')}`
}

function write(name: string, lines: string[], encoding: 'utf8' | 'gb18030' = 'utf8', bom = false): void {
  const content = lines.join('\r\n') + '\r\n'
  if (bom) {
    writeFileSync(join(outDir, name), Buffer.concat([Buffer.from([0xef, 0xbb, 0xbf]), Buffer.from(content, 'utf8')]))
  } else if (encoding === 'gb18030') {
    writeFileSync(join(outDir, name), iconv.encode(content, 'gb18030'))
  } else {
    writeFileSync(join(outDir, name), Buffer.from(content, 'utf8'))
  }
}

// 1. normal-wechat.csv —— 常规混合账单
{
  const lines = [
    ...preamble('2026-06-01 00:00:00', '2026-06-30 23:59:59', 6, '1笔 200.00元', '5笔 310.00元'),
    HEADER,
    row('2026-06-03 10:23:00', '商户消费', '腾讯视频', 'VIP会员连续包月', '支出', '25.00', '招商银行信用卡(4417)', '支付成功', txn('20260603')),
    row('2026-06-05 09:10:00', '商户消费', '网易云音乐', '黑胶VIP连续包月', '支出', '15.00', '零钱', '支付成功', txn('20260605')),
    row('2026-06-10 18:30:00', '商户消费', '美团平台商户', '外卖订单', '支出', '42.50', '工商银行储蓄卡(1234)', '支付成功', txn('20260610')),
    row('2026-06-12 12:00:00', '微信红包', '张三', '红包', '收入', '200.00', '零钱', '已存入零钱', txn('20260612')),
    row('2026-06-15 08:00:00', '商户消费', '中国移动', '话费充值', '支出', '39.00', '招商银行信用卡(4417)', '支付成功', txn('20260615')),
    row('2026-06-20 21:11:00', '转账', '李四', '转账-晚餐AA', '支出', '68.00', '零钱', '对方已收钱', txn('20260620'))
  ]
  write('normal-wechat.csv', lines)
}

// 2. utf8-bom.csv —— UTF-8 BOM
{
  const lines = [
    ...preamble('2026-05-01 00:00:00', '2026-05-31 23:59:59', 2, '0笔 0.00元', '2笔 40.00元'),
    HEADER,
    row('2026-05-03 10:23:00', '商户消费', '腾讯视频', 'VIP会员连续包月', '支出', '25.00', '招商银行信用卡(4417)', '支付成功', txn('20260503')),
    row('2026-05-15 08:00:00', '商户消费', '中国移动', '话费充值', '支出', '15.00', '零钱', '支付成功', txn('20260515'))
  ]
  write('utf8-bom.csv', lines, 'utf8', true)
}

// 3. gbk.csv —— GB18030 编码
{
  const lines = [
    ...preamble('2026-05-01 00:00:00', '2026-05-31 23:59:59', 2, '0笔 0.00元', '2笔 65.00元'),
    HEADER,
    row('2026-05-03 10:23:00', '商户消费', '腾讯视频', 'VIP会员连续包月', '支出', '25.00', '招商银行信用卡(4417)', '支付成功', txn('20260503')),
    row('2026-05-08 19:45:00', '商户消费', '肯德基', '宅急送订单', '支出', '40.00', '零钱', '支付成功', txn('20260508'))
  ]
  write('gbk.csv', lines, 'gb18030')
}

// 4. header-offset.csv —— 表头前多余空行/说明行
{
  const lines = [
    '',
    '﻿',
    '微信支付账单明细,,,,,,,,',
    '（本文件由微信导出）,,,,,,,,',
    '',
    ...preamble('2026-04-01 00:00:00', '2026-04-30 23:59:59', 1, '0笔 0.00元', '1笔 25.00元'),
    '',
    HEADER,
    row('2026-04-03 10:23:00', '商户消费', '腾讯视频', 'VIP会员连续包月', '支出', '25.00', '招商银行信用卡(4417)', '支付成功', txn('20260403')),
    '',
    ''
  ]
  write('header-offset.csv', lines)
}

// 5. refund.csv —— 退款/撤销
{
  const lines = [
    ...preamble('2026-03-01 00:00:00', '2026-03-31 23:59:59', 3, '1笔 25.00元', '2笔 90.00元'),
    HEADER,
    row('2026-03-03 10:23:00', '商户消费', '腾讯视频', 'VIP会员连续包月', '支出', '25.00', '招商银行信用卡(4417)', '支付成功', txn('20260303')),
    row('2026-03-05 14:00:00', '商户消费', '某电商平台', ' incorrect订单', '支出', '90.00', '零钱', '已全额退款', txn('20260305')),
    row('2026-03-06 09:30:00', '商户消费', '某电商平台', '退款', '收入', '90.00', '零钱', '退款成功', txn('20260306'))
  ]
  write('refund.csv', lines)
}

// 6. duplicate-import.csv —— 文件内完全重复（无交易单号 → canonical row + ordinal）
{
  const lines = [
    ...preamble('2026-02-01 00:00:00', '2026-02-28 23:59:59', 2, '0笔 0.00元', '2笔 50.00元'),
    HEADER,
    row('2026-02-03 10:23:00', '商户消费', '测试商户', '虚拟商品A', '支出', '25.00', '零钱', '支付成功', ''),
    row('2026-02-03 10:23:00', '商户消费', '测试商户', '虚拟商品A', '支出', '25.00', '零钱', '支付成功', '')
  ]
  write('duplicate-import.csv', lines)
}

// 7. same-amount-twice.csv —— 同金额不同时间
{
  const lines = [
    ...preamble('2026-02-01 00:00:00', '2026-02-28 23:59:59', 2, '0笔 0.00元', '2笔 50.00元'),
    HEADER,
    row('2026-02-03 10:23:00', '商户消费', '测试商户', '虚拟商品A', '支出', '25.00', '零钱', '支付成功', txn('20260203')),
    row('2026-02-20 15:40:00', '商户消费', '测试商户', '虚拟商品A', '支出', '25.00', '零钱', '支付成功', txn('20260220'))
  ]
  write('same-amount-twice.csv', lines)
}

// 8. malformed.csv —— 坏行混入，其余正常解析
{
  const lines = [
    ...preamble('2026-01-01 00:00:00', '2026-01-31 23:59:59', 3, '0笔 0.00元', '1笔 25.00元'),
    HEADER,
    row('2026-01-03 10:23:00', '商户消费', '腾讯视频', 'VIP会员连续包月', '支出', '25.00', '招商银行信用卡(4417)', '支付成功', txn('20260103')),
    '不是一行有效数据',
    '2026-13-40 99:99:99,商户消费,坏日期,商品,支出,¥10.00,零钱,支付成功,' + txn('20260104'),
    row('2026-01-05 10:23:00', '商户消费', '坏金额', '商品', '支出', 'ABCD', '零钱', '支付成功', txn('20260105')),
    '2026-01-06 10:23:00,只有,三列',
    row('2026-01-07 10:23:00', '商户消费', '正常行', '商品', '支出', '12.00', '零钱', '支付成功', txn('20260107'))
  ]
  write('malformed.csv', lines)
}

// 9. recurring-monthly.csv —— 腾讯视频连续 6 个月 25 元（招行4417 → 微信 → 腾讯视频）
{
  const months = ['2026-01-15', '2026-02-15', '2026-03-15', '2026-04-15', '2026-05-15', '2026-06-15']
  const lines = [
    ...preamble('2026-01-01 00:00:00', '2026-06-30 23:59:59', 8, '0笔 0.00元', '8笔 260.00元'),
    HEADER,
    ...months.map(m => row(`${m} 08:30:00`, '商户消费', '腾讯视频', 'VIP会员连续包月', '支出', '25.00', '招商银行信用卡(4417)', '支付成功', txn(m.replace(/-/g, '')))),
    row('2026-02-20 12:00:00', '商户消费', '美团平台商户', '外卖订单', '支出', '31.00', '零钱', '支付成功', txn('20260220')),
    row('2026-04-02 18:44:00', '商户消费', '滴滴出行', '快车', '支出', '18.60', '零钱', '支付成功', txn('20260402'))
  ]
  write('recurring-monthly.csv', lines)
}

// 10. non-recurring.csv —— 间隔与金额都不稳定
{
  const lines = [
    ...preamble('2026-01-01 00:00:00', '2026-06-30 23:59:59', 4, '0笔 0.00元', '4笔 120.00元'),
    HEADER,
    row('2026-01-05 10:00:00', '商户消费', '测试商户', '随机商品', '支出', '10.00', '零钱', '支付成功', txn('20260105')),
    row('2026-02-25 11:00:00', '商户消费', '测试商户', '随机商品', '支出', '30.00', '零钱', '支付成功', txn('20260225')),
    row('2026-04-08 12:00:00', '商户消费', '测试商户', '随机商品', '支出', '45.50', '零钱', '支付成功', txn('20260408')),
    row('2026-06-30 13:00:00', '商户消费', '测试商户', '随机商品', '支出', '34.50', '零钱', '支付成功', txn('20260630'))
  ]
  write('non-recurring.csv', lines)
}

// 11. dup-jan-jun.csv / dup-jan-aug.csv —— 跨会话重复导入（1–6 月 / 1–8 月）
{
  const monthlyRows: string[] = []
  for (let m = 1; m <= 8; m++) {
    const mm = String(m).padStart(2, '0')
    monthlyRows.push(row(`2026-${mm}-15 08:30:00`, '商户消费', '腾讯视频', 'VIP会员连续包月', '支出', '25.00', '招商银行信用卡(4417)', '支付成功', txn(`2026${mm}15`)))
  }
  const jun = monthlyRows.slice(0, 6)
  const aug = monthlyRows
  write(
    'dup-jan-jun.csv',
    [...preamble('2026-01-01 00:00:00', '2026-06-30 23:59:59', 6, '0笔 0.00元', '6笔 150.00元'), HEADER, ...jun]
  )
  write(
    'dup-jan-aug.csv',
    [...preamble('2026-01-01 00:00:00', '2026-08-31 23:59:59', 8, '0笔 0.00元', '8笔 200.00元'), HEADER, ...aug]
  )
}

console.log('fixtures written to', outDir)
