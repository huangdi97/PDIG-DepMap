import { expect, type Page } from '@playwright/test'

export const CSV_HEADER = 'date,merchant,amount,desc'

export function csvRows(merchant: string): string {
  return [
    CSV_HEADER,
    `2026-01-15,${merchant},25.00,会员月费`,
    `2026-02-15,${merchant},25.00,会员月费`,
    `2026-03-15,${merchant},25.00,会员月费`,
  ].join('\n')
}

/**
 * 真实微信支付账单导出格式（含表头说明行 + 11 列）。
 * 3 条按月同商户交易 —— Core 只在识别到周期性后才产生 Proposal，
 * 单笔商户不会被当成订阅（宁可漏报）。
 */
export function wechatBill(merchant: string, cardHint = '招商银行信用卡(4417)'): string {
  const dates = ['2026-01-15 10:23:00', '2026-02-15 10:23:00', '2026-03-15 10:23:00']
  const rows = dates.map(
    (d, i) =>
      `${d},商户消费,${merchant},VIP会员连续包月,支出,¥25.00,${cardHint},支付成功,42000019782026${String(i + 1).padStart(4, '0')}001001,,/`,
  )
  const head = [
    '微信支付账单明细,,,,,,,',
    '微信支付账单明细,起始时间:[2026-01-01 00:00:00]  终止时间:[2026-03-31 23:59:59],,,,,,,',
    '导出类型:[全部],,,,,,,,',
    '导出时间:[2026-04-01 09:00:00],,,,,,,,',
    '共3笔记录,,,,,,,',
    '收入:0笔 0.00元,,,,,,,,',
    '支出:3笔 75.00元,,,,,,,,',
    '中性交易:0笔 0.00元,,,,,,,,',
    '备注：以下是微信支付账单明细列表：,,,,,,,,',
    '----------------------微信支付账单明细列表--------------------,,,,,,,',
    '交易时间,交易类型,交易对方,商品,收/支,金额(元),支付方式,当前状态,交易单号,商户单号,备注',
  ]
  return [...head, ...rows].join('\n')
}
/** 最小可解析 OFX v1（SGML 风格）：3 条按月交易、同商户、同一金额。 */
export function ofxText(merchant: string): string {
  const tx = (id: string, date: string) => `<STMTTRN>
<TRNTYPE>DEBIT
<DTPOSTED>${date}
<TRNAMT>-25.00
<FITID>${id}
<NAME>${merchant}
</STMTTRN>`
  return `OFXHEADER:100
DATA:OFXSGML
VERSION:102

<OFX>
<BANKMSGSRSV1><STMTTRNRS><STMTRS><BANKTRANLIST>
${tx('t1', '20260115')}
${tx('t2', '20260215')}
${tx('t3', '20260315')}
</BANKTRANLIST></STMTRS></STMTTRNRS></BANKMSGSRSV1>
</OFX>
`
}

export interface ImportOptions {
  adapterId: 'generic_csv' | 'ofx_qfx' | 'wechat_statement'
  accountName: string
  fileName: string
  content: string
  merchantRaw: string
  resolveTo: string
}

export async function runImport(page: Page, options: ImportOptions): Promise<void> {
  await page.goto('/#/import')
  await expect(page.getByTestId('import-view')).toBeVisible()

  await page.getByLabel('数据来源', { exact: true }).selectOption(options.adapterId)
  await page
    .getByLabel('账单归属账户', { exact: true })
    .selectOption({ label: options.accountName })
  await page.setInputFiles('input[type=file]', {
    name: options.fileName,
    mimeType: 'text/plain',
    buffer: Buffer.from(options.content, 'utf8'),
  })

  if (options.adapterId === 'generic_csv') {
    await page.getByLabel('日期列（必填）', { exact: true }).selectOption('date')
    await page.getByLabel('金额列', { exact: true }).selectOption('amount')
    await page.getByLabel('描述列', { exact: true }).selectOption('desc')
    await page.getByLabel('对方列', { exact: true }).selectOption('merchant')
  }

  await page.getByRole('button', { name: '开始解析' }).click()
  const merchantRow = page.locator('.merchant-row', { hasText: options.merchantRaw }).first()
  await expect(merchantRow).toBeVisible({ timeout: 30_000 })
  await merchantRow
    .getByLabel('对应对象', { exact: true })
    .selectOption({ label: options.resolveTo })

  await page.getByRole('button', { name: '确认并生成提案' }).click()
  await expect(page.getByText('提案（未进入 Reality）')).toBeVisible({ timeout: 30_000 })
}
