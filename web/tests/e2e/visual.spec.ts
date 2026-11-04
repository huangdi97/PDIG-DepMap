import { mkdirSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { expect, test } from '@playwright/test'
import { PASSWORD, addNode, createVault, lockFromRail, openNav, openNode } from './helpers.ts'

const OUT = resolve(process.cwd(), 'artifacts/visual')
mkdirSync(OUT, { recursive: true })

const shots: Array<{ name: string; file: string; viewport: string }> = []

async function shot(page: import('@playwright/test').Page, name: string): Promise<void> {
  const size = page.viewportSize() ?? { width: 1440, height: 1000 }
  const file = `${name}-${size.width}x${size.height}.png`
  await page.screenshot({ path: resolve(OUT, file), fullPage: false })
  shots.push({ name, file, viewport: `${size.width}x${size.height}` })
}

test.afterAll(() => {
  writeFileSync(
    resolve(OUT, 'visual-matrix.json'),
    `${JSON.stringify({ generatedAt: new Date().toISOString(), shots }, null, 2)}\n`,
    'utf8',
  )
})

test('visual matrix: onboarding / locked / demo screens / responsive', async ({ page }) => {
  // 1) onboarding（1440）
  await page.setViewportSize({ width: 1440, height: 1000 })
  await page.goto('/')
  await expect(page.getByRole('heading', { name: '个人数字依赖图' })).toBeVisible()
  await shot(page, 'onboarding')

  // 2) 真实 vault → locked（锁定态）
  await createVault(page)
  await openNav(page, '基础设施')
  await addNode(page, { kind: 'account', name: '截图账户' })
  await lockFromRail(page)
  await shot(page, 'locked')

  // 3) demo reality（确定性 sample data）
  for (const [width, height, name] of [
    [1440, 1000, 'now-desktop'],
    [1280, 900, 'now-desktop-1280'],
    [1024, 768, 'now-1024'],
    [768, 1024, 'now-tablet'],
    [390, 844, 'now-mobile'],
    [360, 800, 'now-mobile-360'],
  ] as const) {
    await page.setViewportSize({ width, height })
    await page.goto('/?demo=1')
    await expect(page.getByTestId('now-view')).toBeVisible({ timeout: 60_000 })
    await shot(page, name)
  }

  await page.setViewportSize({ width: 1440, height: 1000 })
  await page.goto('/?demo=1')
  await expect(page.getByTestId('now-view')).toBeVisible({ timeout: 60_000 })

  await openNav(page, '基础设施')
  await expect(page.getByTestId('infrastructure-view')).toBeVisible()
  await shot(page, 'infrastructure')

  await page.locator('.table-row', { hasText: '示例主手机号' }).first().click()
  await expect(page.getByTestId('node-detail')).toBeVisible()
  await shot(page, 'node-detail-phone')

  await openNav(page, '基础设施')
  await page.locator('.table-row', { hasText: '示例云账户' }).first().click()
  await expect(page.getByTestId('node-detail')).toBeVisible()
  await shot(page, 'node-detail-account')

  await openNav(page, '变更')
  await expect(page.getByTestId('change-view')).toBeVisible()
  await shot(page, 'change')

  await page.locator('.plan-row').first().click()
  await expect(page.getByTestId('plan-view')).toBeVisible()
  await shot(page, 'change-plan')

  // pending verification 状态（demo 预置的是 replace_phone_number 计划）
  const prepareAction = page.locator('.action-row', { hasText: '检查旧手机号承担的恢复与认证能力' })
  await prepareAction.getByRole('button', { name: '记录完成' }).click()
  const changeAction = page.locator('.action-row', { hasText: '添加新手机号并迁移关键账户' })
  await changeAction.getByRole('button', { name: '记录完成' }).click()
  await expect(changeAction.getByText('已记录完成')).toBeVisible()
  await shot(page, 'change-pending-verification')

  // stale 状态：Reality 变化
  await openNav(page, '基础设施')
  await addNode(page, { kind: 'service', name: '示例新增服务' })
  await page.locator('.table-row', { hasText: '示例云账户' }).first().click()
  await page.getByRole('button', { name: '添加关系' }).click()
  await page
    .getByRole('dialog')
    .getByLabel('关系类型', { exact: true })
    .selectOption('merchant_agreement')
  await page.getByRole('dialog').getByLabel('方向', { exact: true }).selectOption('outgoing')
  await page
    .getByRole('dialog')
    .getByLabel('另一端对象', { exact: true })
    .selectOption({ label: '示例新增服务' })
  await page.getByRole('dialog').getByRole('button', { name: '确认这条关系' }).click()
  await openNav(page, '变更')
  await page.locator('.plan-row').first().click()
  await expect(page.getByTestId('stale-banner')).toBeVisible()
  await shot(page, 'change-stale')

  await page.getByRole('button', { name: '重新分析当前影响' }).click()
  await expect(page.getByTestId('stale-banner')).toHaveCount(0)
  await shot(page, 'change-reanalyzed')

  await openNav(page, '记录')
  await expect(page.getByTestId('records-view')).toBeVisible()
  await shot(page, 'records')

  // privacy mask
  await openNav(page, '我')
  await page.getByLabel('隐私遮蔽（名称、机构、账号标签）').check()
  await openNav(page, '基础设施')
  await shot(page, 'privacy-on')

  await openNav(page, '我')
  await shot(page, 'me')

  // mobile 关键屏
  await page.setViewportSize({ width: 390, height: 844 })
  await openNav(page, '变更')
  await shot(page, 'change-mobile')
  await openNav(page, '记录')
  await shot(page, 'records-mobile')
})

test('visual: 无横向溢出（各断点）', async ({ page }) => {
  for (const [width, height] of [
    [1440, 1000],
    [1280, 900],
    [1024, 768],
    [768, 1024],
    [390, 844],
    [360, 800],
  ] as const) {
    await page.setViewportSize({ width, height })
    await page.goto('/?demo=1')
    await expect(page.getByTestId('now-view')).toBeVisible({ timeout: 60_000 })
    const overflow = await page.evaluate(() => {
      const doc = document.documentElement
      return { scrollWidth: doc.scrollWidth, clientWidth: doc.clientWidth }
    })
    expect(overflow.scrollWidth, `viewport ${width}`).toBeLessThanOrEqual(overflow.clientWidth + 1)
  }
})
