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
