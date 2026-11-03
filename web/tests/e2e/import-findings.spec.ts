import { expect, test } from '@playwright/test'
import { addNode, addRelation, createVault, openNav, openNode } from './helpers.ts'

test.describe.configure({ mode: 'serial' })

const CSV = [
  'date,merchant,amount,desc',
  '2026-01-15,C视频服务,25.00,会员月费',
  '2026-02-15,C视频服务,25.00,会员月费',
  '2026-03-15,C视频服务,25.00,会员月费',
].join('\n')

test('E2E-16 Findings：SOF 与 PENDING_VERIFICATION 均来自 Core 规则', async ({ page }) => {
  await createVault(page)
  await openNav(page, '基础设施')
  await addNode(page, { kind: 'payment_instrument', name: 'F 银行卡', last4: '1234' })
  await addNode(page, { kind: 'account', name: 'F 账户' })

  await openNode(page, 'F 账户')
  await addRelation(page, {
    relation: 'funding_source',
    direction: 'incoming',
    peer: 'F 银行卡',
    criticality: 'required',
  })

  // 单一/已确认 required 来源 → SINGLE_POINT_OF_FAILURE（absence ≠ non-existence 的措辞必须在）
  await page.goto('/#/findings')
  await expect(page.getByTestId('findings-view')).toBeVisible()
  const sof = page.locator('.finding-row', { hasText: 'SINGLE_POINT_OF_FAILURE' }).first()
  await expect(sof).toBeVisible()
  await expect(sof).toContainText('没有已确认的备用路径')
  await expect(sof).toContainText('仍然未知')
  await expect(sof).toContainText('建议下一步')

  // 计划动作 done 但未验证 → PENDING_VERIFICATION
  await openNav(page, '变更')
  await page
    .locator('.template-row', { hasText: '更换银行卡' })
    .getByRole('button', { name: '创建计划' })
    .click()
  const dialog = page.getByRole('dialog')
  await dialog.getByLabel('2. 选择目标对象', { exact: true }).selectOption({ label: 'F 银行卡' })
  await dialog.getByRole('button', { name: '创建并分析' }).click()
  await expect(page.getByTestId('plan-view')).toBeVisible()

  const changeAction = page.locator('.action-row', { hasText: '迁移必要支付关系' })
  await changeAction.getByRole('button', { name: '记录完成' }).click()
  await expect(changeAction.getByText('已记录完成')).toBeVisible()

  await page.goto('/#/findings')
  await expect(
    page.locator('.finding-row', { hasText: 'PENDING_VERIFICATION' }).first(),
  ).toBeVisible()
  // 不允许伪精确总分
  await expect(page.getByTestId('findings-view')).not.toContainText('安全分')
})

test('E2E-17 Import：显式映射 → 商户解析 → 提案 → 接受成为现实依赖', async ({ page }) => {
  await createVault(page)
  await openNav(page, '基础设施')
  await addNode(page, { kind: 'account', name: 'C 账户' })
  await addNode(page, { kind: 'service', name: 'C 视频服务' })

  await page.goto('/#/import')
  await expect(page.getByTestId('import-view')).toBeVisible()

  await page.getByLabel('数据来源', { exact: true }).selectOption('generic_csv')
  await page.getByLabel('账单归属账户', { exact: true }).selectOption({ label: 'C 账户' })
  await page.setInputFiles('input[type=file]', {
    name: 'sample.csv',
    mimeType: 'text/csv',
    buffer: Buffer.from(CSV, 'utf8'),
  })

  // 通用 CSV 必须显式映射（禁止自动猜测）
  await page.getByLabel('日期列（必填）', { exact: true }).selectOption('date')
  await page.getByLabel('金额列', { exact: true }).selectOption('amount')
  await page.getByLabel('描述列', { exact: true }).selectOption('desc')
  await page.getByLabel('对方列', { exact: true }).selectOption('merchant')

  await page.getByRole('button', { name: '开始解析' }).click()
  await expect(page.getByText('商户 → 对象')).toBeVisible({ timeout: 30_000 })

  const merchantRow = page.locator('.merchant-row', { hasText: 'C视频服务' }).first()
  await expect(merchantRow).toBeVisible()
  await merchantRow.getByLabel('对应对象', { exact: true }).selectOption({ label: 'C 视频服务' })

  await page.getByRole('button', { name: '确认并生成提案' }).click()
  await expect(page.getByText('提案（未进入 Reality）')).toBeVisible({ timeout: 30_000 })

  const proposal = page.locator('.proposal-row', { hasText: 'C 视频服务' }).first()
  await expect(proposal).toBeVisible()
  await expect(proposal).toContainText('pending')

  await proposal.getByRole('button', { name: '接受（criticality: unknown）' }).click()
  await expect(page.locator('.proposal-row', { hasText: 'C 视频服务' }).first()).toContainText(
    'accepted',
  )

  // 接受后才进入 Reality：对象详情出现已确认关系，criticality 仍是 unknown
  await openNode(page, 'C 视频服务')
  await expect(page.getByTestId('node-detail')).toContainText('merchant_agreement')
  await expect(page.getByTestId('node-detail')).toContainText('criticality: unknown')
})

test('E2E-18 Drift：证据只产生“可能变化”，dismiss 不改写现实', async ({ page }) => {
  await createVault(page)
  await openNav(page, '基础设施')
  await addNode(page, { kind: 'account', name: 'D 账户' })
  await addNode(page, { kind: 'service', name: 'D 视频服务' })

  await page.goto('/#/import')
  await page.getByLabel('数据来源', { exact: true }).selectOption('generic_csv')
  await page.getByLabel('账单归属账户', { exact: true }).selectOption({ label: 'D 账户' })
  await page.setInputFiles('input[type=file]', {
    name: 'sample.csv',
    mimeType: 'text/csv',
    buffer: Buffer.from(CSV.replaceAll('C视频服务', 'D视频服务'), 'utf8'),
  })
  await page.getByLabel('日期列（必填）', { exact: true }).selectOption('date')
  await page.getByLabel('金额列', { exact: true }).selectOption('amount')
  await page.getByLabel('描述列', { exact: true }).selectOption('desc')
  await page.getByLabel('对方列', { exact: true }).selectOption('merchant')
  await page.getByRole('button', { name: '开始解析' }).click()

  const merchantRow = page.locator('.merchant-row', { hasText: 'D视频服务' }).first()
  await expect(merchantRow).toBeVisible({ timeout: 30_000 })
  await merchantRow.getByLabel('对应对象', { exact: true }).selectOption({ label: 'D 视频服务' })
  await page.getByRole('button', { name: '确认并生成提案' }).click()

  const drift = page
    .locator('.proposal-row', { hasText: 'D 视频服务' })
    .filter({ hasText: 'possible' })
    .first()
  await expect(drift).toBeVisible({ timeout: 30_000 })
  await expect(page.getByText('可能的变化（Drift）')).toBeVisible()

  // dismiss 不改写 Reality：账户与服务之间不应出现依赖
  await drift.getByRole('button', { name: '忽略' }).click()
  await expect(page.locator('.proposal-row').filter({ hasText: 'dismissed' }).first()).toBeVisible()

  await openNode(page, 'D 视频服务')
  await expect(page.getByTestId('node-detail')).toContainText('没有记录任何指向该对象的关系')
})
