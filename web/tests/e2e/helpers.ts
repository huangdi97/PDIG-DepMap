import { expect, type Page } from '@playwright/test'

export const PASSWORD = 'pdig-e2e-password'

export async function createVault(page: Page): Promise<void> {
  await page.goto('/')
  await page.getByRole('button', { name: '创建本地加密空间' }).click()
  await page.getByLabel('本地密码').fill(PASSWORD)
  await page.getByLabel('确认密码').fill(PASSWORD)
  await page.getByRole('button', { name: '创建', exact: true }).click()
  await expect(page.getByTestId('now-view')).toBeVisible({ timeout: 60_000 })
}

export async function unlock(page: Page): Promise<void> {
  await page.getByLabel('本地密码').fill(PASSWORD)
  await page.getByRole('button', { name: '解锁' }).click()
  // 解锁后停留在原路由（可能是 #/infrastructure 等），因此断言主区域而不是某个具体视图。
  await expect(page.locator('#main')).toBeVisible({ timeout: 60_000 })
}

export async function lockFromRail(page: Page): Promise<void> {
  const railLock = page.locator('.rail-action', { hasText: '锁定' })
  if (await railLock.count()) {
    await railLock.first().click()
  } else {
    await page.locator('.topbar-lock').click()
  }
  await expect(page.getByLabel('本地密码')).toBeVisible()
}

export async function openNav(
  page: Page,
  label: '现在' | '基础设施' | '变更' | '记录' | '我',
): Promise<void> {
  const railItem = page.locator('.rail-item', { hasText: label }).first()
  if (await railItem.isVisible().catch(() => false)) {
    await railItem.click()
    return
  }
  const bottomItem = page.locator('.bottom-item', { hasText: label }).first()
  if (await bottomItem.isVisible().catch(() => false)) {
    await bottomItem.click()
    return
  }
  // 兜底：路由直跳（紧凑布局下导航被折叠的情况）
  const routes: Record<string, string> = {
    现在: '/',
    基础设施: '/infrastructure',
    变更: '/change',
    记录: '/records',
    我: '/me',
  }
  await page.goto(`/#${routes[label]}`)
}

export interface NodeSeed {
  kind: string
  name: string
  subtype?: 'phone' | 'email' | 'other'
  issuer?: string
  last4?: string
}

export async function addNode(page: Page, node: NodeSeed): Promise<void> {
  await page.getByRole('button', { name: '添加对象' }).first().click()
  const dialog = page.getByRole('dialog')
  await dialog.getByLabel('类型', { exact: true }).selectOption(node.kind)
  await dialog.getByLabel('名称', { exact: true }).fill(node.name)
  if (node.kind === 'identity_anchor' && node.subtype) {
    await dialog.getByLabel('身份锚点类型', { exact: true }).selectOption(node.subtype)
  }
  if (node.kind === 'payment_instrument') {
    if (node.issuer) await dialog.getByLabel('发卡机构（可选）').fill(node.issuer)
    if (node.last4) await dialog.getByLabel('卡号后四位（可选）').fill(node.last4)
  }
  await dialog.getByRole('button', { name: '确认添加' }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)
}

export async function openNode(page: Page, name: string): Promise<void> {
  await openNav(page, '基础设施')
  await expect(page.getByTestId('infrastructure-view')).toBeVisible()
  await page.locator('.table-row', { hasText: name }).first().click()
  await expect(page.getByTestId('node-detail')).toBeVisible()
}

export async function addRelation(
  page: Page,
  options: {
    relation: string
    direction: 'incoming' | 'outgoing'
    peer: string
    criticality?: 'unknown' | 'required'
  },
): Promise<void> {
  await page.getByRole('button', { name: '添加关系' }).click()
  const dialog = page.getByRole('dialog')
  await dialog.getByLabel('关系类型', { exact: true }).selectOption(options.relation)
  await dialog.getByLabel('方向', { exact: true }).selectOption(options.direction)
  await dialog.getByLabel('另一端对象', { exact: true }).selectOption({ label: options.peer })
  if (options.criticality)
    await dialog.getByLabel('关键性', { exact: true }).selectOption(options.criticality)
  await dialog.getByRole('button', { name: '确认这条关系' }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)
}

export async function graphRevision(page: Page): Promise<number> {
  await openNav(page, '我')
  await expect(page.getByTestId('me-view')).toBeVisible()
  const text = await page.locator('.facts').first().innerText()
  const match = text.match(/graphRevision\s*(\d+)/)
  return match?.[1] ? Number(match[1]) : -1
}

export async function createPaymentPlan(page: Page, target: string): Promise<void> {
  await openNav(page, '变更')
  await expect(page.getByTestId('change-view')).toBeVisible()
  await page
    .locator('.template-row', { hasText: '更换银行卡' })
    .getByRole('button', { name: '创建计划' })
    .click()
  const dialog = page.getByRole('dialog')
  await dialog.getByLabel('2. 选择目标对象', { exact: true }).selectOption({ label: target })
  await dialog.getByRole('button', { name: '创建并分析' }).click()
  await expect(page.getByTestId('plan-view')).toBeVisible({ timeout: 30_000 })
}
