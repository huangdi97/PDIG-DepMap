import { expect, test } from '@playwright/test'
import { addNode, addRelation, createVault, openNav, openNode } from './helpers.ts'
import { csvRows, ofxText, runImport } from './import-helpers.ts'

test.describe.configure({ mode: 'serial' })

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

  await page.goto('/#/findings')
  await expect(page.getByTestId('findings-view')).toBeVisible()
  const sof = page.locator('.finding-row', { hasText: 'SINGLE_POINT_OF_FAILURE' }).first()
  await expect(sof).toBeVisible()
  await expect(sof).toContainText('没有已确认的备用路径')
  await expect(sof).toContainText('仍然未知')
  await expect(sof).toContainText('建议下一步')

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
  await expect(page.getByTestId('findings-view')).not.toContainText('安全分')
})

test('E2E-17 Import（通用 CSV）：显式映射 → 商户解析 → 提案 → 接受成为现实依赖', async ({
  page,
}) => {
  await createVault(page)
  await openNav(page, '基础设施')
  await addNode(page, { kind: 'account', name: 'C 账户' })
  await addNode(page, { kind: 'service', name: 'C 视频服务' })

  await runImport(page, {
    adapterId: 'generic_csv',
    accountName: 'C 账户',
    fileName: 'sample.csv',
    content: csvRows('C视频服务'),
    merchantRaw: 'C视频服务',
    resolveTo: 'C 视频服务',
  })

  const proposal = page.locator('.proposal-row', { hasText: 'C 视频服务' }).first()
  await expect(proposal).toContainText('pending')
  await proposal.getByRole('button', { name: '接受（criticality: unknown）' }).click()
  await expect(page.locator('.proposal-row', { hasText: 'C 视频服务' }).first()).toContainText(
    'accepted',
  )

  await openNode(page, 'C 视频服务')
  await expect(page.getByTestId('node-detail')).toContainText('merchant_agreement')
  await expect(page.getByTestId('node-detail')).toContainText('criticality: unknown')
})

test('E2E-18 Drift：证据只产生“可能变化”，dismiss 不改写现实', async ({ page }) => {
  await createVault(page)
  await openNav(page, '基础设施')
  await addNode(page, { kind: 'account', name: 'D 账户' })
  await addNode(page, { kind: 'service', name: 'D 视频服务' })

  await runImport(page, {
    adapterId: 'generic_csv',
    accountName: 'D 账户',
    fileName: 'sample.csv',
    content: csvRows('D视频服务'),
    merchantRaw: 'D视频服务',
    resolveTo: 'D 视频服务',
  })

  const drift = page
    .locator('.proposal-row', { hasText: 'D 视频服务' })
    .filter({ hasText: 'possible' })
    .first()
  await expect(drift).toBeVisible({ timeout: 30_000 })
  await drift.getByRole('button', { name: '忽略' }).click()
  await expect(page.locator('.proposal-row').filter({ hasText: 'dismissed' }).first()).toBeVisible()

  await openNode(page, 'D 视频服务')
  await expect(page.getByTestId('node-detail')).toContainText('没有记录任何指向该对象的关系')
})

async function seedTwoSourcesAndDrift(
  page: import('@playwright/test').Page,
  prefix: string,
): Promise<void> {
  await createVault(page)
  await openNav(page, '基础设施')
  await addNode(page, { kind: 'account', name: `${prefix} 账户 A` })
  await addNode(page, { kind: 'account', name: `${prefix} 账户 B` })
  await addNode(page, { kind: 'service', name: `${prefix} 服务` })

  // 服务已有来自 A 的已确认支付关系（尚未绑定 B）
  await openNode(page, `${prefix} 服务`)
  await addRelation(page, {
    relation: 'merchant_agreement',
    direction: 'incoming',
    peer: `${prefix} 账户 A`,
  })

  await runImport(page, {
    adapterId: 'generic_csv',
    accountName: `${prefix} 账户 B`,
    fileName: 'sample.csv',
    content: csvRows(`${prefix}服务`),
    merchantRaw: `${prefix}服务`,
    resolveTo: `${prefix} 服务`,
  })
}

test('E2E-19 Drift「确认为额外路径」：新边加入，旧边保持 active', async ({ page }) => {
  await seedTwoSourcesAndDrift(page, 'E1')

  const drift = page
    .locator('.proposal-row', { hasText: 'E1 服务' })
    .filter({ hasText: 'possible_replacement' })
    .first()
  await expect(drift).toBeVisible({ timeout: 30_000 })
  await drift.getByRole('button', { name: '确认为额外路径' }).click()
  await expect(
    page.locator('.proposal-row').filter({ hasText: 'confirmed_change' }).first(),
  ).toBeVisible()

  await openNode(page, 'E1 服务')
  const detail = page.getByTestId('node-detail')
  await expect(detail).toContainText('E1 账户 A')
  await expect(detail).toContainText('E1 账户 B')
  await expect(detail).not.toContainText('已终止')
})

test('E2E-20 Drift「确认为替代」：新边加入且旧边被 retire', async ({ page }) => {
  await seedTwoSourcesAndDrift(page, 'E2')

  const drift = page
    .locator('.proposal-row', { hasText: 'E2 服务' })
    .filter({ hasText: 'possible_replacement' })
    .first()
  await expect(drift).toBeVisible({ timeout: 30_000 })
  await drift.getByRole('button', { name: '确认为替代' }).click()
  await expect(
    page.locator('.proposal-row').filter({ hasText: 'confirmed_change' }).first(),
  ).toBeVisible()

  await openNode(page, 'E2 服务')
  const detail = page.getByTestId('node-detail')
  await expect(detail).toContainText('E2 账户 B')
  await expect(detail).toContainText('已终止')
  await expect(detail).toContainText('已终止关系 1 条')
})

test('E2E-21 Import（OFX）：无需映射，解析后同样只产生提案', async ({ page }) => {
  await createVault(page)
  await openNav(page, '基础设施')
  await addNode(page, { kind: 'account', name: 'O 账户' })
  await addNode(page, { kind: 'service', name: 'O 服务' })

  await runImport(page, {
    adapterId: 'ofx_qfx',
    accountName: 'O 账户',
    fileName: 'sample.ofx',
    content: ofxText('O服务'),
    merchantRaw: 'O服务',
    resolveTo: 'O 服务',
  })

  const proposal = page.locator('.proposal-row', { hasText: 'O 服务' }).first()
  await expect(proposal).toContainText('pending')
  await proposal.getByRole('button', { name: '接受（criticality: unknown）' }).click()
  await expect(page.locator('.proposal-row', { hasText: 'O 服务' }).first()).toContainText(
    'accepted',
  )

  await openNode(page, 'O 服务')
  await expect(page.getByTestId('node-detail')).toContainText('merchant_agreement')
})
