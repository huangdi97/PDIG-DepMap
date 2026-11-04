import { expect, test } from '@playwright/test'
import {
  PASSWORD,
  addNode,
  addRelation,
  createPaymentPlan,
  createVault,
  graphRevision,
  lockFromRail,
  openNav,
  openNode,
  unlock,
} from './helpers.ts'

test.describe.configure({ mode: 'serial' })

test('E2E-01 首次进入 → 创建 vault → 空现实 → 锁定/解锁', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { name: '个人数字依赖图' })).toBeVisible()
  await page.getByRole('button', { name: '创建本地加密空间' }).click()
  await page.getByLabel('本地密码').fill(PASSWORD)
  await page.getByLabel('确认密码').fill(PASSWORD)
  await page.getByRole('button', { name: '创建', exact: true }).click()
  await expect(page.getByTestId('now-view')).toBeVisible({ timeout: 60_000 })

  // 空现实：明确说明「没记录 ≠ 不存在」
  await expect(page.getByText('当前没有已记录的基础设施')).toBeVisible()
  await expect(page.getByText('这里没有记录，不代表现实中没有这些对象。').first()).toBeVisible()
  await expect(page.getByText('当前没有进行中的变更')).toBeVisible()

  await lockFromRail(page)
  await expect(page.getByTestId('now-view')).toHaveCount(0)
  await unlock(page)
  await expect(page.getByTestId('now-view')).toBeVisible()
})

test('E2E-02 对象创建 → reload 后仍然存在', async ({ page }) => {
  await createVault(page)
  await openNav(page, '基础设施')
  await addNode(page, { kind: 'identity_anchor', name: '测试主手机号', subtype: 'phone' })
  await addNode(page, { kind: 'identity_anchor', name: '测试备用邮箱', subtype: 'email' })
  await addNode(page, { kind: 'account', name: '测试云账户' })

  await expect(page.getByTestId('infrastructure-view')).toContainText('测试主手机号')
  await expect(page.getByTestId('infrastructure-view')).toContainText('测试备用邮箱')

  await page.reload()
  await unlock(page)
  await openNav(page, '基础设施')
  await expect(page.getByTestId('infrastructure-view')).toContainText('测试主手机号')
  await expect(page.getByTestId('infrastructure-view')).toContainText('测试云账户')
})

test('E2E-03 依赖默认 criticality=unknown，且写入会推进 graphRevision', async ({ page }) => {
  await createVault(page)
  await openNav(page, '基础设施')
  await addNode(page, { kind: 'identity_anchor', name: '依赖测试手机号', subtype: 'phone' })
  await addNode(page, { kind: 'account', name: '依赖测试账户' })

  const before = await graphRevision(page)
  await openNode(page, '依赖测试账户')
  await addRelation(page, {
    relation: 'authenticates',
    direction: 'incoming',
    peer: '依赖测试手机号',
  })

  await expect(page.getByTestId('node-detail')).toContainText('criticality: unknown')
  const after = await graphRevision(page)
  expect(after).toBeGreaterThan(before)
})

test('E2E-04 变更计划创建 → 分析 → Current/Transition/After 三态', async ({ page }) => {
  await createVault(page)
  await openNav(page, '基础设施')
  await addNode(page, {
    kind: 'payment_instrument',
    name: '测试银行卡',
    issuer: '测试银行',
    last4: '0000',
  })
  await addNode(page, { kind: 'account', name: '测试订阅账户' })

  await openNode(page, '测试订阅账户')
  await addRelation(page, {
    relation: 'funding_source',
    direction: 'incoming',
    peer: '测试银行卡',
    criticality: 'required',
  })

  await createPaymentPlan(page, '测试银行卡')
  const plan = page.getByTestId('plan-view')
  await expect(plan.getByRole('region', { name: 'Current' })).toBeVisible()
  await expect(plan.getByRole('region', { name: 'Transition' })).toBeVisible()
  await expect(plan.getByRole('region', { name: 'After plan projection' })).toContainText(
    'Plan Projection · 未发生',
  )
  await expect(plan).toContainText('分析基准')
})

test('E2E-05 Reality 变化 → 计划 stale → 动作被阻断', async ({ page }) => {
  await createVault(page)
  await openNav(page, '基础设施')
  await addNode(page, { kind: 'payment_instrument', name: 'Stale 银行卡', last4: '1111' })
  await addNode(page, { kind: 'account', name: 'Stale 账户' })
  await openNode(page, 'Stale 账户')
  await addRelation(page, {
    relation: 'funding_source',
    direction: 'incoming',
    peer: 'Stale 银行卡',
  })

  await createPaymentPlan(page, 'Stale 银行卡')
  await expect(page.getByTestId('stale-banner')).toHaveCount(0)

  // Reality 变化：新增一条已确认关系（graphRevision +1；只新增孤立对象不会改变依赖图）
  await openNav(page, '基础设施')
  await addNode(page, { kind: 'service', name: 'Stale 新服务' })
  await openNode(page, 'Stale 账户')
  await addRelation(page, {
    relation: 'merchant_agreement',
    direction: 'outgoing',
    peer: 'Stale 新服务',
  })

  await openNav(page, '变更')
  await page.locator('.plan-row', { hasText: '更换银行卡' }).first().click()
  await expect(page.getByTestId('stale-banner')).toBeVisible()
  await expect(page.getByRole('button', { name: '记录完成' }).first()).toBeDisabled()
  await expect(page.getByRole('button', { name: '验证通过' }).first()).toBeDisabled()
})

test('E2E-06 重新分析 → 真实差异 → 不自动 done/verified', async ({ page }) => {
  await createVault(page)
  await openNav(page, '基础设施')
  await addNode(page, { kind: 'payment_instrument', name: '重分析银行卡', last4: '2222' })
  await addNode(page, { kind: 'account', name: '重分析账户' })
  await openNode(page, '重分析账户')
  await addRelation(page, {
    relation: 'funding_source',
    direction: 'incoming',
    peer: '重分析银行卡',
    criticality: 'required',
  })
  await createPaymentPlan(page, '重分析银行卡')

  await openNav(page, '基础设施')
  await addNode(page, { kind: 'service', name: '重分析新服务' })
  await openNode(page, '重分析账户')
  await addRelation(page, {
    relation: 'merchant_agreement',
    direction: 'outgoing',
    peer: '重分析新服务',
  })
  await openNav(page, '变更')
  await page.locator('.plan-row', { hasText: '更换银行卡' }).first().click()
  await expect(page.getByTestId('stale-banner')).toBeVisible()

  await page.getByRole('button', { name: '重新分析当前影响' }).click()
  await expect(page.getByTestId('stale-banner')).toHaveCount(0)
  await expect(page.getByText(/分析基准：revision \d+ → \d+/)).toBeVisible()

  // 不自动 done / verified（只断言动作行上的状态徽标，避免命中说明文案）
  await expect(page.locator('.action-row .badge', { hasText: '已记录完成' })).toHaveCount(0)
  await expect(page.locator('.action-row .badge', { hasText: '已验证' })).toHaveCount(0)
})

test('E2E-07 done ≠ verified：记录完成不会变成已验证', async ({ page }) => {
  await createVault(page)
  await openNav(page, '基础设施')
  await addNode(page, { kind: 'payment_instrument', name: '完成银行卡', last4: '3333' })
  await addNode(page, { kind: 'account', name: '完成账户' })
  await openNode(page, '完成账户')
  await addRelation(page, { relation: 'funding_source', direction: 'incoming', peer: '完成银行卡' })
  await createPaymentPlan(page, '完成银行卡')

  const changeAction = page.locator('.action-row', { hasText: '迁移必要支付关系' })
  await changeAction.getByRole('button', { name: '记录完成' }).click()
  await expect(changeAction.getByText('已记录完成')).toBeVisible()
  await expect(changeAction.getByText('待验证')).toBeVisible()
  await expect(changeAction.getByRole('button', { name: '验证通过' })).toBeEnabled()
  await expect(changeAction.getByText('已验证')).toHaveCount(0)
})

test('E2E-08 Make-Before-Break：新路径未验证时旧路径不能退休', async ({ page }) => {
  await createVault(page)
  await openNav(page, '基础设施')
  await addNode(page, { kind: 'identity_anchor', name: '换号前手机号', subtype: 'phone' })
  await addNode(page, { kind: 'account', name: '换号测试账户' })
  await openNode(page, '换号测试账户')
  await addRelation(page, {
    relation: 'authenticates',
    direction: 'incoming',
    peer: '换号前手机号',
    criticality: 'required',
  })

  await openNav(page, '变更')
  await page
    .locator('.template-row', { hasText: '更换手机号' })
    .getByRole('button', { name: '创建计划' })
    .click()
  const dialog = page.getByRole('dialog')
  await dialog.getByLabel('2. 选择目标对象').selectOption({ label: '换号前手机号' })
  await dialog.getByRole('button', { name: '创建并分析' }).click()
  await expect(page.getByTestId('plan-view')).toBeVisible()

  // 完成 prepare → change（change 尚未验证）
  const prepareAction = page.locator('.action-row', { hasText: '检查旧手机号承担的恢复与认证能力' })
  await prepareAction.getByRole('button', { name: '记录完成' }).click()
  await expect(prepareAction.getByText('已记录完成')).toBeVisible()

  const changeAction = page.locator('.action-row', { hasText: '添加新手机号并迁移关键账户' })
  await changeAction.getByRole('button', { name: '记录完成' }).click()
  await expect(changeAction.getByText('已记录完成')).toBeVisible()
  await expect(changeAction.getByText('待验证')).toBeVisible()

  // 旧路径退休被阻断（新路径尚未验证）
  await openNode(page, '换号测试账户')
  await page.getByRole('button', { name: '标记为不再有效' }).first().click()
  await expect(
    page.getByRole('alert').filter({ hasText: '新路径尚未完成真实验证' }).first(),
  ).toBeVisible()
  await expect(page.getByTestId('node-detail')).not.toContainText('已终止')

  // 验证新路径后允许退休
  await openNav(page, '变更')
  await page.locator('.plan-row').first().click()
  const verifyAction = page.locator('.action-row', { hasText: '验证新手机号恢复路径' })
  // 先记录「验证动作已完成」，再单独记录验证通过（done ≠ verified）
  await verifyAction.getByRole('button', { name: '记录完成' }).click()
  await verifyAction.getByRole('button', { name: '验证通过' }).click()
  await expect(verifyAction.getByText('已验证')).toBeVisible()

  await openNode(page, '换号测试账户')
  await page.getByRole('button', { name: '标记为不再有效' }).first().click()
  await expect(page.getByText('已终止').first()).toBeVisible()
})

test('E2E-09 Records 三个时钟独立：发生时间未记录', async ({ page }) => {
  await createVault(page)
  await openNav(page, '基础设施')
  await addNode(page, { kind: 'payment_instrument', name: '记录银行卡', last4: '4444' })
  await addNode(page, { kind: 'account', name: '记录账户' })
  await openNode(page, '记录账户')
  await addRelation(page, { relation: 'funding_source', direction: 'incoming', peer: '记录银行卡' })
  await createPaymentPlan(page, '记录银行卡')

  const action = page.locator('.action-row').first()
  await action.getByRole('button', { name: '记录完成' }).click()

  await openNav(page, '记录')
  await expect(page.getByTestId('records-view')).toBeVisible()
  await expect(page.getByTestId('occurred-at').first()).toHaveText('发生时间未记录')
  await expect(page.getByTestId('records-view')).toContainText('记录时间')
  await expect(page.getByTestId('records-view')).toContainText('验证证据未记录')
})

test('E2E-12 错误口令 fail closed，不产生半解锁状态', async ({ page }) => {
  await createVault(page)
  await lockFromRail(page)
  await page.getByLabel('本地密码').fill(`${PASSWORD}-wrong`)
  await page.getByRole('button', { name: '解锁' }).click()
  await expect(page.getByTestId('unlock-error')).toBeVisible()
  await expect(page.getByTestId('now-view')).toHaveCount(0)

  await unlock(page)
  await expect(page.getByTestId('now-view')).toBeVisible()
})
